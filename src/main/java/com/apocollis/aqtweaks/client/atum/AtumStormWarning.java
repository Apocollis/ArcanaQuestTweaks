package com.apocollis.aqtweaks.client.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.atum.AtumWarningRamp;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Warning level before an Atum sandstorm, from the client provider's own storm countdown. The countdown is
 * trustworthy because {@code AtumWeatherSync} sends Atum's own {@code PacketWeather} on join and on a
 * periodic resync. Only the dust emitter reads it.
 */
@SideOnly(Side.CLIENT)
public final class AtumStormWarning {
    private AtumStormWarning() {}

    /** 0..1; 0 when the warning is off, the world is not Atum, or a storm is active. */
    public static float level(World world) {
        ArcanaQuestTweaksConfig.AtumFog config = ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog;
        if (!config.enableStormWarning || world == null || !(world.provider instanceof WorldProviderAtum)) {
            return 0.0F;
        }
        WorldProviderAtum provider = (WorldProviderAtum) world.provider;
        boolean storming = provider.isStorming || provider.stormStrength > 0.05F;
        return AtumWarningRamp.level(storming, provider.stormTime, config.stormWarningSeconds * 20);
    }
}
