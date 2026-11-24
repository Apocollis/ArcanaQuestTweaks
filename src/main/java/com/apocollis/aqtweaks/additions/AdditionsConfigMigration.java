package com.apocollis.aqtweaks.additions;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Property;

import java.io.File;

public final class AdditionsConfigMigration {
    private AdditionsConfigMigration() {
    }

    public static void migrateLegacyWeaponConfig(File modConfigDir) {
        if (modConfigDir == null) {
            return;
        }
        File dir = new File(modConfigDir, "arcanaquesttweaks");
        File legacyFile = new File(dir, "aqtweaks_weapon.cfg");
        if (!legacyFile.isFile()) {
            return;
        }
        try {
            Configuration legacy = new Configuration(legacyFile);
            legacy.load();
            if (legacy.hasCategory("general")) {
                ConfigCategory cat = legacy.getCategory("general");
                ArcanaQuestTweaksConfig.InfinityBladeGeneral dst =
                        ArcanaQuestTweaksConfig.AdditionsModuleConfig.infinityBlade;

                Property dmg = cat.get("Attack Damage");
                if (dmg != null) {
                    dst.attackDamage = dmg.getDouble(dst.attackDamage);
                }
                Property spd = cat.get("Attack Speed");
                if (spd != null) {
                    dst.attackSpeed = spd.getDouble(dst.attackSpeed);
                }
                Property dur = cat.get("Durability");
                if (dur != null) {
                    dst.durability = dur.getInt(dst.durability);
                }
                Property rep = cat.get("Repair Item");
                if (rep != null) {
                    dst.repairItem = rep.getString();
                }
                Property rec = cat.get("Enable Default Recipe");
                if (rec != null) {
                    dst.enableDefaultRecipe = rec.getBoolean(dst.enableDefaultRecipe);
                }
                ConfigManager.sync(ArcanaQuestTweaks.MODID, Config.Type.INSTANCE);
            }
            // Remove the retired file once migrated so only aqtweaks_additions.cfg remains.
            //noinspection ResultOfMethodCallIgnored
            legacyFile.delete();
        } catch (Exception ignored) {
        }
    }
}
