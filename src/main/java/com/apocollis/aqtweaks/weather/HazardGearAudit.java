package com.apocollis.aqtweaks.weather;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Startup audit for all centralized hazard gear whitelists in {@code aqtweaks_hazard_gear.cfg}.
 * Logs one {@code [hazard-gear]} line per pattern that matches no registered item, catching typos at load time.
 */
public final class HazardGearAudit {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks");

    private HazardGearAudit() {}

    public static void run() {
        Set<ResourceLocation> keys = ForgeRegistries.ITEMS.getKeys();
        List<String> registered = new ArrayList<>(keys.size());
        for (ResourceLocation key : keys) {
            registered.add(key.toString());
        }

        auditList("[wind] Wind Resistant Gear Whitelist", ArcanaQuestTweaksConfig.HazardGearConfig.wind.windResistantGear, registered);
        auditList("[wind] Anchoring Boots Whitelist", ArcanaQuestTweaksConfig.HazardGearConfig.wind.anchoringBoots, registered);
        auditList("[eye_protection] Eye Protection Gear Whitelist", ArcanaQuestTweaksConfig.HazardGearConfig.eyeProtection.eyeProtectionGear, registered);
        auditList("[aether] Channeling Gear Whitelist", ArcanaQuestTweaksConfig.HazardGearConfig.aether.channelingGear, registered);
        auditList("[aether] Insulated Gear Whitelist", ArcanaQuestTweaksConfig.HazardGearConfig.aether.insulatedGear, registered);
        auditList("[aether] Conductive Gear List", ArcanaQuestTweaksConfig.HazardGearConfig.aether.conductiveGear, registered);
        auditList("[atum] Sandstorm Resistant Armor Whitelist", ArcanaQuestTweaksConfig.HazardGearConfig.atum.sandstormResistantArmorWhitelist, registered);
        auditList("[aurorian] Proper Gear Whitelist", ArcanaQuestTweaksConfig.HazardGearConfig.aurorian.properGearWhitelist, registered);
        auditList("[nether] Ash Proper Gear Whitelist", ArcanaQuestTweaksConfig.HazardGearConfig.nether.ashProperGearWhitelist, registered);
        auditList("[twilightforest] Proper Arcane Gear Whitelist", ArcanaQuestTweaksConfig.HazardGearConfig.twilightforest.properGear, registered);
    }

    private static void auditList(String listName, String[] patterns, List<String> registered) {
        if (patterns == null || patterns.length == 0) return;
        for (String raw : patterns) {
            if (raw == null) continue;
            String pattern = raw.trim();
            if (pattern.isEmpty()) continue;
            String base = Wildcard.basePattern(pattern);
            boolean matched = false;
            for (String itemId : registered) {
                if (Wildcard.matches(itemId, base)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                LOGGER.warn("[hazard-gear] Unmatched pattern in {}: {}", listName, pattern);
            }
        }
    }
}
