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
            if (rec.start == null) continue;
            float plate = VillagePlate.resolvePlate(world, rec.xz);
            if (Float.isNaN(plate)) continue;
            if (!VillagePlate.yInVillageVolume(pos.getY(), plate, heightAbove, rec)) continue;

            boolean xzHit = false;
            for (int[] box : rec.landBoxesOrEmpty()) {
                int[] padded = VillagePlate.padded(box, xzPad);
                if (VillagePlate.containsXZ(pos.getX(), pos.getZ(), padded)) {
                    xzHit = true;
                    break;
                }
            }
            if (!xzHit) continue;

            if (rec.start instanceof StructureStart) {
                String boxId = VillagePlate.key(seed, rec.xz);
                if (VillageDebug.once("yhit:" + boxId)) {
                    VillageDebug.log("detect hit pos=%d,%d,%d plate=%.1f landBoxes=%d",
                            pos.getX(), pos.getY(), pos.getZ(), plate, rec.landBoxesOrEmpty().size());
                }
                cir.setReturnValue((StructureStart) rec.start);
                return;
            }
        }
    }
}
