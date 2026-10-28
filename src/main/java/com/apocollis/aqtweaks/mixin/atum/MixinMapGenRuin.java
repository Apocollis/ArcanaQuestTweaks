package com.apocollis.aqtweaks.mixin.atum;

import com.teammetallurgy.atum.utils.AtumConfig;
import com.teammetallurgy.atum.world.gen.structure.pyramid.MapGenPyramid;
import com.teammetallurgy.atum.world.gen.structure.ruins.MapGenRuin;
import net.minecraft.world.gen.structure.MapGenStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A ruin is rejected when a pyramid start overlaps any chunk within 2 of the candidate, and
 * {@code MapGenPyramid.isPyramidInChunk} itself reads 2 more chunks of the pyramid map, so the check sees pyramid
 * starts up to 4 chunks away. The pyramid generator only fills 8 chunks around the chunk being generated, so a
 * candidate at the edge of that window is judged on whatever pyramids happen to be stored already: it depends on the
 * order chunks were visited in. Before the first read of a check, fill the pyramid map around the check centre with
 * Atum's own {@code generate} so the answer no longer depends on visit order (plan
 * 20261027-1844-atum-ruin-pyramid-order.md).
 */
@Mixin(value = MapGenRuin.class, remap = false)
public abstract class MixinMapGenRuin extends MapGenStructure {

    /**
     * True once this check has filled the pyramid map. Reset at the head of every check. Each generator (the server
     * one and every Chunk Steward copy) owns its own {@code MapGenRuin}; if one ruin generator is ever shared across
     * threads this becomes a race.
     */
    @Unique
    private boolean aqtweaks$filledPyramids;

    @Inject(method = "func_75047_a", at = @At("HEAD"), remap = false)
    private void aqtweaks$resetPyramidFill(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        this.aqtweaks$filledPyramids = false;
    }

    @Redirect(
            method = "func_75047_a",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/teammetallurgy/atum/world/gen/structure/pyramid/MapGenPyramid;isPyramidInChunk(II)Z"
            ),
            remap = false)
    private boolean aqtweaks$ensurePyramidsBeforeRuinCheck(MapGenPyramid pyramid, int x, int z) {
        if (!this.aqtweaks$filledPyramids) {
            this.aqtweaks$filledPyramids = true;
            // The first call of a check is the first cell of its 5 by 5 loop, (centre - 2, centre - 2), so the
            // loop centre is (x + 2, z + 2). generate covers 8 chunks around it; the loop reads 4.
            if (AtumConfig.PYRAMID_ENABLED && this.world != null) {
                pyramid.generate(this.world, x + 2, z + 2, null);
            }
        }
        return pyramid.isPyramidInChunk(x, z);
    }
}
