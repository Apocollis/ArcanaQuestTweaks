package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.RunicShieldHud;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Same row correction as {@link MixinOverloadedArmorBarRunic}, for that mod's Lava Charm bar. */
@Pseudo
@Mixin(targets = "locusway.overpoweredarmorbar.overlay.LavaCharmRenderer", remap = false)
public abstract class MixinOverloadedLavaCharmRunic {

    @Redirect(
            method = "renderLavaCharm",
            require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;func_110139_bj()F"))
    private float aqtweaks$runicLayoutAbsorption(EntityPlayerSP player) {
        return RunicShieldHud.absorptionForLayout(player);
    }
}
