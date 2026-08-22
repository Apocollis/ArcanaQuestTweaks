package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(value = StructureStart.class, remap = false)
public abstract class MixinStructureStartVillagePaste {

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
}
