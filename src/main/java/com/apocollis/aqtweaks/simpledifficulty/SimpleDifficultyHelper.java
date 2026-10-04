package com.apocollis.aqtweaks.simpledifficulty;

import com.charles445.simpledifficulty.api.SDCapabilities;
import com.charles445.simpledifficulty.block.BlockCampfire;
import com.charles445.simpledifficulty.api.temperature.ITemperatureCapability;
import com.charles445.simpledifficulty.api.thirst.IThirstCapability;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

public final class SimpleDifficultyHelper {
    private static final boolean LOADED = Loader.isModLoaded("simpledifficulty");

    private SimpleDifficultyHelper() {}

    /**
     * True for a lit Simple Difficulty campfire. Safe to call without Simple Difficulty installed
     * (returns false before any Simple Difficulty class is touched).
     */
    public static boolean isBurningCampfire(IBlockState state) {
        return LOADED && state != null && state.getBlock() instanceof BlockCampfire
                && state.getValue(BlockCampfire.BURNING);
    }

    public static void addThirstExhaustion(EntityPlayer player, float amount) {
        if (!LOADED) return;
        IThirstCapability thirst = SDCapabilities.getThirstData(player);
        if (thirst != null) thirst.addThirstExhaustion(amount);
    }

    public static int getThirstLevel(EntityPlayer player) {
        if (!LOADED) return -1;
        IThirstCapability thirst = SDCapabilities.getThirstData(player);
        return thirst != null ? thirst.getThirstLevel() : -1;
    }

    public static int getTemperatureLevel(EntityPlayer player) {
        if (!LOADED) return -1;
        ITemperatureCapability temp = SDCapabilities.getTemperatureData(player);
        return temp != null ? temp.getTemperatureLevel() : -1;
    }

    /** Thirst 0-20, or {@code null} when Simple Difficulty or the capability is absent. */
    public static Integer thirstOrNull(EntityPlayer player) {
        int level = getThirstLevel(player);
        return level < 0 ? null : level;
    }

    /** Body temperature 0-25, or {@code null} when Simple Difficulty or the capability is absent. */
    public static Integer temperatureOrNull(EntityPlayer player) {
        int level = getTemperatureLevel(player);
        return level < 0 ? null : level;
    }
}
