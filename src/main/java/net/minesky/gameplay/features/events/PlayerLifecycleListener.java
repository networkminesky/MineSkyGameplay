package net.minesky.gameplay.features.events;

import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class PlayerLifecycleListener implements Listener {

    private final Plugin plugin;
    private final boolean disableJoinQuit;

    public PlayerLifecycleListener(Plugin plugin, boolean disableJoinQuit) {
        this.plugin = plugin;
        this.disableJoinQuit = disableJoinQuit;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (disableJoinQuit) {
            event.quitMessage(null);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (disableJoinQuit) {
            event.joinMessage(null);
        }

        Player p = event.getPlayer();
        if (p.hasPlayedBefore()) {
            resetTime(p);
            return;
        }

        plugin.getLogger().info("Jogador " + p.getName() + " é novo no servidor, aplicando definições padrão...");
        p.setPlayerWeather(WeatherType.CLEAR);
        p.setPlayerTime(0, false);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        resetTime(event.getPlayer());
    }

    @EventHandler
    public void onDeepSleep(PlayerDeepSleepEvent event) {
        resetTime(event.getPlayer());
    }

    private void resetTime(Player p) {
        p.resetPlayerTime();
        p.resetPlayerWeather();
    }
}