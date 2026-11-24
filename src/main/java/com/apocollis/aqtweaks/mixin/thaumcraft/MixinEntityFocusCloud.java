package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.battlemage.BattlemageSeekHooks;
import com.apocollis.aqtweaks.thaumcraft.battlemage.BattlemageSeekPicker;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.common.entities.projectile.EntityFocusCloud;
import thaumcraft.common.lib.utils.EntityUtils;

import java.util.List;

/**
 * A Battlemage-owned cloud only affects valid players (hostile cloud) or the owner and its friends (heal cloud).
 * Stock clouds, and Battlemage clouds with the target mixins switched off, are untouched. The hostile or support
 * classification is computed once per cloud.
 */
@Mixin(value = EntityFocusCloud.class, remap = false)
public abstract class MixinEntityFocusCloud {

    @Shadow
    FocusPackage focusPackage;

    @Unique
    private BattlemageSeekPicker.CloudKind aqtweaks$kind;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(
            method = "func_70071_h_",
            at = @At(
                    value = "INVOKE",
                    target = "Lthaumcraft/common/lib/utils/EntityUtils;getEntitiesInRange(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/Entity;Ljava/lang/Class;D)Ljava/util/List;"))
    private List aqtweaks$battlemageCloudScan(World world, double x, double y, double z, Entity except, Class type,
            double range) {
        List raw = EntityUtils.getEntitiesInRange(world, x, y, z, except, type, range);
        if (aqtweaks$kind == null) {
            aqtweaks$kind = BattlemageSeekHooks.cloudKind(focusPackage);
        }
        return BattlemageSeekHooks.cloudTargets((EntityFocusCloud) (Object) this, aqtweaks$kind, raw);
    }
}
