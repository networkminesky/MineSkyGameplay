package net.minesky.gameplay.core.locator;

import com.mojang.datafixers.util.Either;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class LocatorPacketInterceptor extends ChannelOutboundHandlerAdapter {

    public static final String HANDLER_NAME = "mineskygameplay_locator_interceptor";

    private final UUID viewerUuid;
    private final LocatorManager manager;

    public LocatorPacketInterceptor(UUID viewerUuid, LocatorManager manager) {
        this.viewerUuid = viewerUuid;
        this.manager = manager;
    }

    public static void inject(Player player, LocatorManager manager) {
        try {
            Channel channel = getChannel(player);
            if (channel != null) {
                if (channel.pipeline().get(HANDLER_NAME) != null) {
                    channel.pipeline().remove(HANDLER_NAME);
                }
                if (channel.pipeline().get("packet_handler") != null) {
                    channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new LocatorPacketInterceptor(player.getUniqueId(), manager));
                } else {
                    channel.pipeline().addLast(HANDLER_NAME, new LocatorPacketInterceptor(player.getUniqueId(), manager));
                }
            }
        } catch (Throwable t) {
            if (manager.isDebug()) {
                t.printStackTrace();
            }
        }
    }

    public static void uninject(Player player) {
        try {
            Channel channel = getChannel(player);
            if (channel != null && channel.pipeline().get(HANDLER_NAME) != null) {
                channel.pipeline().remove(HANDLER_NAME);
            }
        } catch (Throwable ignored) {
        }
    }

    public static Channel getChannel(Player player) {
        try {
            CraftPlayer craftPlayer = (CraftPlayer) player;
            ServerPlayer serverPlayer = craftPlayer.getHandle();
            if (serverPlayer.connection != null) {
                Connection conn = serverPlayer.connection.connection;
                if (conn != null && conn.channel != null) {
                    return conn.channel;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            for (Field f : handle.getClass().getFields()) {
                if (f.getName().equals("connection")) {
                    Object conn = f.get(handle);
                    for (Field cf : conn.getClass().getFields()) {
                        if (cf.getType().getSimpleName().contains("Connection") || cf.getName().equals("connection")) {
                            Object netManager = cf.get(conn);
                            for (Field nf : netManager.getClass().getFields()) {
                                if (Channel.class.isAssignableFrom(nf.getType())) {
                                    return (Channel) nf.get(netManager);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ClientboundTrackedWaypointPacket packet) {
            try {
                if (!processWaypointPacket(ctx, packet, promise)) {
                    promise.setSuccess();
                    return;
                }
            } catch (Throwable t) {
                if (manager.isDebug()) {
                    t.printStackTrace();
                }
                promise.setSuccess();
                return;
            }
        }
        super.write(ctx, msg, promise);
    }

    private boolean processWaypointPacket(ChannelHandlerContext ctx, ClientboundTrackedWaypointPacket packet, ChannelPromise promise) {
        if (WaypointPacketHelper.isUntrack(packet)) {
            UUID targetId = extractTargetUuid(packet);
            if (targetId != null) {
                manager.markUntracked(viewerUuid, targetId);
            }
            return true;
        }

        UUID targetId = extractTargetUuid(packet);
        if (targetId == null) {
            return true;
        }

        Player viewer = Bukkit.getPlayer(viewerUuid);
        if (viewer == null || !viewer.isOnline()) {
            return false;
        }

        boolean canSee = manager.canSee(viewerUuid, targetId, viewer.getWorld());
        if (!canSee) {
            return false;
        }

        if (!manager.hasTracked(viewerUuid, targetId)) {
            if (WaypointPacketHelper.isUpdate(packet)) {
                TrackedWaypoint tw = WaypointPacketHelper.getWaypoint(packet);
                if (tw != null) {
                    ClientboundTrackedWaypointPacket trackPacket = WaypointPacketHelper.createTrackPacket(tw);
                    if (trackPacket != null) {
                        manager.markTracked(viewerUuid, targetId);
                        ctx.write(trackPacket, promise);
                        return false;
                    }
                }
                return false;
            }
            manager.markTracked(viewerUuid, targetId);
        }

        return true;
    }

    private UUID extractTargetUuid(ClientboundTrackedWaypointPacket packet) {
        TrackedWaypoint waypoint = WaypointPacketHelper.getWaypoint(packet);
        if (waypoint == null) return null;

        try {
            Either<UUID, String> id = waypoint.id();
            if (id != null && id.left().isPresent()) {
                return id.left().get();
            }
        } catch (Throwable ignored) {}

        try {
            for (Method m : waypoint.getClass().getMethods()) {
                if (m.getName().equals("id") && m.getParameterCount() == 0) {
                    Object res = m.invoke(waypoint);
                    if (res instanceof Either<?, ?> either) {
                        if (either.left().isPresent() && either.left().get() instanceof UUID uuid) {
                            return uuid;
                        }
                    } else if (res instanceof UUID uuid) {
                        return uuid;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private static final class WaypointPacketHelper {
        private static final Object OP_TRACK;
        private static final Object OP_UNTRACK;
        private static final Object OP_UPDATE;
        private static final Field OPERATION_FIELD;
        private static final Field WAYPOINT_FIELD;
        private static final Constructor<ClientboundTrackedWaypointPacket> CONSTRUCTOR;
        private static final boolean OP_FIRST;

        static {
            Class<?> opClass = null;
            Object track = null;
            Object untrack = null;
            Object update = null;
            Field opField = null;
            Field wpField = null;
            Constructor<ClientboundTrackedWaypointPacket> ctor = null;
            boolean opFirst = true;

            try {
                for (Class<?> declared : ClientboundTrackedWaypointPacket.class.getDeclaredClasses()) {
                    if (declared.getSimpleName().equals("Operation")) {
                        opClass = declared;
                        break;
                    }
                }
                if (opClass == null) {
                    for (Field f : ClientboundTrackedWaypointPacket.class.getDeclaredFields()) {
                        if (f.getType().isEnum() && f.getType().getName().contains("Operation")) {
                            opClass = f.getType();
                            break;
                        }
                    }
                }

                if (opClass != null) {
                    for (Object enumConstant : opClass.getEnumConstants()) {
                        String name = ((Enum<?>) enumConstant).name();
                        if ("TRACK".equalsIgnoreCase(name)) {
                            track = enumConstant;
                        } else if ("UNTRACK".equalsIgnoreCase(name)) {
                            untrack = enumConstant;
                        } else if ("UPDATE".equalsIgnoreCase(name)) {
                            update = enumConstant;
                        }
                    }

                    for (Constructor<?> c : ClientboundTrackedWaypointPacket.class.getDeclaredConstructors()) {
                        if (c.getParameterCount() == 2) {
                            if (c.getParameterTypes()[0].equals(opClass)) {
                                ctor = (Constructor<ClientboundTrackedWaypointPacket>) c;
                                ctor.setAccessible(true);
                                opFirst = true;
                                break;
                            } else if (c.getParameterTypes()[1].equals(opClass)) {
                                ctor = (Constructor<ClientboundTrackedWaypointPacket>) c;
                                ctor.setAccessible(true);
                                opFirst = false;
                                break;
                            }
                        }
                    }
                }

                for (Field f : ClientboundTrackedWaypointPacket.class.getDeclaredFields()) {
                    if (opClass != null && f.getType().equals(opClass)) {
                        f.setAccessible(true);
                        opField = f;
                    } else if (TrackedWaypoint.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        wpField = f;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

            OP_TRACK = track;
            OP_UNTRACK = untrack;
            OP_UPDATE = update;
            OPERATION_FIELD = opField;
            WAYPOINT_FIELD = wpField;
            CONSTRUCTOR = ctor;
            OP_FIRST = opFirst;
        }

        static boolean isUntrack(ClientboundTrackedWaypointPacket packet) {
            Object op = getOperation(packet);
            return op != null && op.equals(OP_UNTRACK);
        }

        static boolean isUpdate(ClientboundTrackedWaypointPacket packet) {
            Object op = getOperation(packet);
            return op != null && op.equals(OP_UPDATE);
        }

        static boolean isTrack(ClientboundTrackedWaypointPacket packet) {
            Object op = getOperation(packet);
            return op != null && op.equals(OP_TRACK);
        }

        static Object getOperation(ClientboundTrackedWaypointPacket packet) {
            if (packet == null) return null;
            if (OPERATION_FIELD != null) {
                try {
                    return OPERATION_FIELD.get(packet);
                } catch (Throwable ignored) {}
            }
            try {
                for (Method m : packet.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType().isEnum() && m.getReturnType().getSimpleName().contains("Operation")) {
                        return m.invoke(packet);
                    }
                }
            } catch (Throwable ignored) {}
            return null;
        }

        static TrackedWaypoint getWaypoint(ClientboundTrackedWaypointPacket packet) {
            if (packet == null) return null;
            try {
                return packet.waypoint();
            } catch (Throwable t) {
                if (WAYPOINT_FIELD != null) {
                    try {
                        return (TrackedWaypoint) WAYPOINT_FIELD.get(packet);
                    } catch (Throwable ignored) {}
                }
                try {
                    for (Method m : packet.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && TrackedWaypoint.class.isAssignableFrom(m.getReturnType())) {
                            return (TrackedWaypoint) m.invoke(packet);
                        }
                    }
                } catch (Throwable ignored) {}
            }
            return null;
        }

        static ClientboundTrackedWaypointPacket createTrackPacket(TrackedWaypoint waypoint) {
            if (CONSTRUCTOR == null || OP_TRACK == null || waypoint == null) return null;
            try {
                if (OP_FIRST) {
                    return CONSTRUCTOR.newInstance(OP_TRACK, waypoint);
                } else {
                    return CONSTRUCTOR.newInstance(waypoint, OP_TRACK);
                }
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}