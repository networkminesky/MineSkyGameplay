package net.minesky.gameplay.features.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minesky.gameplay.api.MineSkyAPI;
import net.minesky.gameplay.api.dialogapi.DialogAPI;
import net.minesky.gameplay.api.dialogapi.DialogButton;
import net.minesky.gameplay.api.dialogapi.MineSkyDialog;
import net.minesky.gameplay.api.dialogapi.inputs.BoolInput;
import net.minesky.gameplay.api.locator.LocatorAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GameplayCommand implements TabExecutor {

    private static final String PREFIX = "§b§lMineSky §8» §f";
    private static final String ERROR_PREFIX = "§c§lERRO! §c";
    private static final String SUCCESS_PREFIX = "§a§lSUCESSO! §a";

    private static final List<String> ROOT_SUBCOMMANDS = Arrays.asList("dialog", "locator", "help");
    private static final List<String> DIALOG_TYPES = Arrays.asList("test1", "test2", "test3", "test4", "test5");
    private static final List<String> LOCATOR_ACTIONS = Arrays.asList("clear", "show", "hide", "reveal", "unreveal");

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!s.hasPermission("mineskygameplay.command.mineskygameplay")) {
            s.sendMessage(ERROR_PREFIX + "Você não tem permissão para executar este comando.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(s);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("dialog")) {
            return handleDialog(s, args);
        }

        if (sub.equals("locator")) {
            return handleLocator(s, args);
        }

        s.sendMessage(ERROR_PREFIX + "Subcomando desconhecido. Use §e/" + label + " help§c.");
        return true;
    }

    private boolean handleDialog(CommandSender s, String[] args) {
        if (!(s instanceof Player p)) {
            s.sendMessage(ERROR_PREFIX + "Apenas jogadores in-game podem testar diálogos.");
            return true;
        }

        if (args.length == 1) {
            s.sendMessage(ERROR_PREFIX + "Informe o teste de diálogo. Opções: §e" + String.join("§7, §e", DIALOG_TYPES));
            return true;
        }

        DialogAPI api = MineSkyAPI.dialog();
        if (api == null) {
            s.sendMessage(ERROR_PREFIX + "A API de Dialogs não está registrada no servidor.");
            return true;
        }

        String input = args[1].toLowerCase();

        switch (input) {
            case "test1" -> {
                MineSkyDialog mineSkyDialog = api.createDialog()
                        .title(Component.text("Configurações MineSky").color(NamedTextColor.AQUA))
                        .body("Personalize sua experiência no servidor.")
                        .bodyItem(new ItemStack(Material.IRON_SWORD))
                        .addInput(new BoolInput("desativar-chat-global", Component.text("Desativar chat global"), false))
                        .onSubmit((b, d) -> {
                            boolean chatGlobal = d.getBoolean("desativar-chat-global");
                            if (chatGlobal) {
                                p.sendMessage(PREFIX + "§cChat global desativado.");
                            } else {
                                p.sendMessage(PREFIX + "§aChat global ativado.");
                            }
                        })
                        .build();
                mineSkyDialog.show(p);
                p.sendMessage(SUCCESS_PREFIX + "Exibindo diálogo: §eConfigurações (test1)");
            }
            case "test2" -> {
                MineSkyAPI.dialog().createDialog("<gold><bold>CONFIRMAÇÃO")
                        .body("<gray>Deseja realmente resetar sua ilha?")
                        .body("<red><bold>AVISO: <gray>Essa ação é irreversível!")
                        .confirmButton("§aSim, Resetar!", player -> player.sendMessage(SUCCESS_PREFIX + "Sua ilha está sendo resetada..."))
                        .cancelButton("§cCancelar", player -> player.sendMessage(ERROR_PREFIX + "Operação cancelada pelo usuário."))
                        .show(p);
                p.sendMessage(SUCCESS_PREFIX + "Exibindo diálogo: §eConfirmação (test2)");
            }
            case "test3" -> {
                MineSkyDialog.builder("<aqua>Escolha um Servidor")
                        .body("Selecione para onde você deseja ir:")
                        .button("§e§lRankUP", player -> player.performCommand("server rankup"))
                        .button("§b§lSurvival", player -> player.performCommand("server survival"))
                        .button("§a§lMinigames", player -> player.performCommand("server minigames"))
                        .button("§7Fechar", player -> player.sendMessage(PREFIX + "Menu de servidores fechado."))
                        .show(p);
                p.sendMessage(SUCCESS_PREFIX + "Exibindo diálogo: §eSeleção de Servidor (test3)");
            }
            case "test4" -> {
                MineSkyDialog.builder("<gold>Criação de Clã")
                        .body("<gray>Insira os dados do clã que deseja fundar:")
                        .textInput("tag", "Tag do Clã (3-4 letras)", "Ex: SKY", "SKY")
                        .textInput("nome", "Nome do Clã", "Ex: MineSky Warriors", "")
                        .boolInput("recrutar", "Recrutamento Aberto", true)
                        .sliderInput("taxa", "Taxa Diária (Coins)", 10.0f, 1000.0f, 10.0f, 50.0f)
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

                            player.sendMessage(SUCCESS_PREFIX + "Clã criado com sucesso: §e[" + tag + "] " + nome);
                            player.sendMessage("§7Taxa: §f" + taxa + " §8| §7Foco: §f" + foco + " §8| §7Aberto: §f" + (recrutar ? "§aSim" : "§cNão"));

                            MineSkyDialog.builder("<green>Sucesso!")
                                    .body("<white>Parabéns, seu clã <gold>" + nome + " <white>foi fundado!")
                                    .button("§aIr para a Base", b -> b.sendMessage(PREFIX + "Teleportando para a sede do clã..."))
                                    .show(player);
                        }))
                        .cancelButton("Desistir", player -> player.sendMessage(ERROR_PREFIX + "Criação de clã cancelada."))
                        .show(p);
                p.sendMessage(SUCCESS_PREFIX + "Exibindo diálogo: §eFormulário de Clã (test4)");
            }
            case "test5" -> {
                MineSkyDialog.builder("<light_purple>Diálogo Informativo")
                        .body("<gray>Este é um exemplo de anúncio com visual limpo.")
                        .body("<yellow>Visite nosso discord para novidades!")
                        .button("§aEntendido", player -> player.sendMessage(SUCCESS_PREFIX + "Obrigado por ler!"))
                        .show(p);
                p.sendMessage(SUCCESS_PREFIX + "Exibindo diálogo: §eInformativo (test5)");
            }
            default -> s.sendMessage(ERROR_PREFIX + "Teste inválido. Escolha: §e" + String.join("§7, §e", DIALOG_TYPES));
        }

        return true;
    }

    private boolean handleLocator(CommandSender s, String[] args) {
        if (args.length < 2) {
            s.sendMessage(ERROR_PREFIX + "Uso correto: §e/mineskygameplay locator <clear|show|hide|reveal|unreveal> <jogador>");
            return true;
        }

        if (args.length < 3) {
            s.sendMessage(ERROR_PREFIX + "Informe o nick de um jogador online.");
            return true;
        }

        LocatorAPI api = MineSkyAPI.locator();
        if (api == null) {
            s.sendMessage(ERROR_PREFIX + "A API do Locator não está disponível.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null || !target.isOnline()) {
            s.sendMessage(ERROR_PREFIX + "O jogador §e" + args[2] + " §cnão foi encontrado ou está offline.");
            return true;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "clear" -> {
                api.clearPlayerOverrides(target);
                api.unrevealPlayerGlobally(target);
                s.sendMessage(SUCCESS_PREFIX + "Configurações e overrides de locator limpos para §f" + target.getName() + "§a.");
            }
            case "show" -> {
                api.showPlayerGlobally(target);
                s.sendMessage(SUCCESS_PREFIX + "O jogador §f" + target.getName() + " §aagora está visível globalmente.");
            }
            case "hide" -> {
                api.hidePlayerGlobally(target);
                s.sendMessage(SUCCESS_PREFIX + "O jogador §f" + target.getName() + " §afoi escondido globalmente no locator.");
            }
            case "reveal" -> {
                api.revealPlayerGlobally(target);
                s.sendMessage(SUCCESS_PREFIX + "O jogador §f" + target.getName() + " §afoi revelado com destaque globalmente.");
            }
            case "unreveal" -> {
                api.unrevealPlayerGlobally(target);
                s.sendMessage(SUCCESS_PREFIX + "O destaque global de §f" + target.getName() + " §afoi desativado.");
            }
            default -> s.sendMessage(ERROR_PREFIX + "Ação desconhecida. Use: §e" + String.join("§7, §e", LOCATOR_ACTIONS));
        }

        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§8§m----------------§r " + PREFIX + "§8§m----------------");
        s.sendMessage("§e/mineskygameplay dialog <teste> §8- §7Testa diálogos do DialogAPI");
        s.sendMessage("§e/mineskygameplay locator <ação> <player> §8- §7Controla o rastreamento do Locator");
        s.sendMessage("§e/mineskygameplay help §8- §7Exibe este menu de ajuda");
        s.sendMessage("§8§m--------------------------------------------------");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!s.hasPermission("mineskygameplay.command.mineskygameplay")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], ROOT_SUBCOMMANDS, new ArrayList<>());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("dialog")) {
                return StringUtil.copyPartialMatches(args[1], DIALOG_TYPES, new ArrayList<>());
            }
            if (args[0].equalsIgnoreCase("locator")) {
                return StringUtil.copyPartialMatches(args[1], LOCATOR_ACTIONS, new ArrayList<>());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("locator")) {
            List<String> playerNames = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerNames.add(p.getName());
            }
            return StringUtil.copyPartialMatches(args[2], playerNames, new ArrayList<>());
        }

        return Collections.emptyList();
    }
}