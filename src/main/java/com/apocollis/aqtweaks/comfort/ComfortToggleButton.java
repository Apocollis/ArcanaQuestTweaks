package com.apocollis.aqtweaks.comfort;

import codersafterdark.reskillable.client.gui.button.GuiButtonInventoryTab;
import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Inventory switch for the panel, action bar, toast and particles. Tooltips stay on. It is a 32x28 tab on
 * the left frame, directly below the last Reskillable tab.
 */
@SideOnly(Side.CLIENT)
public final class ComfortToggleButton {

    private static final int BUTTON_ID = 4901;
    private static final int TAB_WIDTH = 32;
    private static final int TAB_HEIGHT = 28;
    private static final ResourceLocation TAB =
            new ResourceLocation("aqtweaks", "textures/gui/tab_left.png");
    private static final ResourceLocation ICON =
            new ResourceLocation("aqtweaks", "textures/gui/homestead_icon.png");

    private static Button current;

    /** Runs after Reskillable's own handler so its tabs are already in the button list. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiInventory gui)) return;
        int x = gui.getGuiLeft() - 28;
        int y = gui.getGuiTop() + 7;
        if (Loader.isModLoaded("reskillable")) {
            GuiButton lowest = lowestReskillableTab(event.getButtonList());
            if (lowest != null) {
                x = lowest.x;
                y = lowest.y + lowest.height + 1;
            }
        }
        x += ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.toggleOffsetX;
        y += ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.toggleOffsetY;
        current = new Button(BUTTON_ID, x, y);
        event.getButtonList().add(current);
    }

    private static GuiButton lowestReskillableTab(List<GuiButton> buttons) {
        GuiButton lowest = null;
        for (GuiButton button : buttons) {
            if (button instanceof GuiButtonInventoryTab && (lowest == null || button.y > lowest.y)) {
                lowest = button;
            }
        }
        return lowest;
    }

    @SubscribeEvent
    public void onPress(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getButton().id != BUTTON_ID) return;
        ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.comfortMessages =
                !ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.comfortMessages;
        ConfigManager.sync(ArcanaQuestTweaks.MODID, Config.Type.INSTANCE);
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiInventory gui)) return;
        Button button = current;
        if (button == null || !button.visible || !button.isHoveredAt(event.getMouseX(), event.getMouseY())) return;
        boolean on = ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.comfortMessages;
        List<String> tip = new ArrayList<>();
        tip.add("Comfort HUD: " + (on ? "Enabled" : "Disabled"));
        tip.add("§7Click to toggle the panel, messages, toast and particles");
        gui.drawHoveringText(tip, event.getMouseX(), event.getMouseY());
    }

    private static final class Button extends GuiButton {
        Button(int id, int x, int y) {
            super(id, x, y, TAB_WIDTH, TAB_HEIGHT, "");
        }

        boolean isHoveredAt(int mouseX, int mouseY) {
            return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;
            hovered = isHoveredAt(mouseX, mouseY);
            boolean on = ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.comfortMessages;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(TAB);
            drawModalRectWithCustomSizedTexture(x, y, 0, 0, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH, TAB_HEIGHT);
            GlStateManager.enableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, on ? 1.0F : 0.35F);
            mc.getTextureManager().bindTexture(ICON);
            drawModalRectWithCustomSizedTexture(x + 7, y + 5, 0, 0, 18, 18, 18, 18);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
