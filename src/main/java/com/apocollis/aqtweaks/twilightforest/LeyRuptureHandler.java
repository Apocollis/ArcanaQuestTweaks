package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.comfort.RestedBonuses;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.apocollis.aqtweaks.nether.LavaSpoutHandler;
import com.apocollis.aqtweaks.nether.LoadedBlocks;
import com.apocollis.aqtweaks.network.PacketLeyExposure;
import com.apocollis.aqtweaks.thaumcraft.ThaumcraftHelper;
import com.apocollis.aqtweaks.weather.GearTierRule;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The Ley-Rupture in the Twilight Forest (hazard channel {@code rupture}). Once a second per player it computes
 * the exposure factor (surface-relative height gradient, zeroed by a shelter structure's bounds or a Comfort
 * room), sends it to the client for the vignette, and during the active phase applies the magic scour (two tiers,
 * every 4 s after a short grace), hunger burn, the biome extra and the capped warp, charges carried items and
 * placed Vis Batteries, and surfaces impermanent ley mobs.
 */
public class LeyRuptureHandler {

    public static final DamageSource DAMAGE_LEY = new DamageSource("aqtweaks_ley_rupture").setDamageBypassesArmor()
            .setMagicDamage();
    private static final String TAG_LEY_MOB = "AQTLeyMob";

    public VisBatteryTracker visBatteries;

    private final Random rand = new Random();
    private final Map<UUID, Double> exposure = new HashMap<>();
    private final Map<UUID, Float> sent = new HashMap<>();
    private final Map<UUID, Long> exposedSince = new HashMap<>();
    private final Map<UUID, Long> nextHit = new HashMap<>();
    private final Map<UUID, Integer> hazardWarp = new HashMap<>();
    private final Map<UUID, Integer> chargeWarp = new HashMap<>();
    private final Set<UUID> leyMobs = new HashSet<>();
    private long eventId = Long.MIN_VALUE;

    /** Exposure from the last server pass, 0 when unknown or sheltered. */
    public double exposureOf(EntityPlayer player) {
        return exposure.getOrDefault(player.getUniqueID(), 0.0D);
    }

    /** True while the active phase of a surge is running in the player's world. */
    public boolean active(EntityPlayer player) {
        return Hazards.state(player.world, LeyChannels.RUPTURE).phase == HazardEvent.Phase.ACTIVE;
    }

    public int leyMobCount() {
        return leyMobs.size();
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || !(event.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) event.world;
        if (!LeyChannels.isTf(world)) return;
        if (!ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.enable) return;
        long now = world.getTotalWorldTime();
        HazardState state = Hazards.state(world, LeyChannels.RUPTURE);
        if (state.event != null && state.event.id != eventId) {
            eventId = state.event.id;
            hazardWarp.clear();
            chargeWarp.clear();
            exposedSince.clear();
            nextHit.clear();
        }
        if (state.phase != HazardEvent.Phase.ACTIVE && !leyMobs.isEmpty()) {
            dissipateMobs(world);
        }
        if (now % 20L != 0L) return;
        for (EntityPlayer player : world.playerEntities) {
            if (player instanceof EntityPlayerMP) {
                playerSecond(world, (EntityPlayerMP) player, state, now);
            }
        }
        ArcanaQuestTweaksConfig.LeyRupture c = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley;
        if (visBatteries != null && state.phase == HazardEvent.Phase.ACTIVE
                && now % (c.chargePulseSeconds * 20L) == 0L) {
            visBatteries.pulse(world);
        }
    }

    // ---- per player ----

