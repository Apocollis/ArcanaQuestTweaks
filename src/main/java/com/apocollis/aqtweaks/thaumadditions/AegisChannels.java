package com.apocollis.aqtweaks.thaumadditions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;

/** Server-side record of who is holding an Aegis shield. No Thaumcraft types, so any module can read it. */
public final class AegisChannels {
    private AegisChannels() {}

    /** One raised shield. */
    public static final class Channel {
        public float pool;
        public final float maxPool;
        public final EnumHand hand;
        /** Ticks held since the raise. */
        public int held;
        public final int raisedAtTick;
        public float lastDamage;
        public int lastBlockTick = Integer.MIN_VALUE / 2;

        Channel(float pool, EnumHand hand, int raisedAtTick) {
            this.pool = pool;
            this.maxPool = pool;
            this.hand = hand;
            this.raisedAtTick = raisedAtTick;
        }
    }

    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();

    public static Channel get(EntityLivingBase entity) {
        return entity == null ? null : CHANNELS.get(entity.getUniqueID());
    }

    public static boolean isRaised(EntityLivingBase entity) {
        return get(entity) != null;
    }

    static Channel open(EntityLivingBase entity, float pool, EnumHand hand) {
        Channel channel = new Channel(pool, hand, entity.ticksExisted);
        CHANNELS.put(entity.getUniqueID(), channel);
        return channel;
    }

    static Channel close(EntityLivingBase entity) {
        return CHANNELS.remove(entity.getUniqueID());
    }

    static Channel close(UUID id) {
        return CHANNELS.remove(id);
    }

    static Iterable<Map.Entry<UUID, Channel>> all() {
        return new java.util.ArrayList<>(CHANNELS.entrySet());
    }
}
