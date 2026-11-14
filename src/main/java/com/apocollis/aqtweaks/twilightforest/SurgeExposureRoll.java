package com.apocollis.aqtweaks.twilightforest;

import java.util.Random;

/**
 * Pure helper for the Twilight Forest Ley-Rupture continuous exposure updraft roll (no Minecraft types, unit tested).
 * After {@code delaySeconds} of uninterrupted outdoor surge exposure, rolls once across:
 * <ul>
 *     <li>{@link Outcome#NONE} (no extra effect)</li>
 *     <li>{@link Outcome#FLIGHT} (Flight + Slowfall buffer)</li>
 *     <li>{@link Outcome#LEVITATION} (Levitation + Slowfall buffer)</li>
 * </ul>
 */
public final class SurgeExposureRoll {

    public enum Outcome {
        NONE,
        FLIGHT,
        LEVITATION
    }

    private SurgeExposureRoll() {}

    public static boolean isDue(long exposedTicks, int delaySeconds) {
        return delaySeconds > 0 && exposedTicks >= delaySeconds * 20L;
    }

    public static Outcome roll(int weightNone, int weightFlight, int weightLevitation, Random rand) {
        int wn = Math.max(0, weightNone);
        int wf = Math.max(0, weightFlight);
        int wl = Math.max(0, weightLevitation);
        int total = wn + wf + wl;
        if (total <= 0) {
            return Outcome.NONE;
        }
        int pick = rand.nextInt(total);
        if (pick < wn) {
            return Outcome.NONE;
        }
        if (pick < wn + wf) {
            return Outcome.FLIGHT;
        }
        return Outcome.LEVITATION;
    }

    public static int durationTicks(int durationSeconds) {
        return Math.max(0, durationSeconds) * 20;
    }

    public static int slowFallTicks(int baseDurationSeconds, int extraSeconds) {
        int base = Math.max(0, baseDurationSeconds);
        int extra = Math.max(0, extraSeconds);
        return (base + extra) * 20;
    }

    public static boolean shouldApply(boolean hasEffect, int currentRemainingTicks, int targetDurationTicks) {
        if (targetDurationTicks <= 0) return false;
        return !hasEffect || currentRemainingTicks < targetDurationTicks;
    }
}
