package com.apocollis.aqtweaks.additions.client;

import com.apocollis.aqtweaks.additions.EntityNamelessVessel;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

@SideOnly(Side.CLIENT)
public class RenderNamelessVessel extends GeoEntityRenderer<EntityNamelessVessel> {
    public RenderNamelessVessel(RenderManager renderManager) {
        super(renderManager, new NamelessVesselModel());
        this.shadowSize = 0.55F;
        this.addLayer(new LayerNamelessVesselGlow(this));
    }

    @Override
    protected float getDeathMaxRotation(EntityNamelessVessel entityLivingBaseIn) {
        return 0.0F;
    }
}
