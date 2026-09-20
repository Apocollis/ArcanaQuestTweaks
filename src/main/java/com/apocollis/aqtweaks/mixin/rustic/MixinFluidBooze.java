package com.apocollis.aqtweaks.mixin.rustic;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rustic.common.blocks.fluids.FluidBooze;

@Mixin(value = FluidBooze.class, remap = false)
public abstract class MixinFluidBooze {

    @Inject(method = "inebriate", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$ironGut(World world, EntityPlayer player, float quality, CallbackInfo ci) {
        if (player == null || player instanceof FakePlayer) return;
        if (!ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.ironGut.enable) return;
        if (!Reflect.hasUnlockable(player, "aqtweaks:iron_gut")) return;
        ci.cancel();
    }
}
