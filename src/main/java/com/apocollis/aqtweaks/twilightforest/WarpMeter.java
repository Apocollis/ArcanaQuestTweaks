package com.apocollis.aqtweaks.twilightforest;

/**
 * The surge warp buildup (pure, no Minecraft types). Two counters count exposed charge cycles: every
 * {@code tempCycles} points give 1 temporary warp, every {@code normalCycles} points give 1 normal (sticky) warp. Each
 * cycle spent out of the surge takes 1 off each counter (floor 0), so a short dip in and out does not cost warp but a
 * long stay does.
 */
public final class WarpMeter {
    private int temp;
    private int normal;

    /** One exposed cycle. Returns {temporary warp to give, normal warp to give} for this cycle. */
    public int[] exposedCycle(int tempCycles, int normalCycles) {
        int t = Math.max(1, tempCycles);
        int n = Math.max(1, normalCycles);
        temp++;
        normal++;
        int giveTemp = 0;
        int giveNormal = 0;
        if (temp >= t) {
            temp = 0;
            giveTemp = 1;
        }
        if (normal >= n) {
            normal = 0;
            giveNormal = 1;
        }
        return new int[] {giveTemp, giveNormal};
    }

    /** One cycle out of the surge. */
    public void calmCycle() {
        if (temp > 0) temp--;
        if (normal > 0) normal--;
    }

    public boolean idle() {
        return temp == 0 && normal == 0;
    }

    public int temp() {
        return temp;
    }

    public int normal() {
        return normal;
    }
}
