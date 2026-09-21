package net.minesky.gameplay.core.locator;

import io.netty.channel.Channel;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minesky.gameplay.api.locator.LocatorAPI;
import net.minesky.gameplay.api.locator.LocatorWorldMode;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocatorManager implements LocatorAPI {

    private final Plugin plugin;
    private final Set<String> alwaysEnabledWorlds = ConcurrentHashMap.newKeySet();
    private final Set<String> alwaysDisabledWorlds = ConcurrentHashMap.newKeySet();
    private boolean defaultWorldEnabled = true;
    private boolean debug = false;

    private final Map<UUID, Set<UUID>> allowedTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> hiddenTargets = new ConcurrentHashMap<>();
    private final Set<UUID> globallyHiddenPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> globallyRevealedPlayers = ConcurrentHashMap.newKeySet();

    private final Map<String, Set<UUID>> groups = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerToGroup = new ConcurrentHashMap<>();
    private final Map<String, Boolean> groupMutualVisibility = new ConcurrentHashMap<>();

    private final Map<UUID, Set<UUID>> clientTrackedMap = new ConcurrentHashMap<>();

    private LocatorListener listener;

    public LocatorManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void start() {
        Bukkit.getServicesManager().register(LocatorAPI.class, this, plugin, ServicePriority.Normal);

        this.listener = new LocatorListener(this, plugin);
        Bukkit.getPluginManager().registerEvents(this.listener, plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            LocatorPacketInterceptor.inject(player, this);
            handleJoin(player);
        }
    }

    public void stop() {
        Bukkit.getServicesManager().unregister(LocatorAPI.class, this);

        if (this.listener != null) {
            HandlerList.unregisterAll(this.listener);
            this.listener = null;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            LocatorPacketInterceptor.uninject(player);
        }

        allowedTargets.clear();
        hiddenTargets.clear();
        globallyHiddenPlayers.clear();
        globallyRevealedPlayers.clear();
        groups.clear();
        playerToGroup.clear();
        groupMutualVisibility.clear();
        clientTrackedMap.clear();
        alwaysEnabledWorlds.clear();
        alwaysDisabledWorlds.clear();
    }

    public void loadConfig() {
        alwaysEnabledWorlds.clear();
        alwaysDisabledWorlds.clear();

        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        List<String> enabled = config.getStringList("always-enabled-worlds");
        if (enabled != null) alwaysEnabledWorlds.addAll(enabled);

        List<String> disabled = config.getStringList("always-disabled-worlds");
        if (disabled != null) alwaysDisabledWorlds.addAll(disabled);

        this.defaultWorldEnabled = config.getString("default-world-mode", "ENABLED").equalsIgnoreCase("ENABLED");
        this.debug = config.getBoolean("debug", false);
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean isWorldEnabled(World world) {
        return world != null && isWorldEnabled(world.getName());
    }

    @Override
    public boolean isWorldEnabled(String worldName) {
        return alwaysEnabledWorlds.contains(worldName);
    }

    @Override
    public boolean isWorldDisabled(World world) {
        return world != null && isWorldDisabled(world.getName());
    }

    @Override
    public boolean isWorldDisabled(String worldName) {
        return alwaysDisabledWorlds.contains(worldName);
    }

    @Override
    public LocatorWorldMode getWorldMode(String worldName) {
        if (alwaysEnabledWorlds.contains(worldName)) return LocatorWorldMode.ALWAYS_ENABLED;
        if (alwaysDisabledWorlds.contains(worldName)) return LocatorWorldMode.ALWAYS_DISABLED;
        return LocatorWorldMode.DEFAULT;
    }

    @Override
    public void setWorldMode(String worldName, LocatorWorldMode mode) {
        alwaysEnabledWorlds.remove(worldName);
        alwaysDisabledWorlds.remove(worldName);

        if (mode == LocatorWorldMode.ALWAYS_ENABLED) {
            alwaysEnabledWorlds.add(worldName);
        } else if (mode == LocatorWorldMode.ALWAYS_DISABLED) {
            alwaysDisabledWorlds.add(worldName);
        }
    }

    @Override
    public List<String> getAlwaysEnabledWorlds() {
        return new ArrayList<>(alwaysEnabledWorlds);
    }

    @Override
    public List<String> getAlwaysDisabledWorlds() {
        return new ArrayList<>(alwaysDisabledWorlds);
    }

    @Override
    public void reloadConfig() {
        loadConfig();
    }

    @Override
    public boolean canSee(Player viewer, Player target) {
        if (viewer == null || target == null) return false;
        if (!viewer.getWorld().equals(target.getWorld())) return false;
        return canSee(viewer.getUniqueId(), target.getUniqueId(), viewer.getWorld());
    }

    @Override
    public boolean canSee(UUID viewerId, UUID targetId, World world) {
        if (viewerId.equals(targetId)) return true;

        if (globallyHiddenPlayers.contains(targetId)) return false;
        if (globallyRevealedPlayers.contains(targetId)) return true;

        Set<UUID> hidden = hiddenTargets.get(viewerId);
        if (hidden != null && hidden.contains(targetId)) return false;

        Set<UUID> allowed = allowedTargets.get(viewerId);
        if (allowed != null && allowed.contains(targetId)) return true;

        String viewerGroup = playerToGroup.get(viewerId);
        String targetGroup = playerToGroup.get(targetId);
        if (viewerGroup != null && viewerGroup.equals(targetGroup)) {
            if (groupMutualVisibility.getOrDefault(viewerGroup, true)) return true;
        }

        String worldName = world.getName();
        if (alwaysDisabledWorlds.contains(worldName)) return false;
        if (alwaysEnabledWorlds.contains(worldName)) return true;

        return defaultWorldEnabled;
    }

    @Override
    public void showPlayer(Player viewer, Player target) {
        setPlayerVisibleTo(viewer, target, true);
    }

    @Override
    public void hidePlayer(Player viewer, Player target) {
        setPlayerVisibleTo(viewer, target, false);
    }

    @Override
    public void setPlayerVisibleTo(Player viewer, Player target, boolean visible) {
        if (viewer == null || target == null) return;
        UUID vId = viewer.getUniqueId();
        UUID tId = target.getUniqueId();

        if (visible) {
            allowedTargets.computeIfAbsent(vId, k -> ConcurrentHashMap.newKeySet()).add(tId);
            Set<UUID> hidden = hiddenTargets.get(vId);
            if (hidden != null) hidden.remove(tId);
            syncWaypoint(target);
        } else {
            hiddenTargets.computeIfAbsent(vId, k -> ConcurrentHashMap.newKeySet()).add(tId);
            Set<UUID> allowed = allowedTargets.get(vId);
            if (allowed != null) allowed.remove(tId);
            markUntracked(vId, tId);
            sendUntrackPacket(viewer, tId);
        }
    }

    @Override
    public void resetPlayerVisibility(Player viewer, Player target) {
        if (viewer == null || target == null) return;
        UUID vId = viewer.getUniqueId();
        UUID tId = target.getUniqueId();

        Set<UUID> allowed = allowedTargets.get(vId);
        if (allowed != null) allowed.remove(tId);

        Set<UUID> hidden = hiddenTargets.get(vId);
        if (hidden != null) hidden.remove(tId);

        if (!canSee(viewer, target)) {
            markUntracked(vId, tId);
            sendUntrackPacket(viewer, tId);
        } else {
            syncWaypoint(target);
        }
    }

    @Override
    public void clearPlayerOverrides(Player viewer) {
        if (viewer == null) return;
        allowedTargets.remove(viewer.getUniqueId());
        hiddenTargets.remove(viewer.getUniqueId());
    }

    @Override
    public void hidePlayerGlobally(Player player) {
        if (player == null) return;
        UUID pId = player.getUniqueId();
        globallyHiddenPlayers.add(pId);
        globallyRevealedPlayers.remove(pId);

        for (Player other : player.getWorld().getPlayers()) {
            if (!other.getUniqueId().equals(pId)) {
                markUntracked(other.getUniqueId(), pId);
                sendUntrackPacket(other, pId);
            }
        }
    }

    @Override
    public void showPlayerGlobally(Player player) {
        if (player == null) return;
        globallyHiddenPlayers.remove(player.getUniqueId());
        syncWaypoint(player);
    }

    @Override
    public boolean isPlayerGloballyHidden(Player player) {
        return player != null && globallyHiddenPlayers.contains(player.getUniqueId());
    }

    @Override
    public void revealPlayerGlobally(Player player) {
        if (player == null) return;
        UUID pId = player.getUniqueId();
        globallyRevealedPlayers.add(pId);
        globallyHiddenPlayers.remove(pId);
        syncWaypoint(player);
    }

    @Override
    public void unrevealPlayerGlobally(Player player) {
        if (player == null) return;
        globallyRevealedPlayers.remove(player.getUniqueId());
        for (Player other : player.getWorld().getPlayers()) {
            if (!canSee(other, player)) {
                markUntracked(other.getUniqueId(), player.getUniqueId());
                sendUntrackPacket(other, player.getUniqueId());
            }
        }
    }

    @Override
    public boolean isPlayerGloballyRevealed(Player player) {
        return player != null && globallyRevealedPlayers.contains(player.getUniqueId());
    }

    @Override
    public void createGroup(String groupId) {
        groups.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet());
        groupMutualVisibility.put(groupId, true);
    }

    @Override
    public void deleteGroup(String groupId) {
        Set<UUID> members = groups.remove(groupId);
        groupMutualVisibility.remove(groupId);
        if (members != null) {
            for (UUID uuid : members) {
                playerToGroup.remove(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) syncWaypoint(p);
            }
        }
    }

    @Override
    public void addPlayerToGroup(String groupId, Player player) {
        if (player == null) return;
        createGroup(groupId);
        removePlayerFromGroup(player);

        groups.get(groupId).add(player.getUniqueId());
        playerToGroup.put(player.getUniqueId(), groupId);
        syncWaypoint(player);
    }

    @Override
    public void removePlayerFromGroup(Player player) {
        if (player == null) return;
        UUID pId = player.getUniqueId();
        String currentGroup = playerToGroup.remove(pId);
        if (currentGroup != null) {
            Set<UUID> set = groups.get(currentGroup);
            if (set != null) set.remove(pId);
        }
    }

    @Override
    public Set<UUID> getGroupMembers(String groupId) {
        Set<UUID> members = groups.get(groupId);
        return members != null ? Collections.unmodifiableSet(members) : Collections.emptySet();
    }

    @Override
    public Optional<String> getPlayerGroup(Player player) {
        return Optional.ofNullable(player != null ? playerToGroup.get(player.getUniqueId()) : null);
    }

    @Override
    public void setGroupMutualVisibility(String groupId, boolean visible) {
        groupMutualVisibility.put(groupId, visible);
        Set<UUID> members = groups.get(groupId);
        if (members != null) {
            for (UUID uuid : members) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) syncWaypoint(p);
            }
        }
    }

    public boolean hasTracked(UUID viewerId, UUID targetId) {
        Set<UUID> set = clientTrackedMap.get(viewerId);
        return set != null && set.contains(targetId);
    }

    public void markTracked(UUID viewerId, UUID targetId) {
        clientTrackedMap.computeIfAbsent(viewerId, k -> ConcurrentHashMap.newKeySet()).add(targetId);
    }

    public void markUntracked(UUID viewerId, UUID targetId) {
        Set<UUID> set = clientTrackedMap.get(viewerId);
        if (set != null) set.remove(targetId);
    }

    public void sendUntrackPacket(Player viewer, UUID targetId) {
        ClientboundTrackedWaypointPacket packet = ClientboundTrackedWaypointPacket.removeWaypoint(targetId);
        Channel channel = LocatorPacketInterceptor.getChannel(viewer);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(packet);
        }
    }

    public void syncWaypoint(Player player) {
        if (player == null || !player.isOnline()) return;

        player.getScheduler().run(plugin, scheduledTask -> {
            try {
                ServerPlayer sp = ((CraftPlayer) player).getHandle();
                ServerLevel level = ((CraftWorld) player.getWorld()).getHandle();
                Object wm = level.getWaypointManager();
                if (wm != null) {
                    for (Method m : wm.getClass().getMethods()) {
                        if (m.getName().equals("updatePlayer") || m.getName().equals("updateWaypoint")) {
                            if (m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(sp.getClass())) {
                                m.invoke(wm, sp);
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                if (debug) t.printStackTrace();
            }
        }, null);
    }

    public void handleJoin(Player player) {
        syncWaypoint(player);
    }

    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        allowedTargets.remove(id);
        hiddenTargets.remove(id);
        clientTrackedMap.remove(id);
        removePlayerFromGroup(player);
    }

    public void handleWorldSwitch(Player player, World from, World to) {
        clientTrackedMap.remove(player.getUniqueId());

        if (isWorldDisabled(to)) {
            for (Player other : to.getPlayers()) {
                if (!canSee(player, other)) sendUntrackPacket(player, other.getUniqueId());
                if (!canSee(other, player)) sendUntrackPacket(other, player.getUniqueId());
            }
        } else {
            syncWaypoint(player);
        }
    }
}