package com.apocollis.aqtweaks.patchouli;

import codersafterdark.reskillable.client.gui.button.GuiButtonInventoryTab;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.comfort.ComfortToggleButton;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import vazkii.patchouli.client.book.EntryDisplayState;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.gui.GuiButtonInventoryBook;
import vazkii.patchouli.common.book.Book;

/**
 * Moves Patchouli's inventory guidebook button (the one that replaces the recipe book button) to a 32x28 tab on the
 * left of the inventory, using the same {@code tab_left.png} as the Reskillable and Comfort tabs so a resource pack
 * that restyles those restyles this one. Patchouli keeps handling the click itself (its own
 * {@code ActionPerformedEvent.Pre} handler matches any {@link GuiButtonInventoryBook}); this only swaps the button.
 *
 * <p>Placement: the slot where Reskillable's Abilities tab would sit (between Skills and the Comfort tab), which the
 * pack never fills; if something else occupies it, directly below the lowest tab in the column.
 */
@SideOnly(Side.CLIENT)
public final class PatchouliInventoryTabHandler {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Patchouli");
    private static final int TAB_WIDTH = 32;
    private static final int TAB_HEIGHT = 28;
    private static final ResourceLocation TAB = new ResourceLocation("aqtweaks", "textures/gui/tab_left.png");

    private static boolean warnedMissing;
    private static TabButton current;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        current = null;
        if (!(event.getGui() instanceof GuiInventory gui)) return;
        ArcanaQuestTweaksConfig.PatchouliInventoryTab cfg = ArcanaQuestTweaksConfig.PatchouliModuleConfig.inventoryTab;
        if (!cfg.moveInventoryButtonToSidebar) return;
        List<GuiButton> buttons = event.getButtonList();
        int index = -1;
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i) instanceof GuiButtonInventoryBook) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            if (!warnedMissing) {
                warnedMissing = true;
                LOGGER.info("[AQ-PATCHOULI] no Patchouli inventory book button (Inventory Button Book unset?); sidebar tab skipped");
            }
            return;
        }
        GuiButtonInventoryBook old = (GuiButtonInventoryBook) buttons.get(index);
        int[] pos = place(gui, buttons, old);
        TabButton tab = new TabButton(old.getBook(), old.id, pos[0] + cfg.tabOffsetX, pos[1] + cfg.tabOffsetY);
        buttons.set(index, tab);
        current = tab;
    }

    /** Location 1: the Abilities tab slot when it is empty; Location 2: below the lowest tab in the left column. */
    private static int[] place(GuiInventory gui, List<GuiButton> buttons, GuiButton self) {
        int x = gui.getGuiLeft() - 28;
        int y = gui.getGuiTop() + 7;
        if (Loader.isModLoaded("reskillable")) {
            GuiButtonInventoryTab lowest = null;
            for (GuiButton b : buttons) {
                if (b instanceof GuiButtonInventoryTab t && (lowest == null || t.y > lowest.y)) lowest = t;
            }
            if (lowest != null) {
                x = lowest.x;
                y = lowest.y;
                if (!slotOccupied(buttons, self, x, y)) return new int[] {x, y};
            }
        }
        for (GuiButton b : buttons) {
            if (b == self) continue;
            boolean column = b instanceof GuiButtonInventoryTab || b.id == ComfortToggleButton.BUTTON_ID;
            if (column && b.y + b.height + 1 > y) y = b.y + b.height + 1;
        }
        return new int[] {x, y};
    }

    private static boolean slotOccupied(List<GuiButton> buttons, GuiButton self, int x, int y) {
        for (GuiButton b : buttons) {
            if (b == self) continue;
            if (b instanceof GuiButtonInventoryTab t) {
                // enabled is only updated on the first draw, so ask the tab itself.
                if (t.x == x && t.y == y && t.type.shouldRender()) return true;
            } else if (b.visible && b.enabled && b.x < x + TAB_WIDTH && b.x + b.width > x
                    && b.y < y + TAB_HEIGHT && b.y + b.height > y) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiInventory gui)) return;
        TabButton tab = current;
        if (tab == null || !tab.visible || !tab.hoveredAt(event.getMouseX(), event.getMouseY())) return;
        List<String> tip = new ArrayList<>();
        tip.add(TextFormatting.GOLD + I18n.format(tab.getBook().name));
        EntryDisplayState state = tab.readState();
        if (state != null && state.hasIcon && state.showInInventory) {
            tip.add(TextFormatting.YELLOW + I18n.format("gui.aqtweaks.patchouli.tab.unread"));
        }
        tip.add(TextFormatting.GRAY + I18n.format("gui.aqtweaks.patchouli.tab.click_to_open"));
        gui.drawHoveringText(tip, event.getMouseX(), event.getMouseY());
    }

    private static final class TabButton extends GuiButtonInventoryBook {
        TabButton(Book book, int id, int x, int y) {
            super(book, id, x, y);
            this.width = TAB_WIDTH;
            this.height = TAB_HEIGHT;
        }

        boolean hoveredAt(int mouseX, int mouseY) {
            return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }

        EntryDisplayState readState() {
            try {
                return getBook().contents == null ? null : getBook().contents.getReadState();
            } catch (RuntimeException e) {
                return null;
            }
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            // Hide while the vanilla recipe book is open, like the Reskillable tabs.
            visible = mc.player == null || !mc.player.getRecipeBook().isGuiOpen();
            if (!visible) return;
            hovered = hoveredAt(mouseX, mouseY);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(TAB);
            drawModalRectWithCustomSizedTexture(x, y, 0, 0, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH, TAB_HEIGHT);
            // The tab body is x 6 to 31; centre a 16x16 item in it.
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(getBook().getBookItem(), x + 11, y + 6);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            EntryDisplayState state = readState();
            if (state != null && state.hasIcon && state.showInInventory) {
                GuiBook.drawMarking(getBook(), x + 9, y + 4, 0, state);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
