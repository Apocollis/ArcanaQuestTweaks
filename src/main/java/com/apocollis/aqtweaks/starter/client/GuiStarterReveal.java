package com.apocollis.aqtweaks.starter.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.network.PacketStarterRevealDone;
import com.apocollis.aqtweaks.starter.StarterResult;
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
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The "what you gained" panel (starter class plan 12.10): class title, the levels granted, the class perk and the
 * gear. It fades in (cross-fading from the choice screen when that was open), holds until Continue or the hold
 * time, fades out, then tells the server with {@link PacketStarterRevealDone}. Informational only.
 */
public class GuiStarterReveal extends GuiScreen {

    private static final int FADE_TICKS = 10;
    /** Default hold (12 s). The server owns the real timeout and releases the player either way. */
    private static final int HOLD_TICKS = 240;
    private static final int PAD = 14;
    private static final int BAR_H = 30;
    private static final int BUTTON_W = 90;
    private static final int BUTTON_H = 20;
    private static final int SLOT = 20;
    private static final int SLOTS_PER_ROW = 6;

    private final StarterResult result;
    private final GuiStarterClassSelection previous;
    private final RevealTimeline timeline = new RevealTimeline(FADE_TICKS, HOLD_TICKS);
    private final List<ItemStack> gear;

    private final List<String> levelLines = new ArrayList<>();
    private String perkName = "";
    private List<String> perkDesc = new ArrayList<>();
    private boolean hasPerk;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int viewX;
    private int viewY;
    private int viewW;
    private int viewH;
    private int contentH;
    private int scroll;
    private int captionY;
    private int titleY;
    private int levelsHeaderY;
    private int levelsY;
    private int perkHeaderY;
    private int perkNameY;
    private int perkDescY;
    private int gearHeaderY;
    private int gearY;
    private int dividerA;
    private int dividerB;
    private int dividerC;
    private boolean doneSent;

    public GuiStarterReveal(StarterResult result, GuiStarterClassSelection previous) {
        this.result = result;
        this.previous = previous;
        this.gear = StarterClientMath.capGear(result == null ? null : result.gear, StarterClientMath.GEAR_CAP);
    }

