package com.apocollis.aqtweaks.twilightforest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ColumnSearchTest {
    @Test
    void findsTheTopmostStandableSpot() {
        // y=10..19; solid at 10,11,12 and 15; clear elsewhere
        boolean[] c = new boolean[10];
        c[0] = c[1] = c[2] = c[5] = true;
        // top-down: i=6 stands on the block at i=5, clear at 6 and 7
        assertEquals(16, ColumnSearch.topStandableY(c, 10));
    }

    @Test
    void needsTwoClearBlocksAbove() {
        boolean[] c = {true, false, true, false, false, false};
        // i=1 has a solid block at 2 above it, so it does not count; i=3 stands on the block at 2
        assertEquals(3, ColumnSearch.topStandableY(c, 0));
    }

    @Test
    void noGroundOrNoRoomGivesMinusOne() {
        assertEquals(-1, ColumnSearch.topStandableY(new boolean[] {false, false, false, false}, 0));
        assertEquals(-1, ColumnSearch.topStandableY(new boolean[] {true, true, true, true}, 0));
        assertEquals(-1, ColumnSearch.topStandableY(new boolean[] {true}, 0));
        assertEquals(-1, ColumnSearch.topStandableY(null, 0));
    }
}
