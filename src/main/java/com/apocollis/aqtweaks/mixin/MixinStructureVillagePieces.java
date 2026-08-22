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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

@Mixin(value = StructureVillagePieces.class, remap = false)
public abstract class MixinStructureVillagePieces {

    @Unique
    private static final ThreadLocal<Boolean> AQTWEAKS$RETRYING_HOUSE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Unique
    private static final ThreadLocal<Boolean> AQTWEAKS$RETRYING_PATH = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Unique
    private static Method AQTWEAKS$WAYSTONE_BUILD;

    @Shadow(remap = false)
    private static StructureComponent func_176066_d(StructureVillagePieces.Start start, List<StructureComponent> structureComponents,
                                                    Random rand, int x, int y, int z, EnumFacing facing, int type) {
        throw new IllegalStateException("Mixin shadow");
    }

    @Shadow(remap = false)
    private static StructureComponent func_176069_e(StructureVillagePieces.Start start, List<StructureComponent> structureComponents,
                                                    Random rand, int x, int y, int z, EnumFacing facing, int type) {
        throw new IllegalStateException("Mixin shadow");
    }

    @Inject(method = "func_176066_d", at = @At("RETURN"), cancellable = true)
    private static void aqtweaks$rejectHouseAabbOnWater(StructureVillagePieces.Start start, List<StructureComponent> structureComponents,
                                                        Random rand, int x, int y, int z, EnumFacing facing, int type,
                                                        CallbackInfoReturnable<StructureComponent> cir) {
        if (Boolean.TRUE.equals(AQTWEAKS$RETRYING_HOUSE.get())) return;
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipWaterVillagePieces) return;
        StructureComponent placed = cir.getReturnValue();
        if (placed == null || !VillageLandHelper.isAabbWet(start, placed)) return;
        VillageLandHelper.removeVillagePiece(start, structureComponents, placed);
        if (VillageLandHelper.isWaystonePiece(placed)) {
            VillageDebug.log("waystone aabb wet origin=%d,%d, relocating inland", x, z);
            cir.setReturnValue(aqtweaks$relocateWaystone(start, structureComponents, rand, x, y, z, facing, type));
            return;
        }
        VillageDebug.log("house aabb wet origin=%d,%d, retrying inland", x, z);
        cir.setReturnValue(aqtweaks$placeHouseOnLand(start, structureComponents, rand, x, y, z, facing, type));
    }

    @Unique
    private static StructureComponent aqtweaks$placeHouseOnLand(StructureVillagePieces.Start start, List<StructureComponent> structureComponents,
                                                               Random rand, int x, int y, int z, EnumFacing facing, int type) {
        AQTWEAKS$RETRYING_HOUSE.set(Boolean.TRUE);
        try {
            int maxStep = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWaterRetryDistance);
            for (int[] slot : VillageLandHelper.inlandCandidates(start, x, z, facing, maxStep)) {
                StructureComponent placed = func_176066_d(start, structureComponents, rand, slot[0], y, slot[1], facing, type);
                if (placed != null && !VillageLandHelper.isAabbWet(start, placed)) {
                    VillageDebug.log("house retry hit origin=%d,%d slot=%d,%d", x, z, slot[0], slot[1]);
                    return placed;
                }
                if (placed != null) {
                    VillageLandHelper.removeVillagePiece(start, structureComponents, placed);
                }
            }
            VillageDebug.log("house retry miss origin=%d,%d", x, z);
            return null;
        } finally {
            AQTWEAKS$RETRYING_HOUSE.set(Boolean.FALSE);
        }
    }

    @Unique
    private static StructureComponent aqtweaks$relocateWaystone(StructureVillagePieces.Start start, List<StructureComponent> pieces,
                                                               Random rand, int x, int y, int z, EnumFacing facing, int type) {
        int street = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWaterRetryDistance);
        EnumFacing[] faces = aqtweaks$facingOrder(facing);
        for (int[] slot : VillageLandHelper.inlandCandidates(start, x, z, facing, street)) {
            for (EnumFacing face : faces) {
                StructureComponent retry = aqtweaks$buildWaystone(start, pieces, rand, slot[0], y, slot[1], face, type);
                if (retry != null && !VillageLandHelper.isAabbWet(start, retry)) {
                    VillageDebug.log("waystone relocate hit origin=%d,%d slot=%d,%d", x, z, slot[0], slot[1]);
                    return retry;
                }
                if (retry != null) {
                    VillageLandHelper.removeVillagePiece(start, pieces, retry);
                }
            }
        }
        VillageDebug.log("waystone relocate miss origin=%d,%d", x, z);
        return null;
    }

    @Unique
    private static EnumFacing[] aqtweaks$facingOrder(EnumFacing facing) {
        EnumFacing first = facing != null ? facing : EnumFacing.NORTH;
        return new EnumFacing[] {first, first.rotateY(), first.rotateYCCW(), first.getOpposite()};
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static StructureComponent aqtweaks$buildWaystone(StructureVillagePieces.Start start, List<StructureComponent> pieces,
                                                            Random rand, int x, int y, int z, EnumFacing facing, int type) {
        try {
            Method method = AQTWEAKS$WAYSTONE_BUILD;
            if (method == null) {
                Class<?> clazz = Class.forName("net.blay09.mods.waystones.worldgen.ComponentVillageWaystone");
                method = clazz.getMethod("buildComponent",
                        StructureVillagePieces.PieceWeight.class,
                        StructureVillagePieces.Start.class,
                        List.class,
                        Random.class,
                        int.class, int.class, int.class,
                        EnumFacing.class,
                        int.class);
                AQTWEAKS$WAYSTONE_BUILD = method;
            }
            StructureVillagePieces.PieceWeight weight = new StructureVillagePieces.PieceWeight(
                    (Class<? extends StructureVillagePieces.Village>) method.getDeclaringClass(), 3, 1);
            Object built = method.invoke(null, weight, start, pieces, rand, x, y, z, facing, type);
            return built instanceof StructureComponent ? (StructureComponent) built : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Inject(method = "func_176069_e", at = @At("RETURN"), cancellable = true)
    private static void aqtweaks$omitWetRoad(StructureVillagePieces.Start start, List<StructureComponent> structureComponents,
                                            Random rand, int x, int y, int z, EnumFacing facing, int type,
                                            CallbackInfoReturnable<StructureComponent> cir) {
        if (Boolean.TRUE.equals(AQTWEAKS$RETRYING_PATH.get())) return;
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipWaterVillagePieces) return;
        StructureComponent placed = cir.getReturnValue();
        if (placed == null || !VillageLandHelper.isVillageRoad(placed)) return;
        if (!VillageLandHelper.shouldOmitPath(start, placed)) return;
        VillageLandHelper.removeVillagePiece(start, structureComponents, placed);
        VillageDebug.log("path aabb wet origin=%d,%d %s, retrying inland",
                x, z, VillageLandHelper.pathOmitReason(start, placed));
        cir.setReturnValue(aqtweaks$placePathOnLand(start, structureComponents, rand, x, y, z, facing, type));
    }

    @Unique
    private static StructureComponent aqtweaks$placePathOnLand(StructureVillagePieces.Start start, List<StructureComponent> structureComponents,
                                                              Random rand, int x, int y, int z, EnumFacing facing, int type) {
        AQTWEAKS$RETRYING_PATH.set(Boolean.TRUE);
        try {
            int maxStep = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWaterRetryDistance);
            EnumFacing[] faces = aqtweaks$facingOrder(facing);
            for (int[] slot : VillageLandHelper.inlandCandidates(start, x, z, facing, maxStep)) {
                for (EnumFacing face : faces) {
                    StructureComponent retry = func_176069_e(start, structureComponents, rand, slot[0], y, slot[1], face, type);
                    if (retry != null && !VillageLandHelper.shouldOmitPath(start, retry)) {
                        VillageDebug.log("path retry hit origin=%d,%d slot=%d,%d", x, z, slot[0], slot[1]);
                        return retry;
                    }
                    if (retry != null) {
                        VillageLandHelper.removeVillagePiece(start, structureComponents, retry);
                    }
                }
            }
            VillageDebug.log("path retry miss origin=%d,%d, omitted", x, z);
            return null;
        } finally {
            AQTWEAKS$RETRYING_PATH.set(Boolean.FALSE);
        }
    }
}
