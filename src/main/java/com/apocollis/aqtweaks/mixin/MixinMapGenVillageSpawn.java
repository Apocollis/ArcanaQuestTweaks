package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MapGenVillage.class, remap = false)
public abstract class MixinMapGenVillageSpawn {

    @Inject(method = "func_75047_a", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$rejectCoastalVillage(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.rejectCoastalVillageStarts) return;
        if (VillageLandHelper.isSamplingLandscape()) return;

        World world = Reflect.getMapGenWorld(this);
        if (world == null) return;
        String reason = VillageLandHelper.startRejectReason(world, chunkX, chunkZ);
        if (reason != null) {
            VillageDebug.log("veto chunk=%d,%d well=%d,%d %s",
                    chunkX, chunkZ, chunkX * 16 + 2, chunkZ * 16 + 2, reason);
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
