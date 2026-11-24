package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.RunicRechargeHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.lib.events.PlayerEvents;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Ley-Rupture: a surge stops the runic shield refilling. {@code handleRunicArmor} refills the shield one point at a
 * time behind an {@code AuraHandler.shouldPreserveAura} guard, before it drains vis and raises absorption. Answering
 * "preserve" for a blocked player skips the whole refill (no vis drain, no point gained) and leaves everything else in
 * the method (the cap, lowering the shield when armor is removed) alone. Golden-apple absorption never goes through here.
 */
@Mixin(value = PlayerEvents.class, remap = false)
public abstract class MixinPlayerEventsRunicRecharge {

    @Redirect(
            method = "handleRunicArmor",
            at = @At(
                    value = "INVOKE",
                    target = "Lthaumcraft/common/world/aura/AuraHandler;shouldPreserveAura(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/BlockPos;)Z",
                    remap = false))
    private static boolean aqtweaks$surgeStopsRecharge(World world, EntityPlayer player, BlockPos pos) {
        return RunicRechargeHooks.blocked(player) || AuraHandler.shouldPreserveAura(world, player, pos);
    }
}
