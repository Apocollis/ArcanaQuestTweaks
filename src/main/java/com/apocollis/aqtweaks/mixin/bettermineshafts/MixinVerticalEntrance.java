package com.apocollis.aqtweaks.mixin.bettermineshafts;

import com.apocollis.aqtweaks.bettermineshafts.VerticalEntranceAccess;
import com.yungnickyoung.minecraft.bettermineshafts.world.generator.pieces.VerticalEntrance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(value = VerticalEntrance.class, remap = false)
public abstract class MixinVerticalEntrance implements VerticalEntranceAccess {

    @Shadow
    private BlockPos centerPos;

    @Shadow
    public abstract void setBoundingBox(StructureBoundingBox box);

    @Override
    public BlockPos aqtweaks$getCenterPos() {
        return this.centerPos;
    }

    /**
     * Failed cliff openings return false so vanilla {@code generateStructure}
     * drops the piece. Keep a small underground stub so locate /
     * {@code isInsideStructure} still hit tunnel Y — not the original maxY=256
     * air column.
     */
    @Inject(method = "func_74875_a", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$keepStubEntrance(World world, Random rand, StructureBoundingBox clip,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (this.centerPos == null) {
            return;
        }
        int x = this.centerPos.getX();
        int y = this.centerPos.getY();
        int z = this.centerPos.getZ();
        this.setBoundingBox(new StructureBoundingBox(x - 2, y, z - 2, x + 2, y + 6, z + 2));
        cir.setReturnValue(Boolean.TRUE);
    }
}
