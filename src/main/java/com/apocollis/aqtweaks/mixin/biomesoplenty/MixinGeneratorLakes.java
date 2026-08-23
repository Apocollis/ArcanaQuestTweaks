package com.apocollis.aqtweaks.mixin.biomesoplenty;

import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Random;

@Mixin(targets = "biomesoplenty.common.world.generator.GeneratorLakes", remap = false)
public abstract class MixinGeneratorLakes {

    @Inject(method = "func_180709_b", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipVillageQuicksand(World world, Random rand, BlockPos pos,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!StructureVillageOverlap.enabled() || world == null || pos == null) return;
        if (!aqtweaks$isBopQuicksand(aqtweaks$liquid())) return;
        if (StructureVillageOverlap.overlapsVillage(world,
                pos.getX(), pos.getX() + 15,
                pos.getZ(), pos.getZ() + 15,
                pos.getY(), pos.getY() + 7)) {
            VillageDebug.log("bop quicksand skip village at=%d,%d,%d", pos.getX(), pos.getY(), pos.getZ());
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Unique
    private IBlockState aqtweaks$liquid() {
        try {
            Field field = this.getClass().getDeclaredField("liquid");
            field.setAccessible(true);
            Object value = field.get(this);
            return value instanceof IBlockState ? (IBlockState) value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Unique
    private static boolean aqtweaks$isBopQuicksand(IBlockState liquid) {
        if (liquid == null) return false;
        Block block = liquid.getBlock();
        if (block == null) return false;
        ResourceLocation name = block.getRegistryName();
        if (name == null || !"biomesoplenty".equals(name.getNamespace())) return false;
        String path = name.getPath();
        return "sand".equals(path) || "sand_fluid".equals(path);
    }
}
