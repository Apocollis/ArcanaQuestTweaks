package com.apocollis.aqtweaks.nether;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.aurorian.MoonfrostTier;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.apocollis.aqtweaks.network.PacketAshCloud;
import com.apocollis.aqtweaks.weather.GearTierRule;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Withering ash clouds in the Nether on the {@code ash} hazard channel. At the start of an ash event one cloud
 * is spawned near every active player. A cloud drifts, and its exact cell set is an incremental flood fill of open
 * space, so the gas pours through any opening and never through a wall. Time inside builds a dose that gives
 * Slowness, Wither and ash lung; the dose, like the clouds, lives in memory only.
 */
public class AshCloudManager {

    public static final DamageSource DAMAGE_ASH = new DamageSource("aqtweaks_ash").setDamageBypassesArmor();

    /** The flood fill starts this many ticks before the storm does. */
    private static final long PREFILL_TICKS = 40L;

    private final Random rand = new Random();
    private final List<AshCloud> clouds = new ArrayList<>();
    private final Map<UUID, Double> dose = new HashMap<>();
    private final Map<UUID, Integer> debt = new HashMap<>();
    private long eventId = Long.MIN_VALUE;
    private boolean spawned;
    private int nextId = 1;
    private final List<AshCloud> filling = new ArrayList<>();
    private int fillStart;

    public List<AshCloud> clouds() {
        return clouds;
    }

    public double doseOf(UUID id) {
        return dose.getOrDefault(id, 0.0D);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || !(event.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) event.world;
        if (!(world.provider instanceof WorldProviderHell)) return;
        if (!ArcanaQuestTweaksConfig.NetherModuleConfig.enableAsh) {
            if (!clouds.isEmpty()) clearAll(world);
            return;
        }
        long now = world.getTotalWorldTime();
        HazardState state = Hazards.state(world, NetherHazards.ASH);
        if (state.event != null && state.event.id != eventId) {
            eventId = state.event.id;
            clearAll(world);
            spawned = false;
        }
        // The clouds are created in the last seconds of the warning so their flood fill is finished when the storm
        // starts; they stay silent and harmless (not armed) until then.
        if (state.phase == HazardEvent.Phase.WARNING && !spawned && state.event != null
                && state.event.start - now <= PREFILL_TICKS) {
            spawned = true;
            spawnFor(world, state.event, state.event.start, false);
        }
        if (state.phase == HazardEvent.Phase.ACTIVE) {
            if (!spawned) {
                spawned = true;
                spawnFor(world, state.event, now, true);
            } else {
                arm(world, now);
            }
        }
        if (state.phase == HazardEvent.Phase.CLEAR && !clouds.isEmpty()) {
            clearAll(world);
        }
        // Move, expire and start fills first (backwards, so a removal is safe), then share one cell budget among
        // every cloud that is filling.
        filling.clear();
        for (int i = clouds.size() - 1; i >= 0; i--) {
            AshCloud cloud = clouds.get(i);
            if (tickCloud(world, cloud, now)) filling.add(cloud);
        }
        stepFills(world, now);
        if (now % 20L == 0L) {
            playerSecond(world, now);
        }
    }

    // ---- spawning and drifting ----

    /** Announces the clouds that were filled during the warning, the moment the storm starts. */
    private void arm(WorldServer world, long now) {
        for (AshCloud cloud : clouds) {
            if (cloud.armed) continue;
            cloud.armed = true;
            cloud.lastSent = null;
            if (!cloud.occupied.isEmpty()) {
                double scale = cloud.scale(now);
                broadcast(world, cloud, ArcanaQuestTweaksConfig.NetherModuleConfig.ashReachHorizontal * scale,
                        ArcanaQuestTweaksConfig.NetherModuleConfig.ashReachVertical * scale, now);
            }
        }
    }

    private void spawnFor(WorldServer world, HazardEvent event, long startTick, boolean armed) {
        int cap = ArcanaQuestTweaksConfig.NetherModuleConfig.maxAshClouds;
        for (EntityPlayer player : world.playerEntities) {
            if (clouds.size() >= cap) break;
            if (player.isCreative() || player.isSpectator() || player.isDead) continue;
            if (insideAny(player)) continue;
            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double offset = rand.nextDouble() * 4.0D;
            double cx = player.posX + Math.cos(angle) * offset;
            double cz = player.posZ + Math.sin(angle) * offset;
            double cy = player.posY + 1.0D;
            if (!GasPermeability.isOpen(world, (int) Math.floor(cx), (int) Math.floor(cy), (int) Math.floor(cz))) {
                cx = player.posX;
                cz = player.posZ;
            }
            AshCloud cloud = new AshCloud(nextId++, cx, cy, cz, rand.nextDouble() * Math.PI * 2.0D, startTick, event.end);
            cloud.armed = armed;
            setVelocity(cloud);
            clouds.add(cloud);
            debug("ash cloud " + cloud.id + " spawned near " + player.getName());
        }
    }

