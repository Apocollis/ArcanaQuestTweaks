package com.apocollis.aqtweaks.mixin.depthsupdateperf;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.HeightCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sayys.depthsupdate.core.HeightContext;

import java.util.Map;

@Mixin(targets = "sayys.depthsupdate.core.HeightManager", remap = false)
public abstract class MixinHeightManager {

    @Shadow
    private static volatile Map<Integer, HeightContext> contexts;

    @Shadow
    public static void ensureInitialized() {
    }

    @Inject(method = "initialize", at = @At("RETURN"), remap = false)
    private static void aqtweaks$onInitialize(CallbackInfo ci) {
        HeightCache.invalidate();
    }

    /**
     * @author ArcanaQuestTweaks
     * @reason Fast dimension-indexed array lookup to avoid Integer boxing and map lookup on hot world/chunk/light paths
     */
    @Overwrite
    public static HeightContext get(int dim) {
        if (ArcanaQuestTweaksConfig.DepthsModuleConfig.general.heightCacheEnabled
                && !ArcanaQuestTweaksConfig.DepthsModuleConfig.general.heightCacheVerify) {
            HeightCache.Table t = HeightCache.table;
            int i = dim - HeightCache.MIN_DIM;
            if (t != null && i >= 0 && i < HeightCache.LEN) {
                HeightContext c = t.byDim[i];
                return c != null ? c : HeightContext.VANILLA;
            }
        }
        ensureInitialized();
        return HeightCache.getSlow(dim, contexts);
    }

    /**
     * @author ArcanaQuestTweaks
     * @reason Fast dimension-indexed array lookup to avoid Integer boxing and map lookup on hot world/chunk/light paths
     */
    @Overwrite
    public static boolean isExtended(int dim) {
        if (ArcanaQuestTweaksConfig.DepthsModuleConfig.general.heightCacheEnabled
                && !ArcanaQuestTweaksConfig.DepthsModuleConfig.general.heightCacheVerify) {
            HeightCache.Table t = HeightCache.table;
            int i = dim - HeightCache.MIN_DIM;
            if (t != null && i >= 0 && i < HeightCache.LEN) {
                return t.byDim[i] != null;
            }
        }
        ensureInitialized();
        return HeightCache.isExtendedSlow(dim, contexts);
    }
}
