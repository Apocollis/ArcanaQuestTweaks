package com.apocollis.aqtweaks.starter.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.network.PacketSelectStarterClass;
import com.apocollis.aqtweaks.starter.StarterClassView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The first-join story prompt (starter class plan 12.8 and 12.10): an intro, a question, and one memory per class.
 * It shows only the title, the memory text and an image. Gear, levels and the perk are revealed afterwards.
 * The whole column (intro, question and class rows) scrolls inside the panel; the confirm line and Select button
 * stay at the bottom. Escape never closes it: the server shows it again until a class is chosen.
 */
public class GuiStarterClassSelection extends GuiScreen {

    private static final int ICON_BOX = 40;
    private static final int ROW_GAP = 6;
    private static final int PAD = 14;
    private static final int BAR_H = 32;
    private static final int BUTTON_W = 84;
    private static final int BUTTON_H = 20;
    /** Ticks to wait for the server's answer after Select before the button works again. */
    private static final int RESEND_TICKS = 100;

    private static final class Row {
        StarterClassView view;
        List<String> flavor;
        ItemStack stack;
        ResourceLocation texture;
        int y;
        int h;
    }

    private final String intro;
    private final String prompt;
    private final List<StarterClassView> classes;
    private final List<Row> rows = new ArrayList<>();
    private List<String> introLines = new ArrayList<>();
    private List<String> promptLines = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int viewX;
    private int viewY;
    private int viewW;
    private int viewH;
    private int introY;
    private int dividerTopY;
    private int promptY;
    private int dividerBottomY;
    private int contentH;
    private int scroll;
    private int selected = -1;
    private boolean submitted;
    private int submittedAt;
    private int ticks;

    public GuiStarterClassSelection(String intro, String prompt, List<StarterClassView> classes) {
        this.intro = intro == null ? "" : intro;
        this.prompt = prompt == null ? "" : prompt;
        this.classes = classes == null ? new ArrayList<StarterClassView>() : classes;
    }

    @Override
    public void initGui() {
        super.initGui();
        layout();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ------------------------------------------------------------------------------------------ layout

    private void layout() {
        panelW = StarterClientMath.panelSize(width, 380, 8, 140);
        panelH = StarterClientMath.panelSize(height, 300, 8, 120);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        viewX = panelX + PAD;
        viewY = panelY + PAD;
        viewW = Math.max(60, panelW - 2 * PAD - 8);
        viewH = Math.max(24, panelH - 2 * PAD - BAR_H - 4);

        FontRenderer fr = fontRenderer;
        int lh = fr.FONT_HEIGHT + 2;
        introLines = fr.listFormattedStringToWidth(intro, viewW);
        promptLines = fr.listFormattedStringToWidth(prompt, viewW);
        int y = 0;
        introY = y;
        y += introLines.size() * lh + 6;
        dividerTopY = y;
        y += 4 + 6;
        promptY = y;
        y += promptLines.size() * lh + 6;
        dividerBottomY = y;
        y += 4 + 8;

        rows.clear();
        int textW = Math.max(40, viewW - ICON_BOX - 8 - 12);
        for (StarterClassView v : classes) {
            Row r = new Row();
            r.view = v;
            r.flavor = fr.listFormattedStringToWidth(v.flavor == null ? "" : v.flavor, textW);
            resolveIcon(r, v.icon);
            int textH = 8 + (fr.FONT_HEIGHT + 4) + r.flavor.size() * (fr.FONT_HEIGHT + 1) + 8;
            r.h = Math.max(ICON_BOX + 12, textH);
            r.y = y;
            y += r.h + ROW_GAP;
            rows.add(r);
        }
        contentH = y;
        scroll = StarterClientMath.clampScroll(scroll, contentH, viewH);
    }

    private static void resolveIcon(Row r, String icon) {
        if (icon == null || icon.isEmpty()) {
            return;
        }
        if (icon.startsWith("tex:")) {
            ResourceLocation rl = new ResourceLocation(icon.substring(4));
            r.texture = StarterDraw.emblem(rl.getNamespace(), rl.getPath());
            return;
        }
        Item item = Item.REGISTRY.getObject(new ResourceLocation(icon));
        if (item != null) {
            r.stack = new ItemStack(item);
        }
    }

    // ------------------------------------------------------------------------------------------ drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        renderFaded(mouseX, mouseY, partialTicks, 1f, true);
    }

