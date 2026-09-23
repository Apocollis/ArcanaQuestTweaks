package com.apocollis.aqtweaks.mixin.astral;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.MagicSchoolPresence;
import hellfirepvp.astralsorcery.common.crafting.altar.AbstractAltarRecipe;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractAltarRecipe.class, remap = false)
public abstract class MixinAbstractAltarRecipe {

    @Inject(method = "getPassiveStarlightRequired", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$thriftStarlight(CallbackInfoReturnable<Integer> cir) {
        EntityPlayer player = MagicSchoolPresence.astralCrafter();
        boolean astro = MagicSchoolPresence.unlocked(player, "aqtweaks:astromancer",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.astromancer.enable);
        cir.setReturnValue(MagicSchoolPresence.scaleSpend(cir.getReturnValueI(), astro));
    }

    @Inject(method = "craftingTickTime", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$fasterAltar(CallbackInfoReturnable<Integer> cir) {
        EntityPlayer player = MagicSchoolPresence.astralCrafter();
        if (!MagicSchoolPresence.unlocked(player, "aqtweaks:astromancer",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.astromancer.enable)) {
            return;
        }
        int scaled = (int) Math.floor(cir.getReturnValueI()
                * ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.altarCraftTime);
        cir.setReturnValue(Math.max(1, scaled));
    }
}
