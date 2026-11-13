package com.apocollis.aqtweaks.rarity.client;

import java.util.List;
import java.util.Locale;

import com.apocollis.aqtweaks.rarity.RarityColors;
import com.apocollis.aqtweaks.rarity.RarityModule;
import com.apocollis.aqtweaks.rarity.RarityRainbow;
import com.apocollis.aqtweaks.rarity.RarityRegistry;
import com.apocollis.aqtweaks.rarity.RarityTier;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Mythic rainbow on tooltip line 0 and the optional rarity subtitle on line 1. Client only. */
public class RarityTooltipHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltip(ItemTooltipEvent event) {
        RarityModule.State s = RarityModule.state();
        if (!s.enabled) return;
        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty()) return;
        List<String> tip = event.getToolTip();
        if (tip == null || tip.isEmpty()) return;
        RarityTier tier = RarityRegistry.tierOf(stack.getRarity());
        if (tier == null) return;

        if (tier == RarityTier.MYTHIC) {
            String line0 = tip.get(0);
            String plain = TextFormatting.getTextWithoutFormattingCodes(line0);
            if (plain != null && !plain.isEmpty()) {
                String italic = line0.startsWith(TextFormatting.ITALIC.toString()) ? TextFormatting.ITALIC.toString() : "";
                tip.set(0, italic + RarityRainbow.apply(plain, System.currentTimeMillis(), s.animatedRainbow));
            }
        }

        if (s.subtitle && visible(tier, s.subtitleVisibility)) {
            String key = tier.name().toLowerCase(Locale.ROOT);
            StringBuilder line = new StringBuilder();
            line.append(RarityColors.SECTION).append(s.colorOf(tier));
            if (s.subtitleIcons) line.append(I18n.format("aqtweaks.rarity.icon." + key)).append(' ');
            line.append(I18n.format("aqtweaks.rarity.tier." + key));
            tip.add(Math.min(1, tip.size()), line.toString());
        }
    }

    private static boolean visible(RarityTier tier, RarityModule.SubtitleVisibility v) {
        switch (v) {
            case ABOVE_COMMON:
                return tier.ordinal() > RarityTier.COMMON.ordinal();
            case HIGH_ONLY:
                return tier.ordinal() >= RarityTier.LEGENDARY.ordinal();
            default:
                return true;
        }
    }
}
