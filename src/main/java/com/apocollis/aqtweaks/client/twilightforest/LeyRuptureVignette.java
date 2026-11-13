package com.apocollis.aqtweaks.client.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.twilightforest.ClientLey;
import com.apocollis.aqtweaks.twilightforest.LeyChannels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Violet and emerald static around the screen edges while the player is exposed to a surge. */
@SideOnly(Side.CLIENT)
public class LeyRuptureVignette {
    private static final int BANDS = 14;

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        float exposure = ClientLey.exposure;
        if (exposure <= 0.02F || !ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.enableVignette) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || !LeyChannels.isTf(mc.world) || mc.gameSettings.hideGUI) return;
        ScaledResolution res = event.getResolution();
        int w = res.getScaledWidth();
        int h = res.getScaledHeight();
        double t = (mc.world.getTotalWorldTime() + event.getPartialTicks()) * 0.35D;
        for (int i = 0; i < BANDS; i++) {
            float fade = 1.0F - (float) i / BANDS;
            float pulse = 0.75F + 0.25F * (float) Math.sin(t + i * 0.7D);
            int alpha = (int) (exposure * fade * fade * pulse * 90.0F);
            if (alpha <= 0) continue;
            // Violet on the left and right, emerald on the top and bottom, so the edges shimmer between the two.
            int violet = (alpha << 24) | 0x7A2BD6;
            int emerald = (alpha << 24) | 0x1FC97A;
            int step = i * 4;
            Gui.drawRect(step, 0, step + 4, h, violet);
            Gui.drawRect(w - step - 4, 0, w - step, h, violet);
            Gui.drawRect(0, step, w, step + 4, emerald);
            Gui.drawRect(0, h - step - 4, w, h - step, emerald);
        }
    }
}
