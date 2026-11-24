package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.battlemage.BattlemageSeekHooks;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.entities.monster.EntitySpellBat;

/**
 * A Battlemage's hostile Spell Bat hunts players only, the owner's attack target first, within the stock 12 block
 * radius. Friendly (heal) bats stay stock. Only a new search is changed; a held target is kept.
 */
@Mixin(value = EntitySpellBat.class, remap = false)
public abstract class MixinEntitySpellBatTarget {

    @Inject(method = "findTargetToAttack", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$battlemageBatTarget(CallbackInfoReturnable<EntityLivingBase> cir) {
        EntitySpellBat bat = (EntitySpellBat) (Object) this;
        if (BattlemageSeekHooks.handlesBat(bat)) {
            cir.setReturnValue(BattlemageSeekHooks.pickBat(bat));
        }
    }
}
