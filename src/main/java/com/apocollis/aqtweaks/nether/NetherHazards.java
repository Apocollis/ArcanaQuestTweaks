package com.apocollis.aqtweaks.nether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardChannel;
import com.apocollis.aqtweaks.hazard.HazardTiming;

import net.minecraft.world.World;
import net.minecraft.world.WorldProviderHell;

/** The two Nether hazard channels on the shared hazard engine ({@code docs/hazards.md}). Ids 1 and 2. */
public final class NetherHazards {
    private NetherHazards() {}

    public static final HazardChannel SPOUT = new HazardChannel(1, "spout", NetherHazards::isNether,
            () -> ArcanaQuestTweaksConfig.NetherModuleConfig.enableSpouts, NetherHazards::spoutTiming);

    public static final HazardChannel ASH = new HazardChannel(2, "ash", NetherHazards::isNether,
            () -> ArcanaQuestTweaksConfig.NetherModuleConfig.enableAsh, NetherHazards::ashTiming);

    public static boolean isNether(World world) {
        return world != null && world.provider instanceof WorldProviderHell;
    }

    private static HazardTiming spoutTiming() {
        long window = ArcanaQuestTweaksConfig.NetherModuleConfig.spoutWindowSeconds * 20L;
        return new HazardTiming(
                ArcanaQuestTweaksConfig.NetherModuleConfig.spoutIntervalMinSeconds * 20L,
                ArcanaQuestTweaksConfig.NetherModuleConfig.spoutIntervalMaxSeconds * 20L,
                window, window,
                ArcanaQuestTweaksConfig.NetherModuleConfig.spoutTremorSeconds * 20, 0, 0);
    }

    private static HazardTiming ashTiming() {
        return new HazardTiming(
                ArcanaQuestTweaksConfig.NetherModuleConfig.ashIntervalMinSeconds * 20L,
                ArcanaQuestTweaksConfig.NetherModuleConfig.ashIntervalMaxSeconds * 20L,
                ArcanaQuestTweaksConfig.NetherModuleConfig.ashDurationMinSeconds * 20L,
                ArcanaQuestTweaksConfig.NetherModuleConfig.ashDurationMaxSeconds * 20L,
                ArcanaQuestTweaksConfig.NetherModuleConfig.ashWarningSeconds * 20, 0, 0);
    }

    public static LavaSpoutHandler spouts;
    public static AshCloudManager ash;

    /** Registers the channels and the server handlers. Called from {@code CommonProxy.init}. */
    public static void register() {
        com.apocollis.aqtweaks.hazard.HazardChannels.register(SPOUT);
        com.apocollis.aqtweaks.hazard.HazardChannels.register(ASH);
        spouts = new LavaSpoutHandler();
        ash = new AshCloudManager();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(spouts);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ash);
    }
}
