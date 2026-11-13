package com.apocollis.aqtweaks.hazard;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.network.PacketHazardEvent;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The shared hazard engine. {@link #state} is the one call handlers make on either side. The server owns the
 * timelines, announces each event once with {@link PacketHazardEvent}, and resends the current event on login,
 * dimension change and respawn. Commands use {@link #trigger} and {@link #clear}, so forced events warn like
 * natural ones. Register an instance on the Forge event bus (always on, no parent imports).
 */
public final class Hazards {

    private static final class Entry {
        World world;
        HazardData data;
        HazardTimeline timeline;
    }

    private static final Map<Long, Entry> SERVER = new ConcurrentHashMap<>();

    /** Current state of a channel for the world at its current tick. */
    public static HazardState state(World world, HazardChannel channel) {
        if (world == null || !channel.enabled() || !channel.appliesTo(world)) {
            return HazardState.NONE;
        }
        long now = world.getTotalWorldTime();
        HazardEvent event;
        if (world.isRemote) {
            event = ClientHazards.event(world.provider.getDimension(), channel.id);
        } else {
            event = entry(world, channel).timeline.event();
        }
        return HazardState.of(event, now);
    }

    /** Starts an event on the server and announces it. */
    public static HazardEvent trigger(World world, HazardChannel channel, int durationSeconds,
                                      boolean skipWarning) {
        Entry entry = entry(world, channel);
        HazardEvent event = entry.timeline.trigger(world.getTotalWorldTime(), durationSeconds * 20L, skipWarning,
                channel.timing());
        entry.data.store(channel.id, entry.timeline);
        broadcast(world, event);
        return event;
    }

    /** Ends the current event (ramp-down) or cancels the upcoming one. False if there was nothing to clear. */
    public static boolean clear(World world, HazardChannel channel) {
        Entry entry = entry(world, channel);
        List<HazardEvent> out = entry.timeline.clear(world.getTotalWorldTime(), channel.timing());
        if (out.isEmpty()) return false;
        entry.data.store(channel.id, entry.timeline);
        for (HazardEvent event : out) broadcast(world, event);
        return true;
    }

    private static Entry entry(World world, HazardChannel channel) {
        int dim = world.provider.getDimension();
        long key = ((long) dim << 32) ^ (channel.id & 0xFFFFFFFFL);
        Entry entry = SERVER.get(key);
        if (entry == null || entry.world != world) {
            entry = new Entry();
            entry.world = world;
            entry.data = HazardData.get(world);
            entry.timeline = new HazardTimeline(channel.id,
                    world.getSeed() ^ ((long) dim * 0x2545F4914F6CDD1DL) ^ ((long) (channel.id + 1) * 0x9E3779B97F4A7C15L));
            entry.data.load(channel.id, entry.timeline);
            SERVER.put(key, entry);
        }
        return entry;
    }

    private static void broadcast(World world, HazardEvent event) {
        ArcanaQuestTweaks.NETWORK.sendToDimension(new PacketHazardEvent(world.provider.getDimension(), event),
                world.provider.getDimension());
    }

    private static void sendAll(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;
        World world = player.world;
        if (world == null || world.isRemote) return;
        for (HazardChannel channel : HazardChannels.forWorld(world)) {
            HazardEvent event = entry(world, channel).timeline.event();
            if (event != null) {
                ArcanaQuestTweaks.NETWORK.sendTo(new PacketHazardEvent(world.provider.getDimension(), event),
                        (EntityPlayerMP) player);
            }
        }
    }

    // ---- Forge bus -------------------------------------------------------------------------------------

    public static final class Handler {
        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.side.isClient() || !(event.world instanceof WorldServer)) {
                return;
            }
            World world = event.world;
            long now = world.getTotalWorldTime();
            for (HazardChannel channel : HazardChannels.forWorld(world)) {
                Entry entry = entry(world, channel);
                List<HazardEvent> created = entry.timeline.update(now, channel.timing());
                if (!created.isEmpty()) {
                    entry.data.store(channel.id, entry.timeline);
                    for (HazardEvent e : created) broadcast(world, e);
                }
            }
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload event) {
            World world = event.getWorld();
            if (world.isRemote) return;
            int dim = world.provider.getDimension();
            SERVER.keySet().removeIf(k -> (int) (k >> 32) == dim);
        }

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            sendAll(event.player);
        }

        @SubscribeEvent
        public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
            sendAll(event.player);
        }

        @SubscribeEvent
        public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            sendAll(event.player);
        }
    }

    private Hazards() {}
}
