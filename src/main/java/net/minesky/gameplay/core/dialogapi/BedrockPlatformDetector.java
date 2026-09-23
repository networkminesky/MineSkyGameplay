package net.minesky.gameplay.core.dialogapi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public final class BedrockPlatformDetector {

    private BedrockPlatformDetector() {}

    public static boolean isBedrock(Player player) {
        if (player == null) return false;
        if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (NoClassDefFoundError | Exception ignored) {
            return false;
        }
    }
}