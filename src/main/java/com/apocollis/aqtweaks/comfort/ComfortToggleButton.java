package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Inventory switch for the panel, action bar, toast and particles. Tooltips stay on. */
@SideOnly(Side.CLIENT)
public final class ComfortToggleButton {

    private static final int BUTTON_ID = 4901;
    private static final ResourceLocation ICON =
            new ResourceLocation("aqtweaks", "textures/gui/homestead_icon.png");

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiInventory gui)) return;
        int left = gui.getGuiLeft();
        int x = left + 104 + ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.toggleOffsetX;
        int y = gui.height / 2 - 22 + ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.toggleOffsetY;
        event.getButtonList().add(new Button(BUTTON_ID, x, y));
    }

    @SubscribeEvent
    public void onPress(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getButton().id != BUTTON_ID) return;
        ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.comfortMessages =
                !ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.comfortMessages;
        ConfigManager.sync(ArcanaQuestTweaks.MODID, Config.Type.INSTANCE);
    }

    private static final class Button extends GuiButton {
        Button(int id, int x, int y) {
            super(id, x, y, 20, 18, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            GlStateManager.color(1.0F, 1.0F, 1.0F,
                    ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.comfortMessages ? 1.0F : 0.35F);
            mc.getTextureManager().bindTexture(ICON);
            drawModalRectWithCustomSizedTexture(x + 1, y, 0, 0, 18, 18, 18, 18);
            if (hovered) {
                String tip = ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.comfortMessages
                        ? "Comfort messages: on" : "Comfort messages: off";
                mc.fontRenderer.drawStringWithShadow(tip, x, y - 10, 0xFFFFFF);
            }
        }
    }
}
