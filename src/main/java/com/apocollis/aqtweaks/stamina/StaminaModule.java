package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import com.elenai.elenaidodge2.api.FeathersHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class StaminaModule {

    private String[] weightsBackup = null;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinWorldHighest(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP && !Reflect.isRemote(event.getWorld())) {
            if (com.elenai.elenaidodge2.ModConfig.common != null &&
                com.elenai.elenaidodge2.ModConfig.common.weights != null) {
                weightsBackup = com.elenai.elenaidodge2.ModConfig.common.weights.weights;
                com.elenai.elenaidodge2.ModConfig.common.weights.weights = new String[0];
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorldLowest(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP && !Reflect.isRemote(event.getWorld())) {
            if (com.elenai.elenaidodge2.ModConfig.common != null &&
                com.elenai.elenaidodge2.ModConfig.common.weights != null &&
                weightsBackup != null) {
                com.elenai.elenaidodge2.ModConfig.common.weights.weights = weightsBackup;
                weightsBackup = null;
            }
        }

        // Intercept Spartan thrown projectile entities (Javelins, Throwing Knives, Throwing Axes, Daggers)
        if (!Reflect.isRemote(event.getWorld()) && event.getEntity() != null) {
            net.minecraft.entity.Entity entity = event.getEntity();
            EntityPlayerMP throwerMP = null;

            net.minecraft.entity.Entity shooter = Reflect.getShootingEntity(entity);
            if (shooter instanceof EntityPlayerMP) {
                throwerMP = (EntityPlayerMP) shooter;
            }
            if (throwerMP == null && entity instanceof net.minecraft.entity.projectile.EntityThrowable) {
                net.minecraft.entity.EntityLivingBase tShooter = ((net.minecraft.entity.projectile.EntityThrowable) entity).getThrower();
                if (tShooter instanceof EntityPlayerMP) {
                    throwerMP = (EntityPlayerMP) tShooter;
                }
            }
            if (throwerMP == null && entity instanceof net.minecraftforge.fml.common.registry.IThrowableEntity) {
                net.minecraft.entity.Entity tShooter = ((net.minecraftforge.fml.common.registry.IThrowableEntity) entity).getThrower();
                if (tShooter instanceof EntityPlayerMP) {
                    throwerMP = (EntityPlayerMP) tShooter;
                }
            }

            if (throwerMP != null && !Reflect.isCreative(throwerMP) && !Reflect.isSpectator(throwerMP)) {
                String className = entity.getClass().getName().toLowerCase();
                boolean isThrownWeaponEntity = className.contains("thrown") || className.contains("throwing") ||
                                                className.contains("javelin") || className.contains("knife") ||
                                                className.contains("axe") || className.contains("dagger");

                if (isThrownWeaponEntity) {
                    if (ArcanaQuestTweaksConfig.StaminaModuleConfig.throwingWeapons.enableThrowingCost) {
                        // Stamina cost (1 feather) is handled authoritatively upon release in onItemUseStop.
                        // If player had insufficient stamina, the throw release event was already canceled.
                    }
                }
            }
        }
    }

    public enum WeaponType {
        NONE, LIGHT, MEDIUM, HEAVY
    }

    public static WeaponType getWeaponType(ItemStack stack) {
        if (Reflect.isEmpty(stack)) return WeaponType.LIGHT;
        Item item = Reflect.getItem(stack);
        if (item.getRegistryName() == null) return WeaponType.NONE;
        String name = item.getRegistryName().toString();

        // Check custom lists
        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.lightWeaponsCustom != null) {
            for (String s : ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.lightWeaponsCustom) {
                if (name.equals(s)) return WeaponType.LIGHT;
            }
        }
        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.mediumWeaponsCustom != null) {
            for (String s : ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.mediumWeaponsCustom) {
                if (name.equals(s)) return WeaponType.MEDIUM;
            }
        }
        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.heavyWeaponsCustom != null) {
            for (String s : ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.heavyWeaponsCustom) {
                if (name.equals(s)) return WeaponType.HEAVY;
            }
        }

        // Keyword checks for modded weapons (e.g. Spartan Weaponry)
        String path = item.getRegistryName().getPath().toLowerCase();
        if (path.contains("dagger") || path.contains("parrying_dagger") || path.contains("rapier") || path.contains("knife")) {
            return WeaponType.LIGHT;
        }
        if (path.contains("saber") || path.contains("katana") || path.contains("longsword") || path.contains("mace") || path.contains("spear")) {
            return WeaponType.MEDIUM;
        }
        if (path.contains("greatsword") || path.contains("battleaxe") || path.contains("hammer") || path.contains("warhammer") || path.contains("halberd") || path.contains("pike") || path.contains("glaive") || path.contains("lance") || path.contains("scythe") || path.contains("staff")) {
            return WeaponType.HEAVY;
        }

        // Default fallbacks
        if (item instanceof ItemSword) {
            return WeaponType.MEDIUM;
        }
        if (item instanceof ItemAxe) {
            return WeaponType.HEAVY;
        }

        return WeaponType.NONE;
    }

    /**
     * Checks whether an ItemStack is a throwing weapon (Spartan Weaponry javelins,
     * throwing knives, throwing axes, daggers, etc.).
     * Detection uses registry name keywords and class name fallback.
     */
    public static boolean isThrowingWeapon(ItemStack stack) {
        if (Reflect.isEmpty(stack)) return false;
        Item item = Reflect.getItem(stack);

        // Class name check (covers all Spartan Weaponry throwing weapons & daggers)
        String className = item.getClass().getName().toLowerCase();
        if (className.contains("throwingweapon") || className.contains("itemjavelin") ||
            className.contains("throwingknife") || className.contains("throwingaxe") ||
            className.contains("itemdagger")) {
            return true;
        }

        // Registry name keyword check
        if (item.getRegistryName() != null) {
            String path = item.getRegistryName().getPath().toLowerCase();
            if (path.contains("javelin") || path.contains("throwing_knife") ||
                path.contains("throwing_axe") || path.contains("throwing_dagger") ||
                path.contains("dagger")) {
                return true;
            }
        }

        return false;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        EntityPlayer player = event.player;
        if (player == null || Reflect.isCreative(player) || Reflect.isSpectator(player)) return;

        if (!Reflect.isRemote(player)) {
            // Server side authority
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            handleServerBowDrawing(playerMP);
            handleServerThrowingHold(playerMP);
            handleServerClimbing(playerMP);
            handleServerGrappling(playerMP);
            handleServerGliding(playerMP);
            handleServerShieldBlocking(playerMP);

            // Mining Fatigue exhaustion check
            if (ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.enableMiningCost) {
                int regularFeathers = FeathersHelper.getFeatherLevel(playerMP);
                if (regularFeathers <= ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.miningFatigueThreshold) {
                    Reflect.addPotionEffect(playerMP, new net.minecraft.potion.PotionEffect(net.minecraft.init.MobEffects.MINING_FATIGUE, 40, 2, true, false));
                }
            }

            // Simple Difficulty thirst cost for feather regeneration
            if (ArcanaQuestTweaksConfig.StaminaModuleConfig.simpleDifficulty.enableThirstCost) {
                int currentFeathers = FeathersHelper.getFeatherLevel(playerMP);
                String key = "StaminaTweaksPrevFeathers";
                NBTTagCompound pData = Reflect.getEntityData(playerMP);
                if (Reflect.hasKey(pData, key)) {
                    int prevFeathers = Reflect.getInteger(pData, key);
                    if (currentFeathers > prevFeathers) {
                        int diff = currentFeathers - prevFeathers;
                        float exhaustion = diff * (float) ArcanaQuestTweaksConfig.StaminaModuleConfig.simpleDifficulty.thirstExhaustionPerFeather;
                        Reflect.addThirstExhaustion(playerMP, exhaustion);
                    }
                }
                Reflect.setInteger(pData, key, currentFeathers);
            }
        }
    }

    private void handleServerBowDrawing(EntityPlayerMP player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.enableBowCost) return;

        // Bows only (throwing weapons handled separately in handleServerThrowingHold)
        ItemStack activeStack = Reflect.getActiveItemStack(player);
        boolean isDrawing = Reflect.isHandActive(player) && Reflect.getItem(activeStack) instanceof ItemBow
                            && !isThrowingWeapon(activeStack);

        NBTTagCompound pData = Reflect.getEntityData(player);
        if (isDrawing) {
            int ticks = Reflect.getInteger(pData, "StaminaTweaksBowTicks") + 1;
            int interval = ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldInterval;

            if (ticks >= interval) {
                int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldCost;
                if (Reflect.hasEnoughStamina(player, cost)) {
                    FeathersHelper.decreaseFeathers(player, cost);
                    ticks = 0;
                } else {
                    Reflect.resetActiveHand(player);
                    ticks = 0;
                }
            }
            Reflect.setInteger(pData, "StaminaTweaksBowTicks", ticks);
        } else {
            if (Reflect.getInteger(pData, "StaminaTweaksBowTicks") > 0) {
                Reflect.setInteger(pData, "StaminaTweaksBowTicks", 0);
            }
        }
    }

    private void handleServerThrowingHold(EntityPlayerMP player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.throwingWeapons.enableThrowingCost) return;

        ItemStack activeStack = Reflect.getActiveItemStack(player);
        boolean isAiming = Reflect.isHandActive(player) && isThrowingWeapon(activeStack);

        NBTTagCompound pData = Reflect.getEntityData(player);
        if (isAiming) {
            int ticks = Reflect.getInteger(pData, "StaminaTweaksThrowTicks") + 1;
            int interval = ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldInterval * ArcanaQuestTweaksConfig.StaminaModuleConfig.throwingWeapons.throwingHoldIntervalMultiplier;

            if (ticks >= interval) {
                int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldCost;
                if (Reflect.hasEnoughStamina(player, cost)) {
                    FeathersHelper.decreaseFeathers(player, cost);
                    ticks = 0;
                } else {
                    Reflect.resetActiveHand(player);
                    ticks = 0;
                }
            }
            Reflect.setInteger(pData, "StaminaTweaksThrowTicks", ticks);
        } else {
            if (Reflect.getInteger(pData, "StaminaTweaksThrowTicks") > 0) {
                Reflect.setInteger(pData, "StaminaTweaksThrowTicks", 0);
            }
        }
    }

    private static final double CLIMB_ASCEND_EPS = 0.02;

    private void handleServerClimbing(EntityPlayerMP player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.enableClimbCost) return;

        NBTTagCompound pData = Reflect.getEntityData(player);
        final String prevYKey = "StaminaTweaksClimbPrevY";
        final String ticksKey = "StaminaTweaksLadderTicks";

        if (!Reflect.isOnLadder(player)) {
            if (Reflect.getInteger(pData, ticksKey) > 0) {
                Reflect.setInteger(pData, ticksKey, 0);
            }
            if (Reflect.hasKey(pData, prevYKey)) {
                Reflect.removeTag(pData, prevYKey);
            }
            return;
        }

        net.minecraft.world.World world = Reflect.getWorld(player);
        if (world == null) return;

        int x = net.minecraft.util.math.MathHelper.floor(Reflect.getPosX(player));
        int y = net.minecraft.util.math.MathHelper.floor(Reflect.getBoundingBoxMinY(player));
        int z = net.minecraft.util.math.MathHelper.floor(Reflect.getPosZ(player));
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
        net.minecraft.block.Block block = Reflect.getBlock(world, pos);

        boolean isRope = Reflect.isRopeBlock(block);
        boolean isVine = !isRope && (block instanceof net.minecraft.block.BlockVine
                || block.getClass().getSimpleName().toLowerCase().contains("vine"));

        if (isRope && !ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.enableRopeCost) {
            Reflect.setInteger(pData, ticksKey, 0);
            Reflect.setDouble(pData, prevYKey, Reflect.getPosY(player));
            return;
        }

        int cost = isRope ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ropeCost
                : (isVine ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.vineCost
                        : ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ladderCost);
        int baseInterval = isRope ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ropeInterval
                : (isVine ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.vineInterval
                        : ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ladderInterval);

        double posY = Reflect.getPosY(player);
        boolean ascending = false;
        boolean holding = false;

        if (Reflect.hasKey(pData, prevYKey)) {
            double dy = posY - Reflect.getDouble(pData, prevYKey);
            if (dy > CLIMB_ASCEND_EPS) {
                ascending = true;
            } else if (dy < -CLIMB_ASCEND_EPS && !Reflect.isSneaking(player)) {
                // Sliding down without sneak-hold — free
            } else if (Reflect.isSneaking(player) || Math.abs(dy) <= CLIMB_ASCEND_EPS) {
                holding = true;
            }
        }
        Reflect.setDouble(pData, prevYKey, posY);

        if (ascending || holding) {
            int interval = ascending
                    ? baseInterval
                    : Math.max(1, baseInterval * ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.clingIntervalMultiplier);
            int ticks = Reflect.getInteger(pData, ticksKey) + 1;

            if (ticks >= interval) {
                if (cost > 0 && Reflect.hasEnoughStamina(player, cost)) {
                    FeathersHelper.decreaseFeathers(player, cost);
                }
                ticks = 0; // always reset — avoid latching when spend fails
            }
            Reflect.setInteger(pData, ticksKey, ticks);
        } else if (Reflect.getInteger(pData, ticksKey) > 0) {
            Reflect.setInteger(pData, ticksKey, 0);
        }

        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.fallOnDepleted
                && cost > 0
                && !Reflect.hasEnoughStamina(player, cost)
                && Reflect.getInteger(pData, "StaminaTweaksLedgeClimbState") != 1
                && Reflect.getInteger(pData, "StaminaTweaksLedgeClimbGrace") <= Reflect.getTicksExisted(player)
                && !Reflect.getBoolean(pData, "StaminaTweaksClimbJumpInput")) {
            Reflect.setMotionY(player, -0.15);
        }
    }

    private static final int GRAPPLE_SWING_ENTER_TICKS = 3;
    private static final int GRAPPLE_SWING_EXIT_TICKS = 15;
    private static final int GRAPPLE_COST_HANG = PacketSyncGrappleInput.MODE_NEUTRAL;
    private static final int GRAPPLE_COST_SWING = PacketSyncGrappleInput.MODE_SWING;

    private void handleServerGrappling(EntityPlayerMP player) {
        ArcanaQuestTweaksConfig.Grapple grapple = ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple;
        if (!grapple.enableGrappleCost && !grapple.motorRequiresEmber) return;

        NBTTagCompound pData = Reflect.getEntityData(player);
        if (!Reflect.isGrappling(player)) {
            if (Reflect.getInteger(pData, "StaminaTweaksGrappleTicks") > 0) {
                Reflect.setInteger(pData, "StaminaTweaksGrappleTicks", 0);
            }
            if (Reflect.getInteger(pData, "StaminaTweaksGrappleEmberTicks") > 0) {
                Reflect.setInteger(pData, "StaminaTweaksGrappleEmberTicks", 0);
            }
            if (Reflect.getInteger(pData, "StaminaTweaksGrappleSwingStreak") != 0) {
                Reflect.setInteger(pData, "StaminaTweaksGrappleSwingStreak", 0);
            }
            if (Reflect.getBoolean(pData, "StaminaTweaksGrappleIsSwing")) {
                Reflect.setBoolean(pData, "StaminaTweaksGrappleIsSwing", false);
            }
            return;
        }

        int mode = Reflect.getInteger(pData, "StaminaTweaksGrappleMode");
        boolean motorPacket = Reflect.getBoolean(pData, "StaminaTweaksGrappleMotor");
        boolean grounded = Reflect.getBoolean(pData, "StaminaTweaksGrappleGrounded");
        boolean motorActive = motorPacket && EmberMotorHelper.hasEmber(player, grapple.motorEmberCost);

        // Standing hooked without motor is free. Motor pull still bills hang stamina + Ember
        // even if onGround / ongroundtimer is set (leaving the ground, walking into a wall, etc.).
        if (grounded && !motorPacket) {
            if (Reflect.getInteger(pData, "StaminaTweaksGrappleTicks") > 0) {
                Reflect.setInteger(pData, "StaminaTweaksGrappleTicks", 0);
            }
            if (Reflect.getInteger(pData, "StaminaTweaksGrappleEmberTicks") > 0) {
                Reflect.setInteger(pData, "StaminaTweaksGrappleEmberTicks", 0);
            }
            if (Reflect.getInteger(pData, "StaminaTweaksGrappleSwingStreak") != 0) {
                Reflect.setInteger(pData, "StaminaTweaksGrappleSwingStreak", 0);
            }
            if (Reflect.getBoolean(pData, "StaminaTweaksGrappleIsSwing")) {
                Reflect.setBoolean(pData, "StaminaTweaksGrappleIsSwing", false);
            }
            Reflect.setInteger(pData, "StaminaTweaksGrappleLastCostMode", -1);
            return;
        }

        if (motorActive && EmberMotorHelper.requiresEmber()) {
            int emberTicks = Reflect.getInteger(pData, "StaminaTweaksGrappleEmberTicks") + 1;
            if (emberTicks >= grapple.motorEmberInterval) {
                if (!EmberMotorHelper.consumeEmber(player, grapple.motorEmberCost)) {
                    motorActive = false;
                }
                emberTicks = 0;
            }
            Reflect.setInteger(pData, "StaminaTweaksGrappleEmberTicks", emberTicks);
        } else if (Reflect.getInteger(pData, "StaminaTweaksGrappleEmberTicks") > 0) {
            Reflect.setInteger(pData, "StaminaTweaksGrappleEmberTicks", 0);
        }

        if (!grapple.enableGrappleCost) return;

        if (mode == PacketSyncGrappleInput.MODE_DESCEND) {
            Reflect.setInteger(pData, "StaminaTweaksGrappleTicks", 0);
            Reflect.setInteger(pData, "StaminaTweaksGrappleLastCostMode", -1);
            return;
        }

        int cost;
        int interval;
        int costMode;
        if (motorActive && grapple.motorUsesHangCost) {
            cost = grapple.grappleHoldCost;
            interval = grapple.grappleHoldInterval;
            costMode = 10; // motor hang
        } else if (mode == PacketSyncGrappleInput.MODE_CLIMB) {
            cost = grapple.grappleClimbCost;
            interval = grapple.grappleClimbInterval;
            costMode = PacketSyncGrappleInput.MODE_CLIMB;
        } else if (isGrappleSwinging(player, pData, mode, grapple.grappleSwingSpeedThreshold)) {
            cost = grapple.grappleSwingCost;
            interval = grapple.grappleSwingInterval;
            costMode = GRAPPLE_COST_SWING;
        } else {
            cost = grapple.grappleHoldCost;
            interval = grapple.grappleHoldInterval;
            costMode = GRAPPLE_COST_HANG;
        }

        int prevCostMode = Reflect.getInteger(pData, "StaminaTweaksGrappleLastCostMode");
        boolean hangSwingSwap = (costMode == GRAPPLE_COST_HANG || costMode == GRAPPLE_COST_SWING)
                && (prevCostMode == GRAPPLE_COST_HANG || prevCostMode == GRAPPLE_COST_SWING);
        if (prevCostMode != costMode && !hangSwingSwap) {
            Reflect.setInteger(pData, "StaminaTweaksGrappleTicks", 0);
        }
        Reflect.setInteger(pData, "StaminaTweaksGrappleLastCostMode", costMode);

        int ticks = Reflect.getInteger(pData, "StaminaTweaksGrappleTicks") + 1;
        if (ticks >= interval) {
            if (cost > 0) {
                if (Reflect.hasEnoughStamina(player, cost)) {
                    FeathersHelper.decreaseFeathers(player, cost);
                } else {
                    Reflect.detachGrapple(player);
                }
            }
            ticks = 0;
        }
        Reflect.setInteger(pData, "StaminaTweaksGrappleTicks", ticks);
    }

    private boolean isGrappleSwinging(EntityPlayerMP player, NBTTagCompound pData, int mode, double threshold) {
        boolean wantSwing = mode == PacketSyncGrappleInput.MODE_SWING
                || Reflect.getSpeed(player) >= threshold;
        boolean swinging = Reflect.getBoolean(pData, "StaminaTweaksGrappleIsSwing");
        int streak = Reflect.getInteger(pData, "StaminaTweaksGrappleSwingStreak");
        if (wantSwing) {
            if (swinging) {
                streak = 0;
            } else {
                streak++;
                if (streak >= GRAPPLE_SWING_ENTER_TICKS) {
                    swinging = true;
                    streak = 0;
                }
            }
        } else if (swinging) {
            streak++;
            if (streak >= GRAPPLE_SWING_EXIT_TICKS) {
                swinging = false;
                streak = 0;
            }
        } else {
            streak = 0;
        }
        Reflect.setInteger(pData, "StaminaTweaksGrappleSwingStreak", streak);
        Reflect.setBoolean(pData, "StaminaTweaksGrappleIsSwing", swinging);
        return swinging;
    }

    private void handleServerGliding(EntityPlayerMP player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.glider.enableGliderCost) return;

        NBTTagCompound pData = Reflect.getEntityData(player);
        if (Reflect.isGliding(player)) {
            int ticks = Reflect.getInteger(pData, "StaminaTweaksGliderTicks") + 1;
            int interval = ArcanaQuestTweaksConfig.StaminaModuleConfig.glider.gliderInterval;

            if (ticks >= interval) {
                int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.glider.gliderCost;
                if (Reflect.hasEnoughStamina(player, cost)) {
                    FeathersHelper.decreaseFeathers(player, cost);
                    ticks = 0;
                } else {
                    Reflect.undeployGlider(player);
                    ticks = 0;
                }
            }
            Reflect.setInteger(pData, "StaminaTweaksGliderTicks", ticks);
        } else {
            if (Reflect.getInteger(pData, "StaminaTweaksGliderTicks") > 0) {
                Reflect.setInteger(pData, "StaminaTweaksGliderTicks", 0);
            }
        }
    }

    @SubscribeEvent
    public void onLivingJump(LivingJumpEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (Reflect.isRemote(player) || Reflect.isCreative(player) || Reflect.isSpectator(player)) return;

        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.jumping.enableJumpCost) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        int threshold = ArcanaQuestTweaksConfig.StaminaModuleConfig.jumping.jumpThreshold;
        int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.jumping.jumpCost;

        if (Reflect.hasEnoughStamina(playerMP, threshold)) {
            FeathersHelper.decreaseFeathers(playerMP, cost);
        } else {
            // Block the jump by setting vertical velocity to 0
            Reflect.setMotionY(player, 0.0);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (Reflect.isRemote(player) || Reflect.isCreative(player) || Reflect.isSpectator(player)) return;

        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.enableAttackCost) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack held = Reflect.getHeldItemMainhand(playerMP);
        WeaponType type = getWeaponType(held);

        if (type == WeaponType.NONE) return;

        int cost = (type == WeaponType.LIGHT) ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.lightCost : 
                   (type == WeaponType.HEAVY ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.heavyCost : ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.mediumCost);
        double multiplier = (type == WeaponType.LIGHT) ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.lightDamageMultiplier : 
                             (type == WeaponType.HEAVY ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.heavyDamageMultiplier : ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.mediumDamageMultiplier);

        int currentFeathers = FeathersHelper.getFeatherLevel(playerMP);

        if (Reflect.hasEnoughStamina(playerMP, cost)) {
            FeathersHelper.decreaseFeathers(playerMP, cost);
        } else {
            // Drain remaining usable feathers
            int absorption = Reflect.getAbsorptionFeathers(playerMP);
            int weight = Reflect.getWeight(playerMP);
            int totalUsable = (currentFeathers - weight) + absorption;
            if (totalUsable > 0) {
                FeathersHelper.decreaseFeathers(playerMP, totalUsable);
            }
            // Set attack penalty
            Reflect.setDouble(Reflect.getEntityData(playerMP), "StaminaTweaksAttackPenalty", multiplier);
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        handleWeaponSwing(event.getEntityPlayer());
    }

    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        handleWeaponSwing(event.getEntityPlayer());
    }

    private void handleWeaponSwing(EntityPlayer player) {
        if (player == null || Reflect.isRemote(player) || Reflect.isCreative(player) || Reflect.isSpectator(player)) return;
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.enableAttackCost) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack held = Reflect.getHeldItemMainhand(playerMP);
        WeaponType type = getWeaponType(held);
        if (type == WeaponType.NONE) return;

        int cost = (type == WeaponType.LIGHT) ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.lightCost : 
                   (type == WeaponType.HEAVY ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.heavyCost : ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.mediumCost);

        if (Reflect.hasEnoughStamina(playerMP, cost)) {
            FeathersHelper.decreaseFeathers(playerMP, cost);
        }
    }

    @SubscribeEvent
    public void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (Reflect.isRemote(player) || Reflect.isCreative(player) || Reflect.isSpectator(player)) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack stack = event.getItem();
        if (Reflect.isEmpty(stack)) return;

        if (Reflect.getItem(stack) instanceof ItemBow) {
            if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.enableBowCost) return;
            int drawCost = ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowDrawCost;
            if (Reflect.hasEnoughStamina(playerMP, drawCost)) {
                FeathersHelper.decreaseFeathers(playerMP, drawCost);
            } else {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (Reflect.isRemote(player) || Reflect.isCreative(player) || Reflect.isSpectator(player)) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack stack = event.getItem();
        if (Reflect.isEmpty(stack)) return;

        int duration = event.getDuration();
        int ticksUsed = Reflect.getMaxItemUseDuration(stack) - duration;

        if (Reflect.getItem(stack) instanceof ItemBow) {
            if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.enableBowCost) return;
            int interval = ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldInterval;
            if (ticksUsed > 0 && ticksUsed % interval == 0) {
                int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldCost;
                if (Reflect.hasEnoughStamina(playerMP, cost)) {
                    FeathersHelper.decreaseFeathers(playerMP, cost);
                } else {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (Reflect.isRemote(player) || Reflect.isCreative(player) || Reflect.isSpectator(player)) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack stack = event.getItem();
        if (Reflect.isEmpty(stack)) return;

        if (isThrowingWeapon(stack)) {
            if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.throwingWeapons.enableThrowingCost) return;
            int releaseCost = ArcanaQuestTweaksConfig.StaminaModuleConfig.throwingWeapons.throwingReleaseCost;
            if (Reflect.hasEnoughStamina(playerMP, releaseCost)) {
                FeathersHelper.decreaseFeathers(playerMP, releaseCost);
            } else {
                event.setCanceled(true);
                Reflect.resetActiveHand(playerMP);
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (Reflect.isRemote(player)) return;

        Entity attacker = Reflect.getTrueSource(event.getSource());
        if (!(attacker instanceof EntityPlayer)) return;

        NBTTagCompound attackerData = Reflect.getEntityData(attacker);
        if (Reflect.hasKey(attackerData, "StaminaTweaksAttackPenalty")) {
            double penalty = Reflect.getDouble(attackerData, "StaminaTweaksAttackPenalty");
            event.setAmount((float) (event.getAmount() * penalty));
            Reflect.removeTag(attackerData, "StaminaTweaksAttackPenalty");
        }
    }

    private void handleServerShieldBlocking(EntityPlayerMP player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.shield.enableShieldCost) return;

        NBTTagCompound data = Reflect.getEntityData(player);
        boolean wasBlocking = Reflect.getBoolean(data, "StaminaTweaksShieldActive");
        boolean isBlocking = Reflect.isActiveItemStackBlocking(player);

        if (isBlocking) {
            int interval = ArcanaQuestTweaksConfig.StaminaModuleConfig.shield.shieldHoldInterval;
            int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.shield.shieldHoldCost;

            if (!wasBlocking) {
                Reflect.setBoolean(data, "StaminaTweaksShieldActive", true);
                Reflect.setInteger(data, "StaminaTweaksShieldTicks", 0);
            } else {
                if (interval <= 0) {
                    Reflect.setBoolean(data, "StaminaTweaksShieldActive", false);
                    return;
                }

                int ticks = Reflect.getInteger(data, "StaminaTweaksShieldTicks") + 1;
                if (ticks >= interval) {
                    if (Reflect.hasEnoughStamina(player, cost)) {
                        FeathersHelper.decreaseFeathers(player, cost);
                        ticks = 0;
                    } else {
                        Reflect.resetActiveHand(player);
                        Reflect.setBoolean(data, "StaminaTweaksShieldActive", false);
                        ticks = 0;
                    }
                }
                Reflect.setInteger(data, "StaminaTweaksShieldTicks", ticks);
            }
        } else {
            if (wasBlocking) {
                Reflect.setBoolean(data, "StaminaTweaksShieldActive", false);
                Reflect.setInteger(data, "StaminaTweaksShieldTicks", 0);
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(net.minecraftforge.event.world.BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || Reflect.isRemote(player) || Reflect.isCreative(player) || Reflect.isSpectator(player)) return;

        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.enableMiningCost) return;

        Block block = Reflect.getBlock(event.getState());
        boolean isOreOrObsidian = block == net.minecraft.init.Blocks.OBSIDIAN || 
                                  (block.getRegistryName() != null && block.getRegistryName().toString().toLowerCase().contains("ore"));

        int cost = isOreOrObsidian ? ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.oreCost : ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.defaultCost;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.enableReskillable && 
            Reflect.hasUnlockable(playerMP, ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.miningEfficiencyPerkId)) {
            cost = Math.max(0, cost - ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.miningEfficiencyReduction);
        }
        if (cost <= 0) return;
        if (Reflect.hasEnoughStamina(playerMP, cost)) {
            FeathersHelper.decreaseFeathers(playerMP, cost);
        } else {
            int currentFeathers = FeathersHelper.getFeatherLevel(playerMP);
            int weight = Reflect.getWeight(playerMP);
            int usable = currentFeathers - weight;
            int absorption = Reflect.getAbsorptionFeathers(playerMP);
            int totalUsable = usable + absorption;
            if (totalUsable > 0) {
                FeathersHelper.decreaseFeathers(playerMP, totalUsable);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) event.player;
            FeathersHelper.fillFeathers(playerMP);
        }
    }
}
