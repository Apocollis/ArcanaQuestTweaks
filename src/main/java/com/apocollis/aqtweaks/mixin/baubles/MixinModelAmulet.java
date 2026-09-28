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
 * {@code ModelAmulet.switchTex} returns null for bauble metas that are not amulets.
 * {@code render} would still draw a {@code ModelBiped} body on the bound player skin.
 */
@Mixin(targets = "baubles.compat.thaumicperiphery.ModelAmulet", remap = false)
public class MixinModelAmulet {

    @Shadow(remap = false)
    protected ResourceLocation texture;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
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
        if (this.texture == null) {
            ci.cancel();
        }
    }
}
