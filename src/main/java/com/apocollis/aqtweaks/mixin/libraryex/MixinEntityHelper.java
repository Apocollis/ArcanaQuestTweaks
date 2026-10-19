package com.apocollis.aqtweaks.mixin.libraryex;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.Function;

@Mixin(targets = "logictechcorp.libraryex.utility.EntityHelper", remap = false)
public abstract class MixinEntityHelper {

    @Redirect(
            method = "getEntityLocation(Lnet/minecraft/entity/EntityLivingBase;)Ljava/lang/String;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"),
            remap = false)
    private static Object aqtweaks$computeIfAbsent(Map<Object, Object> map, Object key, Function<Object, Object> fn) {
        synchronized (map) {
            return map.computeIfAbsent(key, fn);
        }
    }
}
