package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stone Cleaver and Wood Splitter skip one durability point for the break that just started. */
public final class PerkDurability {

    private static final Map<UUID, IBlockState> PENDING = new ConcurrentHashMap<>();

    private PerkDurability() {}

    public static void mark(EntityPlayer player, IBlockState state) {
        if (player == null || state == null) return;
        ItemStack tool = player.getHeldItemMainhand();
        if (qualifies(player, tool, state)) {
            PENDING.put(player.getUniqueID(), state);
        }
    }

    public static boolean consume(EntityPlayer player, ItemStack stack) {
        if (player == null) return false;
        IBlockState state = PENDING.remove(player.getUniqueID());
        return state != null && qualifies(player, stack, state);
    }

    public static boolean skip(EntityPlayer player, ItemStack stack, IBlockState state) {
        return qualifies(player, stack, state);
    }

    private static boolean qualifies(EntityPlayer player, ItemStack tool, IBlockState state) {
        if (tool == null || tool.isEmpty() || state == null) return false;
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        if (PerkAccess.on(player, "aqtweaks:stone_cleaver", perks.stoneCleaver.enable)
                && harvest(tool, "pickaxe") >= 3
                && isSoftStone(state)) {
            return true;
        }
        return PerkAccess.on(player, "aqtweaks:wood_splitter", perks.woodSplitter.enable)
                && harvest(tool, "axe") >= 3
                && isLog(state);
    }

    private static int harvest(ItemStack tool, String toolClass) {
        return tool.getItem().getHarvestLevel(tool, toolClass, null, null);
    }

    private static boolean isSoftStone(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.STONE || block == Blocks.COBBLESTONE || block == Blocks.GRAVEL;
    }

    private static boolean isLog(IBlockState state) {
        if (state.getBlock() instanceof BlockLog) return true;
        ItemStack probe = new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state));
        if (probe.isEmpty()) return false;
        int[] ids = OreDictionary.getOreIDs(probe);
        for (int id : ids) {
            if ("logWood".equals(OreDictionary.getOreName(id))) return true;
        }
        return false;
    }
}
