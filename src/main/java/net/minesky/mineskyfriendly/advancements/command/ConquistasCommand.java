package net.minesky.mineskyfriendly.advancements.command;

import net.minesky.mineskyfriendly.advancements.AdvancementsAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConquistasCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores podem executar este comando.");
            return true;
        }

        AdvancementsAPI api = AdvancementsAPI.get();

        if (api.isBedrockPlayer(player)) {
            api.openBedrockMenu(player);
        } else {
            player.sendMessage("§cVocê está no §bJava Edition§f! Pressione a tecla §e[ L ] §fpara abrir o menu nativo de conquistas. Esse comando é de uso exclusivo para jogadores Bedrock.");
        }
        return true;
    }
}