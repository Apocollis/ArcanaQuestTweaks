package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.reskillable.PerkDrops;
import net.minecraftforge.event.world.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.events.ToolEvents;

@Mixin(value = ToolEvents.class, remap = false)
public abstract class MixinToolEvents {

    @Inject(method = "harvestBlockEvent", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$lithomancyOwnsRoll(BlockEvent.HarvestDropsEvent event, CallbackInfo ci) {
        if (PerkDrops.ownsLithomancy(event)) {
            ci.cancel();
        }
    }
}
