package com.apocollis.aqtweaks.gamestages;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.Loader;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GameStagesChiselHooks {

    private static final int NOTIFY_COOLDOWN_TICKS = 40;
    private static final Map<UUID, Long> LAST_NOTIFY_TICK = new ConcurrentHashMap<>();

    private GameStagesChiselHooks() {
    }

    public static String lockedStage(EntityPlayer player, ItemStack output) {
        if (!ArcanaQuestTweaksConfig.GameStagesModuleConfig.general.enable) {
            return null;
        }
        if (player == null || player instanceof FakePlayer) {
            return null;
        }
        if (output == null || output.isEmpty()) {
            return null;
        }
        if (!Loader.isModLoaded("gamestages") || !Loader.isModLoaded("recipestages")) {
            return null;
        }
        return Lookup.lockedStage(player, output);
    }

    public static void notifyLocked(EntityPlayer player, String stage) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        if (stage == null || stage.isEmpty()) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        Long last = LAST_NOTIFY_TICK.get(player.getUniqueID());
        if (last != null && now - last < NOTIFY_COOLDOWN_TICKS) {
            return;
        }
        LAST_NOTIFY_TICK.put(player.getUniqueID(), now);
        player.sendStatusMessage(new TextComponentTranslation("chat.aqtweaks.gamestages.chisel_locked", displayName(stage))
                .setStyle(new Style().setColor(TextFormatting.RED)), true);
    }

    static String displayName(String stage) {
        String[] parts = stage.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.length() == 0 ? stage : sb.toString();
    }

    private static Set<String> allowlist() {
        String[] raw = ArcanaQuestTweaksConfig.GameStagesModuleConfig.general.chiselStageAllowlist;
        if (raw == null || raw.length == 0) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>();
        for (String stage : raw) {
            if (stage != null && !stage.isEmpty()) {
                set.add(stage);
            }
        }
        return set;
    }

    private static final class Lookup {
        private Lookup() {
        }

        static String lockedStage(EntityPlayer player, ItemStack output) {
            Set<String> allow = allowlist();
            for (String stage : GameStagesRecipeIndex.matchingStages(output)) {
                if (!allowlisted(stage, allow)) {
                    continue;
                }
                if (!net.darkhax.gamestages.GameStageHelper.hasStage(player, stage)) {
                    return stage;
                }
            }
            return null;
        }

        private static boolean allowlisted(String stage, Set<String> allow) {
            return allow == null || allow.isEmpty() || allow.contains(stage);
        }
    }
}