    private void playerSecond(WorldServer world, EntityPlayerMP player, HazardState state, long now) {
        UUID id = player.getUniqueID();
        ArcanaQuestTweaksConfig.LeyRupture c = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley;
        double f = 0.0D;
        String biome = "twilightForest";
        if (state.phase != HazardEvent.Phase.CLEAR && !player.isCreative() && !player.isSpectator() && !player.isDead) {
            BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
            boolean sheltered = RestedBonuses.restedTier(player) > 0 || LeyTerrain.structureShelter(world, pos);
            if (!sheltered) {
                f = LeyTerrain.heightExposure(world, player.posX, player.posY, player.posZ);
            }
            biome = LeyTerrain.biomeName(world, player.posX, player.posZ);
        }
        exposure.put(id, f);
        sendExposure(player, state.phase == HazardEvent.Phase.ACTIVE ? (float) f : 0.0F);

        if (state.phase != HazardEvent.Phase.ACTIVE || f <= 0.01D) {
            exposedSince.remove(id);
            return;
        }
        double intensity = LeyTerrain.intensity(biome);
        boolean proper = GearTierRule.allSlotsOnList(player, c.properGear);
        Long since = exposedSince.putIfAbsent(id, now);
        long exposedFor = since == null ? 0L : now - since;

        // Hunger burn: every second once exposed.
        player.addExhaustion((float) (c.hungerBurn * f * intensity * (proper ? 0.4D : 1.0D)));

        if (exposedFor >= c.graceTicks && now >= nextHit.getOrDefault(id, 0L)) {
            nextHit.put(id, now + 80L);
            scour(world, player, biome, f, intensity, proper);
        }
        if (c.enableCharging && f >= c.chargeMinExposure && now % (c.chargePulseSeconds * 20L) == 0L) {
            if (SurgeCharging.pulse(player, f, intensity)) {
                giveWarp(player, chargeWarp, c.chargeWarp, c.chargeWarpCap);
            }
        }
        if (c.enableLeyMobs && now % (c.mobPulseSeconds * 20L) == 0L && rand.nextDouble() < c.mobChance) {
            spawnMobs(world, player);
        }
    }

    private void scour(WorldServer world, EntityPlayerMP player, String biome, double f, double intensity,
                       boolean proper) {
        ArcanaQuestTweaksConfig.LeyRupture c = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley;
        double damage = SurgeMath.scour(proper, c.scourUnprotected, c.scourProper, f, intensity);
        if (damage >= 0.25D) {
            player.attackEntityFrom(DAMAGE_LEY, (float) damage);
        }
        BiomeIntensity.Extra extra = LeyTerrain.extra(biome);
        if (extra != null) {
            applyExtra(world, player, extra);
        }
        if (rand.nextDouble() < c.warpChance * f) {
            giveWarp(player, hazardWarp, 1, c.warpCapPerSurge);
        }
        world.spawnParticle(EnumParticleTypes.SPELL_WITCH, player.posX, player.posY + 0.2D, player.posZ, 12, 0.4D, 0.2D,
                0.4D, 0.05D);
    }

