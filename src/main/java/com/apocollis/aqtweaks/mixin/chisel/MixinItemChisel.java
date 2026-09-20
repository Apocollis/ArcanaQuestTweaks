package com.apocollis.aqtweaks.mixin.chisel;

import com.apocollis.aqtweaks.gamestages.GameStagesChiselHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.chisel.api.carving.ICarvingVariation;
import team.chisel.common.item.ItemChisel;

@Mixin(value = ItemChisel.class, remap = false)
public abstract class MixinItemChisel {

    @Inject(method = "canChisel", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$stageCanChisel(World world, EntityPlayer player, ItemStack chisel,
            ICarvingVariation variation, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() == null || !cir.getReturnValue()) {
            return;
        }
        if (variation == null) {
            return;
        }
        ItemStack output = variation.getStack();
        String stage = GameStagesChiselHooks.lockedStage(player, output);
        if (stage == null) {
            return;
        }
        GameStagesChiselHooks.notifyLocked(player, stage);
        cir.setReturnValue(false);
    }
}
