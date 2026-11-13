package com.apocollis.aqtweaks.nether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.weather.Wildcard;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Whether the ash gas can occupy a block. Open: air, plants, ladders, fire, partial blocks and open doors, gates
 * and trapdoors. Blocked: liquids, full cubes, closed doors, trapdoors and gates (by their {@code OPEN}
 * property), and the {@code Gas Blocking Blocks} list (glass, panes, iron bars). Unloaded chunks are blocked.
 */
public final class GasPermeability {
    private GasPermeability() {}

    private static final Map<IBlockState, Boolean> CACHE = new HashMap<>();

    public static void clearCache() {
        CACHE.clear();
    }

    public static boolean isOpen(World world, int x, int y, int z) {
        IBlockState state = LoadedBlocks.peek(world, x, y, z);
        if (state == null) return false;
        Block block = state.getBlock();
        if (block == Blocks.AIR) return true;
        if (block instanceof BlockDoor || block instanceof BlockTrapDoor || block instanceof BlockFenceGate) {
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState actual = state.getActualState(world, pos);
            if (block instanceof BlockDoor) return actual.getValue(BlockDoor.OPEN);
            if (block instanceof BlockTrapDoor) return actual.getValue(BlockTrapDoor.OPEN);
            return actual.getValue(BlockFenceGate.OPEN);
        }
        Boolean cached = CACHE.get(state);
        if (cached != null) return cached;
        boolean open = compute(world, state, new BlockPos(x, y, z));
        CACHE.put(state, open);
        return open;
    }

    private static boolean compute(World world, IBlockState state, BlockPos pos) {
        if (state.getMaterial().isLiquid()) return false;
        ResourceLocation reg = state.getBlock().getRegistryName();
        if (reg != null && Wildcard.matchesAny(reg.toString(),
                ArcanaQuestTweaksConfig.NetherModuleConfig.gasBlockingBlocks)) {
            return false;
        }
        if (!state.isFullCube() && !state.isOpaqueCube()) return true;
        return state.getCollisionBoundingBox(world, pos) == Block.NULL_AABB;
    }
}
