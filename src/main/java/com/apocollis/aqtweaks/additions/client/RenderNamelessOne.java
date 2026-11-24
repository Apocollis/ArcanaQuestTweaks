package com.apocollis.aqtweaks.additions.client;

import com.apocollis.aqtweaks.additions.EntityNamelessOne;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

@SideOnly(Side.CLIENT)
public class RenderNamelessOne extends GeoEntityRenderer<EntityNamelessOne> {
    private float activeImpetusHeartDarkness = 0.0F;
    private boolean renderingGlowLayer = false;

    public RenderNamelessOne(RenderManager renderManager) {
        super(renderManager, new NamelessOneModel());
        this.shadowSize = 1.15F;
        this.addLayer(new LayerNamelessOneGlow(this));
    }

    void setRenderingGlowLayer(boolean renderingGlowLayer) {
        this.renderingGlowLayer = renderingGlowLayer;
    }

    @Override
    protected float getDeathMaxRotation(EntityNamelessOne entityLivingBaseIn) {
        // Keep 0 so vanilla does not tip the boss 90 degrees sideways during the 4.5s death animation
        return 0.0F;
    }

    @Override
    public Color getRenderColor(EntityNamelessOne animatable, float partialTicks) {
        if (animatable.getAnimState() == EntityNamelessOne.STATE_DEATH || animatable.getHealth() <= 0.0F) {
            float deathAge = animatable.getCustomDeathTicks() + partialTicks;
            // Beat 3 dissolve: fade alpha smoothly from 1.0 to 0.0 over ticks 66..90 (3.30s..4.50s)
            if (deathAge >= 66.0F) {
                float alpha = MathHelper.clamp(1.0F - ((deathAge - 66.0F) / 24.0F), 0.0F, 1.0F);
                return Color.ofRGBA(1.0F, 1.0F, 1.0F, alpha);
            }
        }
        return Color.WHITE;
    }

    @Override
    public void renderEarly(EntityNamelessOne animatable, float ticks, float red, float green, float blue, float partialTicks) {
        super.renderEarly(animatable, ticks, red, green, blue, partialTicks);
        this.activeImpetusHeartDarkness = animatable != null ? animatable.getImpetusHeartDarkness(partialTicks) : 0.0F;
        if ((animatable.getAnimState() == EntityNamelessOne.STATE_DEATH || animatable.getHealth() <= 0.0F)
                && animatable.getCustomDeathTicks() >= 65) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
    }

    @Override
    public void renderRecursively(BufferBuilder builder, GeoBone bone, float red, float green, float blue, float alpha, boolean renderTranslucent) {
        if (this.activeImpetusHeartDarkness > 0.001F && "heart_core".equals(bone.getName())) {
            if (this.renderingGlowLayer) {
                float glowAlpha = alpha * (1.0F - this.activeImpetusHeartDarkness);
                super.renderRecursively(builder, bone, red, green, blue, glowAlpha, renderTranslucent);
                return;
            }
            float tint = 1.0F - 0.98F * this.activeImpetusHeartDarkness;
            super.renderRecursively(builder, bone, red * tint, green * tint, blue * tint, alpha, renderTranslucent);
            return;
        }
        super.renderRecursively(builder, bone, red, green, blue, alpha, renderTranslucent);
    }

    @Override
    public void renderAfter(EntityNamelessOne animatable, float ticks, float red, float green, float blue, float partialTicks) {
        if ((animatable.getAnimState() == EntityNamelessOne.STATE_DEATH || animatable.getHealth() <= 0.0F)
                && animatable.getCustomDeathTicks() >= 65) {
            GlStateManager.disableBlend();
        }
        super.renderAfter(animatable, ticks, red, green, blue, partialTicks);
    }
}
