package com.apocollis.aqtweaks.mixin.embers;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.MagicSchoolPresence;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import teamroots.embers.util.EmberInventoryUtil;

@Mixin(value = EmberInventoryUtil.class, remap = false)
public abstract class MixinEmberInventoryUtil {

    @ModifyVariable(method = "removeEmber", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static double aqtweaks$thrift(double amount, EntityPlayer player) {
        boolean artificer = MagicSchoolPresence.unlocked(player, "aqtweaks:artificer",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.artificer.enable);
        return MagicSchoolPresence.scaleSpend(amount, artificer);
    }
}
