package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenLakes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Random;

@Mixin(WorldGenLakes.class)
public abstract class MixinWorldGenLakes {

    @Unique
    private static volatile Field aqtweaks$lakeBlockField;

    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipVillageWaterLake(World world, Random rand, BlockPos pos,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!StructureVillageOverlap.enabled() || world == null || pos == null) return;
        Block lake = aqtweaks$lakeBlock();
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

    @Unique
    private Block aqtweaks$lakeBlock() {
        Field field = aqtweaks$lakeBlockField;
        if (field == null) {
            field = aqtweaks$findLakeBlockField();
            aqtweaks$lakeBlockField = field;
        }
        if (field == null) return null;
        try {
            Object value = field.get(this);
            return value instanceof Block ? (Block) value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Unique
    private Field aqtweaks$findLakeBlockField() {
        for (Class<?> type = this.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (String name : new String[] {"block", "field_150589_a"}) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }
}
