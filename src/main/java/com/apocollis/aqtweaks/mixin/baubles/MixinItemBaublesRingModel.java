package com.apocollis.aqtweaks.mixin.baubles;

import baubles.api.model.ModelBauble;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.items.baubles.ItemBaubles;

/**
 * Mundane, apprentice, and fancy rings are {@code thaumcraft:baubles} meta 1, 3, and 5.
 * BaublesEX adds {@code getModel} and otherwise builds {@code ModelAmulet} with a null texture.
 * Priority 500 applies after that default-1000 mixin. A null model skips the slot before {@code pushMatrix}.
 */
@Mixin(value = ItemBaubles.class, priority = 500, remap = false)
public class MixinItemBaublesRingModel {

    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true, remap = false)
    private void aqtweaks$skipRingAmuletModel(
            ItemStack stack,
            EntityLivingBase entity,
            RenderPlayer renderPlayer,
            CallbackInfoReturnable<ModelBauble> cir) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        if (name == null || !"thaumcraft".equals(name.getNamespace()) || !"baubles".equals(name.getPath())) {
            return;
        }
        int meta = stack.getMetadata();
        if (meta == 1 || meta == 3 || meta == 5) {
            cir.setReturnValue(null);
        }
    }
}
