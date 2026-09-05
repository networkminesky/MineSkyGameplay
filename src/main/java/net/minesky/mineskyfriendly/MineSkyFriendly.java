package net.minesky.mineskyfriendly;

import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MineSkyFriendly extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();

        if(p.hasPlayedBefore()) {
            resetTime(p);
            return;
        }

        this.getLogger().info("Jogador "+p.getName()+" é novo, fazendo modificações...");

        p.setPlayerWeather(WeatherType.CLEAR);
        p.setPlayerTime(0, false);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        resetTime(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeepSleepEvent e) {
        resetTime(e.getPlayer());
    }

    private void resetTime(final Player p) {
        p.resetPlayerTime();
        p.resetPlayerWeather();
    }

}
