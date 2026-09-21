package net.minesky.gameplay;

import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.minesky.gameplay.core.advancements.AdvancementsManager;
import net.minesky.gameplay.core.advancements.DynamicAdvancementLoader;
import net.minesky.gameplay.core.advancements.command.ConquistasCommand;
import net.minesky.gameplay.core.advancements.hook.MythicHook;
import net.minesky.gameplay.core.locator.LocatorManager;
import net.minesky.gameplay.features.events.ChatListener;
import net.minesky.gameplay.features.events.PlayerLifecycleListener;
import net.minesky.gameplay.features.homes.HomeTeleportCommand;
import net.minesky.gameplay.features.homes.HomesCommand;
import net.minesky.gameplay.features.homes.HomesListener;
import net.minesky.gameplay.features.homes.HomesMenuManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class MineSkyGameplayPlugin extends JavaPlugin implements Listener {

    private LocatorManager locatorManager;
    private AdvancementsManager advancementsManager;
    private DynamicAdvancementLoader advancementLoader;
    private HomesMenuManager homesMenuManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        boolean disableChat = getConfig().getBoolean("disable-chat", true);
        boolean disableJoinQuit = getConfig().getBoolean("disable-join-quit", true);

        getLogger().info("[LocatorAPI] Inicializando LocatorAPI...");
        this.locatorManager = new LocatorManager(this);
        this.locatorManager.start();

        getLogger().info("[AdvancementsAPI] Inicializando AdvancementsAPI...");
        this.advancementsManager = new AdvancementsManager(this);
        this.advancementsManager.start();

        this.advancementLoader = new DynamicAdvancementLoader(this, advancementsManager);
        this.advancementLoader.loadFromDatapack();

        this.homesMenuManager = new HomesMenuManager(this);
        HomesCommand homesCmd = new HomesCommand(homesMenuManager);
        HomeTeleportCommand homeTpCmd = new HomeTeleportCommand(homesMenuManager);

        if (getCommand("conquistas") != null) {
            getCommand("conquistas").setExecutor(new ConquistasCommand());
        }

        registerCommand("homes", homesCmd, homesCmd);
        registerCommand("terrenos", homesCmd, homesCmd);
        registerCommand("salvos", homesCmd, homesCmd);
        registerCommand("home", homeTpCmd, homeTpCmd);

        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            MythicHook.register(this);
        }

        getServer().getPluginManager().registerEvents(new ChatListener(disableChat), this);
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this, disableJoinQuit), this);
        getServer().getPluginManager().registerEvents(new HomesListener(this, homesMenuManager), this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("MineSkyGameplay inicializado com sucesso no Folia!");
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter completer) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(completer);
        }
    }

    @Override
    public void onDisable() {
        if (this.locatorManager != null) {
            this.locatorManager.stop();
            this.locatorManager = null;
        }

        if (this.advancementsManager != null) {
            this.advancementsManager.stop();
            this.advancementsManager = null;
        }

        this.homesMenuManager = null;

        getServer().getAsyncScheduler().cancelTasks(this);
        getServer().getGlobalRegionScheduler().cancelTasks(this);
        getServer().getServicesManager().unregisterAll(this);

        getLogger().info("MineSkyGameplay descarregado com sucesso!");
    }

    @EventHandler
    public void onDatapackReload(ServerResourcesReloadedEvent event) {
        getLogger().info("Detectado reload de datapacks no servidor! Atualizando conquistas...");
        if (advancementLoader != null) {
            advancementLoader.loadFromDatapack();
        }
    }

    public LocatorManager getLocatorManager() {
        return locatorManager;
    }

    public AdvancementsManager getAdvancementsManager() {
        return advancementsManager;
    }

    public HomesMenuManager getHomesMenuManager() {
        return homesMenuManager;
    }
}