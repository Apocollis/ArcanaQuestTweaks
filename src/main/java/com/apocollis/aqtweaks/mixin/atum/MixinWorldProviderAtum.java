package com.apocollis.aqtweaks.mixin.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Customizes Atum sandstorm frequency and duration to make sandstorms a frequent, looming environmental threat.
 */
@Mixin(value = WorldProviderAtum.class, remap = false)
public abstract class MixinWorldProviderAtum extends net.minecraft.world.WorldProvider {

    @Shadow
    public int stormTime;

    @Shadow
    public boolean isStorming;

    @Inject(
            method = {"updateWeather", "func_76567_e"},
            at = @At(
                    value = "NEW",
                    target = "com/teammetallurgy/atum/network/packet/PacketWeather",
                    remap = false
            ),
            remap = false
    )
    private void aqtweaks$customizeStormIntervals(CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.AtumModuleConfig.enableLoomingSandstorms) {
            return;
        }

        World currentWorld = this.world;
        Random rand = currentWorld != null ? currentWorld.rand : new Random();

        if (this.isStorming) {
            // Setting active sandstorm duration
            int minTicks = Math.max(30, ArcanaQuestTweaksConfig.AtumModuleConfig.stormDurationMinSeconds) * 20;
            int maxTicks = Math.max(minTicks, ArcanaQuestTweaksConfig.AtumModuleConfig.stormDurationMaxSeconds * 20);
            this.stormTime = minTicks + (maxTicks > minTicks ? rand.nextInt(maxTicks - minTicks) : 0);
        } else {
            // Setting clear-weather interval until next sandstorm
            int minTicks = Math.max(60, ArcanaQuestTweaksConfig.AtumModuleConfig.stormIntervalMinSeconds) * 20;
            int maxTicks = Math.max(minTicks, ArcanaQuestTweaksConfig.AtumModuleConfig.stormIntervalMaxSeconds * 20);
            this.stormTime = minTicks + (maxTicks > minTicks ? rand.nextInt(maxTicks - minTicks) : 0);
        }
    }
}
