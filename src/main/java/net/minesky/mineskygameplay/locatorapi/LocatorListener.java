package net.minesky.mineskygameplay.locatorapi;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class LocatorListener implements Listener {

    private final LocatorManager manager;
    private final Plugin plugin;

    public LocatorListener(LocatorManager manager, Plugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        LocatorPacketInterceptor.inject(player, manager);
        manager.handleJoin(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        LocatorPacketInterceptor.uninject(player);
        manager.handleQuit(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        // Garante execução na thread correta da nova região no Folia
        player.getScheduler().run(plugin, task -> {
            manager.handleWorldSwitch(player, event.getFrom(), player.getWorld());
        }, null);
    }
}