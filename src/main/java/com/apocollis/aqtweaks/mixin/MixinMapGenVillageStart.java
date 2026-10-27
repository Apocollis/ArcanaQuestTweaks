package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.rtg.StructureAccess;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import com.apocollis.aqtweaks.rtg.VillagePlate;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
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
        World world = StructureAccess.getMapGenWorld(this);
        if (ArcanaQuestTweaksConfig.RtgModuleConfig.surface.rejectCoastalVillageStarts
                && world != null) {
            String reason = VillageLandHelper.startRejectReason(world, chunkX, chunkZ);
            if (reason != null) {
                VillageDebug.log("reject-start chunk=%d,%d well=%d,%d %s",
                        chunkX, chunkZ, chunkX * 16 + 2, chunkZ * 16 + 2, reason);
                StructureAccess.removeStructureStart(this, chunkX, chunkZ);
                return;
            }
        }
        int originX = chunkX * 16 + 2;
        int originZ = chunkZ * 16 + 2;
        int[] resolved = VillageLandHelper.resolvedWellXZ(world, originX, originZ);
        int wellX = resolved[0];
        int wellZ = resolved[1];
        if (world != null && VillageLandHelper.isNeverRaiseAt(world, wellX, wellZ)) {
            VillageDebug.log("reject-start chunk=%d,%d well=%d,%d ocean_well after-walk",
                    chunkX, chunkZ, wellX, wellZ);
            StructureAccess.removeStructureStart(this, chunkX, chunkZ);
            return;
        }
        VillagePlate.remember(world, start, chunkX, chunkZ, wellX, wellZ);
        if (!VillageDebug.enabled()) return;
        int[] xz = StructureAccess.getStructureStartBoxXZ(start);
        List<int[]> landBoxes = VillagePlate.landBoxesOf(start);
        int[] land = VillagePlate.union(landBoxes);
        BiomeProvider provider = world != null ? StructureAccess.getBiomeProvider(world) : null;
        Biome wellBiome = provider != null ? StructureAccess.getBiome(provider, wellX, wellZ) : null;
        VillageDebug.log("register chunk=%d,%d well=%d,%d biome=%s aabb=[%d,%d]x[%d,%d] landBoxes=%d buildings=%d land=[%d,%d]x[%d,%d] minY=%d maxY=%d",
                chunkX, chunkZ, wellX, wellZ, VillageLandHelper.biomeId(wellBiome),
                xz != null ? xz[0] : 0, xz != null ? xz[1] : 0,
                xz != null ? xz[2] : 0, xz != null ? xz[3] : 0,
                landBoxes.size(), VillagePlate.buildingBoxesOf(start).size(),
                land != null ? land[0] : 0, land != null ? land[1] : 0,
                land != null ? land[2] : 0, land != null ? land[3] : 0,
                StructureAccess.getStructureStartMinY(start),
                StructureAccess.getStructureStartMaxY(start));
    }
}
