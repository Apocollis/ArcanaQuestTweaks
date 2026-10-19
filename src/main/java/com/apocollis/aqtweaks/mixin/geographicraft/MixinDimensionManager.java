package com.apocollis.aqtweaks.mixin.geographicraft;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "climateControl.DimensionManager", remap = false)
public abstract class MixinDimensionManager {

    // getBiomeName is @SideOnly(CLIENT); a dedicated server throws NoSuchMethodError here.
    @Redirect(
            method = "salvageSpawn(Lnet/minecraft/world/World;LclimateControl/api/ClimateControlSettings;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;func_185359_l()Ljava/lang/String;"),
            remap = false)
    private String aqtweaks$biomeName(Biome biome) {
        ResourceLocation id = biome.getRegistryName();
        return id != null ? id.toString() : "unknown";
    }
}
