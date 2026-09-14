package net.minesky.mineskygameplay;

import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minesky.mineskygameplay.advancements.AdvancementsAPI;
import net.minesky.mineskygameplay.advancements.command.ConquistasCommand;
import net.minesky.mineskygameplay.advancements.hook.MythicHook;
import net.minesky.mineskygameplay.advancements.loader.DynamicAdvancementLoader;
import net.minesky.mineskygameplay.advancements.menu.BedrockMenuManager;
import org.bukkit.WeatherType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MineSkyGameplay extends JavaPlugin implements Listener {

    private AdvancementsAPI api;
    private BedrockMenuManager menuManager;
    private DynamicAdvancementLoader loader;

    private boolean disableChat = true;
    private boolean disableJoinQuit = true;

    private FileConfiguration config;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        config = this.getConfig();
        disableChat = config.getBoolean("disable-chat", true);
        disableJoinQuit = config.getBoolean("disable-join-quit", true);

        this.api = new AdvancementsAPI(this);
        this.menuManager = new BedrockMenuManager(this);
        this.loader = new DynamicAdvancementLoader(this, api);

        this.loader.loadFromDatapack();

        if (getCommand("conquistas") != null) {
            getCommand("conquistas").setExecutor(new ConquistasCommand());
        }

        if(this.getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            MythicHook.register(this);
        }

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if(disableJoinQuit)
            e.quitMessage(null);
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if(disableJoinQuit)
            e.joinMessage(null);

        final Player p = e.getPlayer();

        if(p.hasPlayedBefore()) {
            resetTime(p);
            return;
        }

        this.getLogger().info("Jogador "+p.getName()+" é novo no servidor, fazendo modificações...");

        p.setPlayerWeather(WeatherType.CLEAR);
        p.setPlayerTime(0, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if(!disableChat) return;

        if(e.isCancelled())
            return;
        e.setCancelled(true);

        e.getPlayer().sendMessage(
                Component.text("O chat está desligado por enquanto, aguarde.")
                        .color(NamedTextColor.RED)
        );
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
