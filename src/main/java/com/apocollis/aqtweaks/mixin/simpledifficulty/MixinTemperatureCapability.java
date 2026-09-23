package com.apocollis.aqtweaks.mixin.simpledifficulty;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.MagicSchoolPresence;
import com.charles445.simpledifficulty.api.temperature.ITemperatureCapability;
import com.charles445.simpledifficulty.capability.TemperatureCapability;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TemperatureCapability.class, remap = false)
public abstract class MixinTemperatureCapability {

    @Inject(method = "tickUpdate", at = @At("RETURN"))
    private void aqtweaks$schoolClamp(EntityPlayer player, World world, TickEvent.Phase phase, CallbackInfo ci) {
        ITemperatureCapability cap = (ITemperatureCapability) (Object) this;
        int temp = cap.getTemperatureLevel();
        int next = temp;
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        var magic = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic;
        if (MagicSchoolPresence.unlocked(player, "aqtweaks:cinder_ward", perks.cinderWard.enable)) {
            next = Math.min(next, magic.cinderTempMax);
        }
        if (MagicSchoolPresence.unlocked(player, "aqtweaks:astral_warmth", perks.astralWarmth.enable)) {
            next = Math.max(next, magic.astralTempMin);
        }
        if (next != temp) {
            cap.setTemperatureLevel(next);
        }
    }
}
