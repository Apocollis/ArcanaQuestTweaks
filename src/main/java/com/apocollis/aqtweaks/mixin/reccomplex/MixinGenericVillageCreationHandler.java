package com.apocollis.aqtweaks.mixin.reccomplex;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import ivorius.reccomplex.world.gen.feature.villages.GenericVillageCreationHandler;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(value = GenericVillageCreationHandler.class, remap = false)
public abstract class MixinGenericVillageCreationHandler {

    @Inject(method = "buildComponent", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$dropWetRcBuilding(StructureVillagePieces.PieceWeight villagePiece,
                                            StructureVillagePieces.Start start,
                                            List<StructureComponent> pieces, Random random,
                                            int x, int y, int z, EnumFacing facing, int type,
                                            CallbackInfoReturnable<StructureVillagePieces.Village> cir) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipWaterVillagePieces) return;
        StructureVillagePieces.Village placed = cir.getReturnValue();
        if (placed == null || !VillageLandHelper.isAabbWet(start, placed)) return;
        VillageDebug.log("rc aabb wet origin=%d,%d, dropped", x, z);
        cir.setReturnValue(null);
    }
}
