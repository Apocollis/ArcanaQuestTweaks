package com.apocollis.aqtweaks.mixin.bettermineshafts;

import com.apocollis.aqtweaks.bettermineshafts.BetterMineshaftLocate;
import com.yungnickyoung.minecraft.bettermineshafts.world.MapGenBetterMineshaft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Locate pin for Better Mineshafts. {@code getNearestStructurePos} ({@code func_180706_b}) is declared
 * on vanilla {@link MapGenMineshaft} and not overridden by {@link MapGenBetterMineshaft}, and Mixin only
 * injects into methods the target class itself declares. So the hook lives on {@code MapGenMineshaft}
 * and is guarded to Better Mineshafts instances; vanilla mineshaft generators are untouched.
 *
 * <p>This json is {@code required: false}: without the BM jar the mixin is skipped.
 */
@Mixin(value = MapGenMineshaft.class, remap = false)
public abstract class MixinMapGenMineshaftLocate {

    @Inject(method = "func_180706_b", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$pinMineshaftLocate(World world, BlockPos pos, boolean findUnexplored,
                                            CallbackInfoReturnable<BlockPos> cir) {
        if (!((Object) this instanceof MapGenBetterMineshaft)) return;
        BlockPos nearest = BetterMineshaftLocate.locate(this, world, pos, cir.getReturnValue(), findUnexplored);
        if (nearest != null) {
            cir.setReturnValue(nearest);
        }
    }
}
