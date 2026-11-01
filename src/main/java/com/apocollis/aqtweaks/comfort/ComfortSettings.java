package com.apocollis.aqtweaks.comfort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gson DTO for {@code aqtweaks_comfort_settings.json}.
 */
public class ComfortSettings {
    public float pet_comfort_value = 3.0f;
    public float threshold_homestead_1 = 24.0f;
    public float threshold_homestead_2 = 48.0f;
    public float threshold_homestead_3 = 66.0f;
    public long promote_ticks = 1200;
    public long promote_ticks_2 = 1200;
    public long promote_ticks_3 = 1200;
    public long damage_cooldown_ticks = 600;
    public long attack_cooldown_ticks = 300;
    public int scan_radius = 12;
    public int scan_dy = 2;
    public float repeat_decay = 0.6f;
    public Map<String, Float> repeat_decay_overrides = defaultDecayOverrides();
    public Integer band_min_categories = null;
    public List<String> band_require_2 = new ArrayList<>();
    public List<String> band_require_3 = new ArrayList<>();
    public String structure_decay_key = "role_family";
    public float non_shell_weight = 1.0f;
    public float unlisted_structure_weight = 0.25f;
    public List<String> natural_blocks = defaultNaturalBlocks();
    public Map<String, String> families = new LinkedHashMap<>();
    public int vigor_base_interval = 30;
    public RestBonus rest_bonus = new RestBonus();
    public ClarityConfig clarity = new ClarityConfig();
    public RestedConfig rested = new RestedConfig();
    public List<String> entry_require_categories = defaultEntryCategories();
    public PenaltiesConfig penalties = PenaltiesConfig.defaults();
    public BonusesConfig bonuses = BonusesConfig.defaults();

    public static List<String> defaultEntryCategories() {
        List<String> list = new ArrayList<>();
        list.add("hearth");
        list.add("bedding");
        list.add("seating");
        return list;
    }

    public static PotionModifier modifier(String potion, float amount) {
        PotionModifier m = new PotionModifier();
        m.potion = potion;
        m.amount = amount;
        return m;
    }

    public static List<PotionModifier> defaultBonusEffects() {
        List<PotionModifier> list = new ArrayList<>();
        list.add(modifier("farmersdelight:comfort", 10.0f));
        return list;
    }

    public static List<PotionModifier> defaultPenaltyEffects() {
        List<PotionModifier> list = new ArrayList<>();
        list.add(modifier("somnia:sleepy", 10.0f));
        list.add(modifier("somnia:exhausted", 25.0f));
        list.add(modifier("somnia:fading", 40.0f));
        return list;
    }

    public static class PenaltiesConfig {
        public boolean enabled = true;
        public TemperaturePenalty temperature = TemperaturePenalty.defaults();
        public RatePenalty thirst = RatePenalty.thirstDefaults();
        public RatePenalty hunger = RatePenalty.hungerDefaults();
        public HealthPenalty health = HealthPenalty.defaults();
        public List<PotionModifier> effects;
        @Deprecated
        public SomniaPenalty somnia;

        public static PenaltiesConfig defaults() {
            PenaltiesConfig p = new PenaltiesConfig();
            p.effects = defaultPenaltyEffects();
            return p;
        }
    }

    public static class TemperaturePenalty {
        public boolean enabled = true;
        public int comfort_min = 11;
        public int comfort_max = 14;
        public float per_point_outside = 1.5f;
        public String[] heat_ignore_potions = new String[] {
            "simpledifficulty:heat_protection",
            "simpledifficulty:heat_resist"
        };
        public String[] cold_ignore_potions = new String[] {
            "simpledifficulty:cold_protection",
            "simpledifficulty:cold_resist"
        };

        public static TemperaturePenalty defaults() {
            return new TemperaturePenalty();
        }
    }

    public static class RatePenalty {
        public boolean enabled = true;
        public float per_missing_point = 0.5f;

        public static RatePenalty thirstDefaults() {
            RatePenalty p = new RatePenalty();
            p.per_missing_point = 0.75f;
            return p;
        }

        public static RatePenalty hungerDefaults() {
            RatePenalty p = new RatePenalty();
            p.per_missing_point = 0.5f;
            return p;
        }
    }

    public static class HealthPenalty {
        public boolean enabled = true;
        public float per_missing_fraction = 15.0f;

        public static HealthPenalty defaults() {
            return new HealthPenalty();
        }
    }

    public static class SomniaPenalty {
        public boolean enabled = true;
        public String sleepy_potion = "somnia:sleepy";
        public float sleepy = 10.0f;
        public String exhausted_potion = "somnia:exhausted";
        public float exhausted = 25.0f;
        public String fading_potion = "somnia:fading";
        public float fading = 40.0f;
    }

    public static class BonusesConfig {
        public boolean enabled = true;
        public List<PotionModifier> effects;
        @Deprecated
        public PotionModifier farmers_delight_comfort;

        public static BonusesConfig defaults() {
            BonusesConfig b = new BonusesConfig();
            b.effects = defaultBonusEffects();
            return b;
        }
    }

    public static class PotionModifier {
        public boolean enabled = true;
        public String potion = "";
        public float amount = 0.0f;
    }

    public static Map<String, Float> defaultDecayOverrides() {
        Map<String, Float> map = new LinkedHashMap<>();
        map.put("structure", 0.5f);
        map.put("pets", 1.0f);
        return map;
    }

    public static List<String> defaultNaturalBlocks() {
        List<String> list = new ArrayList<>();
        list.add("minecraft:stone");
        list.add("minecraft:dirt");
        list.add("minecraft:grass");
        list.add("minecraft:gravel");
        list.add("minecraft:sand");
        list.add("minecraft:leaves");
        list.add("minecraft:leaves2");
        return list;
    }

    /** Sitting or lying score. Entry still uses the looser rest pose. */
    public static class RestBonus {
        public boolean enabled = true;
        public float sitting = 3.0f;
        public float lying = 5.0f;
        public int grace_ticks = 600;
        public List<String> ignore_vehicles = new ArrayList<>();
    }

    /** Visible at-home warp indicator. Rates stay on the 600-tick scan. */
    public static class ClarityConfig {
        public boolean require_temp_warp = false;
        public int duration_ticks = 900;
    }

    /**
     * Native Rested bonuses. A {@link TierPotion} keeps that potion's old refresh rules.
     * Health is the potion attribute (2 half-hearts times the amplifier plus one).
     */
    public static class RestedConfig {
        public int duration_1 = 6000;
        public int duration_2 = 12000;
        public int duration_3 = 18000;
        public float insight_1 = 0.10f;
        public float insight_2 = 0.20f;
        public float insight_3 = 0.30f;
        public int vigor_1 = 25;
        public int vigor_2 = 20;
        public int vigor_3 = 15;
        public int stamina_1 = 2;
        public int stamina_2 = 4;
        public int stamina_3 = 6;
        public List<TierPotion> potions_1 = new ArrayList<>();
        public List<TierPotion> potions_2 = new ArrayList<>();
        public List<TierPotion> potions_3 = new ArrayList<>();
    }

    public static class TierPotion {
        public String potion = "";
        public int duration = 4800;
        public int amplifier = 0;
        /** Learning is removed on granted II and III. Other entries count down when rest ends. */
        public boolean learning = false;
    }
}
