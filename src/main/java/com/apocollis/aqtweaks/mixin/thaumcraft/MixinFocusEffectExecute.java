package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftFocusHooks;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import thaumcraft.common.items.casters.foci.FocusEffectAir;
import thaumcraft.common.items.casters.foci.FocusEffectCurse;
import thaumcraft.common.items.casters.foci.FocusEffectEarth;
import thaumcraft.common.items.casters.foci.FocusEffectFire;
import thaumcraft.common.items.casters.foci.FocusEffectFlux;
import thaumcraft.common.items.casters.foci.FocusEffectFrost;
import thaumcraft.common.items.casters.foci.FocusEffectHeal;

@Mixin(value = {
        FocusEffectFire.class,
        FocusEffectFrost.class,
        FocusEffectAir.class,
        FocusEffectEarth.class,
        FocusEffectFlux.class,
        FocusEffectCurse.class,
        FocusEffectHeal.class
}, remap = false)
public abstract class MixinFocusEffectExecute {

    @ModifyArg(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
                    remap = true
            ),
            index = 0
    )
    private DamageSource aqtweaks$markFocusMagic(DamageSource source) {
        return ThaumcraftFocusHooks.markMagic(source);
    }
}
