package com.apocollis.aqtweaks.mixin.bettermineshafts;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.bettermineshafts.MineshaftSurfaceShaft;
import com.apocollis.aqtweaks.rtg.StructureAccess;
import com.yungnickyoung.minecraft.bettermineshafts.world.MapGenBetterMineshaft;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Refresh the Start's bounding box after {@code generateStructure} ({@code func_75068_a}) removed
 * pieces. The method is declared on vanilla {@link StructureStart}; Better Mineshafts' {@code Start}
 * declares only constructors and Mixin cannot inject into an inherited method, so the hook sits on
 * the declaring class and is guarded to BM Starts. Runs only for BM; every other structure returns
 * at the first line.
 */
@Mixin(value = StructureStart.class, remap = false)
public abstract class MixinStructureStartMineshaftBox {

    @Inject(method = "func_75068_a", at = @At("RETURN"))
    private void aqtweaks$refreshMineshaftBox(World world, Random rand, StructureBoundingBox box, CallbackInfo ci) {
        if (!((Object) this instanceof MapGenBetterMineshaft.Start)) return;
        StructureAccess.updateStructureStartBoundingBox(this);
        if (ArcanaQuestTweaksConfig.BetterMineshaftsModuleConfig.general.surfaceShaftFallback) {
            MineshaftSurfaceShaft.carveForStart(world, (StructureStart) (Object) this, box);
        }
    }
}
