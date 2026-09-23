package com.apocollis.aqtweaks.mixin.bewitchment;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.MagicSchoolPresence;
import com.bewitchment.common.block.tile.entity.TileEntityWitchesAltar;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityWitchesAltar.class, remap = false)
public abstract class MixinTileEntityWitchesAltar {

    @Shadow
    public int gain;

    @Inject(method = "scan", at = @At("RETURN"))
    private void aqtweaks$hearth(int unused, CallbackInfo ci) {
        TileEntity te = (TileEntity) (Object) this;
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        var magic = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic;
        if (!MagicSchoolPresence.nearby(te.getWorld(), te.getPos(), magic.groveRange, "aqtweaks:witch",
                perks.witch.enable)) {
            return;
        }
        this.gain = Math.max(1, (int) Math.floor(this.gain * magic.hearthGain));
    }
}
