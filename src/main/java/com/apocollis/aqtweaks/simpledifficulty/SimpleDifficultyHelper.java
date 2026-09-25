package com.apocollis.aqtweaks.simpledifficulty;

import com.charles445.simpledifficulty.api.SDCapabilities;
import com.charles445.simpledifficulty.api.temperature.ITemperatureCapability;
import com.charles445.simpledifficulty.api.thirst.IThirstCapability;
import net.minecraft.entity.player.EntityPlayer;

public final class SimpleDifficultyHelper {
    private SimpleDifficultyHelper() {}

    public static void addThirstExhaustion(EntityPlayer player, float amount) {
        IThirstCapability thirst = SDCapabilities.getThirstData(player);
        if (thirst != null) thirst.addThirstExhaustion(amount);
    }

    public static int getThirstLevel(EntityPlayer player) {
        IThirstCapability thirst = SDCapabilities.getThirstData(player);
        return thirst != null ? thirst.getThirstLevel() : -1;
    }

    public static int getTemperatureLevel(EntityPlayer player) {
        ITemperatureCapability temp = SDCapabilities.getTemperatureData(player);
        return temp != null ? temp.getTemperatureLevel() : -1;
    }
}
