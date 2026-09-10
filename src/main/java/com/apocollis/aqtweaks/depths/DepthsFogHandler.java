package com.apocollis.aqtweaks.depths;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-only: below Y0, dark gray fog that starts ~32 blocks from the camera.
 */
@SideOnly(Side.CLIENT)
public class DepthsFogHandler {

    private static final float COLOR_BLEND = 0.85f;
    private static final float TARGET_R = 0.10f;
    private static final float TARGET_G = 0.10f;
    private static final float TARGET_B = 0.12f;

    private static final float FOG_START = 32.0f;
    private static final float FOG_END = 52.0f;

    private final BlockPos.MutableBlockPos eyePos = new BlockPos.MutableBlockPos();
    private Entity cachedEntity;
    private float cachedPartial;
    private boolean cachedActive;

    private boolean isActive(Entity entity, float partialTicks) {
        if (entity == cachedEntity && partialTicks == cachedPartial) {
            return cachedActive;
        }
        cachedEntity = entity;
        cachedPartial = partialTicks;
        cachedActive = computeActive(entity, partialTicks);
        return cachedActive;
    }

    private boolean computeActive(Entity entity, float partialTicks) {
        if (entity == null || entity.world == null) return false;
        if (!ArcanaQuestTweaksConfig.DepthsModuleConfig.general.enableDepthsModule) return false;
        if (!ArcanaQuestTweaksConfig.DepthsModuleConfig.client.deepCaveFog) return false;
        if (entity.world.provider != null && entity.world.provider.getDimension() != 0) return false;

        Vec3d eyes = entity.getPositionEyes(partialTicks);
        if (eyes.y >= 0.0) return false;

        eyePos.setPos(eyes.x, eyes.y, eyes.z);
        Material mat = entity.world.getBlockState(eyePos).getMaterial();
        return mat != Material.WATER && mat != Material.LAVA;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFogColors(EntityViewRenderEvent.FogColors event) {
        if (!isActive(event.getEntity(), (float) event.getRenderPartialTicks())) return;

        event.setRed(event.getRed() + (TARGET_R - event.getRed()) * COLOR_BLEND);
        event.setGreen(event.getGreen() + (TARGET_G - event.getGreen()) * COLOR_BLEND);
        event.setBlue(event.getBlue() + (TARGET_B - event.getBlue()) * COLOR_BLEND);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (!isActive(event.getEntity(), (float) event.getRenderPartialTicks())) return;

        net.minecraft.client.renderer.GlStateManager.setFogStart(FOG_START);
        net.minecraft.client.renderer.GlStateManager.setFogEnd(FOG_END);
    }
}
