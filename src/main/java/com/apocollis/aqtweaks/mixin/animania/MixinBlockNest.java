package com.apocollis.aqtweaks.mixin.animania;

import com.animania.common.blocks.BlockNest;
import com.animania.common.helper.AnimaniaHelper;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockNest.class, remap = false)
public abstract class MixinBlockNest {

    @Redirect(
            method = "func_180639_a",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/animania/common/helper/AnimaniaHelper;addItem(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V"
            )
    )
    private void aqtweaks$herdNestEgg(EntityPlayer player, ItemStack stack) {
        AnimaniaHelper.addItem(player, stack);
        if (player == null || player instanceof FakePlayer) return;
        if (player.world == null || player.world.isRemote) return;
        if (stack == null || stack.isEmpty()) return;
        if (!ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.herdAbundance.enable) return;
        if (!Reflect.hasUnlockable(player, "aqtweaks:herd_abundance")) return;
        AnimaniaHelper.addItem(player, stack.copy());
    }
}
