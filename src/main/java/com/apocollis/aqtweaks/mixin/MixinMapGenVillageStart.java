package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import com.apocollis.aqtweaks.rtg.VillagePlate;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MapGenVillage.class, remap = false)
public abstract class MixinMapGenVillageStart {

    @Inject(method = "func_75049_b", at = @At("RETURN"))
    private void aqtweaks$rememberVillageStart(int chunkX, int chunkZ, CallbackInfoReturnable<StructureStart> cir) {
        StructureStart start = cir.getReturnValue();
        if (start == null) return;
        World world = Reflect.getMapGenWorld(this);
        VillagePlate.remember(world, start, chunkX, chunkZ);
        int[] xz = Reflect.getStructureStartBoxXZ(start);
        List<int[]> landBoxes = VillagePlate.landBoxesOf(start);
        int[] land = VillagePlate.union(landBoxes);
        int wellX = chunkX * 16 + 2;
        int wellZ = chunkZ * 16 + 2;
        Biome wellBiome = world != null && world.getBiomeProvider() != null
                ? Reflect.getBiome(world.getBiomeProvider(), wellX, wellZ) : null;
        VillageDebug.log("register chunk=%d,%d well=%d,%d biome=%s aabb=[%d,%d]x[%d,%d] landBoxes=%d buildings=%d land=[%d,%d]x[%d,%d] minY=%d maxY=%d",
                chunkX, chunkZ, wellX, wellZ, VillageLandHelper.biomeId(wellBiome),
                xz != null ? xz[0] : 0, xz != null ? xz[1] : 0,
                xz != null ? xz[2] : 0, xz != null ? xz[3] : 0,
                landBoxes.size(), VillagePlate.buildingBoxesOf(start).size(),
                land != null ? land[0] : 0, land != null ? land[1] : 0,
                land != null ? land[2] : 0, land != null ? land[3] : 0,
                Reflect.getStructureStartMinY(start),
                Reflect.getStructureStartMaxY(start));
    }
}
