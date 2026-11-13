package com.apocollis.aqtweaks.aether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardChannel;
import com.apocollis.aqtweaks.hazard.HazardChannels;
import com.apocollis.aqtweaks.hazard.HazardTiming;
import com.gildedgames.the_aether.world.AetherWorldProvider;

import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

/**
 * The two Aether hazard channels on the shared hazard engine ({@code docs/hazards.md}): {@code gust} (id 3) and
 * {@code lightning} (id 4, its own timer; its warning window is the strike telegraph). Registered only when
 * {@code aether_legacy} is loaded.
 */
public final class AetherChannels {
    private AetherChannels() {}

    public static final HazardChannel GUST = new HazardChannel(3, "gust", AetherChannels::isAether,
            () -> ArcanaQuestTweaksConfig.AetherModuleConfig.squall.enableSqualls, AetherChannels::gustTiming);

    public static final HazardChannel LIGHTNING = new HazardChannel(4, "lightning", AetherChannels::isAether,
            () -> ArcanaQuestTweaksConfig.AetherModuleConfig.squall.enableLightning, AetherChannels::lightningTiming);

    public static boolean isAether(World world) {
        return world != null && world.provider instanceof AetherWorldProvider;
    }

    private static HazardTiming gustTiming() {
        return new HazardTiming(
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.gustIntervalMinSeconds * 20L,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.gustIntervalMaxSeconds * 20L,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.gustDurationMinSeconds * 20L,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.gustDurationMaxSeconds * 20L,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.warningSeconds * 20,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.rampUpTicks,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.rampDownTicks);
    }

    private static HazardTiming lightningTiming() {
        return new HazardTiming(
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningIntervalMinSeconds * 20L,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningIntervalMaxSeconds * 20L,
                20L, 20L,
                ArcanaQuestTweaksConfig.AetherModuleConfig.squall.lightningTelegraphTicks, 0, 0);
    }

    public static AetherSquallHandler handler;

    /** Registers the channels and the server handler. Called from {@code CommonProxy.init}. */
    public static void register() {
        HazardChannels.register(GUST);
        HazardChannels.register(LIGHTNING);
        handler = new AetherSquallHandler();
        MinecraftForge.EVENT_BUS.register(handler);
    }
}
