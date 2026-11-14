package com.apocollis.aqtweaks.atum;

import java.util.function.IntUnaryOperator;

/** Pure argument parsing and durations for {@code /atumweather}. No Minecraft types. */
public final class AtumWeatherCommandHelper {
    private AtumWeatherCommandHelper() {}

    public static final int MAX_SECONDS = 1000000;

    public static final class Parsed {
        /** 0 = not given (use the configured default). */
        public final int seconds;
        public final boolean skipWarning;
        /** Null when the arguments are valid. */
        public final String error;

        Parsed(int seconds, boolean skipWarning, String error) {
            this.seconds = seconds;
            this.skipWarning = skipWarning;
            this.error = error;
        }
    }

    /** Parses {@code [seconds] [skipWarning]} (either order) from {@code args[from..]}. */
    public static Parsed parse(String[] args, int from, boolean allowSkip) {
        int seconds = 0;
        boolean skip = false;
        boolean haveSeconds = false;
        for (int i = from; i < args.length; i++) {
            String a = args[i];
            if (allowSkip && "skipwarning".equalsIgnoreCase(a)) {
                skip = true;
                continue;
            }
            if (haveSeconds) return new Parsed(0, false, "Unexpected argument: " + a);
            try {
                int v = Integer.parseInt(a);
                if (v < 1 || v > MAX_SECONDS) return new Parsed(0, false, "Seconds must be between 1 and " + MAX_SECONDS);
                seconds = v;
                haveSeconds = true;
            } catch (NumberFormatException e) {
                return new Parsed(0, false, "Unexpected argument: " + a);
            }
        }
        return new Parsed(seconds, skip, null);
    }

    /** Ticks for an active storm: the forced seconds, else the looming range, else Atum's own 6000 + rand(6000). */
    public static int stormTicks(int forcedSeconds, boolean looming, int minSeconds, int maxSeconds, IntUnaryOperator rnd) {
        if (forcedSeconds > 0) return forcedSeconds * 20;
        if (looming) {
            int min = Math.max(30, minSeconds) * 20;
            int max = Math.max(min, maxSeconds * 20);
            return min + (max > min ? rnd.applyAsInt(max - min) : 0);
        }
        return 6000 + rnd.applyAsInt(6000);
    }

    /** Ticks of clear weather: the forced seconds, else the looming interval range, else (300 + rand(600)) seconds. */
    public static int clearTicks(int forcedSeconds, boolean looming, int minSeconds, int maxSeconds, IntUnaryOperator rnd) {
        if (forcedSeconds > 0) return forcedSeconds * 20;
        if (looming) {
            int min = Math.max(60, minSeconds) * 20;
            int max = Math.max(min, maxSeconds * 20);
            return min + (max > min ? rnd.applyAsInt(max - min) : 0);
        }
        return (300 + rnd.applyAsInt(600)) * 20;
    }

    /** The pending forced duration to store for the end of a warning: 0 when none was given. */
    public static int pendingTicks(int forcedSeconds) {
        return forcedSeconds > 0 ? forcedSeconds * 20 : 0;
    }

    public static boolean warns(boolean skipWarning, int warningTicks) {
        return !skipWarning && warningTicks > AtumWarningRamp.PINNED_CLEAN_WEATHER;
    }
}
