package com.apocollis.aqtweaks.mixin.embers;

import com.apocollis.aqtweaks.reskillable.MagicSchoolFoundry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import teamroots.embers.tileentity.TileEntityFurnaceBottom;

@Mixin(value = TileEntityFurnaceBottom.class, remap = false)
public abstract class MixinTileEntityFurnaceBottom {

    @Inject(method = "func_73660_a", at = @At("RETURN"))
    private void aqtweaks$foundry(CallbackInfo ci) {
        MagicSchoolFoundry.pulse((ITickable) (Object) this, (TileEntity) (Object) this);
    }
}
