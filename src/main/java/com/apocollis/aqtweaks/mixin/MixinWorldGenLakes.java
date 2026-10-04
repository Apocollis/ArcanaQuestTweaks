package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenLakes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(WorldGenLakes.class)
public abstract class MixinWorldGenLakes {

    /**
     * The lake block ({@code WorldGenLakes.block}). Named by SRG with {@code remap = false}: the
     * build's refmap generator does not map {@code @Shadow} fields, so the MCP name would not
     * resolve in the release jar.
     */
    @Shadow(remap = false)
    private Block field_150556_a;

    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipVillageWaterLake(World world, Random rand, BlockPos pos,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!StructureVillageOverlap.enabled() || world == null || pos == null) return;
        Block lake = this.field_150556_a;
        if (lake == null || lake.getDefaultState().getMaterial() != Material.WATER) {
            return;
        }
        if (StructureVillageOverlap.overlapsVillage(world,
                pos.getX(), pos.getX() + 15,
                pos.getZ(), pos.getZ() + 15,
                pos.getY(), pos.getY() + 7)) {
            VillageDebug.log("water lake skip village at=%d,%d,%d", pos.getX(), pos.getY(), pos.getZ());
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
