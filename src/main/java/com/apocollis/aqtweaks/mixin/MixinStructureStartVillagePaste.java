package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageBridges;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import com.apocollis.aqtweaks.rtg.VillagePlate;
import com.apocollis.aqtweaks.rtg.VillageRelight;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Mixin(value = StructureStart.class, remap = false)
public abstract class MixinStructureStartVillagePaste {

    @Unique
    private static final ThreadLocal<Boolean> AQTWEAKS$SKIP_STAMP = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Redirect(
            method = "func_75068_a",
            at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;")
    )
    private Iterator<?> aqtweaks$snapshotStructureComponents(List<?> list) {
        return VillageLandHelper.snapshotStructureIterator(list);
    }

    @Inject(method = "func_75068_a", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$dropOceanWell(World world, Random rand, StructureBoundingBox box, CallbackInfo ci) {
        AQTWEAKS$SKIP_STAMP.set(Boolean.FALSE);
        if (!((Object) this instanceof MapGenVillage.Start) || world == null) return;
        Object gen = StructureVillageOverlap.findVillageGenerator(world);
        if (!(gen instanceof MapGenVillage)) return;
        if (VillageLandHelper.relocateOrDropWetWell((MapGenVillage) gen, world, this)) {
            AQTWEAKS$SKIP_STAMP.set(Boolean.TRUE);
            ci.cancel();
        }
    }

    @Redirect(
            method = "func_75068_a",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/structure/StructureComponent;func_74875_a(Lnet/minecraft/world/World;Ljava/util/Random;Lnet/minecraft/world/gen/structure/StructureBoundingBox;)Z"
            )
    )
    private boolean aqtweaks$skipWetVillagePaste(StructureComponent component, World world, Random rand,
                                                StructureBoundingBox box) {
        if (ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipWaterVillagePieces
                && VillageLandHelper.isOceanOrRiverFloor(world, component, box)) {
            int[] xz = Reflect.getStructureComponentBoxXZ(component);
            VillageDebug.log("village piece skip water floor type=%s at=%d,%d",
                    component.getClass().getSimpleName(),
                    xz != null ? xz[0] : 0,
                    xz != null ? xz[2] : 0);
            return true;
        }
        return component.addComponentParts(world, rand, box);
    }

    @Inject(method = "func_75068_a", at = @At("RETURN"))
    private void aqtweaks$stampVillagePlate(World world, Random rand, StructureBoundingBox box, CallbackInfo ci) {
        try {
            if (Boolean.TRUE.equals(AQTWEAKS$SKIP_STAMP.get())) return;
            if (!((Object) this instanceof MapGenVillage.Start) || world == null) return;
            Object gen = StructureVillageOverlap.findVillageGenerator(world);
            VillagePlate.ensureStarts(world, gen);
            long seed = Reflect.getSeed(world);
            VillagePlate.Record rec = VillagePlate.recordForStart(seed, this);
            if (rec == null) {
                VillagePlate.remember(world, this);
                rec = VillagePlate.recordForStart(seed, this);
            }
            if (rec != null) {
                VillagePlate.stampDetectionPieces(world, rec, gen);
            }
            VillageBridges.afterVillagePaste(world, (StructureStart) (Object) this, box,
                    rec != null ? VillagePlate.resolvePlate(world, rec) : Float.NaN);
            VillageRelight.afterVillagePaste(world, (StructureStart) (Object) this, box);
        } finally {
            AQTWEAKS$SKIP_STAMP.set(Boolean.FALSE);
        }
    }
}
