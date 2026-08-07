package com.apocollis.aqtweaks.mixin.reccomplex;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import ivorius.ivtoolkit.blocks.IvBlockCollection;
import ivorius.ivtoolkit.world.WorldCache;
import ivorius.reccomplex.world.gen.feature.structure.generic.placement.StructurePlaceContext;
import ivorius.reccomplex.world.gen.feature.structure.generic.placement.rays.RayAverageMatcher;
import ivorius.reccomplex.world.gen.feature.structure.generic.placement.rays.RayMatcher;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.OptionalInt;
import java.util.Set;

@Mixin(value = RayMatcher.class, remap = false)
public abstract class MixinRayMatcher {

    @Shadow
    protected boolean up;

    @Shadow
    protected float requiredRatio;

    @Shadow
    public abstract boolean matches(WorldCache worldCache, StructurePlaceContext context, Set<BlockPos> set, int y, float requiredRatio);

    /**
     * @author ArcanaQuestTweaks
     * @reason Support negative Y levels for Recurrent Complex placement rays
     */
    @Overwrite
    public OptionalInt cast(WorldCache worldCache, StructurePlaceContext context, IvBlockCollection collection, Set<BlockPos> set, int y) {
        Set<BlockPos> shifted = RayAverageMatcher.shifted(context, collection, set);
        int height = worldCache.world.getHeight();
        int minY = (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule && ArcanaQuestTweaksConfig.depthsModule.enableRecurrentComplexNegativeY)
                   ? ArcanaQuestTweaksConfig.depthsModule.minWorldY : 0;

        while (y >= minY && y < height) {
            if (this.matches(worldCache, context, shifted, y, this.requiredRatio)) {
                return OptionalInt.of(y);
            }
            y += this.up ? 1 : -1;
        }

        return OptionalInt.empty();
    }
}
