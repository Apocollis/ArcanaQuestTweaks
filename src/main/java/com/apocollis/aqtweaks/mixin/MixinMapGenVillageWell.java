package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.rtg.StructureAccess;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageExpander;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Places the well on the dry column before roads and houses grow.
 * {@code StructureStart} keeps the original chunk so {@code structureMap} does not move.
 */
@Mixin(value = MapGenVillage.Start.class, remap = false)
public abstract class MixinMapGenVillageWell {

    @Unique
    private static final ThreadLocal<Deque<World>> AQTWEAKS$LAYOUT_WORLD =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "<init>(Lnet/minecraft/world/World;Ljava/util/Random;III)V", at = @At("HEAD"))
    private static void aqtweaks$pushLayoutWorld(World world, Random rand, int chunkX, int chunkZ, int size, CallbackInfo ci) {
        AQTWEAKS$LAYOUT_WORLD.get().push(world);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;Ljava/util/Random;III)V", at = @At("RETURN"))
    private void aqtweaks$onLayoutComplete(World world, Random rand, int chunkX, int chunkZ, int size, CallbackInfo ci) {
        StructureStart start = (StructureStart) (Object) this;
        VillageExpander.expandVillageIfNeeded(start, rand, world);
        if (aqtweaks$nonRoadPieces(start) > 2) {
            StructureAccess.setVillageHasMoreThanTwoComponents(start, true);
        }
        Deque<World> stack = AQTWEAKS$LAYOUT_WORLD.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(
            method = "<init>(Lnet/minecraft/world/World;Ljava/util/Random;III)V",
            at = @At(
                    value = "NEW",
                    target = "net/minecraft/world/gen/structure/StructureVillagePieces$Start"
            )
    )
    private static StructureVillagePieces.Start aqtweaks$startAtDryWell(
            BiomeProvider provider, int type, Random rand, int x, int z, List weights, int size) {
        Deque<World> stack = AQTWEAKS$LAYOUT_WORLD.get();
        World world = stack.isEmpty() ? null : stack.peek();
        int[] resolved = VillageLandHelper.resolvedWellXZ(world, x, z);
        if (resolved[0] != x || resolved[1] != z) {
            VillageDebug.log("well-before from=%d,%d to=%d,%d", x, z, resolved[0], resolved[1]);
        }
        return new StructureVillagePieces.Start(provider, type, rand, resolved[0], resolved[1], weights, size);
    }

    /** Vanilla {@code isSizeableStructure}: every piece that is not a road, including the well. */
    @Unique
    private static int aqtweaks$nonRoadPieces(StructureStart start) {
        int count = 0;
        if (start == null || start.getComponents() == null) return 0;
        for (StructureComponent component : start.getComponents()) {
            if (component != null && !(component instanceof StructureVillagePieces.Road)) {
                count++;
            }
        }
        return count;
    }
}