    private void setVelocity(AshCloud cloud) {
        double perTick = ArcanaQuestTweaksConfig.NetherModuleConfig.ashCloudSpeed / 20.0D;
        cloud.vx = Math.cos(cloud.angle) * perTick;
        cloud.vz = Math.sin(cloud.angle) * perTick;
    }

    /** Moves the cloud and starts a fill when due. True when the cloud has a fill to step this tick. */
    private boolean tickCloud(WorldServer world, AshCloud cloud, long now) {
        if (now >= cloud.endTick) {
            remove(world, cloud);
            return false;
        }
        cloud.x += cloud.vx;
        cloud.z += cloud.vz;
        if (now % 40L == 0L) {
            cloud.angle += (rand.nextDouble() - 0.5D) * 0.7D;
            setVelocity(cloud);
        }
        if (cloud.fill == null && now - cloud.lastFillStart >= 20L) {
            cloud.lastFillStart = now;
            double scale = cloud.scale(now);
            double rh = ArcanaQuestTweaksConfig.NetherModuleConfig.ashReachHorizontal * scale;
            double rv = ArcanaQuestTweaksConfig.NetherModuleConfig.ashReachVertical * scale;
            cloud.fill = new AshCloudField((x, y, z) -> GasPermeability.isOpen(world, x, y, z), cloud.x, cloud.y,
                    cloud.z, rh, rv, cloud.occupied.isEmpty() ? null : cloud.occupied,
                    ArcanaQuestTweaksConfig.NetherModuleConfig.gasMaxCells);
        }
        return cloud.fill != null;
    }

    /** One Gas Cells Per Tick budget for the whole dimension, split across the clouds that are filling. */
    private void stepFills(WorldServer world, long now) {
        int n = filling.size();
        if (n == 0) return;
        int[] shares = FillBudget.split(ArcanaQuestTweaksConfig.NetherModuleConfig.gasCellsPerTick, n, fillStart++);
        for (int i = 0; i < n; i++) {
            AshCloud cloud = filling.get(i);
            if (cloud.fill != null && cloud.fill.step(shares[i])) {
                if (!cloud.fill.result().isEmpty()) {
                    cloud.occupied = cloud.fill.result();
                }
                cloud.fill = null;
                double scale = cloud.scale(now);
                broadcast(world, cloud, ArcanaQuestTweaksConfig.NetherModuleConfig.ashReachHorizontal * scale,
                        ArcanaQuestTweaksConfig.NetherModuleConfig.ashReachVertical * scale, now);
            }
        }
    }

    private void remove(WorldServer world, AshCloud cloud) {
        clouds.remove(cloud);
        ArcanaQuestTweaks.NETWORK.sendToDimension(PacketAshCloud.removed(cloud.id), world.provider.getDimension());
    }

    public void clearAll(WorldServer world) {
        for (AshCloud cloud : new ArrayList<>(clouds)) {
            remove(world, cloud);
        }
        clouds.clear();
    }

    private PacketAshCloud packetOf(AshCloud cloud, double rh, double rv) {
        int[] b = cloud.bounds();
        if (b == null) return null;
        if (b[3] > 120 || b[4] > 120 || b[5] > 120) return null;
        PacketAshCloud p = new PacketAshCloud();
        p.id = cloud.id;
        p.alive = true;
        p.x = cloud.x;
        p.y = cloud.y;
        p.z = cloud.z;
        p.vx = (float) cloud.vx;
        p.vz = (float) cloud.vz;
        p.rh = (float) rh;
        p.rv = (float) rv;
        p.endTick = cloud.endTick;
        p.ox = b[0];
        p.oy = b[1];
        p.oz = b[2];
        p.sx = b[3];
        p.sy = b[4];
        p.sz = b[5];
        p.bits = cloud.bitmap(b);
        return p;
    }

    private void broadcast(WorldServer world, AshCloud cloud, double rh, double rv, long now) {
        if (!cloud.armed) return;
        PacketAshCloud p = packetOf(cloud, rh, rv);
        if (p == null) return;
        if (cloud.lastSent != null && unchanged(cloud.lastSent, cloud.lastSentTick, p, now)) return;
        ArcanaQuestTweaks.NETWORK.sendToDimension(p, world.provider.getDimension());
        cloud.lastSent = p;
        cloud.lastSentTick = now;
    }

    /**
     * True when the client already shows this cloud: the client moves a cloud by {@code vx, vz} every tick, so
     * compare against where it has carried the last packet, not against the packet itself. Same cells, same
     * velocity (the wind veers every 40 ticks) and the same reach within half a block.
     */
    static boolean unchanged(PacketAshCloud prev, long prevTick, PacketAshCloud next, long now) {
        long dt = now - prevTick;
        double px = prev.x + prev.vx * dt;
        double pz = prev.z + prev.vz * dt;
        return Math.abs(next.x - px) < 1.0D && Math.abs(next.z - pz) < 1.0D
                && next.y == prev.y && next.vx == prev.vx && next.vz == prev.vz
                && Math.abs(next.rh - prev.rh) < 0.5F && Math.abs(next.rv - prev.rv) < 0.5F
                && next.endTick == prev.endTick
                && next.ox == prev.ox && next.oy == prev.oy && next.oz == prev.oz
                && next.sx == prev.sx && next.sy == prev.sy && next.sz == prev.sz
                && java.util.Arrays.equals(next.bits, prev.bits);
    }

