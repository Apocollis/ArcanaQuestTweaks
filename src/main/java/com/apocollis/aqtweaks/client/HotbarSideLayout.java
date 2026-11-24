package com.apocollis.aqtweaks.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.ClientModuleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

/**
 * Room used right of the hotbar, nearest first: the offhand slot, the Spartan Weaponry quiver, then Smart HUD's
 * item slots. Each element reserves space only while it is on screen, so an absent one lets the next sit closer.
 * Client side only.
 */
public final class HotbarSideLayout {

    private HotbarSideLayout() {}

    /** Hotbar centre to the hotbar's right edge, where the offhand slot starts. */
    public static final int HOTBAR_HALF_WIDTH = 91;

    public static boolean rightSide() {
        return "right".equalsIgnoreCase(ClientModuleConfig.hotbarSide.offhandSide.trim());
    }

    /** Width the offhand slot takes: vanilla draws it only when the offhand holds an item. */
    public static int offhandReserve() {
        if (!rightSide()) {
            return 0;
        }
        EntityPlayer player = Minecraft.getMinecraft().player;
        return player != null && !player.getHeldItemOffhand().isEmpty()
                ? ClientModuleConfig.hotbarSide.offhandReserve : 0;
    }

    /** Width the quiver slot takes while Spartan Weaponry's quiver HUD is drawing. */
    public static int quiverReserve() {
        if (!rightSide() || !Loader.isModLoaded("spartanweaponry")) {
            return 0;
        }
        return SpartanQuiverProbe.shown() ? ClientModuleConfig.hotbarSide.quiverReserve : 0;
    }

    /** Distance Smart HUD's slots move out from their stock position. */
    public static int smartHudShift() {
        return offhandReserve() + quiverReserve();
    }
}
