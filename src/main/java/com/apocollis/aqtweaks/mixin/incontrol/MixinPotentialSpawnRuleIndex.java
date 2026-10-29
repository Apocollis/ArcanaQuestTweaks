package com.apocollis.aqtweaks.mixin.incontrol;

import com.apocollis.aqtweaks.spawning.IndexedSpawnRule;
import com.apocollis.aqtweaks.spawning.PotentialSpawnIndex;
import com.google.gson.JsonElement;
import java.util.Set;
import mcjty.incontrol.rules.PotentialSpawnRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Remembers each potential-spawn rule's dimension, biomes and creature type for {@link PotentialSpawnIndex}. */
@Mixin(value = PotentialSpawnRule.class, remap = false)
public abstract class MixinPotentialSpawnRuleIndex implements IndexedSpawnRule {
    @Unique
    private String aqtweaks$type;
    @Unique
    private int[] aqtweaks$dimensions;
    @Unique
    private Set<String> aqtweaks$biomes;

    @Inject(method = "parse", at = @At("RETURN"))
    private static void aqtweaks$captureIndex(JsonElement element, CallbackInfoReturnable<PotentialSpawnRule> cir) {
        PotentialSpawnRule rule = cir.getReturnValue();
        if (rule != null) {
            PotentialSpawnIndex.capture(element, rule);
        }
    }

    @Override
    public void aqtweaks$setIndex(String creatureType, int[] dimensions, Set<String> biomes) {
        this.aqtweaks$type = creatureType;
        this.aqtweaks$dimensions = dimensions;
        this.aqtweaks$biomes = biomes;
    }

    @Override
    public String aqtweaks$creatureType() {
        return aqtweaks$type;
    }

    @Override
    public int[] aqtweaks$dimensions() {
        return aqtweaks$dimensions;
    }

    @Override
    public Set<String> aqtweaks$biomes() {
        return aqtweaks$biomes;
    }
}
