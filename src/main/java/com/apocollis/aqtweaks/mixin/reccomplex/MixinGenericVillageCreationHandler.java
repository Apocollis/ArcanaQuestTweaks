package com.apocollis.aqtweaks.mixin.reccomplex;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import ivorius.reccomplex.world.gen.feature.villages.GenericVillageCreationHandler;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(value = GenericVillageCreationHandler.class, remap = false)
public abstract class MixinGenericVillageCreationHandler {

    @Unique
    private static final ThreadLocal<Boolean> AQTWEAKS$RETRYING_RC = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "buildComponent", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$retryWetRcBuilding(StructureVillagePieces.PieceWeight villagePiece,
                                             StructureVillagePieces.Start start,
                                             List<StructureComponent> pieces, Random random,
                                             int x, int y, int z, EnumFacing facing, int type,
                                             CallbackInfoReturnable<StructureVillagePieces.Village> cir) {
        if (Boolean.TRUE.equals(AQTWEAKS$RETRYING_RC.get())) return;
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipWaterVillagePieces) return;
        StructureVillagePieces.Village placed = cir.getReturnValue();
        if (placed == null || !VillageLandHelper.isAabbWet(start, placed)) return;

        VillageDebug.log("rc aabb wet origin=%d,%d, retrying", x, z);
        AQTWEAKS$RETRYING_RC.set(Boolean.TRUE);
        try {
            GenericVillageCreationHandler self = (GenericVillageCreationHandler) (Object) this;
            int maxStep = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWaterRetryDistance);
            for (int[] slot : VillageLandHelper.landCandidates(start, x, z, facing, maxStep)) {
                StructureVillagePieces.Village retry = self.buildComponent(
                        villagePiece, start, pieces, random, slot[0], y, slot[1], facing, type);
                if (retry != null && !VillageLandHelper.isAabbWet(start, retry)) {
                    VillageDebug.log("rc retry hit origin=%d,%d slot=%d,%d", x, z, slot[0], slot[1]);
                    cir.setReturnValue(retry);
                    return;
                }
            }
            VillageDebug.log("rc retry miss origin=%d,%d", x, z);
            cir.setReturnValue(null);
        } finally {
            AQTWEAKS$RETRYING_RC.set(Boolean.FALSE);
        }
    }
}
