package com.apocollis.aqtweaks.comfort;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Shown once per tier. The server remembers the highest tier already toasted. */
@SideOnly(Side.CLIENT)
public final class ComfortToast implements IToast {

    private final int tier;

    public ComfortToast(int tier) {
        this.tier = tier;
    }

    @Override
    public Visibility draw(GuiToast toastGui, long delta) {
        toastGui.getMinecraft().getTextureManager().bindTexture(TEXTURE_TOASTS);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, 160, 32, 256, 256);
        toastGui.getMinecraft().fontRenderer.drawString("Rested " + ComfortClientState.roman(tier), 8, 12, 0xFF5A3A10);
        RenderHelper.disableStandardItemLighting();
        return delta < 5000L ? Visibility.SHOW : Visibility.HIDE;
    }
}
