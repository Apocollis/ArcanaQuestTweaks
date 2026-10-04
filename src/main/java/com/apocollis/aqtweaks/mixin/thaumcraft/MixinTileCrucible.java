package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.simpledifficulty.SimpleDifficultyHelper;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.tiles.crafting.TileCrucible;

@Mixin(value = TileCrucible.class, remap = false)
public abstract class MixinTileCrucible {

    @Redirect(
            method = "func_73660_a",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/state/IBlockState;getMaterial()Lnet/minecraft/block/material/Material;",
                    remap = true
            )
    )
    private Material aqtweaks$campfireHeatCheck(IBlockState state) {
        if (SimpleDifficultyHelper.isBurningCampfire(state)) {
            return Material.LAVA;
        }
        return state.getMaterial();
    }
}
