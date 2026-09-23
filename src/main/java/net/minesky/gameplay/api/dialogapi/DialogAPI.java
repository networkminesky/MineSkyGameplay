package net.minesky.gameplay.api.dialogapi;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public interface DialogAPI {

    static DialogAPI get() {
        RegisteredServiceProvider<DialogAPI> provider = Bukkit.getServicesManager().getRegistration(DialogAPI.class);
        return provider != null ? provider.getProvider() : null;
    }

    /**
     * Exibe o diálogo para o jogador (detecta automaticamente Bedrock ou Java).
     */
    void show(Player player, MineSkyDialog dialog);

    /**
     * Atalho estático para exibir.
     */
    static void display(Player player, MineSkyDialog dialog) {
        DialogAPI api = get();
        if (api != null) {
            api.show(player, dialog);
        }
    }

    /**
     * Cria um novo builder de diálogo.
     */
    MineSkyDialog.Builder createDialog();

    /**
     * Cria um builder já com título em Component.
     */
    MineSkyDialog.Builder createDialog(Component title);

    /**
     * Cria um builder já com título em String (suporta MiniMessage e &).
     */
    MineSkyDialog.Builder createDialog(String title);

    /**
     * Checa se o jogador está conectado via Bedrock (Floodgate).
     */
    boolean isBedrock(Player player);
}