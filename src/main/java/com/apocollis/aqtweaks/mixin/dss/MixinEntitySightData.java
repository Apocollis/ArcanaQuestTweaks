package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.PerkAccess;
import com.fantasticsource.dynamicstealth.server.senses.sight.EntitySightData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntitySightData.class, remap = false)
public abstract class MixinEntitySightData {

    @Inject(method = "hasNightvision", at = @At("RETURN"), cancellable = true)
    private static void aqtweaks$darkVision(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (!(entity instanceof EntityPlayer player)) return;
        if (PerkAccess.on(player, "aqtweaks:dark_vision",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.darkVision.enable)) {
            cir.setReturnValue(true);
        }
    }
}
