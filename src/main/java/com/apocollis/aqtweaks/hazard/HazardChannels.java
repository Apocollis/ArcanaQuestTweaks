package com.apocollis.aqtweaks.hazard;

import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registry of hazard channels. Register in init when the owning mod is present. */
public final class HazardChannels {
    private HazardChannels() {}

    private static final List<HazardChannel> CHANNELS = new CopyOnWriteArrayList<>();

    public static synchronized void register(HazardChannel channel) {
        for (HazardChannel c : CHANNELS) {
            if (c.id == channel.id) {
                if (c == channel) return;
                throw new IllegalStateException("Hazard channel id " + channel.id + " already used by " + c.name);
            }
        }
        CHANNELS.add(channel);
    }

    public static List<HazardChannel> all() {
        return CHANNELS;
    }

    public static HazardChannel byId(int id) {
        for (HazardChannel c : CHANNELS) {
            if (c.id == id) return c;
        }
        return null;
    }

    public static List<HazardChannel> forWorld(World world) {
        List<HazardChannel> out = new ArrayList<>(2);
        for (HazardChannel c : CHANNELS) {
            if (c.enabled() && c.appliesTo(world)) out.add(c);
        }
        return out;
    }
}
