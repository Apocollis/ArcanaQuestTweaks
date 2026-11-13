package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes the gameplay sky light of the Aurorian. {@code calculateSkylightSubtracted} is what crops,
 * saplings and mob spawning read; the lightmap (screen look) does not, so this is independent of the
 * moon phase and celestial angle pinned in {@code MixinAurorianWorldProvider}.
 *
 * <p>Early mixin: no Aurorian types are imported. The provider is recognised by class name, cached per
 * class.
 */
@Mixin(World.class)
public class MixinWorldFixedSkylight {

    private static final String AURORIAN_PROVIDER = "com.shiroroku.theaurorian.World.AurorianWorldProvider";

    private static final ClassValue<Boolean> IS_AURORIAN = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return AURORIAN_PROVIDER.equals(type.getName());
        }
    };

    @Inject(method = "calculateSkylightSubtracted", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$fixedAurorianSkylight(float partialTicks, CallbackInfoReturnable<Integer> cir) {
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.enableFixedLighting) {
            return;
        }
        World world = (World) (Object) this;
        if (world.provider == null || !IS_AURORIAN.get(world.provider.getClass())) {
            return;
        }
        int level = Math.max(0, Math.min(15, ArcanaQuestTweaksConfig.AurorianModuleConfig.skyLightLevel));
        cir.setReturnValue(15 - level);
    }
}
