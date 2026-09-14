package com.apocollis.aqtweaks.gaia.client;

import com.apocollis.aqtweaks.gaia.EntityDeepDwarf;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderDeepDwarf extends RenderLiving<EntityDeepDwarf> {

    public RenderDeepDwarf(RenderManager renderManager) {
        super(renderManager, new ModelDeepDwarf(), 0.4F);
        ModelDeepDwarf model = (ModelDeepDwarf) getMainModel();
        addLayer(LayerDeepDwarfHeldItem.right(this, model.getRightArm()));
        addLayer(LayerDeepDwarfHeldItem.left(this, model.getLeftArm()));
        addLayer(new LayerDeepDwarfGlow(this, DeepDwarfSkin::eyes));
    }

    @Override
    public void transformHeldFull3DItemLayer() {
        GlStateManager.translate(0.0F, 0.1875F, 0.0F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityDeepDwarf entity) {
        return DeepDwarfSkin.body(entity.getTextureType());
    }
}
