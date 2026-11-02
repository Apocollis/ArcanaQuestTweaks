package com.apocollis.aqtweaks.stamina;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

public final class SolCarrotHelper {

    private static final boolean LOADED = Loader.isModLoaded("solcarrot");

    private SolCarrotHelper() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    /**
     * Retrieves the count of food milestones achieved by the player in Spice of Life: Carrot Edition.
     * Returns 0 if SolCarrot is not loaded or on failure.
     */
    public static int milestonesAchieved(EntityPlayer player) {
        if (!LOADED || player == null) return 0;
        try {
            return Impl.milestonesAchieved(player);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static final class Impl {
        static int milestonesAchieved(EntityPlayer player) {
            com.cazsius.solcarrot.tracking.FoodList list = com.cazsius.solcarrot.tracking.FoodList.get(player);
            if (list == null) return 0;
            com.cazsius.solcarrot.tracking.ProgressInfo info = list.getProgressInfo();
            return info != null ? info.milestonesAchieved() : 0;
        }
    }
}
