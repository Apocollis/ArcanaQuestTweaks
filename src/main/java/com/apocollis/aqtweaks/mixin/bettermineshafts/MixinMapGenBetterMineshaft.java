package com.apocollis.aqtweaks.mixin.bettermineshafts;

import com.apocollis.aqtweaks.bettermineshafts.BetterMineshaftCanSpawn;
import com.apocollis.aqtweaks.bettermineshafts.BetterMineshaftLocate;
import com.yungnickyoung.minecraft.bettermineshafts.world.MapGenBetterMineshaft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MapGenBetterMineshaft.class, remap = false)
public abstract class MixinMapGenBetterMineshaft extends MapGenMineshaft {

    @Inject(method = "func_75047_a", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$providerCanSpawn(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        Boolean spawn = BetterMineshaftCanSpawn.test(this, chunkX, chunkZ);
        if (spawn != null) {
            cir.setReturnValue(spawn);
        }
    }

    @Inject(method = "func_180706_b", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$pinMineshaftLocate(World world, BlockPos pos, boolean findUnexplored,
                                            CallbackInfoReturnable<BlockPos> cir) {
        BlockPos nearest = BetterMineshaftLocate.locate(this, world, pos, cir.getReturnValue(), findUnexplored);
        if (nearest != null) {
            cir.setReturnValue(nearest);
        }
    }
}
