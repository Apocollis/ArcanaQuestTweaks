package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.yungnickyoung.minecraft.bettercaves.world.bedrock.FlattenBedrock;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FlattenBedrock.class, remap = false)
public abstract class MixinFlattenBedrock {

    @Inject(method = "flattenBedrock", at = @At("HEAD"), cancellable = true)
    private static void onFlattenBedrock(ChunkPrimer primer, int width, CallbackInfo ci) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule && ArcanaQuestTweaksConfig.depthsModule.adjustBetterCavesBedrock) {
            ci.cancel();
        }
    }
}
