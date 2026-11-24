package com.apocollis.aqtweaks.starter.client;

import com.apocollis.aqtweaks.starter.StarterClassView;
import com.apocollis.aqtweaks.starter.StarterResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * Entry points the starter packets call on the client. Everything runs on the client thread. Client-only: the
 * packet handlers that call this must only reference it from their client side, so a dedicated server never loads it.
 */
public final class StarterClientHooks {

    private StarterClientHooks() {}

    /** Opens the class selection screen. A repeat while the selection or the reveal is already open is ignored. */
    public static void open(final String intro, final String prompt, List<StarterClassView> classes) {
        final List<StarterClassView> copy = classes == null ? new ArrayList<StarterClassView>() : new ArrayList<>(classes);
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                GuiScreen cur = mc.currentScreen;
                if (cur instanceof GuiStarterClassSelection || cur instanceof GuiStarterReveal) {
                    return;
                }
                mc.displayGuiScreen(new GuiStarterClassSelection(intro, prompt, copy));
            }
        });
    }

    /** Shows the "what you gained" panel, cross-fading from the selection screen when that is open. */
    public static void showResult(final StarterResult result) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                GuiScreen cur = mc.currentScreen;
                if (cur instanceof GuiStarterReveal) {
                    return;
                }
                GuiStarterClassSelection prev = cur instanceof GuiStarterClassSelection ? (GuiStarterClassSelection) cur : null;
                mc.displayGuiScreen(new GuiStarterReveal(result, prev));
            }
        });
    }

    /** Closes whichever starter screen is open. */
    public static void closeAll() {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                GuiScreen cur = mc.currentScreen;
                if (cur instanceof GuiStarterClassSelection || cur instanceof GuiStarterReveal) {
                    mc.displayGuiScreen(null);
                }
            }
        });
    }
}