    @Override
    public void initGui() {
        super.initGui();
        if (previous != null) {
            previous.setWorldAndResolution(mc, width, height);
        }
        layout();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ------------------------------------------------------------------------------------------ layout

    private void layout() {
        panelW = StarterClientMath.panelSize(width, 320, 8, 140);
        panelH = StarterClientMath.panelSize(height, 290, 8, 120);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        viewX = panelX + PAD;
        viewY = panelY + PAD;
        viewW = Math.max(60, panelW - 2 * PAD - 8);
        viewH = Math.max(24, panelH - 2 * PAD - BAR_H - 4);

        FontRenderer fr = fontRenderer;
        int lh = fr.FONT_HEIGHT + 2;

        levelLines.clear();
        if (result != null && result.skills != null) {
            for (StarterResult.SkillGain g : result.skills) {
                String name = g.skillName == null || g.skillName.isEmpty() ? g.skillId : g.skillName;
                levelLines.add(name + " " + StarterClientMath.roman(g.level));
            }
        }
        String key = result == null ? null : result.perkKey;
        hasPerk = key != null && !key.isEmpty();
        if (hasPerk) {
            perkName = I18n.format("aqtweaks.perk." + key + ".name");
            perkDesc = fr.listFormattedStringToWidth(I18n.format("aqtweaks.perk." + key + ".desc"), viewW);
        }

        int y = 0;
        captionY = y;
        y += lh;
        titleY = y;
        y += 20;
        dividerA = y;
        y += 4 + 6;
        levelsHeaderY = y;
        y += lh + 2;
        levelsY = y;
        y += Math.max(1, levelLines.size()) * lh + 6;
        if (hasPerk) {
            dividerB = y;
            y += 4 + 6;
            perkHeaderY = y;
            y += lh + 2;
            perkNameY = y;
            y += lh;
            perkDescY = y;
            y += perkDesc.size() * lh + 6;
        }
        if (!gear.isEmpty()) {
            dividerC = y;
            y += 4 + 6;
            gearHeaderY = y;
            y += lh + 2;
            gearY = y;
            int gearRows = (gear.size() + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;
            y += gearRows * (SLOT + 2) + 4;
        }
        contentH = y;
        scroll = StarterClientMath.clampScroll(scroll, contentH, viewH);
    }

    // ------------------------------------------------------------------------------------------ drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float a = timeline.alpha(partialTicks);
        Gui.drawRect(0, 0, width, height, StarterClientMath.withAlpha(0x000000, 0.62f));
        if (previous != null && timeline.stage() == RevealTimeline.Stage.FADE_IN) {
            previous.renderFaded(mouseX, mouseY, partialTicks, 1f - a, false);
        }
        StarterDraw.nineSlice(StarterDraw.PANEL, panelX, panelY, panelW, panelH, 64, 64, 8, a);

        FontRenderer fr = fontRenderer;
        int lh = fr.FONT_HEIGHT + 2;
        boolean text = StarterClientMath.visible(a);
        ItemStack hovered = null;

        StarterDraw.beginScissor(mc, viewX, viewY, viewW + 6, viewH);
        int top = viewY - scroll;
        if (text) {
            fr.drawString(I18n.format("aqtweaks.starter.reveal.title"), viewX, top + captionY, StarterClientMath.withAlpha(StarterDraw.TEXT_DIM, a));
        }
        if (text) {
            String title = result == null || result.className == null ? "" : result.className;
            GlStateManager.pushMatrix();
            GlStateManager.translate(viewX, top + titleY + 2, 0f);
            GlStateManager.scale(1.5f, 1.5f, 1f);
            fr.drawStringWithShadow(title, 0, 0, StarterClientMath.withAlpha(StarterDraw.GOLD, a));
            GlStateManager.popMatrix();
        }
        StarterDraw.divider(viewX, top + dividerA, viewW, a);
        if (text) {
            fr.drawStringWithShadow(I18n.format("aqtweaks.starter.reveal.levels"), viewX, top + levelsHeaderY,
                    StarterClientMath.withAlpha(StarterDraw.GOLD, a));
            for (int i = 0; i < levelLines.size(); i++) {
                fr.drawString(levelLines.get(i), viewX + 6, top + levelsY + i * lh, StarterClientMath.withAlpha(StarterDraw.TEXT, a));
            }
        }
        if (hasPerk) {
            StarterDraw.divider(viewX, top + dividerB, viewW, a);
            if (text) {
                fr.drawStringWithShadow(I18n.format("aqtweaks.starter.reveal.perk"), viewX, top + perkHeaderY,
                        StarterClientMath.withAlpha(StarterDraw.GOLD, a));
                fr.drawString(perkName, viewX + 6, top + perkNameY, StarterClientMath.withAlpha(0xFFFFFF, a));
                for (int i = 0; i < perkDesc.size(); i++) {
                    fr.drawString(perkDesc.get(i), viewX + 6, top + perkDescY + i * lh, StarterClientMath.withAlpha(StarterDraw.TEXT, a));
                }
            }
        }
        if (!gear.isEmpty()) {
            StarterDraw.divider(viewX, top + dividerC, viewW, a);
            if (text) {
                fr.drawStringWithShadow(I18n.format("aqtweaks.starter.reveal.gear"), viewX, top + gearHeaderY,
                        StarterClientMath.withAlpha(StarterDraw.GOLD, a));
            }
            for (int i = 0; i < gear.size(); i++) {
                int sx = viewX + 6 + (i % SLOTS_PER_ROW) * (SLOT + 2);
                int sy = top + gearY + (i / SLOTS_PER_ROW) * (SLOT + 2);
                StarterDraw.nineSlice(StarterDraw.OPTION, sx, sy, SLOT, SLOT, 32, 32, 6, a);
                if (a >= 0.5f) {
                    GlStateManager.enableRescaleNormal();
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(gear.get(i), sx + 2, sy + 2);
                    mc.getRenderItem().renderItemOverlays(fontRenderer, gear.get(i), sx + 2, sy + 2);
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.disableRescaleNormal();
                    GlStateManager.disableLighting();
                }
                if (mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + SLOT
                        && mouseY >= viewY && mouseY < viewY + viewH) {
                    hovered = gear.get(i);
                }
            }
        }
        StarterDraw.endScissor();
        GlStateManager.color(1f, 1f, 1f, 1f);

        if (contentH > viewH) {
            int trackX = viewX + viewW + 2;
            int thumbH = StarterClientMath.thumbHeight(contentH, viewH, viewH);
            int thumbY = viewY + StarterClientMath.thumbOffset(scroll, contentH, viewH, viewH);
            Gui.drawRect(trackX, viewY, trackX + 3, viewY + viewH, StarterClientMath.withAlpha(0x151310, a));
            Gui.drawRect(trackX, thumbY, trackX + 3, thumbY + thumbH, StarterClientMath.withAlpha(0xBD862B, a));
        }

        int bx = panelX + (panelW - BUTTON_W) / 2;
        int by = panelY + panelH - PAD - BUTTON_H;
        boolean enabled = timeline.stage() != RevealTimeline.Stage.FADE_OUT;
        StarterDraw.button(bx, by, BUTTON_W, BUTTON_H, enabled, enabled && inButton(mouseX, mouseY), a);
        if (text) {
            String label = I18n.format("aqtweaks.starter.continue");
            fr.drawStringWithShadow(label, bx + (BUTTON_W - fr.getStringWidth(label)) / 2, by + (BUTTON_H - fr.FONT_HEIGHT) / 2 + 1,
                    StarterClientMath.withAlpha(enabled ? StarterDraw.GOLD : StarterDraw.TEXT_DIM, a));
        }
        GlStateManager.color(1f, 1f, 1f, 1f);

        if (hovered != null && a >= 0.5f) {
            renderToolTip(hovered, mouseX, mouseY);
        }
    }

    private boolean inButton(int mx, int my) {
        int bx = panelX + (panelW - BUTTON_W) / 2;
        int by = panelY + panelH - PAD - BUTTON_H;
        return mx >= bx && mx < bx + BUTTON_W && my >= by && my < by + BUTTON_H;
    }

    // ------------------------------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && inButton(mouseX, mouseY) && timeline.stage() != RevealTimeline.Stage.FADE_OUT) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            timeline.continueClicked();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            timeline.continueClicked();
        } else if (keyCode == Keyboard.KEY_UP) {
            scrollBy(-18);
        } else if (keyCode == Keyboard.KEY_DOWN) {
            scrollBy(18);
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
        timeline.tick();
        if (timeline.done()) {
            finish();
        }
    }

    private void finish() {
        if (!doneSent) {
            doneSent = true;
            ArcanaQuestTweaks.NETWORK.sendToServer(new PacketStarterRevealDone());
        }
        if (mc.currentScreen == this) {
            mc.displayGuiScreen(null);
        }
    }
}
