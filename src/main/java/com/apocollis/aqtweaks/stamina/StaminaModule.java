package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import com.elenai.elenaidodge2.api.FeathersHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class StaminaModule {

    // Elenai's weight list is a global config field, blanked for the duration of a player join so
    // Elenai's own join listener cannot stamp weights before ours does. A nested EntityJoinWorldEvent
    // (a mod spawning an entity from inside a join listener) would clobber a single backup field, so
    // the backup is per-thread and depth-counted: only the outermost join blanks and restores.
    private static final ThreadLocal<String[]> WEIGHTS_BACKUP = new ThreadLocal<>();
    private static final ThreadLocal<Integer> WEIGHTS_DEPTH = ThreadLocal.withInitial(() -> 0);

    private static final int MINING_FATIGUE_DURATION = 40;
    private static final int MINING_FATIGUE_AMPLIFIER = 2;
    private static final int MINING_FATIGUE_REFRESH_TICKS = 20;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinWorldHighest(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof EntityPlayerMP) || event.getWorld().isRemote) return;

        int depth = WEIGHTS_DEPTH.get();
        if (depth == 0
                && com.elenai.elenaidodge2.ModConfig.common != null
                && com.elenai.elenaidodge2.ModConfig.common.weights != null) {
            WEIGHTS_BACKUP.set(com.elenai.elenaidodge2.ModConfig.common.weights.weights);
            com.elenai.elenaidodge2.ModConfig.common.weights.weights = new String[0];
        }
        WEIGHTS_DEPTH.set(depth + 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorldLowest(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof EntityPlayerMP) || event.getWorld().isRemote) return;

        int depth = WEIGHTS_DEPTH.get();
        if (depth <= 0) return;

        depth--;
        if (depth > 0) {
            WEIGHTS_DEPTH.set(depth);
            return;
        }

        String[] backup = WEIGHTS_BACKUP.get();
        if (backup != null
                && com.elenai.elenaidodge2.ModConfig.common != null
                && com.elenai.elenaidodge2.ModConfig.common.weights != null) {
            com.elenai.elenaidodge2.ModConfig.common.weights.weights = backup;
        }
        WEIGHTS_BACKUP.remove();
        WEIGHTS_DEPTH.remove();
    }

    public enum WeaponType {
        NONE, LIGHT, MEDIUM, HEAVY
    }

    public static WeaponType getWeaponType(ItemStack stack) {
        if (stack.isEmpty()) return WeaponType.LIGHT;
        Item item = stack.getItem();
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
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

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
        if (player == null || player.capabilities.isCreativeMode || player.isSpectator()) return;

        if (!player.world.isRemote) {
            // Server side authority
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            // getEntityData hands back the entity's live compound, so one lookup can be threaded
            // through every sub-handler without changing what they read or write.
            NBTTagCompound pData = playerMP.getEntityData();
            handleServerBowDrawing(playerMP, pData);
            handleServerThrowingHold(playerMP, pData);
            handleServerClimbing(playerMP, pData);
            handleServerLedgeMantle(playerMP, pData);
            handleServerGrappling(playerMP, pData);
            handleServerGliding(playerMP, pData);
            handleServerSprinting(playerMP, pData);
            handleServerShieldBlocking(playerMP, pData);

            // Mining Fatigue exhaustion check
            if (ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.enableMiningCost) {
                int regularFeathers = FeathersHelper.getFeatherLevel(playerMP);
                if (regularFeathers <= ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.miningFatigueThreshold) {
                    // Only re-apply as the effect winds down. The refresh window is half the duration,
                    // so the effect never lapses while stamina stays empty.
                    PotionEffect active = playerMP.getActivePotionEffect(MobEffects.MINING_FATIGUE);
                    if (active == null
                            || active.getAmplifier() < MINING_FATIGUE_AMPLIFIER
                            || active.getDuration() <= MINING_FATIGUE_REFRESH_TICKS) {
                        playerMP.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE,
                                MINING_FATIGUE_DURATION, MINING_FATIGUE_AMPLIFIER, true, false));
                    }
                }
            }

            // Simple Difficulty thirst cost for feather regeneration
            if (ArcanaQuestTweaksConfig.StaminaModuleConfig.simpleDifficulty.enableThirstCost) {
                int currentFeathers = FeathersHelper.getFeatherLevel(playerMP);
                String key = "StaminaTweaksPrevFeathers";
                if (pData.hasKey(key)) {
                    int prevFeathers = pData.getInteger(key);
                    if (currentFeathers > prevFeathers) {
                        int diff = currentFeathers - prevFeathers;
                        float exhaustion = diff * (float) ArcanaQuestTweaksConfig.StaminaModuleConfig.simpleDifficulty.thirstExhaustionPerFeather;
                        Reflect.addThirstExhaustion(playerMP, exhaustion);
                    }
                }
                pData.setInteger(key, currentFeathers);
            }
        }
    }

    private void handleServerBowDrawing(EntityPlayerMP player, NBTTagCompound pData) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.enableBowCost) return;

        // Bows only (throwing weapons handled separately in handleServerThrowingHold)
        ItemStack activeStack = player.getActiveItemStack();
        boolean isDrawing = player.isHandActive() && activeStack.getItem() instanceof ItemBow
                            && !isThrowingWeapon(activeStack);

        if (isDrawing) {
            int ticks = pData.getInteger("StaminaTweaksBowTicks") + 1;
            int interval = StaminaPerks.bowHoldInterval(player,
                    ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldInterval);

            if (ticks >= interval) {
                int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldCost;
                if (Reflect.hasEnoughStamina(player, cost)) {
                    Reflect.decreaseFeathers(player, cost);
                    ticks = 0;
                } else {
                    player.resetActiveHand();
                    ticks = 0;
                }
            }
            pData.setInteger("StaminaTweaksBowTicks", ticks);
        } else {
            if (pData.getInteger("StaminaTweaksBowTicks") > 0) {
                pData.setInteger("StaminaTweaksBowTicks", 0);
            }
        }
    }

    private void handleServerThrowingHold(EntityPlayerMP player, NBTTagCompound pData) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.throwingWeapons.enableThrowingCost) return;

        ItemStack activeStack = player.getActiveItemStack();
        boolean isAiming = player.isHandActive() && isThrowingWeapon(activeStack);

        if (isAiming) {
            int ticks = pData.getInteger("StaminaTweaksThrowTicks") + 1;
            int interval = StaminaPerks.bowHoldInterval(player,
                    ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldInterval)
                    * ArcanaQuestTweaksConfig.StaminaModuleConfig.throwingWeapons.throwingHoldIntervalMultiplier;

            if (ticks >= interval) {
                int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldCost;
                if (Reflect.hasEnoughStamina(player, cost)) {
                    Reflect.decreaseFeathers(player, cost);
                    ticks = 0;
                } else {
                    player.resetActiveHand();
                    ticks = 0;
                }
            }
            pData.setInteger("StaminaTweaksThrowTicks", ticks);
        } else {
            if (pData.getInteger("StaminaTweaksThrowTicks") > 0) {
                pData.setInteger("StaminaTweaksThrowTicks", 0);
            }
        }
    }

    private static final double CLIMB_ASCEND_EPS = 0.02;

    private void handleServerClimbing(EntityPlayerMP player, NBTTagCompound pData) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.enableClimbCost) return;

        final String prevYKey = "StaminaTweaksClimbPrevY";
        final String ticksKey = "StaminaTweaksLadderTicks";

        if (!player.isOnLadder()) {
            if (pData.getInteger(ticksKey) > 0) {
                pData.setInteger(ticksKey, 0);
            }
            if (pData.hasKey(prevYKey)) {
                pData.removeTag(prevYKey);
            }
            return;
        }

        net.minecraft.world.World world = player.world;
        if (world == null) return;

        int x = MathHelper.floor(player.posX);
        int y = MathHelper.floor(player.getEntityBoundingBox().minY);
        int z = MathHelper.floor(player.posZ);
        BlockPos pos = new BlockPos(x, y, z);
        Block block = world.getBlockState(pos).getBlock();

        boolean isRope = Reflect.isRopeBlock(block);
        boolean isVine = !isRope && (block instanceof net.minecraft.block.BlockVine
                || block.getClass().getSimpleName().toLowerCase().contains("vine"));

        if (isRope && !ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.enableRopeCost) {
            pData.setInteger(ticksKey, 0);
            pData.setDouble(prevYKey, player.posY);
            return;
        }

        int cost = isRope ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ropeCost
                : (isVine ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.vineCost
                        : ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ladderCost);
        cost = StaminaPerks.climbCost(player, cost);
        int baseInterval = isRope ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ropeInterval
                : (isVine ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.vineInterval
                        : ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ladderInterval);

        double posY = player.posY;
        boolean ascending = false;
        boolean holding = false;

        if (pData.hasKey(prevYKey)) {
            double dy = posY - pData.getDouble(prevYKey);
            if (dy > CLIMB_ASCEND_EPS) {
                ascending = true;
            } else if (dy < -CLIMB_ASCEND_EPS && !player.isSneaking()) {
                // Sliding down without sneak-hold — free
            } else if (player.isSneaking() || Math.abs(dy) <= CLIMB_ASCEND_EPS) {
                holding = true;
            }
        }
        pData.setDouble(prevYKey, posY);

        if (ascending || holding) {
            int interval = ascending
                    ? baseInterval
                    : Math.max(1, baseInterval * ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.clingIntervalMultiplier);
            int ticks = pData.getInteger(ticksKey) + 1;

            if (ticks >= interval) {
                if (cost > 0 && Reflect.hasEnoughStamina(player, cost)) {
                    Reflect.decreaseFeathers(player, cost);
                }
                ticks = 0; // always reset — avoid latching when spend fails
            }
            pData.setInteger(ticksKey, ticks);
        } else if (pData.getInteger(ticksKey) > 0) {
            pData.setInteger(ticksKey, 0);
        }

        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.fallOnDepleted
                && cost > 0
                && !Reflect.hasEnoughStamina(player, cost)
                && pData.getInteger("StaminaTweaksLedgeClimbState") != 1
                && pData.getInteger("StaminaTweaksLedgeClimbGrace") <= player.ticksExisted
                && !pData.getBoolean("StaminaTweaksClimbJumpInput")) {
            player.motionY = -0.15;
        }
    }

    private static void clearLedgeMantle(NBTTagCompound pData) {
        pData.setInteger("StaminaTweaksLedgeClimbState", 0);
        pData.setInteger("StaminaTweaksLedgeMantleTicks", 0);
        pData.setInteger("StaminaTweaksLedgeExtraSpends", 0);
    }

    private void handleServerLedgeMantle(EntityPlayerMP player, NBTTagCompound pData) {
        if (pData.getInteger("StaminaTweaksLedgeClimbState") != 1) return;

        player.fallDistance = 0.0F;

        if (player.onGround || player.isInWater() || player.isInLava() || player.isRiding()) {
            clearLedgeMantle(pData);
            return;
        }

        ArcanaQuestTweaksConfig.LedgeClimb ledge = ArcanaQuestTweaksConfig.StaminaModuleConfig.ledgeClimb;
        int ticks = pData.getInteger("StaminaTweaksLedgeMantleTicks") + 1;
        pData.setInteger("StaminaTweaksLedgeMantleTicks", ticks);

        int extras = pData.getInteger("StaminaTweaksLedgeExtraSpends");
        int extraCost = StaminaPerks.climbCost(player, ledge.ledgeClimbExtraCost);
        boolean extraDue = extraCost > 0
                && extras < ledge.ledgeClimbMaxExtraSpends
                && ticks > ledge.ledgeClimbExtraAfterTicks
                && ticks <= ledge.ledgeClimbExtraAfterTicks + ledge.ledgeClimbExtraInterval * ledge.ledgeClimbMaxExtraSpends
                && (ticks - ledge.ledgeClimbExtraAfterTicks) % ledge.ledgeClimbExtraInterval == 0;

        if (extraDue) {
            if (Reflect.hasEnoughStamina(player, extraCost)) {
                Reflect.decreaseFeathers(player, extraCost);
                pData.setInteger("StaminaTweaksLedgeExtraSpends", extras + 1);
            } else {
                clearLedgeMantle(pData);
                player.motionY = -0.15;
            }
        }
    }

    private static final int GRAPPLE_SWING_ENTER_TICKS = 3;
    private static final int GRAPPLE_SWING_EXIT_TICKS = 15;
    private static final int GRAPPLE_COST_HANG = PacketSyncGrappleInput.MODE_NEUTRAL;
    private static final int GRAPPLE_COST_SWING = PacketSyncGrappleInput.MODE_SWING;

    private void handleServerGrappling(EntityPlayerMP player, NBTTagCompound pData) {
        ArcanaQuestTweaksConfig.Grapple grapple = ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple;
        if (!grapple.enableGrappleCost && !grapple.motorRequiresEmber) return;

        if (!Reflect.isGrappling(player)) {
            if (pData.getInteger("StaminaTweaksGrappleTicks") > 0) {
                pData.setInteger("StaminaTweaksGrappleTicks", 0);
            }
            if (pData.getInteger("StaminaTweaksGrappleEmberTicks") > 0) {
                pData.setInteger("StaminaTweaksGrappleEmberTicks", 0);
            }
            if (pData.getInteger("StaminaTweaksGrappleSwingStreak") != 0) {
                pData.setInteger("StaminaTweaksGrappleSwingStreak", 0);
            }
            if (pData.getBoolean("StaminaTweaksGrappleIsSwing")) {
                pData.setBoolean("StaminaTweaksGrappleIsSwing", false);
            }
            return;
        }

        int mode = pData.getInteger("StaminaTweaksGrappleMode");
        boolean motorPacket = pData.getBoolean("StaminaTweaksGrappleMotor");
        boolean grounded = pData.getBoolean("StaminaTweaksGrappleGrounded");
        boolean motorActive = motorPacket && EmberMotorHelper.hasEmber(player, grapple.motorEmberCost);

        // Standing hooked without motor is free. Motor pull still bills hang stamina + Ember
        // even if onGround / ongroundtimer is set (leaving the ground, walking into a wall, etc.).
        if (grounded && !motorPacket) {
            if (pData.getInteger("StaminaTweaksGrappleTicks") > 0) {
                pData.setInteger("StaminaTweaksGrappleTicks", 0);
            }
            if (pData.getInteger("StaminaTweaksGrappleEmberTicks") > 0) {
                pData.setInteger("StaminaTweaksGrappleEmberTicks", 0);
            }
            if (pData.getInteger("StaminaTweaksGrappleSwingStreak") != 0) {
                pData.setInteger("StaminaTweaksGrappleSwingStreak", 0);
            }
            if (pData.getBoolean("StaminaTweaksGrappleIsSwing")) {
                pData.setBoolean("StaminaTweaksGrappleIsSwing", false);
            }
            pData.setInteger("StaminaTweaksGrappleLastCostMode", -1);
            return;
        }

        if (motorActive && EmberMotorHelper.requiresEmber()) {
            int emberTicks = pData.getInteger("StaminaTweaksGrappleEmberTicks") + 1;
            if (emberTicks >= grapple.motorEmberInterval) {
                if (!EmberMotorHelper.consumeEmber(player, grapple.motorEmberCost)) {
                    motorActive = false;
                }
                emberTicks = 0;
            }
            pData.setInteger("StaminaTweaksGrappleEmberTicks", emberTicks);
        } else if (pData.getInteger("StaminaTweaksGrappleEmberTicks") > 0) {
            pData.setInteger("StaminaTweaksGrappleEmberTicks", 0);
        }

        if (!grapple.enableGrappleCost) return;

        if (mode == PacketSyncGrappleInput.MODE_DESCEND) {
            pData.setInteger("StaminaTweaksGrappleTicks", 0);
            pData.setInteger("StaminaTweaksGrappleLastCostMode", -1);
            return;
        }

        int cost;
        int interval;
        int costMode;
        if (motorActive && grapple.motorUsesHangCost) {
            cost = StaminaPerks.climbCost(player, grapple.grappleHoldCost);
            interval = grapple.grappleHoldInterval;
            costMode = 10; // motor hang
        } else if (mode == PacketSyncGrappleInput.MODE_CLIMB) {
            cost = StaminaPerks.climbCost(player, grapple.grappleClimbCost);
            interval = grapple.grappleClimbInterval;
            costMode = PacketSyncGrappleInput.MODE_CLIMB;
        } else if (isGrappleSwinging(player, pData, mode, grapple.grappleSwingSpeedThreshold)) {
            cost = StaminaPerks.climbCost(player, grapple.grappleSwingCost);
            interval = grapple.grappleSwingInterval;
            costMode = GRAPPLE_COST_SWING;
        } else {
            cost = StaminaPerks.climbCost(player, grapple.grappleHoldCost);
            interval = grapple.grappleHoldInterval;
            costMode = GRAPPLE_COST_HANG;
        }

        int prevCostMode = pData.getInteger("StaminaTweaksGrappleLastCostMode");
        boolean hangSwingSwap = (costMode == GRAPPLE_COST_HANG || costMode == GRAPPLE_COST_SWING)
                && (prevCostMode == GRAPPLE_COST_HANG || prevCostMode == GRAPPLE_COST_SWING);
        if (prevCostMode != costMode && !hangSwingSwap) {
            pData.setInteger("StaminaTweaksGrappleTicks", 0);
        }
        pData.setInteger("StaminaTweaksGrappleLastCostMode", costMode);

        int ticks = pData.getInteger("StaminaTweaksGrappleTicks") + 1;
        if (ticks >= interval) {
            if (cost > 0) {
                if (Reflect.hasEnoughStamina(player, cost)) {
                    Reflect.decreaseFeathers(player, cost);
                } else {
                    Reflect.detachGrapple(player);
                }
            }
            ticks = 0;
        }
        pData.setInteger("StaminaTweaksGrappleTicks", ticks);
    }

    private boolean isGrappleSwinging(EntityPlayerMP player, NBTTagCompound pData, int mode, double threshold) {
        boolean wantSwing = mode == PacketSyncGrappleInput.MODE_SWING
                || Math.sqrt(player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ) >= threshold;
        boolean swinging = pData.getBoolean("StaminaTweaksGrappleIsSwing");
        int streak = pData.getInteger("StaminaTweaksGrappleSwingStreak");
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
        pData.setInteger("StaminaTweaksGrappleSwingStreak", streak);
        pData.setBoolean("StaminaTweaksGrappleIsSwing", swinging);
        return swinging;
    }

    private void handleServerGliding(EntityPlayerMP player, NBTTagCompound pData) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.glider.enableGliderCost) return;

        if (Reflect.isGliding(player)) {
            int ticks = pData.getInteger("StaminaTweaksGliderTicks") + 1;
            int interval = ArcanaQuestTweaksConfig.StaminaModuleConfig.glider.gliderInterval;

            if (ticks >= interval) {
                int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.glider.gliderCost;
                if (Reflect.hasEnoughStamina(player, cost)) {
                    Reflect.decreaseFeathers(player, cost);
                    ticks = 0;
                } else {
                    Reflect.undeployGlider(player);
                    ticks = 0;
                }
            }
            pData.setInteger("StaminaTweaksGliderTicks", ticks);
        } else {
            if (pData.getInteger("StaminaTweaksGliderTicks") > 0) {
                pData.setInteger("StaminaTweaksGliderTicks", 0);
            }
        }
    }

    private void handleServerSprinting(EntityPlayerMP player, NBTTagCompound pData) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.sprinting.enableSprintCost) return;

        if (!player.isSprinting()) {
            if (pData.getInteger("StaminaTweaksSprintTicks") > 0) {
                pData.setInteger("StaminaTweaksSprintTicks", 0);
            }
            return;
        }

        int threshold = ArcanaQuestTweaksConfig.StaminaModuleConfig.sprinting.sprintThreshold;
        if (!Reflect.hasEnoughStamina(player, threshold)) {
            player.setSprinting(false);
            pData.setInteger("StaminaTweaksSprintTicks", 0);
            return;
        }

        int ticks = pData.getInteger("StaminaTweaksSprintTicks") + 1;
        int interval = ArcanaQuestTweaksConfig.StaminaModuleConfig.sprinting.sprintInterval;
        int cost = StaminaPerks.sprintCost(player,
                ArcanaQuestTweaksConfig.StaminaModuleConfig.sprinting.sprintCost);

        if (ticks >= interval) {
            if (cost > 0 && Reflect.hasEnoughStamina(player, cost)) {
                Reflect.decreaseFeathers(player, cost);
            } else if (cost > 0) {
                player.setSprinting(false);
            }
            ticks = 0;
        }
        pData.setInteger("StaminaTweaksSprintTicks", ticks);
    }

    @SubscribeEvent
    public void onLivingJump(LivingJumpEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || player.capabilities.isCreativeMode || player.isSpectator()) return;

        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.jumping.enableJumpCost) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        int threshold = ArcanaQuestTweaksConfig.StaminaModuleConfig.jumping.jumpThreshold;
        int cost = StaminaPerks.jumpCost(playerMP,
                ArcanaQuestTweaksConfig.StaminaModuleConfig.jumping.jumpCost);

        if (Reflect.hasEnoughStamina(playerMP, threshold)) {
            if (cost > 0) {
                Reflect.decreaseFeathers(playerMP, cost);
            }
        } else {
            // Block the jump by setting vertical velocity to 0
            player.motionY = 0.0;
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || player.capabilities.isCreativeMode || player.isSpectator()) return;

        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.enableAttackCost) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack held = playerMP.getHeldItemMainhand();
        WeaponType type = getWeaponType(held);

        if (type == WeaponType.NONE) return;

        int cost = (type == WeaponType.LIGHT) ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.lightCost : 
                   (type == WeaponType.HEAVY ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.heavyCost : ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.mediumCost);
        cost = StaminaPerks.meleeCost(playerMP, cost);

        if (StaminaPerks.tryPowerAttack(playerMP, type, cost)) {
            return;
        }

        double multiplier = (type == WeaponType.LIGHT) ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.lightDamageMultiplier : 
                             (type == WeaponType.HEAVY ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.heavyDamageMultiplier : ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.mediumDamageMultiplier);

        int currentFeathers = FeathersHelper.getFeatherLevel(playerMP);

        if (cost <= 0 || Reflect.hasEnoughStamina(playerMP, cost)) {
            if (cost > 0) {
                Reflect.decreaseFeathers(playerMP, cost);
            }
        } else {
            // Drain remaining usable feathers
            int absorption = Reflect.getAbsorptionFeathers(playerMP);
            int weight = Reflect.getWeight(playerMP);
            int totalUsable = (currentFeathers - weight) + absorption;
            if (totalUsable > 0) {
                Reflect.decreaseFeathers(playerMP, totalUsable);
            }
            // Set attack penalty
            playerMP.getEntityData().setDouble("StaminaTweaksAttackPenalty", multiplier);
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
        if (player == null || player.world.isRemote || player.capabilities.isCreativeMode || player.isSpectator()) return;
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.enableAttackCost) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack held = playerMP.getHeldItemMainhand();
        WeaponType type = getWeaponType(held);
        if (type == WeaponType.NONE) return;

        int cost = (type == WeaponType.LIGHT) ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.lightCost : 
                   (type == WeaponType.HEAVY ? ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.heavyCost : ArcanaQuestTweaksConfig.StaminaModuleConfig.weapons.mediumCost);
        cost = StaminaPerks.meleeCost(playerMP, cost);

        if (cost > 0 && Reflect.hasEnoughStamina(playerMP, cost)) {
            Reflect.decreaseFeathers(playerMP, cost);
        }
    }

    @SubscribeEvent
    public void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || player.capabilities.isCreativeMode || player.isSpectator()) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack stack = event.getItem();
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof ItemBow) {
            if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.enableBowCost) return;
            int drawCost = StaminaPerks.bowDrawCost(playerMP,
                    ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowDrawCost);
            if (Reflect.hasEnoughStamina(playerMP, drawCost)) {
                Reflect.decreaseFeathers(playerMP, drawCost);
            } else {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || player.capabilities.isCreativeMode || player.isSpectator()) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack stack = event.getItem();
        if (stack.isEmpty()) return;

        int duration = event.getDuration();
        int ticksUsed = stack.getMaxItemUseDuration() - duration;

        if (stack.getItem() instanceof ItemBow) {
            if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.enableBowCost) return;
            int interval = StaminaPerks.bowHoldInterval(player,
                    ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldInterval);
            if (ticksUsed > 0 && ticksUsed % interval == 0) {
                int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.bowDrawing.bowHoldCost;
                if (Reflect.hasEnoughStamina(playerMP, cost)) {
                    Reflect.decreaseFeathers(playerMP, cost);
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
        if (player.world.isRemote || player.capabilities.isCreativeMode || player.isSpectator()) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack stack = event.getItem();
        if (stack.isEmpty()) return;

        if (isThrowingWeapon(stack)) {
            if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.throwingWeapons.enableThrowingCost) return;
            int releaseCost = ArcanaQuestTweaksConfig.StaminaModuleConfig.throwingWeapons.throwingReleaseCost;
            if (Reflect.hasEnoughStamina(playerMP, releaseCost)) {
                Reflect.decreaseFeathers(playerMP, releaseCost);
            } else {
                event.setCanceled(true);
                playerMP.resetActiveHand();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingAttack(LivingAttackEvent event) {
        StaminaPerks.tryEvasion(event);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase hurt = event.getEntityLiving();
        if (hurt == null || hurt.world.isRemote) return;

        if (hurt instanceof EntityPlayer victim) {
            StaminaPerks.tryAdrenaline(victim, event.getAmount());
        }

        Entity attacker = event.getSource().getTrueSource();
        StaminaPerks.applyOutgoingMeleeModifiers(attacker, event);
    }

    private void handleServerShieldBlocking(EntityPlayerMP player, NBTTagCompound data) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.shield.enableShieldCost) return;

        boolean wasBlocking = data.getBoolean("StaminaTweaksShieldActive");
        boolean isBlocking = player.isActiveItemStackBlocking();

        if (isBlocking) {
            int interval = StaminaPerks.shieldHoldInterval(player,
                    ArcanaQuestTweaksConfig.StaminaModuleConfig.shield.shieldHoldInterval);
            int cost = ArcanaQuestTweaksConfig.StaminaModuleConfig.shield.shieldHoldCost;

            if (!wasBlocking) {
                data.setBoolean("StaminaTweaksShieldActive", true);
                data.setInteger("StaminaTweaksShieldTicks", 0);
            } else {
                if (interval <= 0) {
                    data.setBoolean("StaminaTweaksShieldActive", false);
                    return;
                }

                int ticks = data.getInteger("StaminaTweaksShieldTicks") + 1;
                if (ticks >= interval) {
                    if (Reflect.hasEnoughStamina(player, cost)) {
                        Reflect.decreaseFeathers(player, cost);
                        ticks = 0;
                    } else {
                        player.resetActiveHand();
                        data.setBoolean("StaminaTweaksShieldActive", false);
                        ticks = 0;
                    }
                }
                data.setInteger("StaminaTweaksShieldTicks", ticks);
            }
        } else {
            if (wasBlocking) {
                data.setBoolean("StaminaTweaksShieldActive", false);
                data.setInteger("StaminaTweaksShieldTicks", 0);
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(net.minecraftforge.event.world.BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote || player.capabilities.isCreativeMode || player.isSpectator()) return;

        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.enableMiningCost) return;

        Block block = event.getState().getBlock();
        boolean isOreOrObsidian = block == net.minecraft.init.Blocks.OBSIDIAN || 
                                  (block.getRegistryName() != null && block.getRegistryName().toString().toLowerCase().contains("ore"));

        int cost = isOreOrObsidian ? ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.oreCost : ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.defaultCost;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.enableReskillable && 
            Reflect.hasUnlockable(playerMP, ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.miningEfficiencyPerkId)) {
            cost = Math.max(0, cost - ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.miningEfficiencyReduction);
        }
        cost = StaminaPerks.gatheringForageCost(playerMP, cost, event.getWorld(), event.getState());
        if (cost <= 0) return;
        if (Reflect.hasEnoughStamina(playerMP, cost)) {
            Reflect.decreaseFeathers(playerMP, cost);
        } else {
            int currentFeathers = FeathersHelper.getFeatherLevel(playerMP);
            int weight = Reflect.getWeight(playerMP);
            int usable = currentFeathers - weight;
            int absorption = Reflect.getAbsorptionFeathers(playerMP);
            int totalUsable = usable + absorption;
            if (totalUsable > 0) {
                Reflect.decreaseFeathers(playerMP, totalUsable);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) event.player;
            FeathersHelper.increaseFeathers(playerMP, FeathersHelper.getMaxFeatherLevel(playerMP));
        }
    }
}
