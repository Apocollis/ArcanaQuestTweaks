package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import com.elenai.elenaidodge2.ModConfig;
import com.elenai.elenaidodge2.gui.DodgeGui;
import com.elenai.elenaidodge2.util.ClientStorage;
import com.elenai.elenaidodge2.util.PatronRewardHandler;
import com.elenai.elenaidodge2.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StaminaModuleClient {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderDodgeGUI(RenderGameOverlayEvent.Post event) {
        EntityPlayer player = Reflect.getClientPlayer();
        if (player == null) return;
        Minecraft mc = Reflect.getMinecraft();

        // Only render if the dodge trait is NOT unlocked
        // (if it is unlocked, Elenai Dodge 2's DodgeGui will render it)
        if (Utils.dodgeTraitUnlocked(player)) return;

        // Perform visibility checks from DodgeGui
        if (ModConfig.client == null || ModConfig.client.hud == null) return;
        if (!ModConfig.client.hud.hud) return;

        if (Reflect.isCreative(player) || Reflect.isSpectator(player)) return;

        boolean compatHud = ModConfig.client.hud.compatHud;
        ElementType type = event.getType();

        // Match RenderGameOverlayEvent conditions
        if ((type == ElementType.ALL && !compatHud) || (type == ElementType.FOOD && compatHud)) {
            // Render
            Reflect.bindTexture(mc, DodgeGui.DODGE_ICONS);
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
            Reflect.bindTexture(mc, Gui.ICONS);
            GlStateManager.disableBlend();
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onPlayerTick(net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START) return;

        EntityPlayer player = event.player;
        if (player == null || Reflect.isCreative(player) || Reflect.isSpectator(player)) return;

        // Only run for the local player on the client side
        EntityPlayer localPlayer = Reflect.getClientPlayer();
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
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    @SideOnly(Side.CLIENT)
    public void onClientTickLowest(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) return;

        EntityPlayer player = Reflect.getClientPlayer();
        if (player == null) return;

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

    @SideOnly(Side.CLIENT)
    private void handleClientClimbing(net.minecraft.client.entity.EntityPlayerSP player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.enableClimbCost) return;

        if (Reflect.isOnLadder(player)) {
            // Keep jump input synced (used by other climb edge cases / older servers)
            boolean isJumpPressed = Reflect.isJumpPressed(player);
            ArcanaQuestTweaks.NETWORK.sendToServer(new PacketSyncClimbingInput(isJumpPressed));
            Reflect.setBoolean(Reflect.getEntityData(player), "StaminaTweaksLastJumpInput", isJumpPressed);

            if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.fallOnDepleted) return;

            int x = net.minecraft.util.math.MathHelper.floor(Reflect.getPosX(player));
            int y = net.minecraft.util.math.MathHelper.floor(Reflect.getBoundingBoxMinY(player));
            int z = net.minecraft.util.math.MathHelper.floor(Reflect.getPosZ(player));
            net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
            net.minecraft.world.World world = Reflect.getWorld(player);
            net.minecraft.block.Block block = world != null ? Reflect.getBlock(world, pos) : net.minecraft.init.Blocks.AIR;

            boolean isRope = Reflect.isRopeBlock(block);
            boolean isVine = !isRope && (block instanceof net.minecraft.block.BlockVine
                    || block.getClass().getSimpleName().toLowerCase().contains("vine"));

            if (isRope && !ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.enableRopeCost) return;

            int cost = isRope ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ropeCost
                    : (isVine ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.vineCost
                            : ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ladderCost);

            if (cost > 0 && !Reflect.hasEnoughStamina(player, cost)) {
                Reflect.setMotionY(player, -0.15);
            }
        } else {
            NBTTagCompound clientData = Reflect.getEntityData(player);
            if (Reflect.getBoolean(clientData, "StaminaTweaksLastJumpInput")) {
                ArcanaQuestTweaks.NETWORK.sendToServer(new PacketSyncClimbingInput(false));
                Reflect.setBoolean(clientData, "StaminaTweaksLastJumpInput", false);
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onInputUpdate(net.minecraftforge.client.event.InputUpdateEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || Reflect.isCreative(player) || Reflect.isSpectator(player)) return;

        if (!Reflect.isOnLadder(player)) return;
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.enableClimbCost) return;
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.fallOnDepleted) return;

        int x = net.minecraft.util.math.MathHelper.floor(Reflect.getPosX(player));
        int y = net.minecraft.util.math.MathHelper.floor(Reflect.getBoundingBoxMinY(player));
        int z = net.minecraft.util.math.MathHelper.floor(Reflect.getPosZ(player));
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
        net.minecraft.world.World world = Reflect.getWorld(player);
        net.minecraft.block.Block block = world != null ? Reflect.getBlock(world, pos) : net.minecraft.init.Blocks.AIR;

        boolean isRope = Reflect.isRopeBlock(block);
        boolean isVine = !isRope && (block instanceof net.minecraft.block.BlockVine
                || block.getClass().getSimpleName().toLowerCase().contains("vine"));

        if (isRope && !ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.enableRopeCost) return;

        int cost = isRope ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ropeCost
                : (isVine ? ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.vineCost
                        : ArcanaQuestTweaksConfig.StaminaModuleConfig.climbing.ladderCost);

        if (cost > 0 && !Reflect.hasEnoughStamina(player, cost)) {
            Reflect.setJumpPressed(player, false);
            Reflect.setSneakPressed(player, false);
        }
    }

    @SideOnly(Side.CLIENT)
    private void handleClientLedgeClimbing(net.minecraft.client.entity.EntityPlayerSP player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.ledgeClimb.enableLedgeClimb) return;

        NBTTagCompound clientData = Reflect.getEntityData(player);
        int state = Reflect.getInteger(clientData, "StaminaTweaksLedgeClimbState");

        if (state == 0) {
            // Check target conditions
            if (Reflect.isOnGround(player) || Reflect.isOnLadder(player) || Reflect.isInWater(player) || Reflect.isInLava(player) || Reflect.isRiding(player)) {
                Reflect.setInteger(clientData, "StaminaTweaksLedgeClimbHeldTicks", 0);
                return;
            }

            // Must hold forward and jump
            if (!Reflect.isJumpPressed(player) || Reflect.getMoveForward(player) <= 0.0F) {
                Reflect.setInteger(clientData, "StaminaTweaksLedgeClimbHeldTicks", 0);
                return;
            }

            // Update consecutive held ticks
            int heldTicks = Reflect.getInteger(clientData, "StaminaTweaksLedgeClimbHeldTicks") + 1;
            Reflect.setInteger(clientData, "StaminaTweaksLedgeClimbHeldTicks", heldTicks);

            // Must hold for at least 5 ticks
            if (heldTicks < 5) return;

            // Only attempt climb when falling or at peak of jump (motionY <= 0.0)
            if (Reflect.getMotionY(player) > 0.0) return;

            // Check raycast for 1-block ledge in front of the player
            double yawRad = Math.toRadians(Reflect.getRotationYaw(player));
            double dx = -Math.sin(yawRad);
            double dz = Math.cos(yawRad);

            net.minecraft.world.World world = player.world;
            double posX = Reflect.getPosX(player);
            double posY = Reflect.getPosY(player);
            double posZ = Reflect.getPosZ(player);

            double foundLedgeY = -1.0D;

            // Check 2 heights (eye level and slightly below eye level)
            double[] checkHeights = new double[]{1.2D, 0.6D};
            for (double h : checkHeights) {
                int wallX = net.minecraft.util.math.MathHelper.floor(posX + dx * 0.7D);
                int wallY = net.minecraft.util.math.MathHelper.floor(posY + h);
                int wallZ = net.minecraft.util.math.MathHelper.floor(posZ + dz * 0.7D);
                net.minecraft.util.math.BlockPos wallPos = new net.minecraft.util.math.BlockPos(wallX, wallY, wallZ);

                net.minecraft.block.state.IBlockState wallState = Reflect.getBlockState(world, wallPos);
                if (Reflect.getCollisionBoundingBox(wallState, world, wallPos) != net.minecraft.block.Block.NULL_AABB) {
                    // Check if block above wall is air/clear for player to stand
                    net.minecraft.util.math.BlockPos space1 = Reflect.up(wallPos);
                    net.minecraft.util.math.BlockPos space2 = Reflect.up(wallPos, 2);

                    if (Reflect.getCollisionBoundingBox(Reflect.getBlockState(world, space1), world, space1) == net.minecraft.block.Block.NULL_AABB &&
                        Reflect.getCollisionBoundingBox(Reflect.getBlockState(world, space2), world, space2) == net.minecraft.block.Block.NULL_AABB) {
                        foundLedgeY = Reflect.getY(wallPos) + 1.0D;
                        break;
                    }
                }
            }

            if (foundLedgeY > 0.0D) {
                // Deduct stamina on server
                ArcanaQuestTweaks.NETWORK.sendToServer(new PacketLedgeClimb());

                // Set client state variables
                Reflect.setInteger(clientData, "StaminaTweaksLedgeClimbState", 1);
                Reflect.setDouble(clientData, "StaminaTweaksLedgeClimbTargetY", foundLedgeY);
                Reflect.setDouble(clientData, "StaminaTweaksLedgeClimbDx", dx);
                Reflect.setDouble(clientData, "StaminaTweaksLedgeClimbDz", dz);
                Reflect.setInteger(clientData, "StaminaTweaksLedgeClimbHeldTicks", 0); // Reset

                // Set initial lift velocity (1/16 of original climb rate. motionY = 0.08 + 0.010625 = 0.090625D)
                Reflect.setMotionY(player, 0.090625D);
                Reflect.setMotionX(player, dx * 0.005D);
                Reflect.setMotionZ(player, dz * 0.005D);
            }
        } else if (state == 1) {
            // Check fail conditions
            if (Reflect.isOnGround(player) || Reflect.isOnLadder(player) || Reflect.isInWater(player) || Reflect.isInLava(player) || Reflect.isRiding(player)) {
                Reflect.setInteger(clientData, "StaminaTweaksLedgeClimbState", 0);
                return;
            }

            // Climbing should only continue while jump and forward keys are still held
            if (!Reflect.isJumpPressed(player) || Reflect.getMoveForward(player) <= 0.0F) {
                Reflect.setInteger(clientData, "StaminaTweaksLedgeClimbState", 0);
                return;
            }

            double targetY = Reflect.getDouble(clientData, "StaminaTweaksLedgeClimbTargetY");
            double dx = Reflect.getDouble(clientData, "StaminaTweaksLedgeClimbDx");
            double dz = Reflect.getDouble(clientData, "StaminaTweaksLedgeClimbDz");

            if (Reflect.getPosY(player) >= targetY + 0.2D) {
                // Clear block - do not add any forward movement on the block after
                Reflect.setMotionX(player, 0.0D);
                Reflect.setMotionZ(player, 0.0D);
                Reflect.setMotionY(player, 0.0D);

                Reflect.setInteger(clientData, "StaminaTweaksLedgeClimbState", 0);
            } else {
                // Continue climbing (1/16 of original climb rate. motionY = 0.08 + 0.010625 = 0.090625D)
                Reflect.setMotionY(player, 0.090625D);
                Reflect.setMotionX(player, dx * 0.005D);
                Reflect.setMotionZ(player, dz * 0.005D);
            }
        }
    }
}
