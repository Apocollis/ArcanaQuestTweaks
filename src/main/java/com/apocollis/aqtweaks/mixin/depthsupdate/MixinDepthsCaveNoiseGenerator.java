package com.apocollis.aqtweaks.mixin.depthsupdate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sayys.depthsupdate.world.generation.noise.CaveNoiseGenerator;
import sayys.depthsupdate.world.generation.noise.ICaveGenerator;
import sayys.depthsupdate.world.generation.noise.CaveSampleContext;

@Mixin(value = CaveNoiseGenerator.class, remap = false)
public class MixinDepthsCaveNoiseGenerator {

    @Redirect(
        method = "generate",
        at = @At(
            value = "INVOKE",
            target = "Lsayys/depthsupdate/world/generation/noise/ICaveGenerator;sample(Lsayys/depthsupdate/world/generation/noise/CaveSampleContext;)V"
        )
    )
    private void onRedirectSample(ICaveGenerator generator, CaveSampleContext context) {
        // Intercept and disable Depths Update's built-in cave generation below Y = 0
        if (context.y >= 0) {
            generator.sample(context);
        }
    }
}
