package com.apocollis.aqtweaks.mixin.somnia;

import com.apocollis.aqtweaks.somnia.SomniaOptimizationHandler;
import com.kingrunes.somnia.common.util.SomniaUtil;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = SomniaUtil.class, remap = false)
public abstract class MixinSomniaUtil {

    /**
     * @author ArcanaQuestTweaks
     * @reason Fix duplicate checkLight() invocations and throttle awake ambient relight checks to 20 ticks
     */
    @Overwrite
    public static void chunkLightCheck(Chunk chunk) {
        SomniaOptimizationHandler.chunkLightCheck(chunk);
    }
}
