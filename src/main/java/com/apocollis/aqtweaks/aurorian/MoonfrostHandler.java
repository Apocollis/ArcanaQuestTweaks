package com.apocollis.aqtweaks.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.comfort.RestedBonuses;
import com.apocollis.aqtweaks.network.PacketMoonfrostState;
import com.apocollis.aqtweaks.simpledifficulty.SimpleDifficultyHelper;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.weather.ShelterHold;
import com.apocollis.aqtweaks.weather.StormShelter;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * The Aurorian Moonfrost storm, the cold twin of {@code AtumStormGameplayHandler}. Registered only if
 * {@code theaurorian} is loaded.
 *
 * <p>Server, once a second per player: shelter, rime, frostbite (every 4 s), headwind exhaustion and the
 * Elenai regeneration slowdown. Client, every tick: the wind push for the local player. Nothing here touches
 * max stamina: regeneration is slowed through Elenai's own regen modifier, with the applied delta kept in
 * entity data and removed on shelter, storm end, leaving the dimension, logout and death.
 */
public class MoonfrostHandler {

    public static final DamageSource DAMAGE_FROST = new DamageSource("aurorian_frost").setDamageBypassesArmor();

    static final String NBT_REGEN_DELTA = "AQTMoonfrostRegenDelta";
    private static final String NBT_RIME = "AQTRime";
    private static final String NBT_RIME_SENT = "AQTRimeSent";
    private static final String NBT_POS_X = "AQTRimeX";
    private static final String NBT_POS_Z = "AQTRimeZ";
    private static final String NBT_WARM = "AQTMoonfrostWarm";
    private static final String NBT_WARM_TICK = "AQTMoonfrostWarmTick";

    /** Storms below this strength do not hurt. */
    public static final float ACTIVE_STRENGTH = 0.15F;
    private static final int WARMTH_RADIUS = 4;

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        World world = player.world;
        if (world == null || player.isDead) return;

        if (!MoonfrostState.isAurorian(world)) {
            if (!world.isRemote && player.ticksExisted % 20 == 0 && player instanceof EntityPlayerMP) {
                cleanupOutside((EntityPlayerMP) player);
            }
            return;
        }

