package com.apocollis.aqtweaks.thaumadditions.client;

import com.apocollis.aqtweaks.network.PacketAegisShield;
import com.apocollis.aqtweaks.thaumadditions.AegisMath;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Client view of every nearby Aegis shield: pool, rune fades and the hit flash. Fed by {@link PacketAegisShield}. */
public class AegisClientState {
    public static final int RUNE_POOL = 5;
    public static final int MAX_RUNES = 5;
    private static final int STALE_TICKS = 40;

    public static final class Shield {
        public float pool;
        public float maxPool;
        public long lastUpdate;
        public int flash;
        public final float[] runeAlpha = new float[MAX_RUNES];
        public final float[] runeFlash = new float[MAX_RUNES];
        public int ticks;

        public int runes() {
            return Math.min(MAX_RUNES, AegisMath.runes(pool, RUNE_POOL));
        }

        public int maxRunes() {
            return Math.max(1, Math.min(MAX_RUNES, AegisMath.runes(maxPool, RUNE_POOL)));
        }
    }

    private static final Map<Integer, Shield> SHIELDS = new HashMap<>();

    public static Map<Integer, Shield> shields() {
        return SHIELDS;
    }

    public static void apply(int entityId, byte mode, float pool, float maxPool) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;
        if (mode == PacketAegisShield.ENDED) {
            SHIELDS.remove(entityId);
            return;
        }
        if (mode == PacketAegisShield.BROKE) {
            Shield gone = SHIELDS.remove(entityId);
            Entity e = mc.world.getEntityByID(entityId);
            if (e != null) {
                for (int i = 0; i < 24; i++) {
                    double dx = (mc.world.rand.nextDouble() - 0.5D) * 1.4D;
                    double dy = mc.world.rand.nextDouble() * 1.6D;
                    double dz = (mc.world.rand.nextDouble() - 0.5D) * 1.4D;
                    mc.world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, e.posX + dx, e.posY + dy, e.posZ + dz,
                            dx * 0.2D, 0.05D, dz * 0.2D);
                }
            }
            return;
        }
        Shield s = SHIELDS.get(entityId);
        if (s == null) {
            s = new Shield();
            SHIELDS.put(entityId, s);
            int count = Math.min(MAX_RUNES, AegisMath.runes(pool, RUNE_POOL));
            for (int i = 0; i < MAX_RUNES; i++) {
                s.runeAlpha[i] = i < count ? 1.0F : 0.0F;
            }
        }
        int before = s.runes();
        s.pool = pool;
        s.maxPool = maxPool;
        s.lastUpdate = mc.world.getTotalWorldTime();
        if (mode == PacketAegisShield.HIT) {
            s.flash = 6;
            int after = s.runes();
            for (int i = after; i < before && i < MAX_RUNES; i++) {
                s.runeFlash[i] = 1.0F;
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            SHIELDS.clear();
            return;
        }
        long now = mc.world.getTotalWorldTime();
        Iterator<Map.Entry<Integer, Shield>> it = SHIELDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Shield> entry = it.next();
            Shield s = entry.getValue();
            if (mc.world.getEntityByID(entry.getKey()) == null || now - s.lastUpdate > STALE_TICKS) {
                it.remove();
                continue;
            }
            s.ticks++;
            if (s.flash > 0) s.flash--;
            int count = s.runes();
            for (int i = 0; i < MAX_RUNES; i++) {
                float target = i < count ? 1.0F : 0.0F;
                s.runeAlpha[i] += (target - s.runeAlpha[i]) * 0.3F;
                if (s.runeFlash[i] > 0.0F) s.runeFlash[i] = Math.max(0.0F, s.runeFlash[i] - 0.1F);
            }
        }
    }
}
