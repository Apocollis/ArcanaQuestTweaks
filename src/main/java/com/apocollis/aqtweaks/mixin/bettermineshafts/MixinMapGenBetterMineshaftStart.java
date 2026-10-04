package com.apocollis.aqtweaks.mixin.bettermineshafts;

import com.apocollis.aqtweaks.bettermineshafts.BetterMineshaftStartSettings;
import com.yungnickyoung.minecraft.bettermineshafts.world.MapGenBetterMineshaft;
import com.yungnickyoung.minecraft.bettermineshafts.world.generator.MineshaftVariantSettings;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = MapGenBetterMineshaft.Start.class, remap = false)
public abstract class MixinMapGenBetterMineshaftStart extends StructureStart {

    /**
     * Local 5 of the constructor: {@code this}=0, World=1, Random=2, chunkX=3, chunkZ=4, settings=5.
     * Addressed by index so the match does not depend on how Mixin counts ordinals.
     *
     * <p>The bounding-box refresh after {@code generateStructure} is NOT here: BM's {@code Start}
     * declares only constructors, so that hook lives in {@link MixinStructureStartMineshaftBox} on
     * {@code StructureStart}.
     */

    @ModifyVariable(
            method = "<init>(Lnet/minecraft/world/World;Ljava/util/Random;IILcom/yungnickyoung/minecraft/bettermineshafts/world/generator/MineshaftVariantSettings;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 5)
    private static MineshaftVariantSettings aqtweaks$localTunnelY(MineshaftVariantSettings settings) {
        return BetterMineshaftStartSettings.withTweaksY(settings);
    }
}
