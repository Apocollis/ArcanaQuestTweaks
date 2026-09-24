package com.apocollis.aqtweaks.reskillable.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.PerkAccess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.math.BlockPos;

/** Lightmap mix for Dark Vision. 0 leaves vanilla light unchanged. */
public final class DarkVisionLight {

    private static final float MAX = 0.8f;
    private static float mix;
    private static long lastTick = Long.MIN_VALUE;

    private DarkVisionLight() {}

    public static float factor(EntityPlayer player) {
        if (player == null || player.world == null) return 0.0f;
        if (player.isPotionActive(MobEffects.NIGHT_VISION)) {
            mix = 0.0f;
            return 0.0f;
        }
        float target = 0.0f;
        if (PerkAccess.on(player, "aqtweaks:dark_vision",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.darkVision.enable)) {
            BlockPos feet = new BlockPos(player.posX, player.posY, player.posZ);
            BlockPos eyes = new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ);
            int light = Math.max(player.world.getLight(feet), player.world.getLight(eyes));
            if (light < 9) {
                target = (108 - 3 * light - light * light) / 135.0f;
            }
        }
        long tick = player.world.getTotalWorldTime();
        if (tick != lastTick) {
            lastTick = tick;
            float step = MAX / 5.0f;
            float delta = target - mix;
            if (Math.abs(delta) <= step) mix = target;
            else mix += Math.copySign(step, delta);
        }
        return mix;
    }
}
