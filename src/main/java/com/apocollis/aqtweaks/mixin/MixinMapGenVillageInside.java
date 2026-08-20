package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillagePlate;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MapGenStructure.class, remap = false)
public abstract class MixinMapGenVillageInside {

    @Inject(method = "func_175797_c", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$villageBoxContains(BlockPos pos, CallbackInfoReturnable<StructureStart> cir) {
        if (!((Object) this instanceof MapGenVillage)) return;
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableVillageBoxDetection) return;
        if (pos == null) return;

        World world = Reflect.getMapGenWorld(this);
        if (world == null) return;

        long seed = Reflect.getSeed(world);
        if (VillagePlate.starts(seed).isEmpty()) {
            VillagePlate.rememberAll(world, this);
        }

        int xzPad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBoxXZPad);
        int heightAbove = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBoxHeight);

        for (VillagePlate.Record rec : VillagePlate.starts(seed)) {
            if (rec.start == null || rec.xz == null) continue;
            int[] padded = VillagePlate.padded(rec.xz, xzPad);
            if (!VillagePlate.containsXZ(pos.getX(), pos.getZ(), padded)) continue;

            float plate = VillagePlate.resolvePlate(world, rec.xz);
            String boxId = VillagePlate.key(seed, rec.xz);
            if (!VillagePlate.yInStartVolume(pos.getY(), rec, plate, heightAbove)) {
                if (VillageDebug.once("ymiss:" + boxId)) {
                    VillageDebug.log("detect ymiss pos=%d,%d,%d plate=%s startY=%d..%d heightAbove=%d aabb=[%d,%d]x[%d,%d]",
                            pos.getX(), pos.getY(), pos.getZ(),
                            Float.isNaN(plate) ? "none" : String.format("%.1f", plate),
                            rec.minY, rec.maxY, heightAbove,
                            rec.xz[0], rec.xz[1], rec.xz[2], rec.xz[3]);
                }
                continue;
            }

            if (rec.start instanceof StructureStart) {
                if (VillageDebug.once("yhit:" + boxId)) {
                    VillageDebug.log("detect hit pos=%d,%d,%d plate=%s startY=%d..%d aabb=[%d,%d]x[%d,%d]",
                            pos.getX(), pos.getY(), pos.getZ(),
                            Float.isNaN(plate) ? "none" : String.format("%.1f", plate),
                            rec.minY, rec.maxY,
                            rec.xz[0], rec.xz[1], rec.xz[2], rec.xz[3]);
                }
                cir.setReturnValue((StructureStart) rec.start);
                return;
            }
        }
        // Miss: let vanilla test child pieces (houses, well, paths).
    }
}
