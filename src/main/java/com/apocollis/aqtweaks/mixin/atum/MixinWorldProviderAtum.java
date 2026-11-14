package com.apocollis.aqtweaks.mixin.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.atum.PendingStorm;
import com.teammetallurgy.atum.utils.AtumConfig;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Customizes Atum sandstorm frequency and duration to make sandstorms a frequent, looming environmental threat,
 * carries the duration a forced {@code /atumweather sandstorm <seconds>} gets when its warning ends, and switches off
 * Atum's own weather sand loop while Tweaks accumulates the sand itself.
 */
@Mixin(value = WorldProviderAtum.class, remap = false)
public abstract class MixinWorldProviderAtum extends net.minecraft.world.WorldProvider implements PendingStorm {

    @Shadow
    public int stormTime;

    @Shadow
    public boolean isStorming;

    /** The duration, in ticks, the next storm gets when its countdown ends; 0 = none. Lives on the provider. */
    @Unique
    private int aqtweaks$pendingStormTicks;

    @Override
    public int aqtweaks$getPendingStormTicks() {
        return this.aqtweaks$pendingStormTicks;
    }

    @Override
    public void aqtweaks$setPendingStormTicks(int ticks) {
        this.aqtweaks$pendingStormTicks = Math.max(0, ticks);
    }

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
        // A forced storm: its warning has run out and the storm starts with the duration the command asked for.
        if (this.isStorming && this.aqtweaks$pendingStormTicks > 0) {
            this.stormTime = this.aqtweaks$pendingStormTicks;
            this.aqtweaks$pendingStormTicks = 0;
            return;
        }
        if (!this.isStorming) {
            this.aqtweaks$pendingStormTicks = 0;
        }
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

    /**
     * Atum's weather sand loop checks {@code SANDSTORM_SAND_RARITY > 0} first, so it walks every player chunk every
     * tick even in clear weather, and it only places up to three layers above strength 0.9. While Tweaks accumulation
     * is on, report 0 (both reads: the guard and the {@code nextInt} argument) so the whole block is skipped.
     */
    @Redirect(
            method = {"updateWeather", "func_76567_e"},
            at = @At(
                    value = "FIELD",
                    target = "Lcom/teammetallurgy/atum/utils/AtumConfig;SANDSTORM_SAND_RARITY:I",
                    opcode = Opcodes.GETSTATIC,
                    remap = false
            ),
            remap = false
    )
    private int aqtweaks$sandRarity() {
        return ArcanaQuestTweaksConfig.AtumModuleConfig.enableSandAccumulation ? 0 : AtumConfig.SANDSTORM_SAND_RARITY;
    }
}
