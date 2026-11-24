package com.apocollis.aqtweaks.additions.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.additions.EntityNamelessOne;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

@SideOnly(Side.CLIENT)
public class LayerNamelessOneGlow extends GeoLayerRenderer<EntityNamelessOne> {
    private static final ResourceLocation GLOW_TEXTURE =
            new ResourceLocation(ArcanaQuestTweaks.MODID, "textures/entity/nameless_one_glowmask.png");

    public LayerNamelessOneGlow(IGeoRenderer<EntityNamelessOne> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(EntityNamelessOne entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch, Color renderColor) {
        GeoModel model = this.getEntityModel().getModel(this.getEntityModel().getModelLocation(entity));
        if (model == null) {
            return;
        }

        Minecraft.getMinecraft().getTextureManager().bindTexture(GLOW_TEXTURE);

        float prevLightX = OpenGlHelper.lastBrightnessX;
        float prevLightY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        float alpha = renderColor.getAlpha() / 255.0F;
        IGeoRenderer<EntityNamelessOne> renderer = this.getRenderer();
        if (renderer instanceof RenderNamelessOne) {
            ((RenderNamelessOne) renderer).setRenderingGlowLayer(true);
        }
        try {
            renderer.render(model, entity, partialTicks, 1.0F, 1.0F, 1.0F, alpha);
        } finally {
            if (renderer instanceof RenderNamelessOne) {
                ((RenderNamelessOne) renderer).setRenderingGlowLayer(false);
            }
        }

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLightX, prevLightY);
    }

    @Override
    public void doRenderLayer(EntityNamelessOne entitylivingbaseIn, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
