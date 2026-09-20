package com.apocollis.aqtweaks.mixin.simpledifficulty;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.charles445.simpledifficulty.util.internal.ThirstUtilInternal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ThirstUtilInternal.class, remap = false)
public abstract class MixinThirstUtilInternal {

    @ModifyVariable(method = "takeDrink(Lnet/minecraft/entity/player/EntityPlayer;IFF)V", at = @At("HEAD"),
            argsOnly = true, ordinal = 1)
    private float aqtweaks$waterCollectorDirty(float dirty, EntityPlayer player, int thirst, float saturation,
            float ignored) {
        if (player == null || player instanceof FakePlayer) return dirty;
        if (!ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.waterCollector.enable) return dirty;
        if (!Reflect.hasUnlockable(player, "aqtweaks:water_collector")) return dirty;
        return 0.0f;
    }
}
