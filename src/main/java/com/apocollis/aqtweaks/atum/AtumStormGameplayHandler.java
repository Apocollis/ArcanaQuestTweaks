package com.apocollis.aqtweaks.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Handles physical and survival gameplay mechanics when players are exposed
 * to the howling wind path of an Atum sandstorm.
 */
public class AtumStormGameplayHandler {

    private static final String NBT_GRIT_TIMER = "AQTweaks_AtumGritTimer";
    /** Breath missing from a full bar. Vanilla refills air every tick on dry land, so the drain is stored here. */
    private static final String NBT_AIR_DEBT = "AQTweaks_AtumAirDebt";

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;
        World world = player.world;
        WorldProvider provider = world.provider;
        if (provider == null) return;
        if (!(provider instanceof WorldProviderAtum) && provider.getDimension() != 17) return;

        if (!ArcanaQuestTweaksConfig.AtumModuleConfig.enableSandstormGameplayEffects) return;

        boolean isStorming = (provider instanceof WorldProviderAtum) &&
                (((WorldProviderAtum) provider).isStorming || ((WorldProviderAtum) provider).stormStrength > 0.15F);
        float stormStrength = (provider instanceof WorldProviderAtum)
                ? ((WorldProviderAtum) provider).stormStrength
                : (isStorming ? 1.0F : 0.0F);

        if (!isStorming || stormStrength < 0.15F) {
            if (!world.isRemote) {
                if (player.getEntityData().hasKey(NBT_GRIT_TIMER)) {
                    player.getEntityData().removeTag(NBT_GRIT_TIMER);
                }
                recoverBreath(player, 300);
            }
            return;
        }

        Vec3d windVec = AtumWindHelper.getWindVector(world, true);
        boolean sheltered = isSheltered(player, world, windVec);

