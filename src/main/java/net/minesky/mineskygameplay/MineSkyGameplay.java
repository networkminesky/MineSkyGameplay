package net.minesky.mineskygameplay;

import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.minesky.mineskygameplay.advancements.AdvancementsAPI;
import net.minesky.mineskygameplay.advancements.command.ConquistasCommand;
import net.minesky.mineskygameplay.advancements.loader.DynamicAdvancementLoader;
import net.minesky.mineskygameplay.advancements.menu.BedrockMenuManager;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MineSkyGameplay extends JavaPlugin implements Listener {

    private AdvancementsAPI api;
    private BedrockMenuManager menuManager;
    private DynamicAdvancementLoader loader;

    @Override
    public void onEnable() {
        this.api = new AdvancementsAPI(this);
        this.menuManager = new BedrockMenuManager(this);
        this.loader = new DynamicAdvancementLoader(this, api);

        this.loader.loadFromDatapack();

        if (getCommand("conquistas") != null) {
            getCommand("conquistas").setExecutor(new ConquistasCommand());
        }

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();

        if(p.hasPlayedBefore()) {
            resetTime(p);
            return;
        }

        this.getLogger().info("Jogador "+p.getName()+" é novo no servidor, fazendo modificações...");

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


    @EventHandler
    public void onDatapackReload(ServerResourcesReloadedEvent event) {
        getLogger().info("Detectado reload de datapacks no servidor! Atualizando conquistas...");
        loader.loadFromDatapack();
    }

    public BedrockMenuManager getMenuManager() {
        return menuManager;
    }

    public DynamicAdvancementLoader getLoader() {
        return loader;
    }

}
