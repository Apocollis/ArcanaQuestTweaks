package com.apocollis.aqtweaks.mixin.charm;

import java.lang.reflect.Method;
import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import svenhjol.charm.base.ASMHooks;

/**
 * Charm ASM calls this instead of {@code StructureComponent.addComponentParts}.
 *
 * <p>Charm passes this return value straight back into {@code StructureStart.generateStructure},
 * where a {@code false} makes vanilla <em>drop the component from the start</em>. That is the same
 * contract the vanilla path in {@code MixinStructureStartVillagePaste} sits on, so this returns
 * {@code true} for the same reason: a wet veto has to skip the paste for this chunk while keeping
 * the piece in the list, or a building spanning chunks is permanently lost the first time one of
 * its chunks vetoes.
 *
 * <p>Do not import Tweaks RTG types here. Preparing this mixin must not load
 * {@code VillageLandHelper} / {@code StructureStart}, or Charm defines {@code ASMHooks} first
 * and CleanMix reports the target loaded too early. This json is on jar {@code MixinConfigs}
 * (early) because Charm is a Tweaks prerequisite.
 */
@Mixin(value = ASMHooks.class, remap = false)
public abstract class MixinASMHooksVillagePaste {

    @Unique
    private static Method aqtweaks$skip;

    @Inject(method = "addComponentParts", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$skipWetVillagePaste(StructureComponent component, World world, Random rand,
                                                     StructureBoundingBox box, CallbackInfoReturnable<Boolean> cir) {
        try {
            Method skip = aqtweaks$skip;
            if (skip == null) {
                skip = Class.forName("com.apocollis.aqtweaks.rtg.VillageCharmPaste")
                        .getMethod("shouldSkip", StructureComponent.class, World.class, StructureBoundingBox.class);
                aqtweaks$skip = skip;
            }
            if (Boolean.TRUE.equals(skip.invoke(null, component, world, box))) {
                cir.setReturnValue(Boolean.TRUE);
            }
        } catch (Throwable ignored) {
        }
    }
}
