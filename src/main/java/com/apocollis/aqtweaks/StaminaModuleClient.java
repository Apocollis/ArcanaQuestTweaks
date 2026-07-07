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
        if (!ArcanaQuestTweaksConfig.staminaModule.climbing.enableClimbCost || !ArcanaQuestTweaksConfig.staminaModule.climbing.fallOnDepleted) return;

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

            boolean isClimbing = Reflect.getMotionY(player) > 0.0 || player.isSneaking();
            if (isClimbing) {
                if (!Reflect.hasEnoughStamina(player, cost)) {
                    Reflect.setMotionY(player, -0.15);
                }
            }

            if (!Reflect.hasEnoughStamina(player, cost)) {
                Reflect.setMotionY(player, -0.15);
            }
        }
    }
}