    private void applyExtra(WorldServer world, EntityPlayerMP player, BiomeIntensity.Extra extra) {
        if ("fire".equalsIgnoreCase(extra.effect)) {
            if (!LavaSpoutHandler.isFireProtected(player)) player.setFire(Math.max(1, extra.ticks / 20));
            return;
        }
        Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(extra.effect));
        if (potion == null) return;
        if (potion == MobEffects.LEVITATION) {
            // Arcane levity only with head clearance, so it cannot pin a player under a ceiling.
            for (int dy = 2; dy <= 5; dy++) {
                IBlockState above = LoadedBlocks.peek(world, (int) Math.floor(player.posX),
                        (int) Math.floor(player.posY) + dy, (int) Math.floor(player.posZ));
                if (above == null || above.getBlock() != Blocks.AIR) return;
            }
        }
        player.addPotionEffect(new PotionEffect(potion, extra.ticks, extra.amplifier, false, true));
    }

    /** Gives temporary warp within the per-surge cap held in {@code ledger}. Returns what was given. */
    public int giveWarp(EntityPlayer player, Map<UUID, Integer> ledger, int wanted, int cap) {
        if (!Loader.isModLoaded("thaumcraft") || !ThaumcraftHelper.available()) return 0;
        int already = ledger.getOrDefault(player.getUniqueID(), 0);
        int allowed = SurgeMath.warpAllowed(already, wanted, cap);
        if (allowed <= 0) return 0;
        ledger.put(player.getUniqueID(), already + allowed);
        ThaumcraftHelper.addWarp(player, 1, allowed);
        ThaumcraftHelper.syncWarp(player);
        return allowed;
    }

    /** The backlash shares the hazard warp ledger (one point a surge by default). */
    public int giveBacklashWarp(EntityPlayer player, int wanted) {
        return giveWarp(player, hazardWarp, wanted,
                ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.warpCapPerSurge);
    }

    private void sendExposure(EntityPlayerMP player, float value) {
        Float last = sent.get(player.getUniqueID());
        if (last != null && Math.abs(last - value) < 0.03F && (value > 0.0F) == (last > 0.0F)) return;
        sent.put(player.getUniqueID(), value);
        ArcanaQuestTweaks.NETWORK.sendTo(new PacketLeyExposure(value), player);
    }

    // ---- ley-touched mobs ----

    private void spawnMobs(WorldServer world, EntityPlayerMP player) {
        ArcanaQuestTweaksConfig.LeyRupture c = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley;
        if (c.mobPool.length == 0 || leyMobs.size() >= c.mobsTotal) return;
        int near = 0;
        for (UUID mobId : leyMobs) {
            Entity mob = world.getEntityFromUuid(mobId);
            if (mob != null && mob.getDistanceSq(player) < 48.0D * 48.0D) near++;
        }
        int count = 1 + rand.nextInt(2);
        for (int i = 0; i < count && near < c.mobsPerPlayer && leyMobs.size() < c.mobsTotal; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double dist = 12.0D + rand.nextDouble() * 12.0D;
            int x = (int) Math.floor(player.posX + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.posZ + Math.sin(angle) * dist);
            int y = groundY(world, x, (int) player.posY, z);
            if (y < 0) continue;
            String id = c.mobPool[rand.nextInt(c.mobPool.length)];
            Entity entity = EntityList.createEntityByIDFromName(new ResourceLocation(id.trim()), world);
            if (!(entity instanceof EntityLiving)) continue;
            entity.setLocationAndAngles(x + 0.5D, y, z + 0.5D, rand.nextFloat() * 360.0F, 0.0F);
            entity.getEntityData().setBoolean(TAG_LEY_MOB, true);
            ((EntityLiving) entity).addPotionEffect(new PotionEffect(MobEffects.GLOWING, 1200, 0, false, false));
            world.spawnEntity(entity);
            leyMobs.add(entity.getUniqueID());
            near++;
            world.spawnParticle(EnumParticleTypes.PORTAL, x + 0.5D, y + 0.5D, z + 0.5D, 20, 0.4D, 0.8D, 0.4D, 0.1D);
        }
    }

    /** Standing Y (air with two free cells above a solid floor) near the given Y, or -1. */
    private static int groundY(WorldServer world, int x, int nearY, int z) {
        for (int y = nearY + 16; y >= nearY - 16; y--) {
            IBlockState floor = LoadedBlocks.peek(world, x, y - 1, z);
            IBlockState here = LoadedBlocks.peek(world, x, y, z);
            IBlockState head = LoadedBlocks.peek(world, x, y + 1, z);
            if (floor == null || here == null || head == null) return -1;
            if (floor.getMaterial().isSolid() && !floor.getMaterial().isLiquid() && here.getBlock() == Blocks.AIR
                    && head.getBlock() == Blocks.AIR) {
                return y;
            }
        }
        return -1;
    }

    public void dissipateMobs(WorldServer world) {
        for (UUID id : new ArrayList<>(leyMobs)) {
            Entity mob = world.getEntityFromUuid(id);
            if (mob != null && !mob.isDead) {
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH, mob.posX, mob.posY + 0.8D, mob.posZ, 25, 0.4D, 0.6D,
                        0.4D, 0.1D);
                mob.setDead();
            }
        }
        leyMobs.clear();
    }

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (event.getEntityLiving().getEntityData().getBoolean(TAG_LEY_MOB)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onXp(LivingExperienceDropEvent event) {
        if (event.getEntityLiving().getEntityData().getBoolean(TAG_LEY_MOB)) {
            event.setDroppedExperience(0);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.player.getUniqueID();
        exposure.remove(id);
        sent.remove(id);
        exposedSince.remove(id);
        nextHit.remove(id);
    }

    /** Ends everything: used by the {@code clear} command. */
    public void reset(WorldServer world) {
        dissipateMobs(world);
        exposedSince.clear();
        nextHit.clear();
    }
}
