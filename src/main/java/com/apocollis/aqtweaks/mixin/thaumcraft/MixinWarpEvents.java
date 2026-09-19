package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftPerkHooks;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import thaumcraft.common.lib.events.WarpEvents;

@Mixin(value = WarpEvents.class, remap = false)
public abstract class MixinWarpEvents {

    @ModifyVariable(method = "checkWarpEvent", at = @At(value = "STORE", ordinal = 2), index = 5)
    private static int aqtweaks$captureBound(int bound) {
        return ThaumcraftPerkHooks.captureWarpBound(bound);
    }

    @ModifyVariable(
            method = "checkWarpEvent",
            at = @At(
                    value = "FIELD",
                    target = "Lthaumcraft/common/lib/network/PacketHandler;INSTANCE:Lnet/minecraftforge/fml/common/network/simpleimpl/SimpleNetworkWrapper;"
            ),
            index = 10,
            argsOnly = false
    )
    private static int aqtweaks$quietMind(int event, EntityPlayer player) {
        return ThaumcraftPerkHooks.quietMindSeverity(player, event);
    }
}
