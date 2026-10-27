package com.apocollis.aqtweaks.client.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
            // Terrain pass. Under a roof, and in clear weather, the horizon stays at 0.75*far to far.
            // Outdoors in a sandstorm the fog closes with storm strength. At full strength it starts at
            // the camera and ends near 0.12*far, so the desert is only readable for a short distance.
            float storm = 0.0F;
            if (provider instanceof WorldProviderAtum) {
                storm = MathHelper.clamp(((WorldProviderAtum) provider).stormStrength, 0.0F, 1.0F);
            }
            BlockPos eye = new BlockPos(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
            if (!entity.world.canSeeSky(eye)) storm = 0.0F;
            float fogStart = farPlaneDistance * (0.75F * (1.0F - storm));
            float fogEnd = farPlaneDistance * ((1.0F - storm) + 0.12F * storm);

            GlStateManager.setFogStart(fogStart);
            GlStateManager.setFogEnd(fogEnd);
        }
    }
}
