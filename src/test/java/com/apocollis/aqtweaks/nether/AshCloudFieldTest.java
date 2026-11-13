package com.apocollis.aqtweaks.nether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.junit.jupiter.api.Test;

class AshCloudFieldTest {
    private static LongOpenHashSet fill(AshCloudField.Open open, double cx, double cy, double cz, double rh,
                                         double rv, LongOpenHashSet prev, int cap) {
        AshCloudField f = new AshCloudField(open, cx, cy, cz, rh, rv, prev, cap);
        while (!f.step(500)) { }
        return f.result();
    }

    private static boolean has(LongOpenHashSet s, int x, int y, int z) {
        return s.contains(AshCloudField.pack(x, y, z));
    }

    @Test
    void packingRoundTrips() {
        long p = AshCloudField.pack(-123, 70, 456);
        assertEquals(-123, AshCloudField.unpackX(p));
        assertEquals(70, AshCloudField.unpackY(p));
        assertEquals(456, AshCloudField.unpackZ(p));
    }

    @Test
    void openSpaceFillsTheReachOnly() {
        LongOpenHashSet s = fill((x, y, z) -> true, 0.5, 64.5, 0.5, 5, 5, null, 100000);
        assertTrue(has(s, 0, 64, 0));
        assertTrue(has(s, 4, 64, 0));
        assertFalse(has(s, 6, 64, 0));
        assertFalse(has(s, 0, 70, 0));
    }

    @Test
    void aWallStopsItAndADoorwayLetsItThrough() {
        // Solid wall at x = 3 for all y, z, except a doorway at y 64..65, z = 0.
        AshCloudField.Open open = (x, y, z) -> x != 3 || (z == 0 && (y == 64 || y == 65));
        LongOpenHashSet s = fill(open, 0.5, 64.5, 0.5, 8, 8, null, 100000);
        assertTrue(has(s, 2, 64, 0));
        assertFalse(has(s, 3, 60, 0));
        assertTrue(has(s, 3, 64, 0));
        assertTrue(has(s, 5, 64, 0));
        assertTrue(has(s, 5, 64, 3));
    }

    @Test
    void aSealedRoomStaysClear() {
        // A hollow cube room from 6..10 on x, y 62..66, z -2..2, walls one thick, no opening.
        AshCloudField.Open open = (x, y, z) -> {
            boolean inBox = x >= 6 && x <= 10 && y >= 62 && y <= 66 && z >= -2 && z <= 2;
            boolean wall = inBox && (x == 6 || x == 10 || y == 62 || y == 66 || z == -2 || z == 2);
            boolean inside = inBox && !wall;
            return !wall;
        };
        LongOpenHashSet s = fill(open, 0.5, 64.5, 0.5, 14, 14, null, 200000);
        assertTrue(has(s, 5, 64, 0));
        assertFalse(has(s, 8, 64, 0));
    }

    @Test
    void theNodeCapBoundsTheFill() {
        LongOpenHashSet s = fill((x, y, z) -> true, 0.5, 64.5, 0.5, 30, 30, null, 500);
        assertTrue(s.size() <= 520);
    }

    @Test
    void theNextFillCannotJumpAWall() {
        // Previous set on the left of a wall; the centre then drifts into the solid wall at x = 3.
        AshCloudField.Open open = (x, y, z) -> x < 3 || x > 3;
        LongOpenHashSet prev = fill(open, 0.5, 64.5, 0.5, 6, 6, null, 100000);
        LongOpenHashSet next = fill(open, 3.5, 64.5, 0.5, 6, 6, prev, 100000);
        assertTrue(has(next, 2, 64, 0));
        assertFalse(has(next, 4, 64, 0));
    }
}
