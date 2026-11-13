package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.thaumcraft.VisBatteryCharge;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.blocks.devices.BlockVisBattery;

/** Portable Vis Battery: the dropped and picked item carries the charge as its damage. */
@Mixin(value = BlockVisBattery.class, remap = false)
public abstract class MixinBlockVisBattery {

    @Inject(method = "func_180651_a", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$dropCharge(IBlockState state, CallbackInfoReturnable<Integer> cir) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.portableVisBatteryCharge) return;
        cir.setReturnValue(VisBatteryCharge.damageFor(state.getValue(BlockVisBattery.CHARGE)));
    }

    @Inject(method = "func_149666_a", at = @At("RETURN"))
    private void aqtweaks$creativeCharged(CreativeTabs tab, NonNullList<ItemStack> items, CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.creativeChargedBatteries || items.isEmpty()) return;
        Item item = Item.getItemFromBlock((Block) (Object) this);
        for (ItemStack stack : items) {
            if (stack.getItem() == item && stack.getMetadata() == VisBatteryCharge.MAX) return;
        }
        for (ItemStack stack : items) {
            if (stack.getItem() == item) {
                items.add(new ItemStack(item, 1, VisBatteryCharge.MAX));
                return;
            }
        }
    }
}