    /** Draws the screen at the given alpha; the reveal screen cross-fades through this. */
    void renderFaded(int mouseX, int mouseY, float partialTicks, float alpha, boolean dim) {
        if (dim) {
            Gui.drawRect(0, 0, width, height, StarterClientMath.withAlpha(0x000000, 0.62f * alpha));
        }
        StarterDraw.nineSlice(StarterDraw.PANEL, panelX, panelY, panelW, panelH, 64, 64, 8, alpha);

        FontRenderer fr = fontRenderer;
        int lh = fr.FONT_HEIGHT + 2;
        boolean text = StarterClientMath.visible(alpha);
        int hoverRow = hoverRow(mouseX, mouseY);

        StarterDraw.beginScissor(mc, viewX, viewY, viewW + 6, viewH);
        int top = viewY - scroll;
        if (text) {
            for (int i = 0; i < introLines.size(); i++) {
                fr.drawString(introLines.get(i), viewX, top + introY + i * lh, StarterClientMath.withAlpha(StarterDraw.TEXT_INTRO, alpha));
            }
        }
        StarterDraw.divider(viewX, top + dividerTopY, viewW, alpha);
        if (text) {
            for (int i = 0; i < promptLines.size(); i++) {
                fr.drawStringWithShadow(promptLines.get(i), viewX, top + promptY + i * lh, StarterClientMath.withAlpha(StarterDraw.GOLD, alpha));
            }
        }
        StarterDraw.divider(viewX, top + dividerBottomY, viewW, alpha);

        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = top + r.y;
            if (ry + r.h < viewY || ry > viewY + viewH) {
                continue;
            }
            net.minecraft.util.ResourceLocation box = i == selected ? StarterDraw.OPTION_SELECTED
                    : i == hoverRow ? StarterDraw.OPTION_HOVER : StarterDraw.OPTION;
            StarterDraw.nineSlice(box, viewX, ry, viewW, r.h, 32, 32, 6, alpha);
            drawIcon(r, viewX + 6, ry + (r.h - 32) / 2, alpha);
            int tx = viewX + ICON_BOX + 8;
            if (text) {
                fr.drawStringWithShadow(r.view.name == null ? "" : r.view.name, tx, ry + 8,
                        StarterClientMath.withAlpha(StarterDraw.GOLD, alpha));
                int fy = ry + 8 + fr.FONT_HEIGHT + 4;
                for (int l = 0; l < r.flavor.size(); l++) {
                    fr.drawString(r.flavor.get(l), tx, fy + l * (fr.FONT_HEIGHT + 1), StarterClientMath.withAlpha(StarterDraw.TEXT, alpha));
                }
            }
        }
        StarterDraw.endScissor();
        GlStateManager.color(1f, 1f, 1f, 1f);

