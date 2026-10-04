package com.apocollis.aqtweaks.rtg;

import net.blay09.mods.waystones.worldgen.ComponentVillageWaystone;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.fml.common.Loader;

import java.util.List;
import java.util.Random;

/**
 * Compile-hard Waystones access for the village relocate (formerly {@code Class.forName} +
 * {@code getMethod} in {@code MixinStructureVillagePieces}). Always-on mixins call this class, never
 * the Waystones type directly, and {@link #available()} keeps the Waystones class from loading when
 * the mod is absent.
 */
public final class WaystoneBridge {

    private static final boolean LOADED = Loader.isModLoaded("waystones");

    private WaystoneBridge() {}

    public static boolean available() {
        return LOADED;
    }

    /** The same gazebo piece Waystones would build at this origin, or null (mod absent, no room). */
    public static StructureComponent build(StructureVillagePieces.Start start, List<StructureComponent> pieces,
                                           Random rand, int x, int y, int z, EnumFacing facing, int type) {
        if (!LOADED) return null;
        StructureVillagePieces.PieceWeight weight =
                new StructureVillagePieces.PieceWeight(ComponentVillageWaystone.class, 3, 1);
        return ComponentVillageWaystone.buildComponent(weight, start, pieces, rand, x, y, z, facing, type);
    }
}
