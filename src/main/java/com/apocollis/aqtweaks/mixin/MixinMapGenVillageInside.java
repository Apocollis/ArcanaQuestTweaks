package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
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
        if (world == null) world = VillageLandHelper.currentWorld();
        if (world == null) return;

        Object start = VillagePlate.startAt(world, this, pos.getX(), pos.getY(), pos.getZ());
        if (start instanceof StructureStart) {
            String boxId = VillagePlate.wellKey(Reflect.getSeed(world),
                    Reflect.getStructureStartChunkX(start), Reflect.getStructureStartChunkZ(start));
            if (VillageDebug.once("yhit:" + boxId)) {
                VillageDebug.log("detect hit pos=%d,%d,%d", pos.getX(), pos.getY(), pos.getZ());
            }
            cir.setReturnValue((StructureStart) start);
        }
    }
}
