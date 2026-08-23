package com.apocollis.aqtweaks.mixin.bewitchment;

import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.bewitchment.common.world.gen.structures.WorldGenWickerman;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(value = WorldGenWickerman.class, remap = false)
public abstract class MixinWorldGenWickerman {

    @Unique
    private boolean aqtweaks$skip;

    @Unique
    private BlockPos aqtweaks$original;

    @Unique
    private BlockPos aqtweaks$placed;

    @Inject(method = "func_180709_b", at = @At("HEAD"))
    private void aqtweaks$reset(World world, Random rand, BlockPos position,
                               CallbackInfoReturnable<Boolean> cir) {
        aqtweaks$skip = false;
        aqtweaks$original = null;
        aqtweaks$placed = null;
    }

    @Redirect(
            method = "func_180709_b",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/structure/template/Template;func_186253_b(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/PlacementSettings;)V"
            )
    )
    private void aqtweaks$placeUnlessVillage(Template template, World world, BlockPos pos,
                                            PlacementSettings settings) {
        aqtweaks$original = pos;
        BlockPos at = StructureVillageOverlap.relocateOrSkip(world, template, pos, settings, "wickerman");
        if (at == null || template == null) {
            aqtweaks$skip = true;
            return;
        }
        aqtweaks$placed = at;
        template.addBlocksToWorld(world, at, settings);
    }

    @Redirect(
            method = "func_180709_b",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;func_72838_d(Lnet/minecraft/entity/Entity;)Z"
            )
    )
    private boolean aqtweaks$spawnUnlessSkipped(World world, Entity entity) {
        if (aqtweaks$skip || world == null) return false;
        if (entity != null && aqtweaks$placed != null && aqtweaks$original != null
                && !aqtweaks$placed.equals(aqtweaks$original)) {
            entity.setPosition(
                    entity.posX + (aqtweaks$placed.getX() - aqtweaks$original.getX()),
                    entity.posY + (aqtweaks$placed.getY() - aqtweaks$original.getY()),
                    entity.posZ + (aqtweaks$placed.getZ() - aqtweaks$original.getZ()));
        }
        return world.spawnEntity(entity);
    }

    @Inject(method = "spawnAnimal", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipAnimals(World world, int x, int y, int z, Random rand, CallbackInfo ci) {
        if (aqtweaks$skip) ci.cancel();
    }

    @ModifyVariable(method = "spawnAnimal", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int aqtweaks$shiftAnimalX(int x) {
        return aqtweaks$shift(x, true, false, false);
    }

    @ModifyVariable(method = "spawnAnimal", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int aqtweaks$shiftAnimalY(int y) {
        return aqtweaks$shift(y, false, true, false);
    }

    @ModifyVariable(method = "spawnAnimal", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int aqtweaks$shiftAnimalZ(int z) {
        return aqtweaks$shift(z, false, false, true);
    }

    @Unique
    private int aqtweaks$shift(int value, boolean dx, boolean dy, boolean dz) {
        if (aqtweaks$placed == null || aqtweaks$original == null) return value;
        if (dx) return value + (aqtweaks$placed.getX() - aqtweaks$original.getX());
        if (dy) return value + (aqtweaks$placed.getY() - aqtweaks$original.getY());
        if (dz) return value + (aqtweaks$placed.getZ() - aqtweaks$original.getZ());
        return value;
    }
}
