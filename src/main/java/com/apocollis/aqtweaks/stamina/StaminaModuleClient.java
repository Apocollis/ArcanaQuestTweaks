package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import com.elenai.elenaidodge2.ModConfig;
import com.elenai.elenaidodge2.gui.DodgeGui;
import com.elenai.elenaidodge2.util.ClientStorage;
import com.elenai.elenaidodge2.util.PatronRewardHandler;
import com.elenai.elenaidodge2.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class StaminaModuleClient {

    /** Wall probe heights for the mantle scan. Read-only — never write into this array. */
    private static final double[] LEDGE_CHECK_HEIGHTS = {0.4D, 0.7D, 1.0D, 1.3D, 1.6D};

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderDodgeGUI(RenderGameOverlayEvent.Post event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;
        Minecraft mc = Minecraft.getMinecraft();

        // Only render if the dodge trait is NOT unlocked
        // (if it is unlocked, Elenai Dodge 2's DodgeGui will render it)
        if (Utils.dodgeTraitUnlocked(player)) return;

        // Perform visibility checks from DodgeGui
        if (ModConfig.client == null || ModConfig.client.hud == null) return;
        if (!ModConfig.client.hud.hud) return;

        if (player.capabilities.isCreativeMode || player.isSpectator()) return;

        boolean compatHud = ModConfig.client.hud.compatHud;
        ElementType type = event.getType();

        // Match Extended DodgeGui overlay events so locked-dodge HUD sits on the same pass
        if ((type == ElementType.ALL && compatHud) || (type == ElementType.FOOD && !compatHud)) {
            // Render
            mc.getTextureManager().bindTexture(DodgeGui.DODGE_ICONS);
            GlStateManager.enableBlend();
            DodgeGui.enableAlpha(DodgeGui.alpha);

            if (DodgeGui.alpha > 0.0F) {
                int height = event.getResolution().getScaledHeight();
                int width = event.getResolution().getScaledWidth();

                DodgeGui.renderFeathers(
                        height,
                        width,
                        ClientStorage.dodges,
                        ClientStorage.weight,
                        ClientStorage.healing,
                        16, 25, 34, 43, 52, 61, 70,
                        PatronRewardHandler.localPatronTier
                );

                DodgeGui.renderAbsorptionFeathers(
                        height,
                        width,
                        ClientStorage.absorption,
                        ClientStorage.weight,
                        ClientStorage.healing,
                        79, 88
                );
            }

            DodgeGui.disableAlpha(DodgeGui.alpha);
            mc.getTextureManager().bindTexture(Gui.ICONS);
            GlStateManager.disableBlend();
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onPlayerTick(net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START) return;

        EntityPlayer player = event.player;
        if (player == null || player.capabilities.isCreativeMode || player.isSpectator()) return;

        // Only run for the local player on the client side
        EntityPlayer localPlayer = Minecraft.getMinecraft().player;
        if (player == localPlayer) {
            // Restore ClientStorage.weightValues from local config if it was cleared/sync-bypassed
            if (ClientStorage.weightValues == null || ClientStorage.weightValues.isEmpty()) {
                if (ModConfig.common != null && ModConfig.common.weights != null && ModConfig.common.weights.weights != null) {
                    ClientStorage.weightValues = Utils.arrayToString(ModConfig.common.weights.weights);
                    
                    // Force Elenai Dodge 2 to re-evaluate equipped armor weight by clearing previousArmor cache
                    if (com.elenai.elenaidodge2.event.ArmorTickEventListener.previousArmor != null) {
                        com.elenai.elenaidodge2.event.ArmorTickEventListener.previousArmor.clear();
                    }
                }
            }
            handleClientClimbing((net.minecraft.client.entity.EntityPlayerSP) player);
            handleClientLedgeClimbing((net.minecraft.client.entity.EntityPlayerSP) player);
            handleClientSprinting((net.minecraft.client.entity.EntityPlayerSP) player);
        }
    }

    @SideOnly(Side.CLIENT)
    private void handleClientSprinting(net.minecraft.client.entity.EntityPlayerSP player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.sprinting.enableSprintCost) return;

        int threshold = ArcanaQuestTweaksConfig.StaminaModuleConfig.sprinting.sprintThreshold;
        if (Reflect.hasEnoughStamina(player, threshold)) return;

        player.setSprinting(false);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameSettings != null) {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    @SideOnly(Side.CLIENT)
    public void onClientTickLowest(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) return;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;
        if (player.isPotionActive(com.elenai.elenaidodge2.init.PotionInit.WEIGHT_EFFECT)) return;

        // Enforce Armor Mastery weight reduction on the client side
        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.enableReskillable && 
            Reflect.hasUnlockable(player, ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.armorMasteryPerkId)) {
            int reducedWeight = Reflect.getWeight(player);
            if (ClientStorage.weight != reducedWeight) {
                ClientStorage.weight = reducedWeight;
                com.elenai.elenaidodge2.network.PacketHandler.instance.sendToServer(
                    new com.elenai.elenaidodge2.network.message.SWeightMessage(reducedWeight)
                );
            }
        }
    }

    /**
     * Perk-adjusted climb cost for the climbable the player is on, or -1 when this climbable is
     * exempt. Must stay in step with the server's billing in {@code StaminaModule.handleServerClimbing}:
     * using the raw config value here let Expert Climber slide the client while the server allowed
     * the climb.
     */
    @SideOnly(Side.CLIENT)
    private static int clientClimbCost(EntityPlayer player) {
        int x = net.minecraft.util.math.MathHelper.floor(player.posX);
        int y = net.minecraft.util.math.MathHelper.floor(player.getEntityBoundingBox().minY);
        int z = net.minecraft.util.math.MathHelper.floor(player.posZ);
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
        net.minecraft.world.World world = player.world;
        net.minecraft.block.Block block = world != null ? world.getBlockState(pos).getBlock() : net.minecraft.init.Blocks.AIR;

        boolean isRope = Reflect.isRopeBlock(block);
        boolean isVine = !isRope && (block instanceof net.minecraft.block.BlockVine
                || block.getClass().getSimpleName().toLowerCase().contains("vine"));

        ArcanaQuestTweaksConfig.Climbing climbing = ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing;
        if (isRope && !climbing.enableRopeCost) return -1;

        int base = isRope ? climbing.ropeCost : (isVine ? climbing.vineCost : climbing.ladderCost);
        return StaminaPerks.climbCost(player, base);
    }

    @SideOnly(Side.CLIENT)
    private void handleClientClimbing(net.minecraft.client.entity.EntityPlayerSP player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.enableClimbCost) return;

        if (player.isOnLadder()) {
            // Keep jump input synced (used by other climb edge cases / older servers)
            boolean isJumpPressed = Reflect.isJumpPressed(player);
            ArcanaQuestTweaks.NETWORK.sendToServer(new PacketSyncClimbingInput(isJumpPressed));
            player.getEntityData().setBoolean("StaminaTweaksLastJumpInput", isJumpPressed);

            if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.fallOnDepleted) return;

            int cost = clientClimbCost(player);
            if (cost < 0) return;

            NBTTagCompound climbData = player.getEntityData();
            boolean ledgeActive = climbData.getInteger("StaminaTweaksLedgeClimbState") == 1;
            boolean mantleIntent = Reflect.isJumpPressed(player) && Reflect.getMoveForward(player) > 0.0F;

            if (!ledgeActive && !mantleIntent && cost > 0 && !Reflect.hasEnoughStamina(player, cost)) {
                player.motionY = -0.15;
            }
        } else {
            NBTTagCompound clientData = player.getEntityData();
            if (clientData.getBoolean("StaminaTweaksLastJumpInput")) {
                ArcanaQuestTweaks.NETWORK.sendToServer(new PacketSyncClimbingInput(false));
                clientData.setBoolean("StaminaTweaksLastJumpInput", false);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void handleClientGrappling(EntityPlayer player) {
        ArcanaQuestTweaksConfig.Grapple grapple = ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple;
        if (!grapple.enableGrappleCost && !grapple.motorRequiresEmber) return;
        if (!Reflect.isGrappleLoaded()) return;

        NBTTagCompound data = player.getEntityData();
        if (!Reflect.isGrappling(player)) {
            if (data.getInteger("StaminaTweaksGrappleClientSentTick") > 0) {
                data.setInteger("StaminaTweaksGrappleClientSentTick", 0);
            }
            return;
        }

        int mode = GrappleClientInput.getMode(player);
        boolean motor = GrappleClientInput.isMotorPulling(player);
        boolean grounded = GrappleClientInput.isStandingOnGround(player);
        int now = player.ticksExisted;
        int lastTick = data.getInteger("StaminaTweaksGrappleClientSentTick");
        int lastMode = data.getInteger("StaminaTweaksGrappleClientSentMode");
        boolean lastMotor = data.getBoolean("StaminaTweaksGrappleClientSentMotor");
        boolean lastGrounded = data.getBoolean("StaminaTweaksGrappleClientSentGrounded");
        boolean sentOnce = lastTick > 0;

        if (!sentOnce || mode != lastMode || motor != lastMotor || grounded != lastGrounded || now - lastTick >= 10) {
            ArcanaQuestTweaks.NETWORK.sendToServer(new PacketSyncGrappleInput(mode, motor, grounded));
            data.setInteger("StaminaTweaksGrappleClientSentTick", Math.max(now, 1));
            data.setInteger("StaminaTweaksGrappleClientSentMode", mode);
            data.setBoolean("StaminaTweaksGrappleClientSentMotor", motor);
            data.setBoolean("StaminaTweaksGrappleClientSentGrounded", grounded);
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    @SideOnly(Side.CLIENT)
    public void onGrappleInputUpdate(net.minecraftforge.client.event.InputUpdateEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.capabilities.isCreativeMode || player.isSpectator()) return;
        handleClientGrappling(player);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onInputUpdate(net.minecraftforge.client.event.InputUpdateEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.capabilities.isCreativeMode || player.isSpectator()) return;

        if (isLedgeRecovering(player)) {
            net.minecraft.util.MovementInput input = event.getMovementInput();
            if (input != null) {
                input.jump = false;
                input.moveForward = 0.0F;
                input.moveStrafe = 0.0F;
            }
        }

        if (!player.isOnLadder()) return;
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.enableClimbCost) return;
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.fallOnDepleted) return;

        int cost = clientClimbCost(player);

        if (cost > 0 && !Reflect.hasEnoughStamina(player, cost)) {
            NBTTagCompound climbData = player.getEntityData();
            boolean ledgeActive = climbData.getInteger("StaminaTweaksLedgeClimbState") == 1;
            boolean mantleIntent = Reflect.isJumpPressed(player) && Reflect.getMoveForward(player) > 0.0F;
            if (!ledgeActive && !mantleIntent) {
                Reflect.setJumpPressed(player, false);
                Reflect.setSneakPressed(player, false);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void handleClientLedgeClimbing(net.minecraft.client.entity.EntityPlayerSP player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.ledgeClimb.enableLedgeClimb) return;

        NBTTagCompound clientData = player.getEntityData();
        int state = clientData.getInteger("StaminaTweaksLedgeClimbState");

        if (state == 0) {
            if (isLedgeRecovering(player)) {
                clientData.setInteger("StaminaTweaksLedgeClimbHeldTicks", 0);
                return;
            }

            // Check target conditions (vines/ladders at the face must not block a 1-block mantle)
            if (player.onGround || player.isInWater() || player.isInLava() || player.isRiding()) {
                clientData.setInteger("StaminaTweaksLedgeClimbHeldTicks", 0);
                return;
            }

            // Must hold forward and jump
            if (!Reflect.isJumpPressed(player) || Reflect.getMoveForward(player) <= 0.0F) {
                clientData.setInteger("StaminaTweaksLedgeClimbHeldTicks", 0);
                return;
            }

            // Update consecutive held ticks
            int heldTicks = clientData.getInteger("StaminaTweaksLedgeClimbHeldTicks") + 1;
            clientData.setInteger("StaminaTweaksLedgeClimbHeldTicks", heldTicks);

            // Must hold for at least 5 ticks
            if (heldTicks < 5) return;

            // Only attempt climb when falling or at peak of jump (motionY <= 0.0)
            if (player.motionY > 0.0) return;

            double yawRad = Math.toRadians(player.rotationYaw);
            double dx = -Math.sin(yawRad);
            double dz = Math.cos(yawRad);

            net.minecraft.world.World world = player.world;
            if (world == null) return;

            double posX = player.posX;
            double posY = player.posY;
            double posZ = player.posZ;

            MantleLip lip = null;

            // Only the block the player is pressed against (look * 0.7 stays in the adjacent cell)
            for (double h : LEDGE_CHECK_HEIGHTS) {
                int wallX = net.minecraft.util.math.MathHelper.floor(posX + dx * 0.7D);
                int wallY = net.minecraft.util.math.MathHelper.floor(posY + h);
                int wallZ = net.minecraft.util.math.MathHelper.floor(posZ + dz * 0.7D);
                BlockPos wallPos = new BlockPos(wallX, wallY, wallZ);
                IBlockState wallState = world.getBlockState(wallPos);
                if (isCollisionEmpty(world, wallPos, wallState)) {
                    continue;
                }

                lip = findMantleLip(world, wallPos, wallState);
                if (lip != null) {
                    break;
                }
            }

            if (lip != null) {
                int grabCost = StaminaPerks.climbCost(player,
                        ArcanaQuestTweaksConfig.StaminaModuleConfig.ledgeClimb.ledgeClimbCost);
                if (grabCost > 0 && !Reflect.hasEnoughStamina(player, grabCost)) {
                    return;
                }

                ArcanaQuestTweaks.NETWORK.sendToServer(new PacketLedgeClimb());

                clientData.setInteger("StaminaTweaksLedgeClimbState", 1);
                clientData.setDouble("StaminaTweaksLedgeClimbTargetY", lip.topY);
                clientData.setInteger("StaminaTweaksLedgeClimbLipX", lip.pos.getX());
                clientData.setInteger("StaminaTweaksLedgeClimbLipY", lip.pos.getY());
                clientData.setInteger("StaminaTweaksLedgeClimbLipZ", lip.pos.getZ());
                clientData.setInteger("StaminaTweaksLedgeClimbHeldTicks", 0);
                clientData.setInteger("StaminaTweaksLedgeMantleTicks", 0);
                clientData.setDouble("StaminaTweaksLedgeClimbLastY", player.posY);

                player.fallDistance = 0.0F;
                player.motionY = 0.090625D;
                player.motionX = 0.0D;
                player.motionZ = 0.0D;
            }
        } else if (state == 1) {
            if (player.isInWater() || player.isInLava() || player.isRiding()) {
                clientData.setInteger("StaminaTweaksLedgeClimbState", 0);
                clientData.setInteger("StaminaTweaksLedgeMantleTicks", 0);
                player.fallDistance = 0.0F;
                return;
            }

            if (!Reflect.isJumpPressed(player) || Reflect.getMoveForward(player) <= 0.0F) {
                clientData.setInteger("StaminaTweaksLedgeClimbState", 0);
                clientData.setInteger("StaminaTweaksLedgeMantleTicks", 0);
                player.fallDistance = 0.0F;
                return;
            }

            ArcanaQuestTweaksConfig.LedgeClimb ledge = ArcanaQuestTweaksConfig.StaminaModuleConfig.ledgeClimb;
            int ticks = clientData.getInteger("StaminaTweaksLedgeMantleTicks") + 1;
            clientData.setInteger("StaminaTweaksLedgeMantleTicks", ticks);
            int extraCost = StaminaPerks.climbCost(player, ledge.ledgeClimbExtraCost);
            if (extraCost > 0
                    && ticks > ledge.ledgeClimbExtraAfterTicks
                    && ticks <= ledge.ledgeClimbExtraAfterTicks + ledge.ledgeClimbExtraInterval * ledge.ledgeClimbMaxExtraSpends
                    && (ticks - ledge.ledgeClimbExtraAfterTicks) % ledge.ledgeClimbExtraInterval == 0
                    && !Reflect.hasEnoughStamina(player, extraCost)) {
                clientData.setInteger("StaminaTweaksLedgeClimbState", 0);
                clientData.setInteger("StaminaTweaksLedgeMantleTicks", 0);
                player.motionY = -0.15D;
                return;
            }

            double targetY = clientData.getDouble("StaminaTweaksLedgeClimbTargetY");
            player.fallDistance = 0.0F;

            boolean stuck = ticks >= 4
                    && player.posY < targetY
                    && Math.abs(player.posY - clientData.getDouble("StaminaTweaksLedgeClimbLastY")) < 0.02D;
            clientData.setDouble("StaminaTweaksLedgeClimbLastY", player.posY);

            if (player.posY >= targetY || stuck) {
                double landY = targetY + 0.1D;
                BlockPos lipPos = new BlockPos(
                        clientData.getInteger("StaminaTweaksLedgeClimbLipX"),
                        clientData.getInteger("StaminaTweaksLedgeClimbLipY"),
                        clientData.getInteger("StaminaTweaksLedgeClimbLipZ"));
                double[] land = nearestLipXZ(player.world, lipPos, player.posX, player.posZ);
                player.setPosition(land[0], landY, land[1]);
                player.motionX = 0.0D;
                player.motionZ = 0.0D;
                player.motionY = 0.0D;
                player.onGround = true;
                player.fallDistance = 0.0F;
                clientData.setInteger("StaminaTweaksLedgeClimbState", 0);
                clientData.setInteger("StaminaTweaksLedgeMantleTicks", 0);
                int pause = ledge.ledgeClimbLandPauseTicks;
                if (pause > 0) {
                    clientData.setInteger("StaminaTweaksLedgeClimbRecoverUntil", player.ticksExisted + pause);
                }
            } else {
                player.motionY = 0.090625D;
                player.motionX = 0.0D;
                player.motionZ = 0.0D;
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static boolean isLedgeRecovering(EntityPlayer player) {
        return player.ticksExisted < player.getEntityData().getInteger("StaminaTweaksLedgeClimbRecoverUntil");
    }

    @SideOnly(Side.CLIENT)
    private record MantleLip(BlockPos pos, double topY) {}

    /** Closest XZ on the lip's real collision to the player. No move if already over the solid. */
    @SideOnly(Side.CLIENT)
    private static double[] nearestLipXZ(World world, BlockPos lipPos, double x, double z) {
        double bestX = x;
        double bestZ = z;
        double bestDist = Double.POSITIVE_INFINITY;
        if (world == null) {
            return new double[] { x, z };
        }
        IBlockState state = world.getBlockState(lipPos);
        for (AxisAlignedBB box : collisionBoxes(world, lipPos, state)) {
            if (box == null || box == Block.NULL_AABB) {
                continue;
            }
            double cx = net.minecraft.util.math.MathHelper.clamp(x, box.minX, box.maxX);
            double cz = net.minecraft.util.math.MathHelper.clamp(z, box.minZ, box.maxZ);
            double dx = cx - x;
            double dz = cz - z;
            double dist = dx * dx + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                bestX = cx;
                bestZ = cz;
            }
        }
        return new double[] { bestX, bestZ };
    }

    @SideOnly(Side.CLIENT)
    private static List<AxisAlignedBB> collisionBoxes(World world, BlockPos pos, IBlockState state) {
        List<AxisAlignedBB> list = new ArrayList<>();
        AxisAlignedBB probe = new AxisAlignedBB(pos).union(new AxisAlignedBB(pos.up()));
        state.addCollisionBoxToList(world, pos, probe, list, (Entity) null, false);
        return list;
    }

    @SideOnly(Side.CLIENT)
    private static boolean isCollisionEmpty(World world, BlockPos pos, IBlockState state) {
        if (!collisionBoxes(world, pos, state).isEmpty()) {
            return false;
        }
        AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
        return box == null || box == Block.NULL_AABB;
    }

    /** Standable lip, or null if there is no two-block air gap above it. */
    @SideOnly(Side.CLIENT)
    private static MantleLip findMantleLip(World world, BlockPos wallPos, IBlockState wallState) {
        BlockPos above = wallPos.up();
        BlockPos above2 = wallPos.up(2);
        BlockPos above3 = wallPos.up(3);
        IBlockState aboveState = world.getBlockState(above);
        if (isCollisionEmpty(world, above, aboveState)
                && isCollisionEmpty(world, above2, world.getBlockState(above2))) {
            return new MantleLip(wallPos, collisionTopY(world, wallPos, wallState));
        }
        IBlockState above2State = world.getBlockState(above2);
        if (!isCollisionEmpty(world, above, aboveState)
                && isCollisionEmpty(world, above2, above2State)
                && isCollisionEmpty(world, above3, world.getBlockState(above3))) {
            return new MantleLip(above, collisionTopY(world, above, aboveState));
        }
        return null;
    }

    /** World Y of the standable lip. Fences/walls use addCollisionBoxToList (1.5), not the 1.0 outline. */
    @SideOnly(Side.CLIENT)
    private static double collisionTopY(World world, BlockPos pos, IBlockState state) {
        double top = Double.NEGATIVE_INFINITY;
        for (AxisAlignedBB box : collisionBoxes(world, pos, state)) {
            if (box != null && box != Block.NULL_AABB) {
                top = Math.max(top, box.maxY);
            }
        }
        if (top != Double.NEGATIVE_INFINITY) {
            return top;
        }
        AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
        if (box == null || box == Block.NULL_AABB) {
            return pos.getY() + 1.0D;
        }
        if (box.minY >= pos.getY() - 0.001D) {
            return box.maxY;
        }
        return pos.getY() + box.maxY;
    }
}
