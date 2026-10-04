package com.apocollis.aqtweaks.mixin.vanilla;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Protected {@code MapGenStructure} members: the start map, saved data and two protected methods. */
@Mixin(MapGenStructure.class)
public interface AccessorMapGenStructure {

    @Accessor("structureMap")
    Long2ObjectMap<StructureStart> aqtweaks$getStructureMap();

    @Accessor("structureData")
    MapGenStructureData aqtweaks$getStructureData();

    @Invoker("initializeStructureData")
    void aqtweaks$initializeStructureData(World world);

    @Invoker("canSpawnStructureAtCoords")
    boolean aqtweaks$canSpawnStructureAtCoords(int chunkX, int chunkZ);
}
