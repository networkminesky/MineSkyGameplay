package net.minesky.gameplay.features.homes;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HomesCommand implements CommandExecutor, TabCompleter {

    private final HomesMenuManager menuManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public HomesCommand(HomesMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length == 0) {
                sender.sendMessage(mm.deserialize("<red>Uso correto pelo console: /" + label + " <jogador></red>"));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            menuManager.sendConsoleList(sender, target.getUniqueId(), target.getName() == null ? args[0] : target.getName());
            return true;
        }

        if (args.length == 0) {
            menuManager.openMenu(player, player.getUniqueId(), player.getName(), 0, null);
            return true;
        }

        if (!player.hasPermission("mineskygameplay.command.homes.other")) {
            player.sendMessage(mm.deserialize("<red>Você não possui permissão para ver as homes de outras pessoas.</red>"));
            return true;
        }

        String targetInput = args[0];
        if (targetInput.contains(":")) {
            String[] parts = targetInput.split(":", 2);
            targetInput = parts[0];
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetInput);
        menuManager.openMenu(player, target.getUniqueId(), target.getName() == null ? targetInput : target.getName(), 0, null);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("mineskygameplay.command.homes.other")) {
            return List.of();
        }

        if (args.length == 1) {
            String input = args[0];
            List<String> suggestions = new ArrayList<>();

            if (input.contains(":")) {
                String[] parts = input.split(":", 2);
                String nick = parts[0];
                String searchHome = parts.length > 1 ? parts[1].toLowerCase() : "";

                OfflinePlayer target = Bukkit.getOfflinePlayer(nick);
                try {
                    List<SavedLocationEntry> entries = menuManager.fetchPlayerEntries(target.getUniqueId(), target.getName()).join();
                    for (SavedLocationEntry entry : entries) {
                        if (entry.getName().toLowerCase().startsWith(searchHome)) {
                            suggestions.add(nick + ":" + entry.getName());
                        }
                    }
                } catch (Exception ignored) {
                }
                return suggestions;
            }

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(input.toLowerCase())) {
                    suggestions.add(online.getName());
                    suggestions.add(online.getName() + ":");
                }
            }
            return suggestions;
        }

        return List.of();
    }
}