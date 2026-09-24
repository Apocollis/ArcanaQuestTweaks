package com.apocollis.aqtweaks.mixin.playerrevive;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.PerkAccess;
import com.creativemd.playerrevive.Revival;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Revival.class, remap = false)
public abstract class MixinRevival {

    @Shadow
    public java.util.ArrayList<EntityPlayer> revivingPlayers;

    @Shadow
    private float progress;

    @Inject(method = "tick", at = @At("RETURN"))
    private void aqtweaks$fastRevive(CallbackInfo ci) {
        if (revivingPlayers == null || revivingPlayers.isEmpty()) return;
        int extra = 0;
        boolean enabled = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.fastRevive.enable;
        for (EntityPlayer helper : revivingPlayers) {
            if (PerkAccess.on(helper, "aqtweaks:fast_revive", enabled)) extra++;
        }
        if (extra > 0) progress += extra;
    }
}
