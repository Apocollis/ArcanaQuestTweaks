package com.apocollis.aqtweaks.additions;

public final class InfinityBladeStats {
    public static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0D;
    public static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;

    private InfinityBladeStats() {
    }

    public static double attackDamageModifier(double configuredTotalDamage) {
        double clamped = Math.max(PLAYER_BASE_ATTACK_DAMAGE, configuredTotalDamage);
        return clamped - PLAYER_BASE_ATTACK_DAMAGE;
    }

    public static double attackSpeedModifier(double configuredAttacksPerSecond) {
        double clamped = Math.max(0.1D, configuredAttacksPerSecond);
        return clamped - PLAYER_BASE_ATTACK_SPEED;
    }

    public static int maxDamage(int configuredDurability) {
        return Math.max(1, configuredDurability);
    }

    public static String normalizeRepairItemId(String configuredRepairItem) {
        if (configuredRepairItem == null) {
            return null;
        }
        String trimmed = configuredRepairItem.trim();
        if (trimmed.isEmpty() || "none".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }
}
