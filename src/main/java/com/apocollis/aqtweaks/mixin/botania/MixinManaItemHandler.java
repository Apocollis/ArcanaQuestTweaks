package com.apocollis.aqtweaks.mixin.botania;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.MagicSchoolPresence;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import vazkii.botania.api.mana.ManaItemHandler;

@Mixin(value = ManaItemHandler.class, remap = false)
public abstract class MixinManaItemHandler {

    @ModifyVariable(method = "requestMana", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int aqtweaks$thriftRequest(int mana, ItemStack stack, EntityPlayer player) {
        return scale(mana, player);
    }

    @ModifyVariable(method = "requestManaExact", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int aqtweaks$thriftExact(int mana, ItemStack stack, EntityPlayer player) {
        return scale(mana, player);
    }

    @ModifyVariable(method = "requestManaForTool", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int aqtweaks$thriftTool(int mana, ItemStack stack, EntityPlayer player) {
        return scale(mana, player);
    }

    @ModifyVariable(method = "requestManaExactForTool", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int aqtweaks$thriftExactTool(int mana, ItemStack stack, EntityPlayer player) {
        return scale(mana, player);
    }

    private static int scale(int mana, EntityPlayer player) {
        boolean druid = MagicSchoolPresence.unlocked(player, "aqtweaks:druid",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.druid.enable);
        return MagicSchoolPresence.scaleSpend(mana, druid);
    }
}
