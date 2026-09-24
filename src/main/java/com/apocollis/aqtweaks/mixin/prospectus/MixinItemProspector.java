package com.apocollis.aqtweaks.mixin.prospectus;

import cam72cam.prospectus.ItemProspector;
import com.apocollis.aqtweaks.reskillable.ProspectAccuracy;
import com.apocollis.aqtweaks.reskillable.ProspectorSample;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemProspector.class, remap = false)
public abstract class MixinItemProspector {

    @Accessor("accuracy")
    abstract int aqtweaks$accuracy();

    @Accessor("VARIANT")
    abstract String aqtweaks$variant();

    @Inject(method = "func_180614_a", at = @At("HEAD"))
    private void aqtweaks$begin(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ, CallbackInfoReturnable<EnumActionResult> cir) {
        ProspectorSample.begin(player);
    }

    @Redirect(method = "func_180614_a", at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"))
    private double aqtweaks$roll() {
        int field = aqtweaks$accuracy();
        int boosted = ProspectAccuracy.chance(field, aqtweaks$variant(), ProspectorSample.player());
        double roll = Math.random();
        if (field <= 0 || boosted == field) return roll;
        return roll * field / (double) boosted;
    }

    @Redirect(method = "func_180614_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", remap = true))
    private IBlockState aqtweaks$state(World world, BlockPos pos) {
        ProspectorSample.pos(pos);
        return world.getBlockState(pos);
    }

    @Redirect(method = "func_180614_a", at = @At(value = "INVOKE", target = "Lcam72cam/prospectus/Prospectus;isStackWhitelisted(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean aqtweaks$whitelist(ItemStack stack) {
        boolean counted = InvokerProspectus.aqtweaks$whitelisted(stack);
        if (counted) ProspectorSample.note();
        return counted;
    }

    @Inject(method = "func_180614_a", at = @At("RETURN"))
    private void aqtweaks$finish(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ, CallbackInfoReturnable<EnumActionResult> cir) {
        ProspectorSample.finish();
    }
}
