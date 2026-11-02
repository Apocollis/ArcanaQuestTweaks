package com.apocollis.aqtweaks.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Sound registry for Atum environmental audio tweaks.
 */
@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public class AtumSounds {

    public static final SoundEvent SANDSTORM_WIND = createSound("sandstorm.wind");

    private static SoundEvent createSound(String name) {
        ResourceLocation loc = new ResourceLocation(ArcanaQuestTweaks.MODID, name);
        SoundEvent sound = new SoundEvent(loc);
        sound.setRegistryName(loc);
        return sound;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(SANDSTORM_WIND);
    }
}
