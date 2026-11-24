package com.apocollis.aqtweaks.mixin.reskillable;

import codersafterdark.reskillable.api.data.PlayerData;
import codersafterdark.reskillable.api.data.PlayerSkillInfo;
import codersafterdark.reskillable.api.unlockable.Unlockable;
import codersafterdark.reskillable.client.base.RenderHelper;
import codersafterdark.reskillable.client.gui.GuiSkillInfo;
import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.network.PacketTogglePerk;
import com.apocollis.aqtweaks.reskillable.PerkToggleState;
import com.apocollis.aqtweaks.reskillable.ToggleablePerks;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Perk toggling in Reskillable's skill detail screen (plan 20261121-1542). {@code remap = false}: the class is a
 * Reskillable class, and the vanilla override {@code mouseClicked} keeps its SRG name {@code func_73864_a} in that jar.
 * A plain left-click on an unlocked toggleable perk sends the toggle (it does nothing in Reskillable today); a
 * disabled perk gets a dark overlay with "OFF" on its tile and a status line in its tooltip.
 */
@Mixin(value = GuiSkillInfo.class, remap = false)
public abstract class MixinGuiSkillInfo {

    @Shadow
    private Unlockable hoveredUnlockable;

    @Shadow
    private boolean isUnlocked;

    /** The unlockable {@code drawUnlockable} is drawing right now (read by the icon redirect). */
    @Unique
    private Unlockable aqtweaks$drawing;

    @Inject(method = "func_73864_a", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$toggleClick(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (mouseButton != 0 || GuiScreen.isShiftKeyDown() || this.hoveredUnlockable == null || !this.isUnlocked) return;
        String id = aqtweaks$id(this.hoveredUnlockable);
        if (id == null || !ToggleablePerks.isToggleable(id)) return;
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        ArcanaQuestTweaks.NETWORK.sendToServer(new PacketTogglePerk(id));
        ci.cancel();
    }

    @Inject(method = "drawUnlockable", at = @At("HEAD"))
    private void aqtweaks$markDrawing(PlayerData data, PlayerSkillInfo info, Unlockable unlockable, int mouseX, int mouseY,
            CallbackInfo ci) {
        this.aqtweaks$drawing = unlockable;
    }

    @Redirect(method = "drawUnlockable",
            at = @At(value = "INVOKE", target = "Lcodersafterdark/reskillable/client/gui/GuiSkillInfo;func_146110_a(IIFFIIFF)V"))
    private void aqtweaks$drawIcon(int x, int y, float u, float v, int w, int h, float tw, float th) {
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, w, h, tw, th);
        Unlockable drawing = this.aqtweaks$drawing;
        String id = drawing == null ? null : aqtweaks$id(drawing);
        if (id == null || !ToggleablePerks.isToggleable(id) || !PerkToggleState.isClientDisabled(id)) return;
        Gui.drawRect(x, y, x + w, y + h, 0xB0000000);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 2.0F, y + 5.0F, 0.0F);
        GlStateManager.scale(0.75F, 0.75F, 1.0F);
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow("OFF", 0, 0, 0xFF5555);
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Redirect(method = "makeUnlockableTooltip",
            at = @At(value = "INVOKE", target = "Lcodersafterdark/reskillable/client/base/RenderHelper;renderTooltip(IILjava/util/List;)V"))
    private void aqtweaks$tooltip(int x, int y, List<String> tip) {
        String id = this.hoveredUnlockable == null ? null : aqtweaks$id(this.hoveredUnlockable);
        if (id != null && this.isUnlocked && ToggleablePerks.isToggleable(id)) {
            boolean off = PerkToggleState.isClientDisabled(id);
            tip.add(off ? TextFormatting.RED + I18n.format("gui.aqtweaks.reskillable.perk_disabled")
                    : TextFormatting.GREEN + I18n.format("gui.aqtweaks.reskillable.perk_enabled"));
            tip.add(TextFormatting.GRAY + I18n.format("gui.aqtweaks.reskillable.click_to_toggle"));
        }
        RenderHelper.renderTooltip(x, y, tip);
    }

    @Unique
    private static String aqtweaks$id(Unlockable unlockable) {
        ResourceLocation key = unlockable.getRegistryName();
        return key == null ? null : key.toString();
    }
}
