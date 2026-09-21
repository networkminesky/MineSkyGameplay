package net.minesky.gameplay.features.homes;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minesky.gameplay.MineSkyGameplayPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

public class HomesListener implements Listener {

    private final MineSkyGameplayPlugin plugin;
    private final HomesMenuManager menuManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public HomesListener(MineSkyGameplayPlugin plugin, HomesMenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        menuManager.cancelPendingTeleport(event.getPlayer().getUniqueId());
        menuManager.getSearchPrompts().remove(event.getPlayer().getUniqueId());
        menuManager.getCreateHomePrompts().remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof HomesMenuManager.HomesMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) {
            return;
        }

        if (slot == 45) {
            if (holder.getPage() > 0) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                menuManager.openMenu(player, holder.getTargetUUID(), holder.getTargetName(), holder.getPage() - 1, holder.getFilter());
            }
            return;
        }

        if (slot == 53) {
            int maxPerPage = 45;
            int totalPages = (int) Math.ceil((double) holder.getEntries().size() / maxPerPage);
            if (holder.getPage() < totalPages - 1) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                menuManager.openMenu(player, holder.getTargetUUID(), holder.getTargetName(), holder.getPage() + 1, holder.getFilter());
            }
            return;
        }

        if (slot == 47) {
            if (holder.getFilter() != null && !holder.getFilter().isBlank()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                menuManager.openMenu(player, holder.getTargetUUID(), holder.getTargetName(), 0, null);
                return;
            }

            player.closeInventory();
            menuManager.getSearchPrompts().put(player.getUniqueId(), "");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            player.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><b>[Busca]</b></gradient> <gray>Digite no chat o termo para buscar (ou <red>cancelar</red>):</gray>"));
            return;
        }

        if (slot == 51 && holder.getTargetUUID().equals(player.getUniqueId())) {
            player.closeInventory();
            menuManager.getCreateHomePrompts().put(player.getUniqueId(), true);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            player.sendMessage(mm.deserialize("<gradient:#11998e:#38ef7d><b>[Nova Home]</b></gradient> <gray>Digite no chat o nome da nova home (ou <red>cancelar</red>):</gray>"));
            return;
        }

        if (slot >= 0 && slot < 45) {
            int index = (holder.getPage() * 45) + slot;
            if (index >= holder.getEntries().size()) {
                return;
            }

            SavedLocationEntry entry = holder.getEntries().get(index);
            player.closeInventory();

            World world = Bukkit.getWorld(entry.getWorld());
            if (world == null) {
                player.sendMessage(mm.deserialize("<red>O mundo deste local não está carregado!</red>"));
                return;
            }

            if (entry.getType() == SavedLocationEntry.Type.CLAIM) {
                SafeLocationUtil.findSafeLocation(plugin, world, entry.getX(), entry.getZ()).thenAccept(targetLoc -> {
                    player.getScheduler().run(plugin, task -> {
                        menuManager.startTeleport(player, targetLoc, entry.getName());
                    }, null);
                });
            } else {
                Location targetLoc = new Location(world, entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5);
                menuManager.startTeleport(player, targetLoc, entry.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (menuManager.getSearchPrompts().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            menuManager.getSearchPrompts().remove(player.getUniqueId());

            String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            player.getScheduler().run(plugin, task -> {
                if (text.equalsIgnoreCase("cancelar")) {
                    player.sendMessage(mm.deserialize("<gray>Pesquisa cancelada.</gray>"));
                    menuManager.openMenu(player, player.getUniqueId(), player.getName(), 0, null);
                } else {
                    menuManager.openMenu(player, player.getUniqueId(), player.getName(), 0, text);
                }
            }, null);
            return;
        }

        if (menuManager.getCreateHomePrompts().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            menuManager.getCreateHomePrompts().remove(player.getUniqueId());

            String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            player.getScheduler().run(plugin, task -> {
                if (text.equalsIgnoreCase("cancelar")) {
                    player.sendMessage(mm.deserialize("<gray>Criação cancelada.</gray>"));
                    menuManager.openMenu(player, player.getUniqueId(), player.getName(), 0, null);
                } else {
                    String cleanName = text.replaceAll("[^a-zA-Z0-9_-]", "");
                    if (cleanName.isBlank()) {
                        player.sendMessage(mm.deserialize("<red>Nome inválido para home!</red>"));
                        menuManager.openMenu(player, player.getUniqueId(), player.getName(), 0, null);
                        return;
                    }
                    player.performCommand("sethome " + cleanName);
                }
            }, null);
        }
    }
}