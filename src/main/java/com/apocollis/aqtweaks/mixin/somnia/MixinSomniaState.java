package com.apocollis.aqtweaks.mixin.somnia;

import com.apocollis.aqtweaks.somnia.SomniaSleepHandler;
import com.kingrunes.somnia.common.util.SomniaState;
import com.kingrunes.somnia.server.ServerTickHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = SomniaState.class, remap = false)
public abstract class MixinSomniaState {

    /**
     * @author ArcanaQuestTweaks
     * @reason Percentage-based sleep threshold, non-spectator filtering, and 3-tier sleep state evaluation
     */
    @Overwrite
    public static SomniaState getState(ServerTickHandler handler) {
        return SomniaSleepHandler.getState(handler);
    }
}
