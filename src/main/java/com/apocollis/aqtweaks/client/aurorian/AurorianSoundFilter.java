package com.apocollis.aqtweaks.client.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.aurorian.MoonfrostState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * While a Moonfrost storm (or its warning) is active in the Aurorian, suppress Dynamic Surroundings and
 * AmbientSounds wind, weather and animal loops so the storm wind plays cleanly. Our own sounds are never
 * touched. Clear weather is left alone.
 */
@SideOnly(Side.CLIENT)
public class AurorianSoundFilter {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlaySound(PlaySoundEvent event) {
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.suppressExternalWindInAurorian) return;
        ISound sound = event.getSound();
        if (sound == null) return;
        World world = Minecraft.getMinecraft().world;
        if (world == null || !MoonfrostState.isAurorian(world)) return;
        ResourceLocation loc = sound.getSoundLocation();
        if (loc == null) return;

        String domain = loc.getNamespace().toLowerCase();
        String path = loc.getPath().toLowerCase();
        if (domain.equals("aqtweaks")) return;
        if (!domain.equals("dsurround") && !domain.equals("ambientsounds")) return;

        if (!isWindWeatherOrAnimal(path)) return;
        com.apocollis.aqtweaks.hazard.HazardState state = MoonfrostState.get(world);
        if (state.strength > 0.05F || state.warning > 0.30F) {
            event.setResultSound(null);
        }
    }

    private static boolean isWindWeatherOrAnimal(String path) {
        return path.contains("wind") || path.contains("weather") || path.contains("rain") || path.contains("storm")
                || path.contains("bird") || path.contains("cricket") || path.contains("cicada")
                || path.contains("frog") || path.contains("owl") || path.contains("crow")
                || path.contains("nightingale") || path.contains("insect");
    }
}
