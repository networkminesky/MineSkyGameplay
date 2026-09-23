package net.minesky.gameplay.features.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minesky.gameplay.api.MineSkyAPI;
import net.minesky.gameplay.api.dialogapi.DialogAPI;
import net.minesky.gameplay.api.dialogapi.DialogButton;
import net.minesky.gameplay.api.dialogapi.DialogInputField;
import net.minesky.gameplay.api.dialogapi.MineSkyDialog;
import net.minesky.gameplay.api.dialogapi.inputs.BoolInput;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GameplayCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!s.hasPermission("mineskygameplay.command.mineskygameplay"))
            return true;

        if(args.length == 0) {
            s.sendMessage("§e/mineskygameplay dialog §7- Tests a dialog using the DialogAPI.");
            return true;
        }

        if(args[0].equalsIgnoreCase("dialog")) {
            if(!(s instanceof Player p)) {
                s.sendMessage("§cApenas jogadores podem executar esse comando.");
                return true;
            }

            if(args.length == 1) {
                s.sendMessage("§cInforme um dialog de teste");
                return true;
            }

            DialogAPI api = MineSkyAPI.dialog();
            if (api == null) {
                s.sendMessage("§cAPI de Dialogs não está disponível.");
                return true;
            }
            final String input = args[1].toLowerCase();

            if(input.equals("test1")) {
                MineSkyDialog mineSkyDialog = api.createDialog()
                        .title(Component.text("Configurações MineSky").color(NamedTextColor.AQUA))
                        .body("Personalize sua experiência no servidor.")
                        .bodyItem(new ItemStack(Material.IRON_SWORD))
                        .addInput(new BoolInput("desativar-chat-global", Component.text("Desativar chat global"), false))
                        .onSubmit((b, d) -> {
                            boolean chatGlobal = d.getBoolean("desativar-chat-global");
                            if(chatGlobal) {
                                p.sendMessage("§cChat global desativado.");
                            } else {
                                p.sendMessage("§aChat global ativado.");
                            }
                        })
                        .build();

                mineSkyDialog.show(p);
            }

            if(input.equals("test2")) {
                MineSkyAPI.dialog().createDialog("<gold><bold>CONFIRMAÇÃO")
                        .body("<gray>Deseja realmente resetar sua ilha?")
                        .body("<red><bold>AVISO: <gray>Essa ação é irreversível!")
                        .confirmButton("§aSim, Resetar!", player -> {
                            player.sendMessage("§aSua ilha está sendo resetada...");
                        })
                        .cancelButton("§cCancelar", player -> {
                            player.sendMessage("§cOperação cancelada.");
                        })
                        .show(p);
            }

            if(input.equals("test3")) {
                MineSkyDialog.builder("<aqua>Escolha um Servidor")
                        .body("Selecione para onde você deseja ir:")
                        .button("§e§lRankUP", player -> player.performCommand("server rankup"))
                        .button("§b§lSurvival", player -> player.performCommand("server survival"))
                        .button("§a§lMinigames", player -> player.performCommand("server minigames"))
                        .button("§7Fechar", player -> player.sendMessage("§eMenu fechado."))
                        .show(p);
            }

            if(input.equals("test4")) {
                MineSkyDialog.builder("<gold>Criação de Clã")
                        .body("<gray>Insira os dados do clã que deseja fundar:")
                        // Campo de texto
                        .textInput("tag", "Tag do Clã (3-4 letras)", "Ex: SKY", "SKY")
                        .textInput("nome", "Nome do Clã", "Ex: MineSky Warriors", "")
                        // Booleano / Toggle
                        .boolInput("recrutar", "Recrutamento Aberto", true)
                        // Slider numérico
                        .sliderInput("taxa", "Taxa Diária (Coins)", 10.0f, 1000.0f, 10.0f, 50.0f)
                        // Dropdown / Opções
                        .dropdownInput("foco", "Foco do Clã", b -> b
                                .option("pvp", "§cCombate / PvP")
                                .option("farm", "§aEconomia / Farm")
                                .option("construcao", "§bConstrução / RPG")
                                .initial("farm")
                        )
                        .button(DialogButton.confirm("Fundar Clã", (player, response) -> {
                            String tag = response.getText("tag");
                            String nome = response.getText("nome");
                            boolean recrutar = response.getBoolean("recrutar");
                            float taxa = response.getFloat("taxa");
                            String foco = response.getSelectedOption("foco");

                            player.sendMessage("§aClã criado: §e[" + tag + "] " + nome);
                            player.sendMessage("§7Taxa: §f" + taxa + " | Foco: §f" + foco + " | Aberto: §f" + recrutar);

                            // Abrindo outro Diálogo de Boas-Vindas imediatamente (100% thread-safe no Folia):
                            MineSkyDialog.builder("<green>Sucesso!")
                                    .body("<white>Parabéns, seu clã <gold>" + nome + " <white>foi fundado!")
                                    .button("§aIr para a Base", b -> b.sendMessage("§7Teleportando..."))
                                    .show(player);
                        }))
                        .cancelButton("Desistir", player -> player.sendMessage("§cCriação cancelada."))
                        .show(p);
            }

            if(input.equals("test5")) {
                api.createDialog(Component.text(""));
            }
        }

        return false;
    }
}
