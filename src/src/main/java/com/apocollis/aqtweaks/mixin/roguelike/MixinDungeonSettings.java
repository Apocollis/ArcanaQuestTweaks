package com.apocollis.aqtweaks.mixin.roguelike;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import greymerk.roguelike.dungeon.settings.DungeonSettings;
import greymerk.roguelike.dungeon.settings.LevelSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = DungeonSettings.class, remap = false)
public abstract class MixinDungeonSettings {

    @Shadow
    private Map<Integer, LevelSettings> levels;

    /**
     * Controls level count for Roguelike Dungeons.
     * When force10LevelsForTesting is true, forces 10 levels for testing.
     * When false, dynamically returns the exact number of levels defined in the dungeon's JSON settings.
     */
    @Inject(method = "getNumLevels", at = @At("HEAD"), cancellable = true)
    private void getNumLevelsDepths(CallbackInfoReturnable<Integer> cir) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule) {
            if (ArcanaQuestTweaksConfig.depthsModule.force10LevelsForTesting) {
                cir.setReturnValue(10);
            } else if (this.levels != null && !this.levels.isEmpty()) {
                int maxLevel = 0;
                for (Integer lvl : this.levels.keySet()) {
                    if (lvl != null && lvl + 1 > maxLevel) {
                        maxLevel = lvl + 1;
                    }
                }
                if (maxLevel > 0) {
                    cir.setReturnValue(maxLevel);
                }
            }
        }
    }

    /**
     * Intercepts getLevelSettings() (0-arg map getter).
     * Ensures any level index queried by DungeonSettingsParser (.get(i)) is auto-populated
     * on-the-fly so that setNumRooms / setScatter in multi-level JSON files never receives null.
     */
    @Inject(method = "getLevelSettings()Ljava/util/Map;", at = @At("RETURN"))
    private void populateAllLevelsInMap(CallbackInfoReturnable<Map<Integer, LevelSettings>> cir) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule) {
            Map<Integer, LevelSettings> map = cir.getReturnValue();
            if (map != null) {
                for (int level = 0; level < 10; level++) {
                    if (!map.containsKey(level)) {
                        LevelSettings fallbackBase = null;
                        for (int i = level - 1; i >= 0; i--) {
                            if (map.containsKey(i)) {
                                fallbackBase = map.get(i);
                                break;
                            }
                        }
                        LevelSettings newLevel = (fallbackBase != null)
                                ? new LevelSettings(fallbackBase).withLevel(level)
                                : new LevelSettings(level);
                        map.put(level, newLevel);
                    }
                }
            }
        }
    }

    /**
     * Provides fallback level settings for levels when requested directly by index.
     */
    @Inject(method = "getLevelSettings(I)Lgreymerk/roguelike/dungeon/settings/LevelSettings;", at = @At("HEAD"), cancellable = true)
    private void getLevelSettingsFallback(int level, CallbackInfoReturnable<LevelSettings> cir) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule && level >= 0) {
            Map<Integer, LevelSettings> levelsMap = this.levels;
            if (levelsMap != null) {
                if (levelsMap.containsKey(level)) {
                    cir.setReturnValue(levelsMap.get(level));
                } else {
                    LevelSettings fallbackBase = null;
                    for (int i = level - 1; i >= 0; i--) {
                        if (levelsMap.containsKey(i)) {
                            fallbackBase = levelsMap.get(i);
                            break;
                        }
                    }
                    LevelSettings newLevel = (fallbackBase != null)
                            ? new LevelSettings(fallbackBase).withLevel(level)
                            : new LevelSettings(level);
                    levelsMap.put(level, newLevel);
                    cir.setReturnValue(newLevel);
                }
            }
        }
    }
}