        if (world.isRemote) {
            // Client-side: smooth physical wind push and velocity adjustments
            handleClientWindPush(player, windVec, stormStrength, sheltered);
        } else if (player instanceof EntityPlayerMP) {
            // Server-side: stamina drain, hunger exhaustion, and grit blindness
            handleServerStormEffects((EntityPlayerMP) player, windVec, stormStrength, sheltered);
        }
    }

    private void handleClientWindPush(EntityPlayer player, Vec3d windVec, float stormStrength, boolean sheltered) {
        if (sheltered || !ArcanaQuestTweaksConfig.AtumModuleConfig.enableWindPush) return;

        double baseForce = ArcanaQuestTweaksConfig.AtumModuleConfig.windPushForce * stormStrength;
        if (player.isSneaking()) {
            baseForce *= 0.30D; // Sneaking/crouching anchors against the wind
        }

        player.motionX += windVec.x * baseForce;
        player.motionZ += windVec.z * baseForce;
    }

    private void handleServerStormEffects(EntityPlayerMP player, Vec3d windVec, float stormStrength, boolean sheltered) {
        if (sheltered) {
            if (player.getEntityData().getInteger(NBT_GRIT_TIMER) > 0) {
                player.getEntityData().setInteger(NBT_GRIT_TIMER, 0);
            }
            recoverBreath(player, 10);
            return;
        }

        // 1. Headwind movement resistance: stamina & hunger exhaustion
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableHeadwindStaminaDrain) {
            double moveX = player.posX - player.prevPosX;
            double moveZ = player.posZ - player.prevPosZ;
            double moveDot = moveX * windVec.x + moveZ * windVec.z;

            // moveDot < 0 means moving against the wind direction
            if (player.isSprinting() && moveDot < -0.04D) {
                player.addExhaustion(0.025F * stormStrength);
                if (player.ticksExisted % 70 == 0 && Loader.isModLoaded("elenaidodge2")) {
                    decreaseStamina(player, 1);
                }
            } else if (moveDot < -0.02D) {
                player.addExhaustion(0.010F * stormStrength);
                if (player.ticksExisted % 160 == 0 && Loader.isModLoaded("elenaidodge2")) {
                    decreaseStamina(player, 1);
                }
            }
        }

        // 2. Stinging sand grit exposure (blindness & slowness when facing headwind unprotected)
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableStingingGritBlindness) {
            if (isProtectedHeadgear(player)) {
                if (player.getEntityData().getInteger(NBT_GRIT_TIMER) > 0) {
                    player.getEntityData().setInteger(NBT_GRIT_TIMER, 0);
                }
            } else {
                Vec3d look = player.getLook(1.0F);
                double lookDot = look.x * windVec.x + look.z * windVec.z;

                // lookDot < -0.45 means facing within ~60 degrees of the incoming gale
                // look.y > -0.50 means looking ahead/up rather than shielding eyes by looking down at sand
                if (lookDot < -0.45D && look.y > -0.50D && !player.isSneaking()) {
                    int timer = player.getEntityData().getInteger(NBT_GRIT_TIMER) + 1;
                    int maxTicks = (int) (ArcanaQuestTweaksConfig.AtumModuleConfig.gritExposureSecondsBeforeBlindness * 20.0D);

                    if (timer >= maxTicks) {
                        player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 0, false, false));
                        player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 0, false, false));
                        timer = maxTicks / 2; // Reapply every 1.5s if player keeps staring into the gale
                    }
                    player.getEntityData().setInteger(NBT_GRIT_TIMER, timer);
                } else {
                    int timer = player.getEntityData().getInteger(NBT_GRIT_TIMER);
                    if (timer > 0) {
                        player.getEntityData().setInteger(NBT_GRIT_TIMER, Math.max(0, timer - 2));
                    }
                }
            }
        }

        // 3. Sand inhalation. Headgear or the shelter return above refills the bar.
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableSandInhalationChoking) {
            if (isProtectedHeadgear(player)) {
                recoverBreath(player, 10);
            } else {
                int debt = Math.min(320, player.getEntityData().getInteger(NBT_AIR_DEBT) + 4);
                player.getEntityData().setInteger(NBT_AIR_DEBT, debt);
                player.setAir(300 - debt);
                if (debt >= 300 && player.ticksExisted % 20 == 0) {
                    player.attackEntityFrom(DamageSource.DROWN, 2.0F);
                    player.world.playSound(null, player.posX, player.posY, player.posZ,
                            SoundEvents.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 0.8F, 1.0F);
                    player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 0, false, false));
                    player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 0, false, false));
                }
            }
        }

        // 4. Flesh abrasion when the body is not covered.
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableAbrasiveSandScour
                && player.ticksExisted % 80 == 0
                && isBodyScoured(player)) {
            player.attackEntityFrom(DamageSource.GENERIC, 1.0F);
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_SAND_HIT, SoundCategory.PLAYERS, 0.6F, 0.8F);
        }

        // 5. Low howl while standing in a strong storm.
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableDesertStormAmbience
                && stormStrength > 0.40F
                && player.world.rand.nextInt(240) == 0) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.AMBIENT_CAVE, SoundCategory.AMBIENT, 0.35F, 0.55F + player.world.rand.nextFloat() * 0.2F);
        }
    }

    private static void recoverBreath(EntityPlayer player, int amount) {
        if (!player.getEntityData().hasKey(NBT_AIR_DEBT) && amount < 300) return;
        int debt = Math.max(0, player.getEntityData().getInteger(NBT_AIR_DEBT) - amount);
        if (debt == 0) {
            player.getEntityData().removeTag(NBT_AIR_DEBT);
            player.setAir(300);
        } else {
            player.getEntityData().setInteger(NBT_AIR_DEBT, debt);
            player.setAir(300 - debt);
        }
    }

    /**
     * Checks if the player is in shelter (indoors/underground) or in the leeward wind shadow
     * behind a solid dune, wall, pyramid step, or cliff.
     */
    public static boolean isSheltered(EntityPlayer player, World world, Vec3d windVec) {
        BlockPos eyePos = new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        if (!world.canSeeSky(eyePos)) {
            return true; // Indoors or underground
        }

        double shelterDist = ArcanaQuestTweaksConfig.AtumModuleConfig.shelterWindShadowDistance;

        // Check eye height upwind
        Vec3d eyeVec = player.getPositionEyes(1.0F);
        Vec3d upEye = new Vec3d(eyeVec.x - windVec.x * shelterDist, eyeVec.y, eyeVec.z - windVec.z * shelterDist);
        RayTraceResult rayEye = world.rayTraceBlocks(upEye, eyeVec, false, true, false);
        if (rayEye != null && rayEye.typeOfHit == RayTraceResult.Type.BLOCK) {
            return true;
        }

        // Check torso height upwind (allows crouching/standing behind 1-block low dune crests or walls)
        Vec3d torsoVec = new Vec3d(player.posX, player.posY + 0.8D, player.posZ);
        Vec3d upTorso = new Vec3d(torsoVec.x - windVec.x * shelterDist, torsoVec.y, torsoVec.z - windVec.z * shelterDist);
        RayTraceResult rayTorso = world.rayTraceBlocks(upTorso, torsoVec, false, true, false);
        return rayTorso != null && rayTorso.typeOfHit == RayTraceResult.Type.BLOCK;
    }

    /**
     * Checks if the player is wearing headgear that protects against stinging sand grit.
     */
    public static boolean isProtectedHeadgear(EntityPlayer player) {
        ItemStack helmet = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (helmet.isEmpty()) return false;

        ResourceLocation reg = helmet.getItem().getRegistryName();
        if (reg == null) return false;
        String name = reg.toString();

        for (String whitelisted : ArcanaQuestTweaksConfig.AtumModuleConfig.protectiveHeadgear) {
            if (name.equalsIgnoreCase(whitelisted)) {
                return true;
            }
        }

        if ("atum".equals(reg.getNamespace())) {
            String path = reg.getPath();
            if (path.contains("helmet") || path.contains("bandage") || path.contains("mask") || path.contains("wanderer")) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when chest, legs, or boots are missing, or total armor is under 10.
     * A full Atum wanderer or mummy set on those three slots is enough on its own.
     */
    private static boolean isBodyScoured(EntityPlayer player) {
        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        ItemStack legs = player.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
        ItemStack boots = player.getItemStackFromSlot(EntityEquipmentSlot.FEET);
        if (chest.isEmpty() || legs.isEmpty() || boots.isEmpty()) return true;
        if (isAtumCloth(chest) && isAtumCloth(legs) && isAtumCloth(boots)) return false;
        return player.getTotalArmorValue() < 10;
    }

    private static boolean isAtumCloth(ItemStack stack) {
        ResourceLocation reg = stack.getItem().getRegistryName();
        if (reg == null || !"atum".equals(reg.getNamespace())) return false;
        String path = reg.getPath();
        return path.contains("wanderer") || path.contains("mummy");
    }

    private static void decreaseStamina(EntityPlayerMP player, int amount) {
        try {
            com.elenai.elenaidodge2.api.FeathersHelper.decreaseFeathers(player, amount);
        } catch (Throwable ignored) {
        }
    }
}
