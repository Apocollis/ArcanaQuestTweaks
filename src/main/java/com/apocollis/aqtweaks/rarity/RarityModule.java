package com.apocollis.aqtweaks.rarity;

import java.util.Locale;
import java.util.Map;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.RarityGeneral;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.RarityModuleConfig;

import net.minecraft.util.text.TextFormatting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Rarity overhaul entry point: reads the config into an immutable snapshot and applies the tier colors. */
public final class RarityModule {
    private static final Logger LOG = LogManager.getLogger("AQTweaks-Rarity");

    private RarityModule() {}

    public enum SubtitleVisibility { ALL, ABOVE_COMMON, HIGH_ONLY }

    /** Immutable parsed config. A new instance replaces the old one on reload. */
    public static final class State {
        public final boolean enabled;
        public final char[] colors = new char[RarityTier.values().length];
        public final boolean animatedRainbow;
        public final boolean enchantMapping;
        public final Map<RarityEnchantTier.EnchRank, RarityTier> enchantMap;
        public final boolean qualityMapping;
        public final Map<String, RarityTier> qualityMap;
        public final RarityOverrideParser.Table overrides;
        public final boolean subtitle;
        public final SubtitleVisibility subtitleVisibility;
        public final boolean subtitleIcons;

        State(boolean enabled, boolean animatedRainbow, boolean enchantMapping,
                Map<RarityEnchantTier.EnchRank, RarityTier> enchantMap, boolean qualityMapping,
                Map<String, RarityTier> qualityMap, RarityOverrideParser.Table overrides, boolean subtitle,
                SubtitleVisibility vis, boolean icons) {
            this.enabled = enabled;
            this.animatedRainbow = animatedRainbow;
            this.enchantMapping = enchantMapping;
            this.enchantMap = enchantMap;
            this.qualityMapping = qualityMapping;
            this.qualityMap = qualityMap;
            this.overrides = overrides;
            this.subtitle = subtitle;
            this.subtitleVisibility = vis;
            this.subtitleIcons = icons;
        }

        public char colorOf(RarityTier t) {
            return colors[t.ordinal()];
        }
    }

    private static volatile State state;

    public static State state() {
        State s = state;
        if (s == null) {
            s = build();
            state = s;
        }
        return s;
    }

    /** Called once from CommonProxy.preInit on both sides. */
    public static void init() {
        State s = build();
        RarityRegistry.addTiers(formatting(s.colorOf(RarityTier.LEGENDARY)));
        state = s;
        applyColors(s);
        RarityOverrides.invalidate();
    }

    /** Called when the config changes in game. */
    public static void reload() {
        State s = build();
        state = s;
        applyColors(s);
        RarityOverrides.invalidate();
    }

    private static void applyColors(State s) {
        if (!s.enabled) return;
        RarityRegistry.recolor(RarityTier.UNCOMMON, formatting(s.colorOf(RarityTier.UNCOMMON)));
        RarityRegistry.recolor(RarityTier.RARE, formatting(s.colorOf(RarityTier.RARE)));
        RarityRegistry.recolor(RarityTier.EPIC, formatting(s.colorOf(RarityTier.EPIC)));
        RarityRegistry.recolor(RarityTier.LEGENDARY, formatting(s.colorOf(RarityTier.LEGENDARY)));
        RarityRegistry.recolor(RarityTier.MYTHIC, formatting(s.colorOf(RarityTier.LEGENDARY)));
    }

    private static State build() {
        RarityGeneral g = RarityModuleConfig.general;
        RarityEnchantTier.Parsed enchants = RarityEnchantTier.parse(g.enchantmentRarityMap);
        for (String w : enchants.warnings) LOG.warn("Enchantment Rarity Map: {}", w);
        RarityQualityTier.Parsed quality = RarityQualityTier.parse(g.qualityColorMap);
        for (String w : quality.warnings) LOG.warn("Quality Color Map: {}", w);
        RarityOverrideParser.Result overrides = RarityOverrideParser.parse(g.customItemRarities);
        for (String w : overrides.warnings) LOG.warn("Custom Item Rarities: {}", w);

        boolean animated = !"STATIC_RAINBOW".equals(g.mythicStyle == null ? "" : g.mythicStyle.trim().toUpperCase(Locale.ROOT));
        if (g.mythicStyle != null && !g.mythicStyle.trim().isEmpty()) {
            String m = g.mythicStyle.trim().toUpperCase(Locale.ROOT);
            if (!m.equals("ANIMATED_RAINBOW") && !m.equals("STATIC_RAINBOW")) {
                LOG.warn("Mythic Style \"{}\" is not ANIMATED_RAINBOW or STATIC_RAINBOW; using ANIMATED_RAINBOW.", g.mythicStyle);
            }
        }

        SubtitleVisibility vis = SubtitleVisibility.ALL;
        String rawVis = g.subtitleVisibility == null ? "" : g.subtitleVisibility.trim().toUpperCase(Locale.ROOT);
        try {
            if (!rawVis.isEmpty()) vis = SubtitleVisibility.valueOf(rawVis);
        } catch (IllegalArgumentException e) {
            LOG.warn("Subtitle Visibility \"{}\" is not ALL, ABOVE_COMMON or HIGH_ONLY; using ALL.", g.subtitleVisibility);
        }
        String rawStyle = g.subtitleStyle == null ? "" : g.subtitleStyle.trim().toUpperCase(Locale.ROOT);
        boolean icons = !rawStyle.equals("CLEAN");
        if (!rawStyle.isEmpty() && !rawStyle.equals("CLEAN") && !rawStyle.equals("ICON")) {
            LOG.warn("Subtitle Style \"{}\" is not ICON or CLEAN; using ICON.", g.subtitleStyle);
        }

        State s = new State(g.enable, animated, g.enchantmentRarityMapping, enchants.map, g.qualityRarityMapping,
                quality.map, overrides.table, g.showRaritySubtitle, vis, icons);
        s.colors[RarityTier.COMMON.ordinal()] = 'f';
        s.colors[RarityTier.UNCOMMON.ordinal()] = color("Color Uncommon", g.colorUncommon, 'a');
        s.colors[RarityTier.RARE.ordinal()] = color("Color Rare", g.colorRare, '3');
        s.colors[RarityTier.EPIC.ordinal()] = color("Color Epic", g.colorEpic, 'd');
        char legendary = color("Color Legendary", g.colorLegendary, '6');
        s.colors[RarityTier.LEGENDARY.ordinal()] = legendary;
        s.colors[RarityTier.MYTHIC.ordinal()] = legendary;
        return s;
    }

    private static char color(String key, String raw, char fallback) {
        if (RarityColors.isInvalid(raw)) {
            LOG.warn("{} \"{}\" is not a color code (one of 0-9, a-f); using '{}'.", key, raw, fallback);
            return fallback;
        }
        return RarityColors.parse(raw);
    }

    static TextFormatting formatting(char code) {
        for (TextFormatting f : TextFormatting.values()) {
            String s = f.toString();
            if (s.length() == 2 && s.charAt(1) == code) return f;
        }
        return TextFormatting.WHITE;
    }
}
