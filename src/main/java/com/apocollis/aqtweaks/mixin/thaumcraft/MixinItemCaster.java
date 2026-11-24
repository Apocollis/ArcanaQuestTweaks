package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumadditions.AegisChannelHandler;
import com.apocollis.aqtweaks.thaumcraft.ThaumcraftPerkHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.items.casters.ItemCaster;

@Mixin(value = ItemCaster.class, remap = false)
public abstract class MixinItemCaster {

    @ModifyVariable(method = "consumeVis", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float aqtweaks$fullFontCost(float amount, ItemStack stack, EntityPlayer player, float ignored,
            boolean crafting, boolean sim) {
        return ThaumcraftPerkHooks.fullFontCost(player, amount, crafting);
    }

    @Inject(method = "consumeVis", at = @At("RETURN"))
    private void aqtweaks$fullFontStamp(ItemStack stack, EntityPlayer player, float amount, boolean crafting,
            boolean sim, CallbackInfoReturnable<Boolean> cir) {
        if (sim || crafting) return;
        if (cir.getReturnValue() == null || !cir.getReturnValue()) return;
        ThaumcraftPerkHooks.fullFontStamp(player, crafting);
    }

    /** {@code onItemRightClick}: while Shield Broken is active, a focus containing Aegis cannot be cast. */
    @Inject(method = "func_77659_a", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$aegisLockout(World world, EntityPlayer player, EnumHand hand,
            CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        ItemStack held = player.getHeldItem(hand);
        if (AegisChannelHandler.refuse(player, held)) {
            cir.setReturnValue(new ActionResult<>(EnumActionResult.FAIL, held));
        }
    }

    /** After a stock cast went through, an Aegis focus starts being held: the stock item never starts a "use". */
    @Inject(method = "func_77659_a", at = @At("RETURN"))
    private void aqtweaks$aegisRaise(World world, EntityPlayer player, EnumHand hand,
            CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        ActionResult<ItemStack> result = cir.getReturnValue();
        if (result != null && result.getType() == EnumActionResult.SUCCESS) {
            AegisChannelHandler.onCast(player, hand);
        }
    }

    /** {@code getItemUseAction}: the raised-shield pose (and vanilla's blocking test) for an Aegis focus. */
    @Inject(method = "func_77661_b", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$aegisUseAction(ItemStack stack, CallbackInfoReturnable<EnumAction> cir) {
        EnumAction action = AegisChannelHandler.useAction(stack);
        if (action != null) {
            cir.setReturnValue(action);
        }
    }
}
