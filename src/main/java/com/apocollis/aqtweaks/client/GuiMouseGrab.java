package com.apocollis.aqtweaks.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.cleanroommc.client.input.mouse.MouseTurnEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Drops mouse look while a GUI is open and for a few camera frames after a GUI close
 * so the recenter warp is not applied as yaw.
 */
@SideOnly(Side.CLIENT)
public final class GuiMouseGrab {

    private static int suppressFrames;

    public GuiMouseGrab() {
    }

    public static void armSuppress() {
        suppressFrames = 2;
    }

    /** True once per camera update while the post-close window is still open. */
    public static boolean consumeLook() {
        if (suppressFrames <= 0) {
            return false;
        }
        suppressFrames--;
        return true;
    }

    @SubscribeEvent
    public void onMouseTurn(MouseTurnEvent event) {
        if (!ArcanaQuestTweaksConfig.MineMenuModuleConfig.general.fixGuiMouseGrab) {
            return;
        }
        if (Minecraft.getMinecraft().currentScreen != null || consumeLook()) {
            event.setCanceled(true);
        }
    }
}

