package com.apocollis.aqtweaks.mixin.charm;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Charm ASM calls this instead of {@code StructureComponent.addComponentParts}.
 * Returning false matches Charm's Pre {@code DENY} path.
 */
@Mixin(targets = "svenhjol.charm.base.ASMHooks", remap = false)
public abstract class MixinASMHooksVillagePaste {

    @Inject(method = "addComponentParts", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$skipWetVillagePaste(StructureComponent component, World world, Random rand,
                                                     StructureBoundingBox box, CallbackInfoReturnable<Boolean> cir) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipWaterVillagePieces) return;
        if (!VillageLandHelper.isOceanOrRiverFloor(world, component, box)) return;
        int[] xz = Reflect.getStructureComponentBoxXZ(component);
        VillageDebug.log("village piece skip water floor charm type=%s at=%d,%d",
                component.getClass().getSimpleName(),
                xz != null ? xz[0] : 0,
                xz != null ? xz[2] : 0);
        cir.setReturnValue(Boolean.FALSE);
    }
}
