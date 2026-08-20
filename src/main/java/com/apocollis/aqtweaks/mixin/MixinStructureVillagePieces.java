package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(value = StructureVillagePieces.class, remap = false)
public abstract class MixinStructureVillagePieces {

    @Unique
    private static final ThreadLocal<Boolean> AQTWEAKS$RETRYING_HOUSE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Shadow(remap = false)
    private static StructureComponent func_176066_d(StructureVillagePieces.Start start, List<StructureComponent> structureComponents,
                                                    Random rand, int x, int y, int z, EnumFacing facing, int type) {
        throw new IllegalStateException("Mixin shadow");
    }

    @Inject(method = "func_176066_d", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$retryHouseOnLand(StructureVillagePieces.Start start, List<StructureComponent> structureComponents,
                                                  Random rand, int x, int y, int z, EnumFacing facing, int type,
                                                  CallbackInfoReturnable<StructureComponent> cir) {
        if (Boolean.TRUE.equals(AQTWEAKS$RETRYING_HOUSE.get())) return;
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipWaterVillagePieces) return;
        if (!VillageLandHelper.isWaterAt(start, x, z)) return;
        VillageDebug.log("house origin wet x=%d z=%d, retrying", x, z);
        cir.setReturnValue(aqtweaks$placeHouseOnLand(start, structureComponents, rand, x, y, z, facing, type));
    }

    @Inject(method = "func_176066_d", at = @At("RETURN"), cancellable = true)
    private static void aqtweaks$rejectHouseAabbOnWater(StructureVillagePieces.Start start, List<StructureComponent> structureComponents,
                                                        Random rand, int x, int y, int z, EnumFacing facing, int type,
                                                        CallbackInfoReturnable<StructureComponent> cir) {
        if (Boolean.TRUE.equals(AQTWEAKS$RETRYING_HOUSE.get())) return;
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipWaterVillagePieces) return;
        StructureComponent placed = cir.getReturnValue();
        if (placed == null || !VillageLandHelper.isAabbWet(start, placed)) return;
        VillageDebug.log("house aabb wet origin=%d,%d, removed retrying", x, z);
        structureComponents.remove(placed);
        cir.setReturnValue(aqtweaks$placeHouseOnLand(start, structureComponents, rand, x, y, z, facing, type));
    }

    @Unique
    private static StructureComponent aqtweaks$placeHouseOnLand(StructureVillagePieces.Start start, List<StructureComponent> structureComponents,
                                                               Random rand, int x, int y, int z, EnumFacing facing, int type) {
        AQTWEAKS$RETRYING_HOUSE.set(Boolean.TRUE);
        try {
            int maxStep = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWaterRetryDistance);
            for (int[] slot : VillageLandHelper.landCandidates(start, x, z, facing, maxStep)) {
                StructureComponent placed = func_176066_d(start, structureComponents, rand, slot[0], y, slot[1], facing, type);
                if (placed != null && !VillageLandHelper.isAabbWet(start, placed)) {
                    VillageDebug.log("house retry hit origin=%d,%d slot=%d,%d", x, z, slot[0], slot[1]);
                    return placed;
                }
                if (placed != null) {
                    structureComponents.remove(placed);
                }
            }
            VillageDebug.log("house retry miss origin=%d,%d", x, z);
            return null;
        } finally {
            AQTWEAKS$RETRYING_HOUSE.set(Boolean.FALSE);
        }
    }
}
