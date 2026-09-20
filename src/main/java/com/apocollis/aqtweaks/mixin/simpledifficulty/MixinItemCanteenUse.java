package com.apocollis.aqtweaks.mixin.simpledifficulty;

import com.charles445.simpledifficulty.item.ItemCanteen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemCanteen.class, remap = false)
public abstract class MixinItemCanteenUse {

    @Inject(method = "func_77659_a", at = @At("HEAD"), remap = true)
    private void aqtweaks$pushFiller(World world, EntityPlayer player, EnumHand hand,
            CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        SimpleDifficultyFillContext.PLAYER.set(player);
    }

    @Inject(method = "func_77659_a", at = @At("RETURN"), remap = true)
    private void aqtweaks$popFiller(World world, EntityPlayer player, EnumHand hand,
            CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        SimpleDifficultyFillContext.PLAYER.remove();
    }
}
