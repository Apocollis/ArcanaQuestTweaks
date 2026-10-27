package com.apocollis.aqtweaks.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.teammetallurgy.atum.entity.bandit.EntityBanditBase;
import com.teammetallurgy.atum.entity.efreet.EntityEfreetBase;
import com.teammetallurgy.atum.entity.stone.EntityStoneBase;
import com.teammetallurgy.atum.entity.undead.EntityUndeadBase;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
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
 * Handles physical and survival gameplay mechanics when players and mobs are exposed
 * to the howling wind path of an Atum sandstorm.
 */
public class AtumStormGameplayHandler {

    private static final String NBT_GRIT_TIMER = "AQTweaks_AtumGritTimer";
    /** Breath missing from a full bar. Vanilla refills air every tick on dry land, so the drain is stored here. */
    private static final String NBT_AIR_DEBT = "AQTweaks_AtumAirDebt";
    private static final String NBT_MOB_SHELTERED = "AQTweaks_StormSheltered";

    public static final DamageSource DAMAGE_SANDSTORM = new DamageSource("atum_sandstorm").setDamageBypassesArmor();

    private static final EntityEquipmentSlot[] ARMOR_SLOTS = new EntityEquipmentSlot[] {
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET
    };

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity == null || entity.isDead) return;

        World world = entity.world;
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
            if (!world.isRemote && entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (player.getEntityData().hasKey(NBT_GRIT_TIMER)) {
                    player.getEntityData().removeTag(NBT_GRIT_TIMER);
                }
                recoverBreath(player, 300);
            }
            return;
        }

        Vec3d windVec = AtumWindHelper.getWindVector(world, true);

        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            boolean sheltered = isSheltered(player, world, windVec);

            if (world.isRemote) {
                // Client-side: smooth physical wind push and velocity adjustments
                handleClientWindPush(player, windVec, stormStrength, sheltered);
            } else if (player instanceof EntityPlayerMP) {
                // Server-side: stamina drain, hunger exhaustion, grit blindness, choking, 3-tier scour, armor wear
                handleServerPlayerEffects((EntityPlayerMP) player, windVec, stormStrength, sheltered);
            }
        } else if (!world.isRemote && ArcanaQuestTweaksConfig.AtumModuleConfig.enableSandstormMobEffects) {
            // Server-side mob handling
            handleServerMobEffects(entity, world, windVec, stormStrength);
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

    private void handleServerPlayerEffects(EntityPlayerMP player, Vec3d windVec, float stormStrength, boolean sheltered) {
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

        // 4. Three-tier sand scour damage (every 80 ticks / 4 seconds)
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableAbrasiveSandScour && player.ticksExisted % 80 == 0) {
            float damage = getScourDamage(player);
            if (damage > 0.0F) {
                player.attackEntityFrom(DAMAGE_SANDSTORM, damage);
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.BLOCK_SAND_HIT, SoundCategory.PLAYERS, 0.6F, 0.8F);
            }
        }

        // 5. Sandblasting armor durability wear
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableArmorAbrasionWear) {
            int wearInterval = Math.max(1, ArcanaQuestTweaksConfig.AtumModuleConfig.abrasionWearIntervalSeconds) * 20;
            if (player.ticksExisted % wearInterval == 0) {
                damageArmorDurability(player);
            }
        }

        // 6. Low howl while standing in a strong storm.
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableDesertStormAmbience
                && stormStrength > 0.40F
                && player.world.rand.nextInt(240) == 0) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.AMBIENT_CAVE, SoundCategory.AMBIENT, 0.35F, 0.55F + player.world.rand.nextFloat() * 0.2F);
        }
    }

    private void handleServerMobEffects(EntityLivingBase entity, World world, Vec3d windVec, float stormStrength) {
        if (entity.noClip || !entity.isEntityAlive()) return;

        boolean sheltered = isMobSheltered(entity, world, windVec);
        if (sheltered) return;

        // 1. Physical wind push
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableWindPush) {
            double baseForce = ArcanaQuestTweaksConfig.AtumModuleConfig.windPushForce * stormStrength;
            IAttributeInstance kbAttr = entity.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
            double kb = kbAttr != null ? kbAttr.getAttributeValue() : 0.0D;
            if (kb < 1.0D) {
                double force = baseForce * (1.0D - kb);
                entity.motionX += windVec.x * force;
                entity.motionZ += windVec.z * force;
                entity.velocityChanged = true;
            }
        }

        // 2. Sand scour damage & slowness every 80 ticks (4 seconds)
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableAbrasiveSandScour && entity.ticksExisted % 80 == 0) {
            applyMobScour(entity, world);
        }

        // 3. Sandblasting armor durability wear
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableArmorAbrasionWear) {
            int wearInterval = Math.max(1, ArcanaQuestTweaksConfig.AtumModuleConfig.abrasionWearIntervalSeconds) * 20;
            if (entity.ticksExisted % wearInterval == 0) {
                if (!(entity instanceof EntityBanditBase)) {
                    damageArmorDurability(entity);
                }
            }
        }
    }

    private void applyMobScour(EntityLivingBase entity, World world) {
        // Atum supernatural, construct, and fauna immunity
        if (entity instanceof EntityUndeadBase
                || entity instanceof EntityStoneBase
                || entity instanceof EntityEfreetBase
                || entity.getClass().getName().startsWith("com.teammetallurgy.atum.entity.animal.")) {
            return;
        }

        float damage;
        boolean applySlowness = false;

        // Atum bandit / brigand mortal inhabitants: treated as wearing full Atum gear (2.0 dmg / 4s)
        if (entity instanceof EntityBanditBase) {
            damage = (float) ArcanaQuestTweaksConfig.AtumModuleConfig.scourDamageAtumArmor;
        } else {
            // Non-native / foreign / vanilla / modded mobs
            damage = getScourDamage(entity);
            applySlowness = true;
        }

        if (damage > 0.0F) {
            entity.attackEntityFrom(DAMAGE_SANDSTORM, damage);
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                    SoundEvents.BLOCK_SAND_HIT, SoundCategory.HOSTILE, 0.6F, 0.8F);
            if (applySlowness) {
                entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 0, false, false));
            }
        }
    }

    /**
     * Computes the 3-tier scour damage dealt every 4 seconds (80 ticks):
     * - Exposed (missing any of HEAD, CHEST, LEGS, FEET): 6.0 damage
     * - Fully Armored (all 4 slots filled with standard armor): 4.0 damage
     * - Full Atum / Whitelisted Set (all 4 slots resistant): 2.0 damage
     */
    public static float getScourDamage(EntityLivingBase entity) {
        ItemStack head = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        ItemStack chest = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        ItemStack legs = entity.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
        ItemStack feet = entity.getItemStackFromSlot(EntityEquipmentSlot.FEET);

        if (isSlotMissingArmor(head) || isSlotMissingArmor(chest) || isSlotMissingArmor(legs) || isSlotMissingArmor(feet)) {
            return (float) ArcanaQuestTweaksConfig.AtumModuleConfig.scourDamageExposed;
        }

        if (isResistantArmor(head) && isResistantArmor(chest) && isResistantArmor(legs) && isResistantArmor(feet)) {
            return (float) ArcanaQuestTweaksConfig.AtumModuleConfig.scourDamageAtumArmor;
        }

        return (float) ArcanaQuestTweaksConfig.AtumModuleConfig.scourDamageFullArmor;
    }

    private static boolean isSlotMissingArmor(ItemStack stack) {
        return stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemArmor);
    }

    /**
     * Checks if an armor item is immune to sandblasting and contributes to Tier 2 scour mitigation.
     * All Atum-native armor and whitelisted magic armor sets qualify.
     */
    public static boolean isResistantArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemArmor)) return false;
        ResourceLocation reg = stack.getItem().getRegistryName();
        if (reg == null) return false;
        if ("atum".equals(reg.getNamespace())) {
            return true;
        }
        String id = reg.toString();
        for (String pattern : ArcanaQuestTweaksConfig.AtumModuleConfig.sandstormResistantArmorWhitelist) {
            if (matchesPattern(id, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Slowly degrades durability on non-Atum, non-whitelisted armor pieces exposed to the sandstorm.
     */
    public static void damageArmorDurability(EntityLivingBase entity) {
        for (EntityEquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemStackFromSlot(slot);
            if (!stack.isEmpty() && stack.isItemStackDamageable() && !isResistantArmor(stack)) {
                stack.damageItem(1, entity);
            }
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
     * Checks if the entity is in shelter (indoors/underground) or in the leeward wind shadow
     * behind a solid dune, wall, pyramid step, or cliff.
     */
    public static boolean isSheltered(EntityLivingBase entity, World world, Vec3d windVec) {
        BlockPos eyePos = new BlockPos(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        if (!world.canSeeSky(eyePos)) {
            return true; // Indoors or underground
        }

        double shelterDist = ArcanaQuestTweaksConfig.AtumModuleConfig.shelterWindShadowDistance;

        // Check eye height upwind
        Vec3d eyeVec = entity.getPositionEyes(1.0F);
        Vec3d upEye = new Vec3d(eyeVec.x - windVec.x * shelterDist, eyeVec.y, eyeVec.z - windVec.z * shelterDist);
        RayTraceResult rayEye = world.rayTraceBlocks(upEye, eyeVec, false, true, false);
        if (rayEye != null && rayEye.typeOfHit == RayTraceResult.Type.BLOCK) {
            return true;
        }

        // Check torso height upwind (allows crouching/standing behind 1-block low dune crests or walls)
        Vec3d torsoVec = new Vec3d(entity.posX, entity.posY + 0.8D, entity.posZ);
        Vec3d upTorso = new Vec3d(torsoVec.x - windVec.x * shelterDist, torsoVec.y, torsoVec.z - windVec.z * shelterDist);
        RayTraceResult rayTorso = world.rayTraceBlocks(upTorso, torsoVec, false, true, false);
        return rayTorso != null && rayTorso.typeOfHit == RayTraceResult.Type.BLOCK;
    }

    /**
     * Staggered shelter checking for mobs to prevent per-tick raytracing spikes.
     */
    private static boolean isMobSheltered(EntityLivingBase entity, World world, Vec3d windVec) {
        long time = world.getTotalWorldTime();
        if ((entity.getEntityId() + time) % 20 == 0 || !entity.getEntityData().hasKey(NBT_MOB_SHELTERED)) {
            boolean sheltered = isSheltered(entity, world, windVec);
            entity.getEntityData().setBoolean(NBT_MOB_SHELTERED, sheltered);
            return sheltered;
        }
        return entity.getEntityData().getBoolean(NBT_MOB_SHELTERED);
    }

    /**
     * Checks if the player is wearing headgear or Baubles goggles that protect against stinging sand grit and choking.
     * Only Atum-native headgear and goggles protect; generic non-Atum helmets do not.
     */
    public static boolean isProtectedHeadgear(EntityPlayer player) {
        // 1. Check primary helmet slot
        ItemStack helmet = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            ResourceLocation reg = helmet.getItem().getRegistryName();
            if (reg != null) {
                if ("atum".equals(reg.getNamespace())) {
                    String path = reg.getPath();
                    if (path.contains("helmet") || path.contains("bandage") || path.contains("mask")
                            || path.contains("wanderer") || path.contains("eyes_of_atem") || path.contains("halo_of_ra")) {
                        return true;
                    }
                }
                if (isGoggles(helmet)) {
                    return true;
                }
            }
        }

        // 2. Check Baubles head accessory slot if Baubles is loaded
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.enableBaublesGogglesProtection
                && Loader.isModLoaded("baubles")) {
            if (BaublesGogglesHelper.hasGogglesInHeadSlot(player)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if an item represents goggles or is on the protective headgear whitelist.
     */
    public static boolean isGoggles(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation reg = stack.getItem().getRegistryName();
        if (reg == null) return false;
        String id = reg.toString();
        for (String pattern : ArcanaQuestTweaksConfig.AtumModuleConfig.protectiveHeadgear) {
            if (matchesPattern(id, pattern)) {
                return true;
            }
        }
        return reg.getPath().contains("goggles");
    }

    public static boolean matchesPattern(String text, String pattern) {
        if (text == null || pattern == null) return false;
        if (pattern.equals("*") || pattern.equalsIgnoreCase(text)) return true;
        if (!pattern.contains("*")) return pattern.equalsIgnoreCase(text);
        String regex = "\\Q" + pattern.replace("*", "\\E.*\\Q") + "\\E";
        return text.matches("(?i)" + regex);
    }

    private static void decreaseStamina(EntityPlayerMP player, int amount) {
        try {
            com.elenai.elenaidodge2.api.FeathersHelper.decreaseFeathers(player, amount);
        } catch (Throwable ignored) {
        }
    }
}