    private void sendAllTo(EntityPlayerMP player) {
        WorldServer world = DimensionManager.getWorld(-1);
        if (world == null || player.dimension != -1) return;
        long now = world.getTotalWorldTime();
        for (AshCloud cloud : clouds) {
            if (!cloud.armed) continue;
            double scale = cloud.scale(now);
            PacketAshCloud p = packetOf(cloud, ArcanaQuestTweaksConfig.NetherModuleConfig.ashReachHorizontal * scale,
                    ArcanaQuestTweaksConfig.NetherModuleConfig.ashReachVertical * scale);
            if (p != null) ArcanaQuestTweaks.NETWORK.sendTo(p, player);
        }
    }

    // ---- effects ----

    public boolean insideAny(EntityPlayer player) {
        int fx = (int) Math.floor(player.posX);
        int fy = (int) Math.floor(player.posY);
        int fz = (int) Math.floor(player.posZ);
        int ey = (int) Math.floor(player.posY + player.getEyeHeight());
        for (AshCloud cloud : clouds) {
            if (!cloud.armed) continue;
            if (cloud.contains(fx, fy, fz) || cloud.contains(fx, ey, fz)) return true;
        }
        return false;
    }

    private void playerSecond(WorldServer world, long now) {
        for (EntityPlayer player : world.playerEntities) {
            if (player.isCreative() || player.isSpectator() || player.isDead) continue;
            UUID id = player.getUniqueID();
            boolean inside = insideAny(player);
            double current = dose.getOrDefault(id, 0.0D);
            if (!inside && current <= 0.0D && !debt.containsKey(id)) continue;

            MoonfrostTier tier = GearTierRule.tierOf(player,
                    ArcanaQuestTweaksConfig.HazardGearConfig.nether.witherWardEffect,
                    ArcanaQuestTweaksConfig.HazardGearConfig.nether.ashProperGearWhitelist);
            double gain;
            switch (tier) {
                case EXPOSED:
                    gain = ArcanaQuestTweaksConfig.NetherModuleConfig.ashGainExposed;
                    break;
                case PROPER:
                    gain = ArcanaQuestTweaksConfig.NetherModuleConfig.ashGainProper;
                    break;
                default:
                    gain = ArcanaQuestTweaksConfig.NetherModuleConfig.ashGainArmored;
                    break;
            }
            double next = AshDoseTracker.update(current, inside, gain,
                    ArcanaQuestTweaksConfig.NetherModuleConfig.ashThreshold3);
            if (next <= 0.0D) {
                dose.remove(id);
            } else {
                dose.put(id, next);
            }
            int level = AshDoseTracker.level(next, ArcanaQuestTweaksConfig.NetherModuleConfig.ashThreshold1,
                    ArcanaQuestTweaksConfig.NetherModuleConfig.ashThreshold2,
                    ArcanaQuestTweaksConfig.NetherModuleConfig.ashThreshold3);
            if (inside) {
                applyEffects(player, level, now);
            }
            ashLung(player, inside);
        }
    }

    private void applyEffects(EntityPlayer player, int level, long now) {
        if (level >= 1) {
            player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 0, false, false));
            int amp = level - 1;
            PotionEffect existing = player.getActivePotionEffect(MobEffects.WITHER);
            if (existing == null || existing.getAmplifier() <= amp) {
                player.addPotionEffect(new PotionEffect(MobEffects.WITHER, 80, amp, false, true));
            }
        }
        if (level >= 2 && (now / 20L) % 4L == 0L) {
            player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 40, 0, false, false));
        }
    }

    /** Ash lung from the first second: the air bar drains, then drowning-type damage. */
    private void ashLung(EntityPlayer player, boolean inside) {
        UUID id = player.getUniqueID();
        int d = debt.getOrDefault(id, 0);
        if (inside) {
            d = Math.min(320, d + 80);
            debt.put(id, d);
            player.setAir(300 - d);
            if (d >= 300) {
                player.attackEntityFrom(DAMAGE_ASH, 2.0F);
            }
        } else if (d > 0) {
            d = Math.max(0, d - 160);
            if (d == 0) {
                debt.remove(id);
                player.setAir(300);
            } else {
                debt.put(id, d);
                player.setAir(300 - d);
            }
        }
    }

    private void forget(EntityPlayer player) {
        dose.remove(player.getUniqueID());
        debt.remove(player.getUniqueID());
    }

    // ---- lifecycle ----

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.player);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) forget((EntityPlayer) event.getEntityLiving());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) sendAllTo((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        forget(event.player);
        if (event.player instanceof EntityPlayerMP) sendAllTo((EntityPlayerMP) event.player);
    }

    private static void debug(String message) {
        if (ArcanaQuestTweaksConfig.NetherModuleConfig.netherHazardDebug) {
            org.apache.logging.log4j.LogManager.getLogger("AQTweaks-Nether").info("[nether-hazards] " + message);
        }
    }
}
