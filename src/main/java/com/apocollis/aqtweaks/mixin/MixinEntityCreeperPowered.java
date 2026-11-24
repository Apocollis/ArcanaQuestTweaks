package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.thaumadditions.PoweredCreeper;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.network.datasync.DataParameter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Lets Shock set the creeper's synced powered flag directly (no lightning, no extra damage). */
@Mixin(EntityCreeper.class)
public abstract class MixinEntityCreeperPowered implements PoweredCreeper {

    @Shadow
    @Final
    private static DataParameter<Boolean> POWERED;

    @Override
    public void aqtweaks$setPowered() {
        ((EntityCreeper) (Object) this).getDataManager().set(POWERED, Boolean.TRUE);
    }
}
