package com.apocollis.aqtweaks.starter.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure helpers for the starter class screens: Roman numerals, gear cap, scroll clamping, panel sizing, alpha
 * colours and the scrollbar thumb. No Minecraft types, so these are unit tested.
 */
public final class StarterClientMath {

    /** The reveal shows at most this many gear icons. */
    public static final int GEAR_CAP = 12;

    private StarterClientMath() {}

    /** Roman numeral for 1 to 3999; anything outside that is returned as plain digits. */
    public static String roman(int n) {
        if (n < 1 || n > 3999) {
            return Integer.toString(n);
        }
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        int rest = n;
        for (int i = 0; i < values.length; i++) {
            while (rest >= values[i]) {
                sb.append(symbols[i]);
                rest -= values[i];
            }
        }
        return sb.toString();
    }

    /** The first {@code cap} entries of {@code gear}; a null list gives an empty list. */
    public static <T> List<T> capGear(List<T> gear, int cap) {
        List<T> out = new ArrayList<>();
        if (gear == null || cap <= 0) {
            return out;
        }
        for (T t : gear) {
            if (out.size() >= cap) {
                break;
            }
            out.add(t);
        }
        return out;
    }

    /** Scroll offset clamped to [0, content - view]. */
    public static int clampScroll(int scroll, int contentHeight, int viewHeight) {
        int max = Math.max(0, contentHeight - viewHeight);
        return Math.max(0, Math.min(scroll, max));
    }

    /** Panel size on one axis: at most {@code max}, leaving {@code margin} on both sides, never below {@code min}. */
    public static int panelSize(int available, int max, int margin, int min) {
        int size = Math.min(max, available - 2 * margin);
        return Math.max(Math.min(min, available), size);
    }

    /** ARGB colour from an RGB colour and an alpha in [0, 1]. Alpha below 4/255 would be read as opaque by the font renderer. */
    public static int withAlpha(int rgb, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** False when the alpha is so low that drawing text would show it opaque or is pointless. */
    public static boolean visible(float alpha) {
        return alpha >= 0.03f;
    }

    /** Height of the scrollbar thumb; the whole track when nothing scrolls. */
    public static int thumbHeight(int contentHeight, int viewHeight, int trackHeight) {
        if (contentHeight <= viewHeight || contentHeight <= 0) {
            return trackHeight;
        }
        int h = Math.round(trackHeight * (viewHeight / (float) contentHeight));
        return Math.max(8, Math.min(trackHeight, h));
    }

    /** Offset of the thumb from the track top for the given scroll. */
    public static int thumbOffset(int scroll, int contentHeight, int viewHeight, int trackHeight) {
        int range = contentHeight - viewHeight;
        if (range <= 0) {
            return 0;
        }
        int free = trackHeight - thumbHeight(contentHeight, viewHeight, trackHeight);
        return Math.round(free * (Math.max(0, Math.min(scroll, range)) / (float) range));
    }

    /** Index of the row that contains {@code y} (content space), or -1. {@code tops} and {@code heights} are per row. */
    public static int rowAt(int[] tops, int[] heights, int y) {
        for (int i = 0; i < tops.length; i++) {
            if (y >= tops[i] && y < tops[i] + heights[i]) {
                return i;
            }
        }
        return -1;
    }
}
