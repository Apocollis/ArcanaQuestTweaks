package com.apocollis.aqtweaks.client.nether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.nether.NetherHazards;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Local fog while the camera is inside an ash cloud. The distances are applied from {@code MixinEntityRendererFog}
 * after every fog event handler (so Dynamic Surroundings cannot override them); the colour shifts toward a dark
 * grey-brown here. {@link AshCloudRenderer#depth()} is the eased inside-the-cloud factor.
 */
@SideOnly(Side.CLIENT)
public class AshFogHandler {

    /** Called from the setupFog RETURN injection. */
    public static void applyAfterSetupFog(int startCoords, float partialTicks, float farPlane) {
        if (startCoords < 0 || !ArcanaQuestTweaksConfig.NetherModuleConfig.enableAshFog) return;
        World world = Minecraft.getMinecraft().world;
        if (world == null || !NetherHazards.isNether(world)) return;
        float f = AshCloudRenderer.depth();
        if (f <= 0.01F) return;
        float fogStart = Math.min(farPlane * 0.1F, 4.0F) * (1.0F - f);
        float fogEnd = farPlane * (1.0F - 0.85F * f);
        GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
        GlStateManager.setFogStart(fogStart);
        GlStateManager.setFogEnd(Math.max(fogEnd, fogStart + 1.0F));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFogColors(EntityViewRenderEvent.FogColors event) {
        float f = AshCloudRenderer.depth();
        if (f <= 0.01F || !ArcanaQuestTweaksConfig.NetherModuleConfig.enableAshFog) return;
        World world = Minecraft.getMinecraft().world;
        if (world == null || !NetherHazards.isNether(world)) return;
        float a = f * 0.75F;
        event.setRed(event.getRed() + (0.20F - event.getRed()) * a);
        event.setGreen(event.getGreen() + (0.17F - event.getGreen()) * a);
        event.setBlue(event.getBlue() + (0.15F - event.getBlue()) * a);
    }
}
