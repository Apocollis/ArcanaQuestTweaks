package com.apocollis.aqtweaks.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardChannel;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.HazardTiming;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.shiroroku.theaurorian.World.AurorianWorldProvider;

import net.minecraft.world.World;

/**
 * The Moonfrost storm as a channel of the shared hazard engine ({@code docs/hazards.md}). The server decides
 * each storm and announces it; both sides read the same {@link HazardState} from the world clock.
 */
public final class MoonfrostState {
    private MoonfrostState() {}

    /** Hazard channel id 0. */
    public static final HazardChannel CHANNEL = new HazardChannel(0, "moonfrost", MoonfrostState::isAurorian,
            MoonfrostState::enabled, MoonfrostState::timing);

    public static boolean isAurorian(World world) {
        return world != null && world.provider instanceof AurorianWorldProvider;
    }

    public static boolean enabled() {
        return ArcanaQuestTweaksConfig.AurorianModuleConfig.enableMoonfrostStorms;
    }

    private static HazardTiming timing() {
        return new HazardTiming(
                ArcanaQuestTweaksConfig.AurorianModuleConfig.stormIntervalMinSeconds * 20L,
                ArcanaQuestTweaksConfig.AurorianModuleConfig.stormIntervalMaxSeconds * 20L,
                ArcanaQuestTweaksConfig.AurorianModuleConfig.stormDurationMinSeconds * 20L,
                ArcanaQuestTweaksConfig.AurorianModuleConfig.stormDurationMaxSeconds * 20L,
                ArcanaQuestTweaksConfig.AurorianModuleConfig.warningTicks,
                ArcanaQuestTweaksConfig.AurorianModuleConfig.rampTicks,
                ArcanaQuestTweaksConfig.AurorianModuleConfig.rampTicks);
    }

    /** Current state for the world; {@link HazardState#NONE} if storms are off, not Aurorian, or unannounced. */
    public static HazardState get(World world) {
        return Hazards.state(world, CHANNEL);
    }

    public static boolean isStorming(World world) {
        return get(world).strength >= 0.15F;
    }
}
