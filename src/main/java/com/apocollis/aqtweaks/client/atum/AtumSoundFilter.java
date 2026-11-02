package com.apocollis.aqtweaks.client.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side ambient audio filter for the Atum dimension (dim 17).
 * - Suppresses temperate/woodland wildlife (chirping songbirds, owls, crickets, cicadas, frogs) in the desert.
 * - During active sandstorms, suppresses external Dynamic Surroundings and AmbientSounds wind loops
 *   so AQTweaks' dynamic, sheltered howling sandstorm audio plays cleanly without overlapping noise.
 * - In clear weather, external ambient wind loops continue playing normally.
 */
@SideOnly(Side.CLIENT)
public class AtumSoundFilter {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlaySound(PlaySoundEvent event) {
        ISound sound = event.getSound();
        if (sound == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;

        World world = mc.world;
        WorldProvider provider = world.provider;
        if (provider == null) return;
        if (!(provider instanceof WorldProviderAtum) && provider.getDimension() != 17) {
            return;
        }

        ResourceLocation loc = sound.getSoundLocation();
        if (loc == null) return;

        String domain = loc.getNamespace().toLowerCase();
        String path = loc.getPath().toLowerCase();

        ArcanaQuestTweaksConfig.AtumFog config = ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog;

        // 1. Suppress temperate/woodland wildlife (chirping birds, crickets, frogs, etc.) in Atum desert
        if (config.suppressAnimalSoundsInAtum && isAnimalOrBirdSound(domain, path)) {
            event.setResultSound(null);
            return;
        }

        // 2. During sandstorms, suppress external wind loops so our howling sandstorm sound plays cleanly
        if (config.suppressExternalStormWindInAtum && isExternalWindOrStormSound(domain, path)) {
            boolean isStorming = (provider instanceof WorldProviderAtum) &&
                    (((WorldProviderAtum) provider).isStorming || ((WorldProviderAtum) provider).stormStrength > 0.05F);
            if (isStorming) {
                event.setResultSound(null);
            }
        }
    }

    private static boolean isAnimalOrBirdSound(String domain, String path) {
        if (!domain.equals("dsurround") && !domain.equals("ambientsounds")) {
            return false;
        }
        return path.contains("bird") || path.contains("cricket") || path.contains("cicada")
                || path.contains("frog") || path.contains("owl") || path.contains("crow")
                || path.contains("seagull") || path.contains("rooster") || path.contains("dogbark")
                || path.contains("nightingale") || path.contains("insect") || path.contains("bison")
                || path.contains("elephant") || path.contains("gnatt");
    }

    private static boolean isExternalWindOrStormSound(String domain, String path) {
        if (domain.equals("aqtweaks")) {
            return false; // Never suppress our own sandstorm audio
        }
        if (domain.equals("dsurround")) {
            return path.contains("wind") || path.contains("weather") || path.contains("rain");
        }
        if (domain.equals("ambientsounds")) {
            return path.contains("wind") || path.contains("storm") || path.contains("rain");
        }
        return false;
    }
}
