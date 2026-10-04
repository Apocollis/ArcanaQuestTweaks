package com.apocollis.aqtweaks.mixin.thaumcraft;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
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
        if (aqtweaks$isBurningCampfire(state)) {
            return Material.LAVA;
        }
        return state.getMaterial();
    }

    private static boolean aqtweaks$isBurningCampfire(IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        ResourceLocation regName = block.getRegistryName();
        if (regName != null && "simpledifficulty".equals(regName.getNamespace()) && "campfire".equals(regName.getPath())) {
            for (IProperty<?> prop : state.getPropertyKeys()) {
                if ("burning".equals(prop.getName()) && prop.getValueClass() == Boolean.class) {
                    //noinspection unchecked
                    return Boolean.TRUE.equals(state.getValue((IProperty<Boolean>) prop));
                }
            }
        }
        return false;
    }
}
