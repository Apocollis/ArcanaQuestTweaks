package com.apocollis.aqtweaks.mixin.bewitchment;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.MagicSchoolPresence;
import com.bewitchment.api.capability.magicpower.MagicPower;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = MagicPower.class, remap = false)
public abstract class MixinMagicPower {

    @ModifyVariable(method = "attemptDrain(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/entity/player/EntityPlayer;IZ)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int aqtweaks$witchThrift(int amount, TileEntity te, EntityPlayer player, int ignored, boolean simulate) {
        boolean witch = MagicSchoolPresence.unlocked(player, "aqtweaks:witch",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.witch.enable);
        return MagicSchoolPresence.scaleSpend(amount, witch);
    }
}
