package com.apocollis.aqtweaks.client.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-only fog controller for the Atum dimension.
 * Replaces Atum's blinding exponential fog with clean, uniform linear fog across the dimension.
 * Keeps horizons, caves, and structures consistently clear and visible without any artificial darkening.
 */
@SideOnly(Side.CLIENT)
public class AtumFogHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.world == null) return;
        WorldProvider provider = entity.world.provider;
        if (!(provider instanceof WorldProviderAtum) && (provider == null || provider.getDimension() != 17)) {
            return;
        }
        if (!ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog.disableUndergroundFog) {
            return;
        }

        GlStateManager.setFog(GlStateManager.FogMode.LINEAR);
        float farPlaneDistance = event.getFarPlaneDistance();

        if (event.getFogMode() < 0) {
            // Sky pass: clean linear sky fog, eliminating Atum's blinding yellow EXP fog
            GlStateManager.setFogStart(0.0F);
            GlStateManager.setFogEnd(farPlaneDistance);
        } else {
            // Terrain pass: clean linear horizon fog everywhere in Atum
            // Caves and rooms stay crisp and clear, while outdoors horizon fades naturally.
            // During sandstorms, fog pulls in slightly to enhance the storm atmosphere.
            float stormStrength = (provider instanceof WorldProviderAtum) ? ((WorldProviderAtum) provider).stormStrength : 0.0F;
            float fogStart = (stormStrength > 0.05F) ? (farPlaneDistance * 0.40F) : (farPlaneDistance * 0.75F);

            GlStateManager.setFogStart(fogStart);
            GlStateManager.setFogEnd(farPlaneDistance);
        }
    }
}
