package net.minesky.gameplay;

import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.minesky.gameplay.core.advancements.AdvancementsManager;
import net.minesky.gameplay.core.advancements.DynamicAdvancementLoader;
import net.minesky.gameplay.core.advancements.command.ConquistasCommand;
import net.minesky.gameplay.core.advancements.hook.MythicHook;
import net.minesky.gameplay.core.locator.LocatorManager;
import net.minesky.gameplay.features.events.ChatListener;
import net.minesky.gameplay.features.events.PlayerLifecycleListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class MineSkyGameplayPlugin extends JavaPlugin implements Listener {

    private LocatorManager locatorManager;
    private AdvancementsManager advancementsManager;
    private DynamicAdvancementLoader advancementLoader;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        boolean disableChat = getConfig().getBoolean("disable-chat", true);
        boolean disableJoinQuit = getConfig().getBoolean("disable-join-quit", true);

        // 1. Inicializa o Core da LocatorAPI
        getLogger().info("[LocatorAPI] Inicializando LocatorAPI...");
        this.locatorManager = new LocatorManager(this);
        this.locatorManager.start();

        // 2. Inicializa o Core da AdvancementsAPI
        getLogger().info("[AdvancementsAPI] Inicializando AdvancementsAPI...");
        this.advancementsManager = new AdvancementsManager(this);
        this.advancementsManager.start();

        // Carrega Conquistas de Datapack
        this.advancementLoader = new DynamicAdvancementLoader(this, advancementsManager);
        this.advancementLoader.loadFromDatapack();

        // 3. Registra Comandos e Hooks Externos
        if (getCommand("conquistas") != null) {
            getCommand("conquistas").setExecutor(new ConquistasCommand());
        }

        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            MythicHook.register(this);
        }

        // 4. Registra Recursos Internos de Gameplay
        getServer().getPluginManager().registerEvents(new ChatListener(disableChat), this);
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this, disableJoinQuit), this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("MineSkyGameplay inicializado com sucesso no Folia!");
    }

    @Override
    public void onDisable() {
        // Encerra LocatorAPI e desinjeta Netty de todos os canais com segurança
        if (this.locatorManager != null) {
            this.locatorManager.stop();
            this.locatorManager = null;
        }

        // Encerra AdvancementsAPI e desregistra Services
        if (this.advancementsManager != null) {
            this.advancementsManager.stop();
            this.advancementsManager = null;
        }

        // Cancela todas as tarefas remanescentes no Folia associadas a este plugin
        getServer().getAsyncScheduler().cancelTasks(this);
        getServer().getGlobalRegionScheduler().cancelTasks(this);

        // Desregistra qualquer serviço residual no Bukkit
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
}