package com.apocollis.aqtweaks.gaia.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LayerDeepDwarfHeldItem implements LayerRenderer<EntityLivingBase> {

    private final RenderLiving<?> renderer;
    private final ModelRenderer limb;
    private final EntityEquipmentSlot slot;

    private LayerDeepDwarfHeldItem(RenderLiving<?> renderer, ModelRenderer limb, EntityEquipmentSlot slot) {
        this.renderer = renderer;
        this.limb = limb;
        this.slot = slot;
    }

    public static LayerDeepDwarfHeldItem right(RenderLiving<?> renderer, ModelRenderer limb) {
        return new LayerDeepDwarfHeldItem(renderer, limb, EntityEquipmentSlot.MAINHAND);
    }

    public static LayerDeepDwarfHeldItem left(RenderLiving<?> renderer, ModelRenderer limb) {
        return new LayerDeepDwarfHeldItem(renderer, limb, EntityEquipmentSlot.OFFHAND);
    }

    @Override
    public void doRenderLayer(EntityLivingBase living, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack stack = living.getItemStackFromSlot(slot);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, -0.04F, 0.0F);
        if (slot == EntityEquipmentSlot.MAINHAND) {
            renderHeldItem(living, stack, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, EnumHandSide.RIGHT);
        } else {
            renderHeldItem(living, stack, ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, EnumHandSide.LEFT);
        }
        GlStateManager.popMatrix();
    }

    private void renderHeldItem(EntityLivingBase living, ItemStack stack, ItemCameraTransforms.TransformType camera, EnumHandSide handSide) {
        if (stack.isEmpty()) {
            return;
        }
        GlStateManager.pushMatrix();
        if (living.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }
        limb.postRender(0.0625F);
        GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        boolean left = handSide == EnumHandSide.LEFT;
        GlStateManager.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
        Minecraft.getMinecraft().getItemRenderer().renderItemSide(living, stack, camera, left);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
