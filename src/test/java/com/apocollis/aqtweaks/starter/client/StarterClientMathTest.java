package com.apocollis.aqtweaks.starter.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class StarterClientMathTest {

    @Test
    void romanNumerals() {
        assertEquals("I", StarterClientMath.roman(1));
        assertEquals("IV", StarterClientMath.roman(4));
        assertEquals("VIII", StarterClientMath.roman(8));
        assertEquals("XIV", StarterClientMath.roman(14));
        assertEquals("XL", StarterClientMath.roman(40));
        assertEquals("MCMXCIV", StarterClientMath.roman(1994));
    }

    @Test
    void romanFallsBackToDigitsOutOfRange() {
        assertEquals("0", StarterClientMath.roman(0));
        assertEquals("-3", StarterClientMath.roman(-3));
        assertEquals("4000", StarterClientMath.roman(4000));
    }

    @Test
    void gearIsCappedAtTwelve() {
        List<Integer> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            many.add(i);
        }
        List<Integer> capped = StarterClientMath.capGear(many, StarterClientMath.GEAR_CAP);
        assertEquals(12, capped.size());
        assertEquals(Integer.valueOf(0), capped.get(0));
        assertEquals(Integer.valueOf(11), capped.get(11));
    }

    @Test
    void gearCapKeepsShortListsAndHandlesNull() {
        assertEquals(Arrays.asList(1, 2, 3), StarterClientMath.capGear(Arrays.asList(1, 2, 3), 12));
        assertTrue(StarterClientMath.capGear(null, 12).isEmpty());
        assertTrue(StarterClientMath.capGear(Arrays.asList(1, 2), 0).isEmpty());
    }

    @Test
    void scrollIsClamped() {
        assertEquals(0, StarterClientMath.clampScroll(-10, 500, 200));
        assertEquals(300, StarterClientMath.clampScroll(999, 500, 200));
        assertEquals(120, StarterClientMath.clampScroll(120, 500, 200));
        assertEquals(0, StarterClientMath.clampScroll(50, 100, 200), "content shorter than the view never scrolls");
    }

    @Test
    void panelSizeFitsSmallWindows() {
        assertEquals(380, StarterClientMath.panelSize(1000, 380, 8, 140));
        assertEquals(284, StarterClientMath.panelSize(300, 380, 8, 140));
        assertEquals(140, StarterClientMath.panelSize(150, 380, 8, 140), "never below the minimum when it fits");
        assertEquals(100, StarterClientMath.panelSize(100, 380, 8, 140), "never larger than the window");
    }

    @Test
    void alphaColourKeepsRgbAndClamps() {
        assertEquals(0xFFE0B24A, StarterClientMath.withAlpha(0xE0B24A, 1f));
        assertEquals(0x80000000, StarterClientMath.withAlpha(0x000000, 0.5f));
        assertEquals(0x00123456, StarterClientMath.withAlpha(0x123456, -1f));
        assertEquals(0xFF123456, StarterClientMath.withAlpha(0x123456, 5f));
        assertFalse(StarterClientMath.visible(0.0f));
        assertTrue(StarterClientMath.visible(0.5f));
    }

    @Test
    void scrollbarThumb() {
        assertEquals(100, StarterClientMath.thumbHeight(80, 100, 100), "no scrolling fills the track");
        assertEquals(50, StarterClientMath.thumbHeight(200, 100, 100));
        assertEquals(8, StarterClientMath.thumbHeight(100000, 100, 100), "never thinner than 8");
        assertEquals(0, StarterClientMath.thumbOffset(0, 200, 100, 100));
        assertEquals(50, StarterClientMath.thumbOffset(100, 200, 100, 100), "scrolled to the end puts the thumb at the bottom");
        assertEquals(0, StarterClientMath.thumbOffset(10, 50, 100, 100));
    }

    @Test
    void rowLookup() {
        int[] tops = {10, 70, 140};
        int[] heights = {60, 70, 50};
        assertEquals(-1, StarterClientMath.rowAt(tops, heights, 5));
        assertEquals(0, StarterClientMath.rowAt(tops, heights, 10));
        assertEquals(0, StarterClientMath.rowAt(tops, heights, 69));
        assertEquals(1, StarterClientMath.rowAt(tops, heights, 70));
        assertEquals(2, StarterClientMath.rowAt(tops, heights, 189));
        assertEquals(-1, StarterClientMath.rowAt(tops, heights, 190));
    }
}
