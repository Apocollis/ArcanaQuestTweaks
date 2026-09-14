package com.apocollis.aqtweaks.comfort;

import java.util.ArrayList;
import java.util.List;

/**
 * Gson DTO for {@code aqtweaks_comfort_settings.json}.
 */
public class ComfortSettings {
    public float pet_comfort_value = 3.0f;
    public float threshold_homestead_1 = 15.0f;
    public float threshold_homestead_2 = 40.0f;
    public float threshold_homestead_3 = 60.0f;
    public long promote_ticks = 1200;
    public long damage_cooldown_ticks = 600;
    public long attack_cooldown_ticks = 300;
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
}
