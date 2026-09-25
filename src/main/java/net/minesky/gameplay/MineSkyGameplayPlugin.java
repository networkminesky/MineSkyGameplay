package net.minesky.gameplay;

import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minesky.gameplay.core.advancements.AdvancementsManager;
import net.minesky.gameplay.core.advancements.DynamicAdvancementLoader;
import net.minesky.gameplay.core.advancements.command.ConquistasCommand;
import net.minesky.gameplay.core.advancements.hook.MythicHook;
import net.minesky.gameplay.core.dialogapi.DialogManager;
import net.minesky.gameplay.core.locator.LocatorManager;
import net.minesky.gameplay.features.commands.GameplayCommand;
import net.minesky.gameplay.features.events.ChatListener;
import net.minesky.gameplay.features.events.PlayerLifecycleListener;
import net.minesky.gameplay.features.homes.HomeTeleportCommand;
import net.minesky.gameplay.features.homes.HomesCommand;
import net.minesky.gameplay.features.homes.HomesListener;
import net.minesky.gameplay.features.homes.HomesMenuManager;
import org.bukkit.ServerLinks;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;

public final class MineSkyGameplayPlugin extends JavaPlugin implements Listener {

    private LocatorManager locatorManager;
    private AdvancementsManager advancementsManager;
    private DynamicAdvancementLoader advancementLoader;
    private HomesMenuManager homesMenuManager;
    private DialogManager dialogManager;

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

        getLogger().info("[DialogAPI] Inicializando DialogAPI Cross-Platform (Java + Bedrock)...");
        this.dialogManager = new DialogManager(this);
        this.dialogManager.start();

        this.advancementLoader = new DynamicAdvancementLoader(this, advancementsManager);
        this.advancementLoader.loadFromDatapack();

        this.homesMenuManager = new HomesMenuManager(this);
        HomesCommand homesCmd = new HomesCommand(homesMenuManager);
        HomeTeleportCommand homeTpCmd = new HomeTeleportCommand(homesMenuManager);
        GameplayCommand gameplayCommand = new GameplayCommand();

        if (getCommand("conquistas") != null) {
            getCommand("conquistas").setExecutor(new ConquistasCommand());
        }

        registerCommand("homes", homesCmd, homesCmd);
        registerCommand("home", homeTpCmd, homeTpCmd);
        registerCommand("mineskygameplay", gameplayCommand, gameplayCommand);

        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            MythicHook.register(this);
        }

        getServer().getPluginManager().registerEvents(new ChatListener(disableChat), this);
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this, disableJoinQuit), this);
        getServer().getPluginManager().registerEvents(new HomesListener(this, homesMenuManager), this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("MineSkyGameplay inicializado com sucesso no Folia!");

        for (ServerLinks.ServerLink link : getServer().getServerLinks().getLinks()) {
            getServer().getServerLinks().removeLink(link);
        }

        getServer().getServerLinks().addLink(
                Component.text("⏿ Site Oficial").color(TextColor.fromHexString("#03e7fc")),
                URI.create("https://minesky.com.br")
        );
        getServer().getServerLinks().addLink(
                Component.text("\uF804").color(NamedTextColor.WHITE)
                        .append(Component.text(" Loja MineSky").color(NamedTextColor.GOLD)),
                URI.create("https://minesky.com.br/loja")
        );
        getServer().getServerLinks().addLink(
                Component.text("༂").color(NamedTextColor.WHITE)
                        .append(Component.text(" Discord MineSky").color(TextColor.fromHexString("#7e87ef"))),
                URI.create("https://minesky.com.br/discord")
        );
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
        if (this.dialogManager != null) {
            this.dialogManager.stop();
            this.dialogManager = null;
        }

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

    public DialogManager getDialogManager() { return dialogManager; }
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