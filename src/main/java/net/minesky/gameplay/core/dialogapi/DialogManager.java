package net.minesky.gameplay.core.dialogapi;

import net.kyori.adventure.text.Component;
import net.minesky.gameplay.MineSkyGameplayPlugin;
import net.minesky.gameplay.api.dialogapi.DialogAPI;
import net.minesky.gameplay.api.dialogapi.MineSkyDialog;
import net.minesky.gameplay.core.dialogapi.renderers.BedrockDialogRenderer;
import net.minesky.gameplay.core.dialogapi.renderers.JavaDialogRenderer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

public final class DialogManager implements DialogAPI {

    private final MineSkyGameplayPlugin plugin;

    public DialogManager(MineSkyGameplayPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getServicesManager().register(DialogAPI.class, this, plugin, ServicePriority.High);
    }

    public void stop() {
        plugin.getServer().getServicesManager().unregister(DialogAPI.class, this);
    }

    @Override
    public void show(Player player, MineSkyDialog dialog) {
        if (player == null || !player.isOnline() || dialog == null) return;

        player.getScheduler().run(plugin, task -> {
            if (isBedrock(player)) {
                BedrockDialogRenderer.renderAndShow(plugin, player, dialog);
            } else {
                JavaDialogRenderer.renderAndShow(plugin, player, dialog);
            }
        }, null);
    }

    @Override
    public MineSkyDialog.Builder createDialog() {
        return MineSkyDialog.builder();
    }

    @Override
    public MineSkyDialog.Builder createDialog(Component title) {
        return MineSkyDialog.builder(title);
    }

    @Override
    public MineSkyDialog.Builder createDialog(String title) {
        return MineSkyDialog.builder(title);
    }

    @Override
    public boolean isBedrock(Player player) {
        return BedrockPlatformDetector.isBedrock(player);
    }
}