        HazardState state = MoonfrostState.get(world);
        if (world.isRemote) {
            clientTick(player, world, state);
        } else if (player instanceof EntityPlayerMP) {
            MoonfrostSnowHandler.holdAir((EntityPlayerMP) player);
            if (player.ticksExisted % 20 == 0) {
                MoonfrostSnowHandler.burialSecond((EntityPlayerMP) player);
                serverSecond((EntityPlayerMP) player, world, state);
            }
        }
    }

    // ---- client ----

    private void clientTick(EntityPlayer player, World world, HazardState state) {
        if (!player.isUser() || state.strength < ACTIVE_STRENGTH) return;
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.enableWindPush) return;
        // Like the Atum sandstorm, the push also moves gliding and creative-flying players (no creative exemption).
        Vec3d wind = AurorianWindHelper.getWindVector(world, true);
        if (isShelteredHeld(player, world, wind)) return;

        double force = ArcanaQuestTweaksConfig.AurorianModuleConfig.windPushForce * state.strength;
        force *= com.apocollis.aqtweaks.weather.WindResistanceGear.factor(player, 0.70D);
        player.motionX += wind.x * force;
        player.motionZ += wind.z * force;
    }

    // ---- server ----

    private void serverSecond(EntityPlayerMP player, World world, HazardState state) {
        boolean active = state.strength >= ACTIVE_STRENGTH && !player.isCreative() && !player.isSpectator();
        NBTTagCompound data = player.getEntityData();

        Vec3d wind = AurorianWindHelper.getWindVector(world, true);
        boolean sheltered = true;
        if (active) {
            sheltered = isShelteredHeld(player, world, wind);
        }
        boolean exposed = active && !sheltered;
        MoonfrostTier tier = exposed ? MoonfrostGear.tierOf(player) : MoonfrostTier.ARMORED;

        double dx = 0.0D;
        double dz = 0.0D;
        boolean hasLast = data.hasKey(NBT_POS_X);
        if (hasLast) {
            dx = player.posX - data.getDouble(NBT_POS_X);
            dz = player.posZ - data.getDouble(NBT_POS_Z);
        }
        data.setDouble(NBT_POS_X, player.posX);
        data.setDouble(NBT_POS_Z, player.posZ);

        int stack = updateRime(player, data, exposed, sheltered, hasLast, dx, dz, tier);

        if (exposed && ArcanaQuestTweaksConfig.AurorianModuleConfig.enableFrostbite && player.ticksExisted % 80 == 0) {
            float damage = MoonfrostGear.frostbiteDamage(tier);
            if (RimeTracker.doublesDamage(stack)) {
                damage *= 2.0F;
            }
            if (damage > 0.0F) {
                player.attackEntityFrom(DAMAGE_FROST, damage);
                world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.BLOCK_SNOW_HIT, SoundCategory.PLAYERS, 0.7F, 0.7F);
            }
        }

        if (exposed && ArcanaQuestTweaksConfig.AurorianModuleConfig.enableHeadwindStaminaDrain && hasLast) {
            headwind(player, wind, state.strength, dx, dz);
        }

        if (exposed && state.strength > 0.40F && world.rand.nextInt(12) == 0) {
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.AMBIENT_CAVE, SoundCategory.AMBIENT, 0.35F, 0.50F + world.rand.nextFloat() * 0.15F);
        }

        applyRegenDelta(player, data, exposed && ArcanaQuestTweaksConfig.AurorianModuleConfig.enableRegenPause
                ? regenTarget(tier) : 0);
    }

    private int updateRime(EntityPlayerMP player, NBTTagCompound data, boolean exposed, boolean sheltered,
                           boolean hasLast, double dx, double dz, MoonfrostTier tier) {
        double rime = data.getDouble(NBT_RIME);
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.enableRime) {
            if (rime > 0.0D) {
                rime = 0.0D;
                data.removeTag(NBT_RIME);
            }
            sendRime(player, data, 0);
            return 0;
        }
        if (rime <= 0.0D && !exposed) {
            sendRime(player, data, 0);
            return 0;
        }

        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        double speedValue = speed != null ? speed.getAttributeValue() : 0.1D;
        double moved = Math.sqrt(dx * dx + dz * dz);
        boolean moving = !hasLast || RimeTracker.isMoving(moved, speedValue,
                ArcanaQuestTweaksConfig.AurorianModuleConfig.rimeMoveThresholdFactor);
        boolean warm = !exposed && rime > 0.0D && warmthNearby(player, player.world);
        boolean thawing = sheltered || !exposed;
        rime = RimeTracker.step(rime, moving, thawing, warm, MoonfrostGear.rimeGain(tier));
        if (rime <= 0.0D) {
            data.removeTag(NBT_RIME);
        } else {
            data.setDouble(NBT_RIME, rime);
        }

        int stack = RimeTracker.stack(rime);
        int slow = RimeTracker.slownessAmplifier(stack);
        if (slow >= 0) {
            player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, slow, false, false));
        }
        int fatigue = RimeTracker.fatigueAmplifier(stack);
        if (fatigue >= 0) {
            player.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 40, fatigue, false, false));
        }
        sendRime(player, data, stack);
        return stack;
    }

    private static void sendRime(EntityPlayerMP player, NBTTagCompound data, int stack) {
        if (data.getInteger(NBT_RIME_SENT) != stack) {
            data.setInteger(NBT_RIME_SENT, stack);
            ArcanaQuestTweaks.NETWORK.sendTo(new PacketMoonfrostState(stack), player);
        }
    }

    private void headwind(EntityPlayerMP player, Vec3d wind, float strength, double dx, double dz) {
        double moveDot = dx * wind.x + dz * wind.z; // blocks over the last second; negative = into the wind
        if (player.isSprinting() && moveDot < -0.8D) {
            player.addExhaustion(0.5F * strength);
            if (player.ticksExisted % 80 == 0) {
                decreaseStamina(player, 1);
            }
        } else if (moveDot < -0.4D) {
            player.addExhaustion(0.2F * strength);
            if (player.ticksExisted % 160 == 0) {
                decreaseStamina(player, 1);
            }
        }
    }

    // ---- shelter ----

    /** Roof, upwind wall, a lit campfire within 4 blocks, or a Comfort room (Rested). */
    public static boolean isShelteredFull(EntityPlayer player, World world, Vec3d wind) {
        // Leaves do not count as a roof, so walking under a tree does not switch the storm off.
        if (StormShelter.isSheltered(player, world, wind,
                ArcanaQuestTweaksConfig.AurorianModuleConfig.shelterWindShadowDistance, true)) {
            return true;
        }
        return RestedBonuses.restedTier(player) > 0 || warmthNearby(player, world);
    }

    private static final ShelterHold CLIENT_HOLD = new ShelterHold();
    private static final java.util.Map<java.util.UUID, ShelterHold> SERVER_HOLDS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** {@link #isShelteredFull} held for {@code Shelter Change Delay Seconds} before it changes. */
    public static boolean isShelteredHeld(EntityPlayer player, World world, Vec3d wind) {
        boolean raw = isShelteredFull(player, world, wind);
        long delay = (long) (ArcanaQuestTweaksConfig.AurorianModuleConfig.shelterChangeDelaySeconds * 20.0D);
        ShelterHold hold = world.isRemote ? CLIENT_HOLD
                : SERVER_HOLDS.computeIfAbsent(player.getUniqueID(), k -> new ShelterHold());
        return hold.update(raw, world.getTotalWorldTime(), delay);
    }

    private static void forgetHold(EntityPlayer player) {
        SERVER_HOLDS.remove(player.getUniqueID());
    }

    /** Lit campfire within 4 blocks. Cached for 10 ticks in entity data, so the client can ask every tick. */
    public static boolean warmthNearby(EntityPlayer player, World world) {
        NBTTagCompound data = player.getEntityData();
        int now = player.ticksExisted;
        int last = data.getInteger(NBT_WARM_TICK);
        if (data.hasKey(NBT_WARM_TICK) && now >= last && now - last < 10) {
            return data.getBoolean(NBT_WARM);
        }
        boolean warm = false;
        if (Loader.isModLoaded("simpledifficulty")) {
            BlockPos origin = new BlockPos(player);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            scan:
            for (int x = -WARMTH_RADIUS; x <= WARMTH_RADIUS; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -WARMTH_RADIUS; z <= WARMTH_RADIUS; z++) {
                        if (x * x + y * y + z * z > WARMTH_RADIUS * WARMTH_RADIUS) continue;
                        pos.setPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        if (!world.isBlockLoaded(pos)) continue;
                        if (SimpleDifficultyHelper.isBurningCampfire(world.getBlockState(pos))) {
                            warm = true;
                            break scan;
                        }
                    }
                }
            }
        }
        data.setInteger(NBT_WARM_TICK, now);
        data.setBoolean(NBT_WARM, warm);
        return warm;
    }

    // ---- Elenai regeneration ----

    private static int regenBase() {
        int base = 30;
        try {
            int rate = com.elenai.elenaidodge2.ModConfig.common.feathers.rate;
            if (rate > 0) {
                base = rate;
            }
        } catch (Throwable ignored) {
        }
        return base;
    }

    private static int regenTarget(MoonfrostTier tier) {
        double multiplier = Math.max(1.0D, MoonfrostGear.regenMultiplier(tier));
        return (int) Math.round(regenBase() * (multiplier - 1.0D));
    }

    /** A positive delta lengthens the interval between regenerated feathers (slower). */
    private static void applyRegenDelta(EntityPlayerMP player, NBTTagCompound data, int target) {
        if (!Loader.isModLoaded("elenaidodge2")) return;
        int applied = data.getInteger(NBT_REGEN_DELTA);
        int diff = target - applied;
        if (diff == 0) return;
        try {
            if (diff > 0) {
                com.elenai.elenaidodge2.api.FeathersHelper.increaseRegenModifier(player, diff);
            } else {
                com.elenai.elenaidodge2.api.FeathersHelper.decreaseRegenModifier(player, -diff);
            }
        } catch (Throwable ignored) {
            return;
        }
        if (target == 0) {
            data.removeTag(NBT_REGEN_DELTA);
        } else {
            data.setInteger(NBT_REGEN_DELTA, target);
        }
    }

    private static void decreaseStamina(EntityPlayerMP player, int amount) {
        if (!Loader.isModLoaded("elenaidodge2")) return;
        try {
            com.elenai.elenaidodge2.api.FeathersHelper.decreaseFeathers(player, amount);
        } catch (Throwable ignored) {
        }
    }

    // ---- cleanup ----

    /** Outside the Aurorian: drop leftover rime and any regen delta. */
    private static void cleanupOutside(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        MoonfrostSnowHandler.clearBurial(player);
        if (data.hasKey(NBT_REGEN_DELTA)) {
            applyRegenDelta(player, data, 0);
        }
        if (data.hasKey(NBT_RIME) || data.hasKey(NBT_POS_X) || data.hasKey(NBT_RIME_SENT)) {
            data.removeTag(NBT_RIME);
            data.removeTag(NBT_POS_X);
            data.removeTag(NBT_POS_Z);
            sendRime(player, data, 0);
            data.removeTag(NBT_RIME_SENT);
        }
    }

    private static void clearAll(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        applyRegenDelta(player, data, 0);
        data.removeTag(NBT_RIME);
        data.removeTag(NBT_POS_X);
        data.removeTag(NBT_POS_Z);
        data.removeTag(NBT_RIME_SENT);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP) {
            clearAll((EntityPlayerMP) event.getEntityLiving());
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        forgetHold(event.player);
        if (event.player instanceof EntityPlayerMP) {
            clearAll((EntityPlayerMP) event.player);
        }
    }



}
