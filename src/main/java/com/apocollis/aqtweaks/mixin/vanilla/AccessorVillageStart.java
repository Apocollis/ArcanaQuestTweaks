package com.apocollis.aqtweaks.mixin.vanilla;

import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The village Start's biome provider (the Start has no World field in 1.12.2). */
@Mixin(StructureVillagePieces.Start.class)
public interface AccessorVillageStart {

    @Accessor("biomeProvider")
    BiomeProvider aqtweaks$getBiomeProvider();
}
