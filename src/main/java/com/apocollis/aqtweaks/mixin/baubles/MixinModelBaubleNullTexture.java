package com.apocollis.aqtweaks.mixin.baubles;

import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A null texture must not reach {@code TextureManager.bindTexture}. That throws inside
 * {@code BaublesRenderLayer} and the rest of the bauble pass is not drawn.
 */
@Mixin(targets = "baubles.api.model.ModelBauble", remap = false)
public abstract class MixinModelBaubleNullTexture {

    @Shadow(remap = false)
    public abstract ResourceLocation getTexture(ItemStack stack, EntityLivingBase entity, RenderPlayer renderPlayer);

    @Inject(method = "renderWithTexture", at = @At("HEAD"), cancellable = true, remap = false)
    private void aqtweaks$skipNullTexture(
            RenderPlayer renderPlayer,
            EntityLivingBase entity,
            ItemStack stack,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            float scale,
            float partialTicks,
            CallbackInfo ci) {
        if (this.getTexture(stack, entity, renderPlayer) == null) {
            ci.cancel();
        }
    }
}
