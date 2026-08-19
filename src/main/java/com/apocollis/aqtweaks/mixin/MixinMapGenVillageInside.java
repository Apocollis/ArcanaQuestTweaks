package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
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

        Reflect.initializeStructureData(this, world);

        int xzPad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBoxXZPad);
        int heightAbove = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBoxHeight);

        for (Object startObj : Reflect.getMapGenStructureStarts(this)) {
            if (!Reflect.isSizeableStructure(startObj)) continue;
            int[] box = Reflect.getStructureStartBoxXZ(startObj);
            if (box == null) continue;
            int[] padded = VillagePlate.padded(box, xzPad);
            if (!VillagePlate.containsXZ(pos.getX(), pos.getZ(), padded)) continue;

            float plate = VillagePlate.resolve(world, startObj, box);
            if (!VillagePlate.yInSlab(pos.getY(), plate, heightAbove)) continue;

            if (startObj instanceof StructureStart) {
                cir.setReturnValue((StructureStart) startObj);
                return;
            }
        }
        cir.setReturnValue(null);
    }
}
