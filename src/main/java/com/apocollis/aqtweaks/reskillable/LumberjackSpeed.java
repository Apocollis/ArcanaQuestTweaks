package com.apocollis.aqtweaks.reskillable;

import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Lumberjack (plan 20261120-1700, section 13): logs break {@code Lumberjack Speed Multiplier} times faster. The break
 * speed event runs on both sides, because the client decides when a dig is finished and the server only checks it.
 * The handler runs last so it multiplies whatever Tree Chopper (which sets original speed divided by the log count)
 * and the other handlers left.
 */
public final class LumberjackSpeed {

    private LumberjackSpeed() {}

    /** A log: a {@code BlockLog} or anything whose item carries the {@code logWood} ore dictionary name. */
    public static boolean isLog(IBlockState state) {
        if (state == null) {
            return false;
        }
        if (state.getBlock() instanceof BlockLog) {
            return true;
        }
        try {
            ItemStack stack = new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state));
            if (stack.isEmpty()) {
                return false;
            }
            int wanted = OreDictionary.getOreID("logWood");
            for (int id : OreDictionary.getOreIDs(stack)) {
                if (id == wanted) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            // an odd modded block: not a log
        }
        return false;
    }

    /** The new break speed: {@code speed * multiplier} when the multiplier is above 1, else unchanged. */
    public static float apply(float speed, double multiplier) {
        return multiplier > 1.0D ? (float) (speed * multiplier) : speed;
    }
}
