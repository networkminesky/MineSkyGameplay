package net.minesky.gameplay.features.homes;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minesky.gameplay.MineSkyGameplayPlugin;
import net.mineskyguildas.data.Guilds;
import net.mineskyguildas.enums.GuildRoles;
import net.mineskyguildas.handlers.GuildHandler;
import net.mineskyguildas.utils.Utils;
import net.william278.huskclaims.api.BukkitHuskClaimsAPI;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ServerWorldClaim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HomesMenuManager {

    private final MineSkyGameplayPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Map<UUID, String> searchPrompts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> createHomePrompts = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> pendingTeleports = new ConcurrentHashMap<>();

    public HomesMenuManager(MineSkyGameplayPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player viewer, UUID targetUUID, String targetName, int page, String filter) {
        viewer.getScheduler().run(plugin, task -> {
            viewer.playSound(viewer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        }, null);

        fetchPlayerEntries(targetUUID, targetName).thenAccept(entries -> {
            List<SavedLocationEntry> filtered = new ArrayList<>();
            for (SavedLocationEntry entry : entries) {
                if (filter == null || filter.isBlank() || entry.getName().toLowerCase().contains(filter.toLowerCase())) {
                    filtered.add(entry);
                }
            }

            viewer.getScheduler().run(plugin, scheduledTask -> {
                if (!viewer.isOnline()) {
                    return;
                }
                Inventory inventory = buildInventory(viewer, targetUUID, targetName, page, filter, filtered, entries);
                viewer.openInventory(inventory);
            }, null);
        });
    }

    public CompletableFuture<List<SavedLocationEntry>> fetchPlayerEntries(UUID targetUUID, String targetName) {
        CompletableFuture<List<SavedLocationEntry>> future = new CompletableFuture<>();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            List<SavedLocationEntry> entries = new ArrayList<>();
            loadEssentialsHomes(targetUUID, entries);
            loadClanHome(targetUUID, entries);
            loadHuskClaims(targetUUID, targetName, entries, () -> future.complete(entries));
        });
        return future;
    }

    private void loadClanHome(UUID uuid, List<SavedLocationEntry> entries) {
        if (!Bukkit.getPluginManager().isPluginEnabled("MineSkyGuildas2")) {
            return;
        }

        try {
            Guilds guild = GuildHandler.getGuildByPlayer(uuid);
            if (guild == null) {
                return;
            }

            GuildRoles role = guild.getRole(uuid);
            if (role == null || role == GuildRoles.RECRUIT) {
                return;
            }

            Location base = guild.getBase();
            if (base != null && base.getWorld() != null) {
                entries.add(new SavedLocationEntry(
                        SavedLocationEntry.Type.CLAN,
                        "Base do Clã [" + Utils.getTag(guild.getTag()) + "]",
                        base.getWorld().getName(),
                        base.getBlockX(),
                        base.getBlockY(),
                        base.getBlockZ(),
                        0
                ));
            }
        } catch (Exception ignored) {
        }
    }

    public Component deserialize(String string) {
        return mm.deserialize(string).decoration(TextDecoration.ITALIC, false);
    }

    public void sendConsoleList(CommandSender sender, UUID targetUUID, String targetName) {
        fetchPlayerEntries(targetUUID, targetName).thenAccept(entries -> {
            sender.sendMessage(deserialize("<gradient:#667eea:#764ba2>====================================</gradient>"));
            sender.sendMessage(deserialize("<gold><b>Locais de:</b></gold> <white>" + targetName + "</white>"));
            sender.sendMessage(deserialize("<gradient:#667eea:#764ba2>====================================</gradient>"));

            int homeIdx = 1;
            int claimIdx = 1;
            for (SavedLocationEntry entry : entries) {
                if (entry.getType() == SavedLocationEntry.Type.HOME) {
                    sender.sendMessage(deserialize("<aqua>[" + homeIdx++ + "] Home: " + entry.getName() + "</aqua> <gray>(" + entry.getWorld() + " | X: " + entry.getX() + ", Y: " + entry.getY() + ", Z: " + entry.getZ() + ")</gray>"));
                } else if (entry.getType() == SavedLocationEntry.Type.CLAN) {
                    sender.sendMessage(deserialize("<red>[Clã] " + entry.getName() + "</red> <gray>(" + entry.getWorld() + " | X: " + entry.getX() + ", Y: " + entry.getY() + ", Z: " + entry.getZ() + ")</gray>"));
                } else {
                    sender.sendMessage(deserialize("<yellow>[" + claimIdx++ + "] " + entry.getName() + "</yellow> <gray>(" + entry.getWorld() + " | X: " + entry.getX() + ", Z: " + entry.getZ() + " | Área: " + entry.getArea() + "m²)</gray>"));
                }
            }
            if (entries.isEmpty()) {
                sender.sendMessage(deserialize("<red>Nenhum local ou terreno encontrado para este jogador.</red>"));
            }
            sender.sendMessage(deserialize("<gradient:#667eea:#764ba2>====================================</gradient>"));
        });
    }

    public void startTeleport(Player player, Location targetLoc, String destinationName) {
        if (player.hasPermission("mineskygameplay.bypass.cooldown")) {
            executeTeleport(player, targetLoc, destinationName);
            return;
        }

        cancelPendingTeleport(player.getUniqueId());

        Location startLoc = player.getLocation().clone();
        AtomicInteger remainingSeconds = new AtomicInteger(5);

        player.sendMessage(deserialize("<gradient:#ff9900:#ff5500><b>[Teleporte]</b></gradient> <gray>Teleportando para <white>" + destinationName + "</white> em <gold>5 segundos</gold>. Não se mova!</gray>"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

        ScheduledTask scheduledTask = player.getScheduler().runAtFixedRate(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                pendingTeleports.remove(player.getUniqueId());
                return;
            }

            if (player.getLocation().distanceSquared(startLoc) > 0.25) {
                task.cancel();
                pendingTeleports.remove(player.getUniqueId());
                player.sendMessage(deserialize("<red><b>[Teleporte]</b> Teleporte cancelado! Você se moveu.</red>"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            int current = remainingSeconds.decrementAndGet();
            if (current > 0) {
                player.sendMessage(deserialize("<gradient:#ff9900:#ff5500><b>[Teleporte]</b></gradient> <gray>Teleportando em <gold>" + current + "s</gold>...</gray>"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.2f);
            } else {
                task.cancel();
                pendingTeleports.remove(player.getUniqueId());
                executeTeleport(player, targetLoc, destinationName);
            }
        }, null, 20L, 20L);

        pendingTeleports.put(player.getUniqueId(), scheduledTask);
    }

    public void cancelPendingTeleport(UUID uuid) {
        ScheduledTask existing = pendingTeleports.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }
    }

    private void executeTeleport(Player player, Location targetLoc, String destinationName) {
        player.teleportAsync(targetLoc).thenAccept(success -> {
            if (success) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.sendMessage(deserialize("<gradient:#43e97b:#38f9d7>Teleportado com sucesso para <white>" + destinationName + "</white>!</gradient>"));
            } else {
                player.sendMessage(deserialize("<red>Falha ao teleportar para este local!</red>"));
            }
        });
    }

    private void loadEssentialsHomes(UUID uuid, List<SavedLocationEntry> entries) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
            return;
        }

        Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials == null) {
            return;
        }

        User user = essentials.getUser(uuid);
        if (user == null) {
            return;
        }

        for (String homeName : user.getHomes()) {
            try {
                Location loc = user.getHome(homeName);
                if (loc != null && loc.getWorld() != null) {
                    entries.add(new SavedLocationEntry(
                            SavedLocationEntry.Type.HOME,
                            homeName,
                            loc.getWorld().getName(),
                            loc.getBlockX(),
                            loc.getBlockY(),
                            loc.getBlockZ(),
                            0
                    ));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void loadHuskClaims(UUID uuid, String targetName, List<SavedLocationEntry> entries, Runnable callback) {
        if (!Bukkit.getPluginManager().isPluginEnabled("HuskClaims")) {
            callback.run();
            return;
        }

        try {
            BukkitHuskClaimsAPI api = BukkitHuskClaimsAPI.getInstance();
            String safeName = (targetName != null && !targetName.isBlank()) ? targetName : "Player";
            net.william278.huskclaims.user.User user = net.william278.huskclaims.user.User.of(uuid, safeName);

            Map<net.william278.huskclaims.claim.ClaimWorld, List<Claim>> localClaims = api.getUserClaims(user);
            if (localClaims != null && !localClaims.isEmpty()) {
                int count = 1;
                for (Map.Entry<net.william278.huskclaims.claim.ClaimWorld, List<Claim>> entry : localClaims.entrySet()) {
                    String worldName = entry.getKey().getName(BukkitHuskClaimsAPI.getInstance().getPlugin());
                    for (Claim claim : entry.getValue()) {
                        var center = claim.getRegion().getCenter();
                        int cx = (int) center.getBlockX();
                        int cz = (int) center.getBlockZ();
                        int area = (int) claim.getRegion().getSurfaceArea();

                        entries.add(new SavedLocationEntry(
                                SavedLocationEntry.Type.CLAIM,
                                "terreno" + count++,
                                worldName,
                                cx,
                                64,
                                cz,
                                area
                        ));
                    }
                }
                callback.run();
                return;
            }

            api.getGlobalUserClaims(user).thenAccept(claims -> {
                if (claims != null) {
                    int count = 1;
                    for (ServerWorldClaim swc : claims) {
                        Claim claim = swc.claim();
                        String worldName = swc.serverWorld().world().getName();
                        var center = claim.getRegion().getCenter();
                        int cx = (int) center.getBlockX();
                        int cz = (int) center.getBlockZ();
                        int area = (int) claim.getRegion().getSurfaceArea();

                        entries.add(new SavedLocationEntry(
                                SavedLocationEntry.Type.CLAIM,
                                "terreno" + count++,
                                worldName,
                                cx,
                                64,
                                cz,
                                area
                        ));
                    }
                }
                callback.run();
            }).exceptionally(throwable -> {
                callback.run();
                return null;
            });
        } catch (Exception e) {
            callback.run();
        }
    }

    private Inventory buildInventory(Player player, UUID targetUUID, String targetName, int page, String filter, List<SavedLocationEntry> filtered, List<SavedLocationEntry> allEntries) {
        HomesMenuHolder holder = new HomesMenuHolder(page, filter, filtered, targetUUID, targetName);
        boolean isOwn = player.getUniqueId().equals(targetUUID);

        Component title = isOwn
                ? deserialize("<gradient:#667eea:#764ba2><b>Locais Salvos</b></gradient> <dark_gray>»</dark_gray> <gray>Pág. " + (page + 1))
                : deserialize("<gradient:#667eea:#764ba2><b>Locais de " + targetName + "</b></gradient> <dark_gray>»</dark_gray> <gray>Pág. " + (page + 1));

        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        int maxPerPage = 45;
        int totalPages = (int) Math.ceil((double) filtered.size() / maxPerPage);
        if (totalPages == 0) {
            totalPages = 1;
        }

        int startIndex = page * maxPerPage;
        int endIndex = Math.min(startIndex + maxPerPage, filtered.size());

        for (int i = startIndex; i < endIndex; i++) {
            SavedLocationEntry entry = filtered.get(i);
            inv.setItem(i - startIndex, createEntryItem(entry));
        }

        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta sepMeta = separator.getItemMeta();
        sepMeta.displayName(Component.empty());
        separator.setItemMeta(sepMeta);

        inv.setItem(46, separator);
        inv.setItem(48, separator);
        inv.setItem(50, separator);
        inv.setItem(52, separator);

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.displayName(deserialize("<gradient:#ff416c:#ff4b2b><b>◀ Página Anterior</b></gradient>"));
            meta.lore(List.of(deserialize("<gray>Ir para a página <white>" + page + "</white>.</gray>")));
            prev.setItemMeta(meta);
            inv.setItem(45, prev);
        } else {
            inv.setItem(45, separator);
        }

        ItemStack searchItem = new ItemStack(Material.SPYGLASS);
        ItemMeta searchMeta = searchItem.getItemMeta();
        searchMeta.displayName(deserialize("<gradient:#00c6ff:#0072ff><b>Buscar & Filtrar</b></gradient>"));
        List<Component> searchLore = new ArrayList<>();
        if (filter != null && !filter.isBlank()) {
            searchLore.add(deserialize("<gray>Filtro ativo: <aqua>\"" + filter + "\"</aqua></gray>"));
            searchLore.add(Component.empty());
            searchLore.add(deserialize("<red>▶ Clique: <white>Remover filtro</white></red>"));
        } else {
            searchLore.add(deserialize("<gray>Nenhum filtro aplicado.</gray>"));
            searchLore.add(Component.empty());
            searchLore.add(deserialize("<yellow>▶ Clique para filtrar por nome</yellow>"));
        }
        searchMeta.lore(searchLore);
        searchItem.setItemMeta(searchMeta);
        inv.setItem(47, searchItem);

        ItemStack infoItem = new ItemStack(Material.COMPASS);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.displayName(deserialize("<gradient:#f7971e:#ffd200><b>Informações</b></gradient>"));
        int homesCount = 0;
        int claimsCount = 0;
        for (SavedLocationEntry entry : allEntries) {
            if (entry.getType() == SavedLocationEntry.Type.HOME) {
                homesCount++;
            } else if (entry.getType() == SavedLocationEntry.Type.CLAIM) {
                claimsCount++;
            }
        }
        infoMeta.lore(List.of(
                deserialize("<gray>Homes: <gradient:#43e97b:#38f9d7><b>" + homesCount + "</b></gradient></gray>"),
                deserialize("<gray>Terrenos: <gradient:#f6d365:#fda085><b>" + claimsCount + "</b></gradient></gray>"),
                Component.empty(),
                deserialize("<gray>Páginas: <white>" + (page + 1) + " / " + totalPages + "</white></gray>")
        ));
        infoItem.setItemMeta(infoMeta);
        inv.setItem(49, infoItem);

        if (isOwn) {
            ItemStack createItem = new ItemStack(Material.BEACON);
            ItemMeta createMeta = createItem.getItemMeta();
            createMeta.displayName(deserialize("<gradient:#11998e:#38ef7d><b>+ Nova Home</b></gradient>"));
            createMeta.lore(List.of(
                    deserialize("<gray>Salve sua posição atual rapidamente.</gray>"),
                    Component.empty(),
                    deserialize("<white>ℹ Também pode usar <yellow>/sethome <nome></yellow></white>"),
                    deserialize("<green>▶ Clique para definir pelo chat</green>")
            ));
            createItem.setItemMeta(createMeta);
            inv.setItem(51, createItem);
        } else {
            inv.setItem(51, separator);
        }

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.displayName(deserialize("<gradient:#ff416c:#ff4b2b><b>Próxima Página ▶</b></gradient>"));
            meta.lore(List.of(deserialize("<gray>Ir para a página <white>" + (page + 2) + "</white>.</gray>")));
            next.setItemMeta(meta);
            inv.setItem(53, next);
        } else {
            inv.setItem(53, separator);
        }

        return inv;
    }

    private ItemStack createEntryItem(SavedLocationEntry entry) {
        if (entry.getType() == SavedLocationEntry.Type.HOME) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(22);
            meta.displayName(deserialize("<gradient:#4facfe:#00f2fe><b>✦ " + entry.getName() + "</b></gradient>"));
            meta.lore(List.of(
                    deserialize("<dark_gray>Ponto de Retorno (Home)</dark_gray>"),
                    Component.empty(),
                    deserialize("<gray>Mundo: <white>" + getWorldName(entry.getWorld()) + "</white></gray>"),
                    deserialize("<gray>Coordenadas: <gradient:#4facfe:#00f2fe>X: " + entry.getX() + " | Y: " + entry.getY() + " | Z: " + entry.getZ() + "</gradient></gray>"),
                    Component.empty(),
                    deserialize("<yellow>▶ Clique para iniciar teleporte</yellow>")
            ));
            item.setItemMeta(meta);
            return item;
        } else if (entry.getType() == SavedLocationEntry.Type.CLAN) {
            ItemStack item = new ItemStack(Material.SHIELD);
            ItemMeta meta = item.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.displayName(deserialize("<gradient:#ff416c:#8a2387><b>✦ " + entry.getName() + "</b></gradient>"));
            meta.lore(List.of(
                    deserialize("<dark_gray>Base de Operações do Clã</dark_gray>"),
                    Component.empty(),
                    deserialize("<gray>Mundo: <white>" + getWorldName(entry.getWorld()) + "</white></gray>"),
                    deserialize("<gray>Coordenadas: <gradient:#ff416c:#8a2387>X: " + entry.getX() + " | Y: " + entry.getY() + " | Z: " + entry.getZ() + "</gradient></gray>"),
                    Component.empty(),
                    deserialize("<white>Atalhos: <yellow>/home cla</yellow>, <yellow>/home base</yellow></white>"),
                    deserialize("<yellow>▶ Clique para iniciar teleporte</yellow>")
            ));
            item.setItemMeta(meta);
            return item;
        } else {
            ItemStack item = new ItemStack(Material.GOLDEN_SHOVEL);
            ItemMeta meta = item.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.displayName(deserialize("<gradient:#f6d365:#fda085><b>✦ " + entry.getName() + "</b></gradient>"));
            meta.lore(List.of(
                    deserialize("<dark_gray>Área Protegida (Terreno)</dark_gray>"),
                    Component.empty(),
                    deserialize("<gray>Mundo: <white>" + getWorldName(entry.getWorld()) + "</white></gray>"),
                    deserialize("<gray>Centro: <gradient:#f6d365:#fda085>X: " + entry.getX() + " | Z: " + entry.getZ() + "</gradient></gray>"),
                    deserialize("<gray>Área de Bloco: <white>" + entry.getArea() + " blocos²</white></gray>"),
                    Component.empty(),
                    deserialize("<yellow>▶ Clique para ir ao ponto seguro!</yellow>")
            ));
            item.setItemMeta(meta);
            return item;
        }
    }

    public static String getWorldName(String worldId) {
        return switch (worldId) {
            case "minesky_overworld" -> "Overworld";
            case "minesky_nether" -> "<red>Nether</red>";
            case "minesky_the_end" -> "<light_purple>The End</light_purple>";
            default -> worldId;
        };
    }

    public static class HomesMenuHolder implements InventoryHolder {

        private Inventory inventory;
        private final int page;
        private final String filter;
        private final List<SavedLocationEntry> entries;
        private final UUID targetUUID;
        private final String targetName;

        public HomesMenuHolder(int page, String filter, List<SavedLocationEntry> entries, UUID targetUUID, String targetName) {
            this.page = page;
            this.filter = filter;
            this.entries = entries;
            this.targetUUID = targetUUID;
            this.targetName = targetName;
        }

        public int getPage() {
            return page;
        }

        public String getFilter() {
            return filter;
        }

        public List<SavedLocationEntry> getEntries() {
            return entries;
        }

        public UUID getTargetUUID() {
            return targetUUID;
        }

        public String getTargetName() {
            return targetName;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    public Map<UUID, String> getSearchPrompts() {
        return searchPrompts;
    }

    public Map<UUID, Boolean> getCreateHomePrompts() {
        return createHomePrompts;
    }

    public MineSkyGameplayPlugin getPlugin() {
        return plugin;
    }
}