        drawScrollbar(alpha);
        drawBottomBar(mouseX, mouseY, alpha, text);
    }

    private void drawIcon(Row r, int x, int y, float alpha) {
        if (r.texture != null) {
            mc.getTextureManager().bindTexture(r.texture);
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, alpha);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 32, 32, 32, 32);
            GlStateManager.color(1f, 1f, 1f, 1f);
        } else if (r.stack != null && alpha >= 0.5f) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0f);
            GlStateManager.scale(2f, 2f, 1f);
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(r.stack, 0, 0);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
            GlStateManager.disableLighting();
        }
    }

    private void drawScrollbar(float alpha) {
        if (contentH <= viewH) {
            return;
        }
        int trackX = viewX + viewW + 2;
        int thumbH = StarterClientMath.thumbHeight(contentH, viewH, viewH);
        int thumbY = viewY + StarterClientMath.thumbOffset(scroll, contentH, viewH, viewH);
        Gui.drawRect(trackX, viewY, trackX + 3, viewY + viewH, StarterClientMath.withAlpha(0x151310, alpha));
        Gui.drawRect(trackX, thumbY, trackX + 3, thumbY + thumbH, StarterClientMath.withAlpha(0xBD862B, alpha));
    }

    private void drawBottomBar(int mouseX, int mouseY, float alpha, boolean text) {
        FontRenderer fr = fontRenderer;
        int barTop = panelY + panelH - PAD - BUTTON_H;
        int bx = buttonX();
        boolean enabled = selected >= 0 && !submitted;
        boolean hover = enabled && inButton(mouseX, mouseY);
        StarterDraw.button(bx, barTop, BUTTON_W, BUTTON_H, enabled, hover, alpha);
        if (text) {
            String label = I18n.format("aqtweaks.starter.select");
            int color = enabled ? StarterDraw.GOLD : StarterDraw.TEXT_DIM;
            fr.drawStringWithShadow(label, bx + (BUTTON_W - fr.getStringWidth(label)) / 2, barTop + (BUTTON_H - fr.FONT_HEIGHT) / 2 + 1,
                    StarterClientMath.withAlpha(color, alpha));
            if (selected >= 0) {
                String name = rows.get(selected).view.name == null ? "" : rows.get(selected).view.name;
                String line = I18n.format("aqtweaks.starter.confirm", name);
                List<String> lines = fr.listFormattedStringToWidth(line, Math.max(40, bx - (panelX + PAD) - 8));
                int y = barTop + (BUTTON_H - Math.min(2, lines.size()) * (fr.FONT_HEIGHT + 1)) / 2;
                for (int i = 0; i < Math.min(2, lines.size()); i++) {
                    fr.drawString(lines.get(i), panelX + PAD, y + i * (fr.FONT_HEIGHT + 1), StarterClientMath.withAlpha(StarterDraw.TEXT, alpha));
                }
            }
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private int buttonX() {
        return panelX + panelW - PAD - BUTTON_W;
    }

    private boolean inButton(int mx, int my) {
        int by = panelY + panelH - PAD - BUTTON_H;
        return mx >= buttonX() && mx < buttonX() + BUTTON_W && my >= by && my < by + BUTTON_H;
    }

    private boolean inView(int mx, int my) {
        return mx >= viewX && mx < viewX + viewW && my >= viewY && my < viewY + viewH;
    }

    private int hoverRow(int mx, int my) {
        if (!inView(mx, my)) {
            return -1;
        }
        int cy = my - viewY + scroll;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            if (cy >= r.y && cy < r.y + r.h) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0 || submitted) {
            return;
        }
        int row = hoverRow(mouseX, mouseY);
        if (row >= 0) {
            selected = row;
            click();
            return;
        }
        if (selected >= 0 && inButton(mouseX, mouseY)) {
            click();
            submit();
        }
    }

    private void click() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void submit() {
        if (selected < 0 || selected >= rows.size() || submitted) {
            return;
        }
        String id = rows.get(selected).view.id;
        if (id == null) {
            return;
        }
        submitted = true;
        submittedAt = ticks;
        ArcanaQuestTweaks.NETWORK.sendToServer(new PacketSelectStarterClass(id));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Escape is deliberately ignored: the server shows this screen again until a class is chosen.
        if (keyCode == Keyboard.KEY_UP) {
            scrollBy(-18);
        } else if (keyCode == Keyboard.KEY_DOWN) {
            scrollBy(18);
        } else if (keyCode == Keyboard.KEY_PRIOR) {
            scrollBy(-viewH + 12);
        } else if (keyCode == Keyboard.KEY_NEXT) {
            scrollBy(viewH - 12);
        } else if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && selected >= 0) {
            submit();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scrollBy(wheel > 0 ? -24 : 24);
        }
    }

    private void scrollBy(int delta) {
        scroll = StarterClientMath.clampScroll(scroll + delta, contentH, viewH);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        ticks++;
        if (submitted && ticks - submittedAt > RESEND_TICKS) {
            submitted = false;
        }
    }
}
