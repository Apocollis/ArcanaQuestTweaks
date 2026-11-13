package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardChannel;
import com.apocollis.aqtweaks.hazard.HazardChannels;
import com.apocollis.aqtweaks.hazard.HazardTiming;

import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import twilightforest.world.WorldProviderTwilightForest;

/**
 * The Ley-Rupture hazard channel (id 5, {@code rupture}) on the shared hazard engine. Its warning window is the
 * 30 s telegraph and its active window is the surge. Registered only when {@code twilightforest} is loaded.
 */
public final class LeyChannels {
    private LeyChannels() {}

    public static final HazardChannel RUPTURE = new HazardChannel(5, "rupture", LeyChannels::isTf,
            () -> ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.enable, LeyChannels::timing);

    public static boolean isTf(World world) {
        return world != null && world.provider instanceof WorldProviderTwilightForest;
    }

    private static HazardTiming timing() {
        ArcanaQuestTweaksConfig.LeyRupture c = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley;
        return new HazardTiming(c.intervalMinSeconds * 20L, c.intervalMaxSeconds * 20L, c.durationMinSeconds * 20L,
                c.durationMaxSeconds * 20L, c.warningSeconds * 20, 0, 0);
    }

    public static LeyRuptureHandler handler;

    /** Registers the channel and the server handlers. Called from {@code CommonProxy.init}. */
    public static void register() {
        HazardChannels.register(RUPTURE);
        handler = new LeyRuptureHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        MinecraftForge.EVENT_BUS.register(new LeyRuptureBacklash());
        if (net.minecraftforge.fml.common.Loader.isModLoaded("thaumcraft")) {
            VisBatteryTracker tracker = new VisBatteryTracker();
            handler.visBatteries = tracker;
            MinecraftForge.EVENT_BUS.register(tracker);
        }
    }
}
