package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VolleyPlannerTest {

    private static final List<String> FIVE = Arrays.asList("a", "b", "c", "d", "e");
    private static final VolleyPlanner.Readiness ALL = new VolleyPlanner.Readiness() {
        @Override
        public boolean ready(String focusId) {
            return true;
        }
    };

    /** Ticks the planner one tick at a time from {@code from}, collecting (tick, id) casts until {@code to}. */
    private static List<Object[]> run(VolleyPlanner p, long from, long to, VolleyPlanner.Readiness r) {
        List<Object[]> shots = new ArrayList<>();
        for (long t = from; t < to; t++) {
            String id = p.next(t, FIVE, r);
            if (id != null) {
                shots.add(new Object[] {t, id});
            }
        }
        return shots;
    }

    @Test
    void aVolleyFiresEachFocusOnceThirtyTicksApart() {
        VolleyPlanner p = new VolleyPlanner(30, 80, 200, new Random(1L));
        List<Object[]> shots = run(p, 0, 125, ALL);
        assertEquals(5, shots.size());
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < shots.size(); i++) {
            assertEquals(i * 30L, (long) (Long) shots.get(i)[0]);
            seen.add((String) shots.get(i)[1]);
        }
        assertEquals(new HashSet<>(FIVE), seen);
    }

    @Test
    void aPauseFollowsTheFifthShotThenANewVolleyStarts() {
        VolleyPlanner p = new VolleyPlanner(30, 80, 200, new Random(2L));
        List<Object[]> shots = run(p, 0, 400, ALL);
        // volley 1: ticks 0..120; pause until 200; volley 2 starts at 200.
        assertEquals(120L, (long) (Long) shots.get(4)[0]);
        assertEquals(200L, (long) (Long) shots.get(5)[0]);
        VolleyPlanner q = new VolleyPlanner(30, 80, 200, new Random(2L));
        run(q, 0, 150, ALL);
        assertTrue(q.pausing(150L));
        run(q, 150, 201, ALL);
        assertFalse(q.pausing(200L));
    }

    @Test
    void theFirstOfANewVolleyIsNeverTheLastOfThePrevious() {
        for (long seed = 0; seed < 300; seed++) {
            VolleyPlanner p = new VolleyPlanner(30, 80, 200, new Random(seed));
            List<Object[]> shots = run(p, 0, 330, ALL);
            assertEquals(10, shots.size(), "two full volleys");
            assertNotEquals(shots.get(4)[1], shots.get(5)[1], "seed " + seed);
        }
    }

    @Test
    void noFocusFiresTwiceInsideOneVolley() {
        for (long seed = 0; seed < 100; seed++) {
            VolleyPlanner p = new VolleyPlanner(30, 80, 200, new Random(seed));
            List<Object[]> shots = run(p, 0, 330, ALL);
            Set<String> first = new HashSet<>();
            Set<String> second = new HashSet<>();
            for (int i = 0; i < 5; i++) {
                first.add((String) shots.get(i)[1]);
                second.add((String) shots.get(5 + i)[1]);
            }
            assertEquals(5, first.size());
            assertEquals(5, second.size());
        }
    }

    @Test
    void aFocusThatIsNotReadyIsSkippedAndKeepsItsPlace() {
        VolleyPlanner p = new VolleyPlanner(30, 80, 400, new Random(3L));
        final Set<String> blocked = new HashSet<>(Arrays.asList("a"));
        VolleyPlanner.Readiness r = new VolleyPlanner.Readiness() {
            @Override
            public boolean ready(String focusId) {
                return !blocked.contains(focusId);
            }
        };
        List<Object[]> shots = run(p, 0, 130, r);
        assertEquals(4, shots.size());
        for (Object[] s : shots) {
            assertNotEquals("a", s[1]);
        }
        // a becomes ready: it is cast next, completing the volley.
        blocked.clear();
        String id = p.next(130L, FIVE, r);
        assertEquals("a", id);
    }

    @Test
    void aVolleyEndsAtTheMaxTicksEvenWhenNothingIsReady() {
        VolleyPlanner p = new VolleyPlanner(30, 80, 200, new Random(4L));
        VolleyPlanner.Readiness none = new VolleyPlanner.Readiness() {
            @Override
            public boolean ready(String focusId) {
                return false;
            }
        };
        assertNull(p.next(0L, FIVE, none));
        assertTrue(p.inVolley());
        assertNull(p.next(199L, FIVE, none));
        assertTrue(p.inVolley());
        assertNull(p.next(200L, FIVE, none));
        assertFalse(p.inVolley());
        assertTrue(p.pausing(201L));
        assertFalse(p.pausing(280L));
        // the pause is over: a new volley can fire at once.
        assertTrue(p.next(280L, FIVE, ALL) != null);
    }

    @Test
    void theClockKeepsRunningWhileNothingCanBeCast() {
        // Target too close: not ready for 70 ticks, then ready. The shot is not owed back: the gap is wall time.
        VolleyPlanner p = new VolleyPlanner(30, 80, 200, new Random(5L));
        VolleyPlanner.Readiness none = new VolleyPlanner.Readiness() {
            @Override
            public boolean ready(String focusId) {
                return false;
            }
        };
        for (long t = 0; t < 70; t++) {
            assertNull(p.next(t, FIVE, none));
        }
        assertTrue(p.next(70L, FIVE, ALL) != null);
        assertNull(p.next(71L, FIVE, ALL), "the gap after a shot still applies");
        assertTrue(p.next(100L, FIVE, ALL) != null);
        // the volley limit counts from the first call (tick 0), so it would end at tick 200; the fifth shot lands at 190.
        assertTrue(p.next(130L, FIVE, ALL) != null);
        assertTrue(p.next(160L, FIVE, ALL) != null);
        assertTrue(p.next(190L, FIVE, ALL) != null, "the fifth shot fits inside the 200 tick limit");
        assertTrue(p.pausing(191L), "the fifth shot ended the volley; the pause is running");
    }

    @Test
    void anIdleMageStartsAFreshVolley() {
        VolleyPlanner p = new VolleyPlanner(30, 80, 200, new Random(6L));
        assertTrue(p.next(0L, FIVE, ALL) != null);
        assertTrue(p.next(30L, FIVE, ALL) != null);
        // no target for a long time, then it comes back mid-volley state: fresh volley with all five to fire.
        assertTrue(p.next(5000L, FIVE, ALL) != null);
        assertEquals(4, p.remaining().size());
    }

    @Test
    void noFociMeansNoCast() {
        VolleyPlanner p = new VolleyPlanner(30, 80, 200, new Random(7L));
        assertNull(p.next(0L, new ArrayList<String>(), ALL));
    }
}
