package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.BetterCavesReplaceable;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CarverUtils.class, remap = false)
public abstract class MixinCarverUtils {

    /**
     * Overrides CarverUtils.canReplaceBlock to allow Better Caves to carve through ALL RTG
     * terrain blocks (terracotta, clay, sandstone, granite, diorite, andesite, basalt, etc.)
     * and modded stone/dirt types without stopping or fragmenting caves.
     *
     * <p>Only cancels with {@code true}. A miss leaves parent Better Caves to decide.
     */
    @Inject(method = "canReplaceBlock", at = @At("HEAD"), cancellable = true)
    private static void onCanReplaceBlock(IBlockState state, IBlockState stateAbove, CallbackInfoReturnable<Boolean> cir) {
        if (ArcanaQuestTweaksConfig.DepthsModuleConfig.general.enableDepthsModule
                && BetterCavesReplaceable.allow(state)) {
            cir.setReturnValue(true);
        }
    }
}
