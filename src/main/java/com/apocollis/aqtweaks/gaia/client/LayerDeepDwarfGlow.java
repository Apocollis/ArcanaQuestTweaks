package com.apocollis.aqtweaks.gaia.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Supplier;

@SideOnly(Side.CLIENT)
public class LayerDeepDwarfGlow implements LayerRenderer<EntityLiving> {

    private final RenderLiving<?> renderer;
    private final Supplier<ResourceLocation> texture;

    public LayerDeepDwarfGlow(RenderLiving<?> renderer, Supplier<ResourceLocation> texture) {
        this.renderer = renderer;
        this.texture = texture;
    }

    @Override
    public void doRenderLayer(EntityLiving entity, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        renderer.bindTexture(texture.get());
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        GlStateManager.disableLighting();
        if (entity.isInvisible()) {
            GlStateManager.depthMask(false);
        } else {
            GlStateManager.depthMask(true);
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 61680 % 65536 / 1.0F, 61680 / 65536 / 1.0F);
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        renderer.getMainModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        int bright = entity.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bright % 65536, bright / 65536);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
    }

    @Override
    public boolean shouldCombineTextures() {
        return true;
    }
}
