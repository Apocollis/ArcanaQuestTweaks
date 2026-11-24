package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.battlemage.BattlemageSeekHooks;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.entities.projectile.EntityFocusProjectile;
import thaumcraft.common.lib.utils.EntityUtils;

import java.util.List;

/**
 * A Battlemage's Seek Hostiles projectile only considers valid players, the mob's attack target first (plan 2.2).
 * Redirects the seek scan; the view cone, line of sight, steering and 5-tick interval stay stock.
 */
@Mixin(value = EntityFocusProjectile.class, remap = false)
public abstract class MixinEntityFocusProjectileSeek {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(
            method = "func_70071_h_",
            at = @At(
                    value = "INVOKE",
                    target = "Lthaumcraft/common/lib/utils/EntityUtils;getEntitiesInRangeSorted(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Ljava/lang/Class;D)Ljava/util/List;"))
    private List aqtweaks$battlemageSeekScan(World world, Entity self, Class type, double range) {
        List raw = EntityUtils.getEntitiesInRangeSorted(world, self, type, range);
        return BattlemageSeekHooks.seekList((EntityFocusProjectile) (Object) this, raw);
    }
}
