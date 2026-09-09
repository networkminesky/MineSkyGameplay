package net.minesky.mineskygameplay.advancements.command;

import net.minesky.mineskygameplay.advancements.AdvancementsAPI;
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
            player.sendMessage("§cVocê está no §lJava Edition§c! Pressione a tecla §lL §cpara abrir o menu nativo de conquistas. Esse comando é de uso exclusivo para jogadores §lBedrock§c.");
        }
        return true;
    }
}