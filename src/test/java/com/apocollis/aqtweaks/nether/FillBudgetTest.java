package com.apocollis.aqtweaks.nether;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FillBudgetTest {
    private static int sum(int[] a) {
        int t = 0;
        for (int v : a) t += v;
        return t;
    }

    @Test
    void sharesAlwaysSumToTheTotal() {
        for (int n = 1; n <= 8; n++) {
            for (int start = 0; start < 10; start++) {
                assertEquals(3000, sum(FillBudget.split(3000, n, start)), n + "/" + start);
            }
        }
    }

    @Test
    void evenSplitHasNoRemainder() {
        assertArrayEquals(new int[] {1000, 1000, 1000}, FillBudget.split(3000, 3, 0));
    }

    @Test
    void remainderRotatesWithTheStart() {
        assertArrayEquals(new int[] {4, 3, 3}, FillBudget.split(10, 3, 0));
        assertArrayEquals(new int[] {3, 4, 3}, FillBudget.split(10, 3, 1));
        assertArrayEquals(new int[] {3, 3, 4}, FillBudget.split(10, 3, 2));
        assertArrayEquals(new int[] {4, 3, 3}, FillBudget.split(10, 3, 3));
    }

    @Test
    void emptyAndNegative() {
        assertEquals(0, FillBudget.split(100, 0, 0).length);
        assertEquals(0, sum(FillBudget.split(-5, 2, 0)));
        assertEquals(5, sum(FillBudget.split(5, 2, -1)));
    }
}
