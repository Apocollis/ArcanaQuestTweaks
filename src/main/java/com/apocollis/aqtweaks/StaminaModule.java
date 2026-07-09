package com.apocollis.aqtweaks;

import com.elenai.elenaidodge2.api.FeathersHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class StaminaModule {

    private String[] weightsBackup = null;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinWorldHighest(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP && !event.getWorld().isRemote) {
            if (com.elenai.elenaidodge2.ModConfig.common != null &&
                com.elenai.elenaidodge2.ModConfig.common.weights != null) {
                weightsBackup = com.elenai.elenaidodge2.ModConfig.common.weights.weights;
                com.elenai.elenaidodge2.ModConfig.common.weights.weights = new String[0];
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorldLowest(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP && !event.getWorld().isRemote) {
            if (com.elenai.elenaidodge2.ModConfig.common != null &&
                com.elenai.elenaidodge2.ModConfig.common.weights != null &&
                weightsBackup != null) {
                com.elenai.elenaidodge2.ModConfig.common.weights.weights = weightsBackup;
                weightsBackup = null;
            }
        }
    }

    public enum WeaponType {
        NONE, LIGHT, MEDIUM, HEAVY
    }

    public static WeaponType getWeaponType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return WeaponType.LIGHT;
        Item item = stack.getItem();
        if (item.getRegistryName() == null) return WeaponType.NONE;
        String name = item.getRegistryName().toString();

        // Check custom lists
        if (ArcanaQuestTweaksConfig.staminaModule.weapons.lightWeaponsCustom != null) {
            for (String s : ArcanaQuestTweaksConfig.staminaModule.weapons.lightWeaponsCustom) {
                if (name.equals(s)) return WeaponType.LIGHT;
            }
        }
        if (ArcanaQuestTweaksConfig.staminaModule.weapons.mediumWeaponsCustom != null) {
            for (String s : ArcanaQuestTweaksConfig.staminaModule.weapons.mediumWeaponsCustom) {
                if (name.equals(s)) return WeaponType.MEDIUM;
            }
        }
        if (ArcanaQuestTweaksConfig.staminaModule.weapons.heavyWeaponsCustom != null) {
            for (String s : ArcanaQuestTweaksConfig.staminaModule.weapons.heavyWeaponsCustom) {
                if (name.equals(s)) return WeaponType.HEAVY;
            }
        }

        // Keyword checks for modded weapons (e.g. Spartan Weaponry)
        String path = item.getRegistryName().getResourcePath().toLowerCase();
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

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        EntityPlayer player = event.player;
        if (player == null || Reflect.isCreative(player) || player.isSpectator()) return;

        if (!player.world.isRemote) {
            // Server side authority
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            handleServerBowDrawing(playerMP);
            handleServerClimbing(playerMP);
            handleServerGrappling(playerMP);
            handleServerGliding(playerMP);
            handleServerShieldBlocking(playerMP);

            // Mining Fatigue exhaustion check
            if (ArcanaQuestTweaksConfig.staminaModule.mining.enableMiningCost) {
                int regularFeathers = FeathersHelper.getFeatherLevel(playerMP);
                if (regularFeathers <= ArcanaQuestTweaksConfig.staminaModule.mining.miningFatigueThreshold) {
                    playerMP.addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.init.MobEffects.MINING_FATIGUE, 40, 2, true, false));
                }
            }

            // Simple Difficulty thirst cost for feather regeneration
            if (ArcanaQuestTweaksConfig.staminaModule.simpleDifficulty.enableThirstCost) {
                int currentFeathers = FeathersHelper.getFeatherLevel(playerMP);
                String key = "StaminaTweaksPrevFeathers";
                if (playerMP.getEntityData().hasKey(key)) {
                    int prevFeathers = playerMP.getEntityData().getInteger(key);
                    if (currentFeathers > prevFeathers) {
                        int diff = currentFeathers - prevFeathers;
                        float exhaustion = diff * (float) ArcanaQuestTweaksConfig.staminaModule.simpleDifficulty.thirstExhaustionPerFeather;
                        Reflect.addThirstExhaustion(playerMP, exhaustion);
                    }
                }
                playerMP.getEntityData().setInteger(key, currentFeathers);
            }
        }
    }

    private void handleServerBowDrawing(EntityPlayerMP player) {
        if (!ArcanaQuestTweaksConfig.staminaModule.bowDrawing.enableBowCost) return;

        boolean isDrawing = player.isHandActive() && player.getActiveItemStack().getItem() instanceof ItemBow;
        boolean wasDrawing = player.getEntityData().getBoolean("StaminaTweaksBowActive");

        if (isDrawing) {
            if (!wasDrawing) {
                // Initial draw cost
                int cost = ArcanaQuestTweaksConfig.staminaModule.bowDrawing.bowDrawCost;
                if (Reflect.hasEnoughStamina(player, cost)) {
                    FeathersHelper.decreaseFeathers(player, cost);
                    player.getEntityData().setBoolean("StaminaTweaksBowActive", true);
                    player.getEntityData().setInteger("StaminaTweaksBowTicks", 0);
                } else {
                    Reflect.resetActiveHand(player);
                    player.getEntityData().setBoolean("StaminaTweaksBowActive", false);
                }
            } else {
                // Holding cost over time
                int ticks = player.getEntityData().getInteger("StaminaTweaksBowTicks") + 1;
                if (ticks >= ArcanaQuestTweaksConfig.staminaModule.bowDrawing.bowHoldInterval) {
                    int cost = ArcanaQuestTweaksConfig.staminaModule.bowDrawing.bowHoldCost;
                    if (Reflect.hasEnoughStamina(player, cost)) {
                        FeathersHelper.decreaseFeathers(player, cost);
                        ticks = 0;
                    } else {
                        Reflect.resetActiveHand(player);
                        player.getEntityData().setBoolean("StaminaTweaksBowActive", false);
                        ticks = 0;
                    }
                }
                player.getEntityData().setInteger("StaminaTweaksBowTicks", ticks);
            }
        } else {
            if (wasDrawing) {
                player.getEntityData().setBoolean("StaminaTweaksBowActive", false);
                player.getEntityData().setInteger("StaminaTweaksBowTicks", 0);
            }
        }
    }

    private void handleServerClimbing(EntityPlayerMP player) {
        if (!ArcanaQuestTweaksConfig.staminaModule.climbing.enableClimbCost) return;

        if (Reflect.isOnLadder(player)) {
            int x = MathHelper.floor(player.posX);
            int y = MathHelper.floor(player.getEntityBoundingBox().minY);
            int z = MathHelper.floor(player.posZ);
            BlockPos pos = new BlockPos(x, y, z);
            Block block = player.world.getBlockState(pos).getBlock();

            boolean isRope = Reflect.isRopeBlock(block);
            boolean isVine = !isRope && (block instanceof BlockVine || block.getClass().getSimpleName().toLowerCase().contains("vine"));
            
            int interval;
            int cost;
            boolean costEnabled;
            
            if (isRope) {
                interval = ArcanaQuestTweaksConfig.staminaModule.climbing.ropeInterval;
                cost = ArcanaQuestTweaksConfig.staminaModule.climbing.ropeCost;
                costEnabled = ArcanaQuestTweaksConfig.staminaModule.climbing.enableRopeCost;
            } else if (isVine) {
                interval = ArcanaQuestTweaksConfig.staminaModule.climbing.vineInterval;
                cost = ArcanaQuestTweaksConfig.staminaModule.climbing.vineCost;
                costEnabled = true;
            } else {
                interval = ArcanaQuestTweaksConfig.staminaModule.climbing.ladderInterval;
                cost = ArcanaQuestTweaksConfig.staminaModule.climbing.ladderCost;
                costEnabled = true;
            }

            if (!costEnabled) return;

            String yKey = "StaminaTweaksClimbPrevY";
            boolean isJumpPressed = player.getEntityData().getBoolean("StaminaTweaksClimbJumpInput");
            boolean isClimbing;
            if (player.getEntityData().hasKey(yKey)) {
                double prevY = player.getEntityData().getDouble(yKey);
                isClimbing = isJumpPressed || (player.posY > prevY + 0.001) || player.isSneaking();
            } else {
                // First tick on ladder — sneaking or jumping counts
                isClimbing = isJumpPressed || player.isSneaking();
            }
            player.getEntityData().setDouble(yKey, player.posY);

            if (isClimbing) {
                // Calculate sub-interval for granular charging (1 half-feather per sub-interval)
                int singleInterval = Math.max(1, interval / Math.max(1, cost));

                if (Reflect.hasEnoughStamina(player, 1)) {
                    int ticks = player.getEntityData().getInteger("StaminaTweaksClimbTicks") + 1;
                    if (ticks >= singleInterval) {
                        FeathersHelper.decreaseFeathers(player, 1);
                        ticks = 0;
                    }
                    player.getEntityData().setInteger("StaminaTweaksClimbTicks", ticks);
                } else {
                    player.getEntityData().setInteger("StaminaTweaksClimbTicks", 0);
                    if (ArcanaQuestTweaksConfig.staminaModule.climbing.fallOnDepleted) {
                        Reflect.setMotionY(player, -0.15);
                    }
                }
            } else {
                // Slowly decay climb ticks instead of resetting to 0 instantly
                int ticks = player.getEntityData().getInteger("StaminaTweaksClimbTicks");
                if (ticks > 0) {
                    player.getEntityData().setInteger("StaminaTweaksClimbTicks", ticks - 1);
                }
            }

            // If feathers are completely depleted, slide down
            if (!Reflect.hasEnoughStamina(player, 1) && ArcanaQuestTweaksConfig.staminaModule.climbing.fallOnDepleted) {
                Reflect.setMotionY(player, -0.15);
            }
        } else {
            // Decay climb ticks when off the ladder completely
            int ticks = player.getEntityData().getInteger("StaminaTweaksClimbTicks");
            if (ticks > 0) {
                player.getEntityData().setInteger("StaminaTweaksClimbTicks", ticks - 1);
            }
            player.getEntityData().removeTag("StaminaTweaksClimbPrevY");
            player.getEntityData().removeTag("StaminaTweaksClimbJumpInput");
        }
    }

    private void handleServerGrappling(EntityPlayerMP player) {
        if (!Reflect.isGrappleLoaded() || !ArcanaQuestTweaksConfig.staminaModule.grapple.enableGrappleCost) return;

        if (Reflect.isGrappling(player)) {
            int cost = ArcanaQuestTweaksConfig.staminaModule.grapple.grappleHoldCost;

            if (Reflect.hasEnoughStamina(player, cost)) {
                int ticks = player.getEntityData().getInteger("StaminaTweaksGrappleTicks") + 1;
                if (ticks >= ArcanaQuestTweaksConfig.staminaModule.grapple.grappleHoldInterval) {
                    FeathersHelper.decreaseFeathers(player, cost);
                    ticks = 0;
                }
                player.getEntityData().setInteger("StaminaTweaksGrappleTicks", ticks);
            } else {
                // Detach grapple hook
                Reflect.detachGrapple(player);
                player.getEntityData().setInteger("StaminaTweaksGrappleTicks", 0);
            }
        } else {
            player.getEntityData().setInteger("StaminaTweaksGrappleTicks", 0);
        }
    }

    private void handleServerGliding(EntityPlayerMP player) {
        if (!Reflect.isGliderLoaded() || !ArcanaQuestTweaksConfig.staminaModule.glider.enableGliderCost) return;

        if (Reflect.isGliding(player)) {
            int cost = ArcanaQuestTweaksConfig.staminaModule.glider.gliderCost;

            if (Reflect.hasEnoughStamina(player, cost)) {
                int ticks = player.getEntityData().getInteger("StaminaTweaksGliderTicks") + 1;
                if (ticks >= ArcanaQuestTweaksConfig.staminaModule.glider.gliderInterval) {
                    FeathersHelper.decreaseFeathers(player, cost);
                    ticks = 0;
                }
                player.getEntityData().setInteger("StaminaTweaksGliderTicks", ticks);
            } else {
                // Undeploy glider
                Reflect.undeployGlider(player);
                player.getEntityData().setInteger("StaminaTweaksGliderTicks", 0);
            }
        } else {
            player.getEntityData().setInteger("StaminaTweaksGliderTicks", 0);
        }
    }

    @SubscribeEvent
    public void onLivingJump(LivingJumpEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || Reflect.isCreative(player) || player.isSpectator()) return;

        if (!ArcanaQuestTweaksConfig.staminaModule.jumping.enableJumpCost) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        int threshold = ArcanaQuestTweaksConfig.staminaModule.jumping.jumpThreshold;
        int cost = ArcanaQuestTweaksConfig.staminaModule.jumping.jumpCost;

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
        if (player.world.isRemote || Reflect.isCreative(player) || player.isSpectator()) return;

        if (!ArcanaQuestTweaksConfig.staminaModule.weapons.enableAttackCost) return;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        ItemStack held = playerMP.getHeldItemMainhand();
        WeaponType type = getWeaponType(held);

        if (type == WeaponType.NONE) return;

        int cost = (type == WeaponType.LIGHT) ? ArcanaQuestTweaksConfig.staminaModule.weapons.lightCost : 
                   (type == WeaponType.HEAVY ? ArcanaQuestTweaksConfig.staminaModule.weapons.heavyCost : ArcanaQuestTweaksConfig.staminaModule.weapons.mediumCost);
        double multiplier = (type == WeaponType.LIGHT) ? ArcanaQuestTweaksConfig.staminaModule.weapons.lightDamageMultiplier : 
                             (type == WeaponType.HEAVY ? ArcanaQuestTweaksConfig.staminaModule.weapons.heavyDamageMultiplier : ArcanaQuestTweaksConfig.staminaModule.weapons.mediumDamageMultiplier);

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
            playerMP.getEntityData().setDouble("StaminaTweaksAttackPenalty", multiplier);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (source == null || !(source.getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
        if (attacker.world.isRemote) return;

        if (attacker.getEntityData().hasKey("StaminaTweaksAttackPenalty")) {
            double penalty = attacker.getEntityData().getDouble("StaminaTweaksAttackPenalty");
            event.setAmount((float) (event.getAmount() * penalty));
            attacker.getEntityData().removeTag("StaminaTweaksAttackPenalty");
        }
    }

    private void handleServerShieldBlocking(EntityPlayerMP player) {
        if (!ArcanaQuestTweaksConfig.staminaModule.shield.enableShieldCost) return;

        boolean isBlocking = player.isActiveItemStackBlocking();
        boolean wasBlocking = player.getEntityData().getBoolean("StaminaTweaksShieldActive");

        if (isBlocking) {
            if (!wasBlocking) {
                // Initial block cost
                int cost = ArcanaQuestTweaksConfig.staminaModule.shield.shieldRaiseCost;
                if (Reflect.hasEnoughStamina(player, cost)) {
                    FeathersHelper.decreaseFeathers(player, cost);
                    player.getEntityData().setBoolean("StaminaTweaksShieldActive", true);
                    player.getEntityData().setInteger("StaminaTweaksShieldTicks", 0);
                } else {
                    Reflect.resetActiveHand(player);
                    player.getEntityData().setBoolean("StaminaTweaksShieldActive", false);
                }
            } else {
                // Holding cost over time
                int ticks = player.getEntityData().getInteger("StaminaTweaksShieldTicks") + 1;
                if (ticks >= ArcanaQuestTweaksConfig.staminaModule.shield.shieldHoldInterval) {
                    int cost = ArcanaQuestTweaksConfig.staminaModule.shield.shieldHoldCost;
                    if (Reflect.hasEnoughStamina(player, cost)) {
                        FeathersHelper.decreaseFeathers(player, cost);
                        ticks = 0;
                    } else {
                        Reflect.resetActiveHand(player);
                        player.getEntityData().setBoolean("StaminaTweaksShieldActive", false);
                        ticks = 0;
                    }
                }
                player.getEntityData().setInteger("StaminaTweaksShieldTicks", ticks);
            }
        } else {
            if (wasBlocking) {
                player.getEntityData().setBoolean("StaminaTweaksShieldActive", false);
                player.getEntityData().setInteger("StaminaTweaksShieldTicks", 0);
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(net.minecraftforge.event.world.BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote || Reflect.isCreative(player) || player.isSpectator()) return;

        if (!ArcanaQuestTweaksConfig.staminaModule.mining.enableMiningCost) return;

        Block block = event.getState().getBlock();
        boolean isOreOrObsidian = block == net.minecraft.init.Blocks.OBSIDIAN || 
                                  (block.getRegistryName() != null && block.getRegistryName().toString().toLowerCase().contains("ore"));

        int cost = isOreOrObsidian ? ArcanaQuestTweaksConfig.staminaModule.mining.oreCost : ArcanaQuestTweaksConfig.staminaModule.mining.defaultCost;

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        if (ArcanaQuestTweaksConfig.staminaModule.reskillable.enableReskillable && 
            Reflect.hasUnlockable(playerMP, ArcanaQuestTweaksConfig.staminaModule.reskillable.miningEfficiencyPerkId)) {
            cost = Math.max(0, cost - ArcanaQuestTweaksConfig.staminaModule.reskillable.miningEfficiencyReduction);
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
            FeathersHelper.increaseFeathers(playerMP, 20);
        }
    }
}
