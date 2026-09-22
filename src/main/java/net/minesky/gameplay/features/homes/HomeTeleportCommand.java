package net.minesky.gameplay.features.homes;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HomeTeleportCommand implements CommandExecutor, TabCompleter {

    private final HomesMenuManager menuManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public HomeTeleportCommand(HomesMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    private boolean isClanAlias(String name) {
        String lower = name.toLowerCase();
        return lower.equals("cla") || lower.equals("base");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando exclusivo para jogadores!");
            return true;
        }

        if (args.length == 0) {
            menuManager.openMenu(player, player.getUniqueId(), player.getName(), 0, null);
            return true;
        }

        String targetLocationName = args[0];
        String targetPlayerName = player.getName();
        OfflinePlayer targetOwner = player;

        if (targetLocationName.contains(":")) {
            if (!player.hasPermission("mineskygameplay.command.homes.other")) {
                player.sendMessage(mm.deserialize("<red>Você não tem permissão para usar homes de outros jogadores.</red>"));
                return true;
            }
            String[] split = targetLocationName.split(":", 2);
            targetPlayerName = split[0];
            targetLocationName = split.length > 1 ? split[1] : "";
            targetOwner = Bukkit.getOfflinePlayer(targetPlayerName);
        }

        final String resolvedName = targetLocationName;
        final String resolvedPlayerName = targetPlayerName;

        menuManager.fetchPlayerEntries(targetOwner.getUniqueId(), targetPlayerName).thenAccept(entries -> {
            SavedLocationEntry match = null;

            for (SavedLocationEntry entry : entries) {
                if (entry.getType() == SavedLocationEntry.Type.CLAN && isClanAlias(resolvedName)) {
                    match = entry;
                    break;
                }
                if (entry.getName().equalsIgnoreCase(resolvedName)) {
                    match = entry;
                    break;
                }
            }

            if (match == null) {
                player.sendMessage(mm.deserialize("<red>Local ou base '" + resolvedName + "' não foi encontrado!</red>"));
                return;
            }

            SavedLocationEntry finalEntry = match;
            player.getScheduler().run(menuManager.getPlugin(), task -> {
                World world = Bukkit.getWorld(finalEntry.getWorld());
                if (world == null) {
                    player.sendMessage(mm.deserialize("<red>O mundo deste local não está carregado no momento!</red>"));
                    return;
                }

                String destinationName = resolvedPlayerName.equals(player.getName()) ? finalEntry.getName() : resolvedPlayerName + ":" + finalEntry.getName();

                if (finalEntry.getType() == SavedLocationEntry.Type.CLAIM) {
                    SafeLocationUtil.findSafeLocation(menuManager.getPlugin(), world, finalEntry.getX(), finalEntry.getZ()).thenAccept(targetLoc -> {
                        player.getScheduler().run(menuManager.getPlugin(), schedTask -> {
                            menuManager.startTeleport(player, targetLoc, destinationName);
                        }, null);
                    });
                } else {
                    Location destination = new Location(world, finalEntry.getX() + 0.5, finalEntry.getY(), finalEntry.getZ() + 0.5);
                    menuManager.startTeleport(player, destination, destinationName);
                }
            }, null);
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            String input = args[0];
            List<String> list = new ArrayList<>();

            if (input.contains(":") && player.hasPermission("mineskygameplay.command.homes.other")) {
                String[] parts = input.split(":", 2);
                String targetNick = parts[0];
                String filter = parts.length > 1 ? parts[1].toLowerCase() : "";

                OfflinePlayer target = Bukkit.getOfflinePlayer(targetNick);
                try {
                    List<SavedLocationEntry> entries = menuManager.fetchPlayerEntries(target.getUniqueId(), target.getName()).join();
                    for (SavedLocationEntry entry : entries) {
                        if (entry.getName().toLowerCase().startsWith(filter)) {
                            list.add(targetNick + ":" + entry.getName());
                        }
                    }
                } catch (Exception ignored) {
                }
                return list;
            }

            try {
                List<SavedLocationEntry> entries = menuManager.fetchPlayerEntries(player.getUniqueId(), player.getName()).join();
                boolean hasClan = false;
                for (SavedLocationEntry entry : entries) {
                    if (entry.getType() == SavedLocationEntry.Type.CLAN) {
                        hasClan = true;
                    }
                    if (entry.getName().toLowerCase().startsWith(input.toLowerCase())) {
                        list.add(entry.getName());
                    }
                }

                if (hasClan) {
                    for (String aliasClan : List.of("cla", "base")) {
                        if (aliasClan.startsWith(input.toLowerCase()) && !list.contains(aliasClan)) {
                            list.add(aliasClan);
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            if (player.hasPermission("mineskygameplay.command.homes.other")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.getName().equalsIgnoreCase(player.getName()) && online.getName().toLowerCase().startsWith(input.toLowerCase())) {
                        list.add(online.getName() + ":");
                    }
                }
            }

            return list;
        }

        return List.of();
    }
}