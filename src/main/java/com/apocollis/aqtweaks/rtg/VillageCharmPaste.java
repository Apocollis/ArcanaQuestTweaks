package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;

/**
 * Charm {@code ASMHooks.addComponentParts} wet-paste skip. Loaded on first paste, not when
 * {@code MixinASMHooksVillagePaste} is prepared (that mixin must not pull {@link VillageLandHelper}
 * or Charm loads {@code ASMHooks} too early).
 */
public final class VillageCharmPaste {

    private VillageCharmPaste() {}

    public static boolean shouldSkip(StructureComponent component, World world, StructureBoundingBox box) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipWaterVillagePieces) return false;
        if (!VillageLandHelper.isOceanOrRiverFloor(world, component, box)) return false;
        int[] xz = Reflect.getStructureComponentBoxXZ(component);
        VillageDebug.log("village piece skip water floor charm type=%s at=%d,%d",
                component.getClass().getSimpleName(),
                xz != null ? xz[0] : 0,
                xz != null ? xz[2] : 0);
        return true;
    }
}
