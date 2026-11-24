package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.DssWeaponConfig;
import com.apocollis.aqtweaks.stamina.DssWeaponBalance;
import dynamicswordskills.skills.Combo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** After each hit the combo timer is DSS's own grace plus the weapon's attack period, so slow weapons keep combos. */
@Mixin(value = Combo.class, remap = false)
public abstract class MixinCombo {

    @Shadow
    private int comboTimer;

    @Shadow
    @Final
    private int timeLimit;

    @Inject(method = "add", at = @At("RETURN"))
    private void aqtweaks$addWeaponPeriod(EntityPlayer player, Entity entity, float damage, CallbackInfo ci) {
        if (!DssWeaponConfig.scaleComboWindow || comboTimer <= 0 || player == null) return;
        comboTimer = timeLimit + DssWeaponBalance.attackPeriodTicks(player);
    }
}
