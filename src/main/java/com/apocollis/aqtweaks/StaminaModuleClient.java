package com.apocollis.aqtweaks;

import com.elenai.elenaidodge2.ModConfig;
import com.elenai.elenaidodge2.gui.DodgeGui;
import com.elenai.elenaidodge2.util.ClientStorage;
import com.elenai.elenaidodge2.util.PatronRewardHandler;
import com.elenai.elenaidodge2.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StaminaModuleClient {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderDodgeGUI(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        // Only render if the dodge trait is NOT unlocked
        // (if it is unlocked, Elenai Dodge 2's DodgeGui will render it)
        if (Utils.dodgeTraitUnlocked(player)) return;

        // Perform visibility checks from DodgeGui
        if (ModConfig.client == null || ModConfig.client.hud == null) return;
        if (!ModConfig.client.hud.hud) return;

        if (Reflect.isCreative(player) || player.isSpectator()) return;

        boolean compatHud = ModConfig.client.hud.compatHud;
        ElementType type = event.getType();

        // Match RenderGameOverlayEvent conditions
        if ((type == ElementType.ALL && !compatHud) || (type == ElementType.FOOD && compatHud)) {
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
        if (player == null || Reflect.isCreative(player) || player.isSpectator()) return;

        // Only run for the local player on the client side
        Minecraft mc = Minecraft.getMinecraft();
        if (player == mc.player) {
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

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        // Enforce Armor Mastery weight reduction on the client side
        if (ArcanaQuestTweaksConfig.staminaModule.reskillable.enableReskillable && 
            Reflect.hasUnlockable(player, ArcanaQuestTweaksConfig.staminaModule.reskillable.armorMasteryPerkId)) {
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
        if (!ArcanaQuestTweaksConfig.staminaModule.climbing.enableClimbCost) return;

        if (Reflect.isOnLadder(player)) {
            // Send jump input every tick while on ladder to prevent state desync
            boolean isJumpPressed = player.movementInput.jump;
            ArcanaQuestTweaks.NETWORK.sendToServer(new PacketSyncClimbingInput(isJumpPressed));
            player.getEntityData().setBoolean("StaminaTweaksLastJumpInput", isJumpPressed);

            if (!ArcanaQuestTweaksConfig.staminaModule.climbing.fallOnDepleted) return;

            // Get the block at player's position to see if rope climbing cost is enabled
            int x = net.minecraft.util.math.MathHelper.floor(player.posX);
            int y = net.minecraft.util.math.MathHelper.floor(player.getEntityBoundingBox().minY);
            int z = net.minecraft.util.math.MathHelper.floor(player.posZ);
            net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
            net.minecraft.block.Block block = player.world.getBlockState(pos).getBlock();

            boolean isRope = Reflect.isRopeBlock(block);
            boolean isVine = !isRope && (block instanceof net.minecraft.block.BlockVine || block.getClass().getSimpleName().toLowerCase().contains("vine"));
            int cost = isRope ? ArcanaQuestTweaksConfig.staminaModule.climbing.ropeCost : (isVine ? ArcanaQuestTweaksConfig.staminaModule.climbing.vineCost : ArcanaQuestTweaksConfig.staminaModule.climbing.ladderCost);

            if (isRope && !ArcanaQuestTweaksConfig.staminaModule.climbing.enableRopeCost) return;

            boolean isClimbing = player.movementInput.jump || Reflect.getMotionY(player) > 0.0 || player.isSneaking();
            if (isClimbing) {
                if (!Reflect.hasEnoughStamina(player, 1)) {
                    Reflect.setMotionY(player, -0.15);
                }
            }

            if (!Reflect.hasEnoughStamina(player, 1)) {
                Reflect.setMotionY(player, -0.15);
            }
        } else {
            // Clean up last jump input state
            if (player.getEntityData().getBoolean("StaminaTweaksLastJumpInput")) {
                ArcanaQuestTweaks.NETWORK.sendToServer(new PacketSyncClimbingInput(false));
                player.getEntityData().setBoolean("StaminaTweaksLastJumpInput", false);
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onInputUpdate(net.minecraftforge.client.event.InputUpdateEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || Reflect.isCreative(player) || player.isSpectator()) return;

        if (Reflect.isOnLadder(player)) {
            // Get the block at player's position to see if rope climbing cost is enabled
            int x = net.minecraft.util.math.MathHelper.floor(player.posX);
            int y = net.minecraft.util.math.MathHelper.floor(player.getEntityBoundingBox().minY);
            int z = net.minecraft.util.math.MathHelper.floor(player.posZ);
            net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
            net.minecraft.block.Block block = player.world.getBlockState(pos).getBlock();

            boolean isRope = Reflect.isRopeBlock(block);
            boolean isVine = !isRope && (block instanceof net.minecraft.block.BlockVine || block.getClass().getSimpleName().toLowerCase().contains("vine"));
            int cost = isRope ? ArcanaQuestTweaksConfig.staminaModule.climbing.ropeCost : (isVine ? ArcanaQuestTweaksConfig.staminaModule.climbing.vineCost : ArcanaQuestTweaksConfig.staminaModule.climbing.ladderCost);

            if (isRope && !ArcanaQuestTweaksConfig.staminaModule.climbing.enableRopeCost) return;

            if (!Reflect.hasEnoughStamina(player, 1)) {
                event.getMovementInput().jump = false;
                event.getMovementInput().sneak = false;
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void handleClientLedgeClimbing(net.minecraft.client.entity.EntityPlayerSP player) {
        if (!ArcanaQuestTweaksConfig.staminaModule.ledgeClimb.enableLedgeClimb) return;

        int state = player.getEntityData().getInteger("StaminaTweaksLedgeClimbState");

        if (state == 0) {
            // Check target conditions
            if (player.onGround || Reflect.isOnLadder(player) || player.isInWater() || player.isInLava() || player.isRiding()) {
                player.getEntityData().setInteger("StaminaTweaksLedgeClimbHeldTicks", 0);
                return;
            }

            // Must hold forward and jump
            if (!player.movementInput.jump || player.movementInput.moveForward <= 0.0F) {
                player.getEntityData().setInteger("StaminaTweaksLedgeClimbHeldTicks", 0);
                return;
            }

            // Update consecutive held ticks
            int heldTicks = player.getEntityData().getInteger("StaminaTweaksLedgeClimbHeldTicks") + 1;
            player.getEntityData().setInteger("StaminaTweaksLedgeClimbHeldTicks", heldTicks);

            // Must hold for at least 5 ticks
            if (heldTicks < 5) return;

            // Check if player has enough stamina
            int cost = ArcanaQuestTweaksConfig.staminaModule.ledgeClimb.ledgeClimbCost;
            if (!Reflect.hasEnoughStamina(player, cost)) return;

            // Ledge detection
            double yawRad = Math.toRadians(player.rotationYaw);
            double dx = -Math.sin(yawRad);
            double dz = Math.cos(yawRad);

            double reach = 0.4D;
            double checkX = player.posX + dx * reach;
            double checkZ = player.posZ + dz * reach;

            net.minecraft.world.World world = player.world;
            double foundLedgeY = -1.0D;

            // Scan from top to bottom at potential wall heights
            double[] checkHeights = new double[] { 1.5D, 1.0D, 0.5D };
            for (double h : checkHeights) {
                net.minecraft.util.math.BlockPos wallPos = new net.minecraft.util.math.BlockPos(checkX, player.posY + h, checkZ);
                net.minecraft.block.state.IBlockState wallState = world.getBlockState(wallPos);
                
                // If it is solid block
                if (wallState.getCollisionBoundingBox(world, wallPos) != net.minecraft.block.Block.NULL_AABB) {
                    double ledgeY = wallPos.getY() + 1.0D;
                    double diff = ledgeY - player.posY;
                    if (diff > 0.5D && diff <= 2.2D) {
                        // Check if space above is clear
                        net.minecraft.util.math.BlockPos space1 = wallPos.up();
                        net.minecraft.util.math.BlockPos space2 = wallPos.up(2);
                        if (world.getBlockState(space1).getCollisionBoundingBox(world, space1) == net.minecraft.block.Block.NULL_AABB &&
                            world.getBlockState(space2).getCollisionBoundingBox(world, space2) == net.minecraft.block.Block.NULL_AABB) {
                            foundLedgeY = ledgeY;
                            break;
                        }
                    }
                }
            }

            if (foundLedgeY > 0.0D) {
                // Deduct stamina on server
                ArcanaQuestTweaks.NETWORK.sendToServer(new PacketLedgeClimb());

                // Set client state variables
                player.getEntityData().setInteger("StaminaTweaksLedgeClimbState", 1);
                player.getEntityData().setDouble("StaminaTweaksLedgeClimbTargetY", foundLedgeY);
                player.getEntityData().setDouble("StaminaTweaksLedgeClimbDx", dx);
                player.getEntityData().setDouble("StaminaTweaksLedgeClimbDz", dz);
                player.getEntityData().setInteger("StaminaTweaksLedgeClimbHeldTicks", 0); // Reset

                // Set initial lift velocity (1/8 of original climb rate. Original net rate = 0.25 - 0.08 = 0.17. Target net rate = 0.02125. motionY = 0.08 + 0.02125 = 0.10125D)
                player.motionY = 0.10125D;
                player.motionX = dx * 0.005D;
                player.motionZ = dz * 0.005D;
            }
        } else if (state == 1) {
            // Check fail conditions
            if (player.onGround || Reflect.isOnLadder(player) || player.isInWater() || player.isInLava() || player.isRiding()) {
                player.getEntityData().setInteger("StaminaTweaksLedgeClimbState", 0);
                return;
            }

            // Climbing should only continue while jump and forward keys are still held
            if (!player.movementInput.jump || player.movementInput.moveForward <= 0.0F) {
                player.getEntityData().setInteger("StaminaTweaksLedgeClimbState", 0);
                return;
            }

            double targetY = player.getEntityData().getDouble("StaminaTweaksLedgeClimbTargetY");
            double dx = player.getEntityData().getDouble("StaminaTweaksLedgeClimbDx");
            double dz = player.getEntityData().getDouble("StaminaTweaksLedgeClimbDz");

            if (player.posY >= targetY + 0.2D) {
                // Clear block - do not add any forward movement on the block after
                player.motionX = 0.0D;
                player.motionZ = 0.0D;
                player.motionY = 0.0D;

                player.getEntityData().setInteger("StaminaTweaksLedgeClimbState", 0);
            } else {
                // Continue climbing (1/8 of original climb rate. motionY = 0.08 + 0.02125 = 0.10125D)
                player.motionY = 0.10125D;
                player.motionX = dx * 0.005D;
                player.motionZ = dz * 0.005D;
            }
        }
    }
}
