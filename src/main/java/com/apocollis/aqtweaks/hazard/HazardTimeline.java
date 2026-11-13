package com.apocollis.aqtweaks.hazard;

import com.apocollis.aqtweaks.weather.StormSchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side schedule of one hazard channel in one dimension. Holds a single event: the upcoming one
 * (warning window included) or the active one. When it ends, the next is drawn from the end tick. Gap and
 * duration are hashes of {@code (key, index)} through {@link StormSchedule}, so only the index, the last end
 * tick and the event need saving, and a config change affects only the next draw. No Minecraft types.
 */
public final class HazardTimeline {

    /** If an event ended more than this long ago when we notice, treat it as an absence: fresh gap from now. */
    static final long MAX_LATE_TICKS = 100L;

    private final int channel;
    private final long key;
    private long index;
    private long lastEnd;
    private HazardEvent event;

    public HazardTimeline(int channel, long key) {
        this.channel = channel;
        this.key = key;
    }

    public void restore(long index, long lastEnd, HazardEvent event) {
        this.index = index;
        this.lastEnd = lastEnd;
        this.event = event;
    }

    public HazardEvent event() {
        return event;
    }

    public long index() {
        return index;
    }

    public long lastEnd() {
        return lastEnd;
    }

    /** Advances to {@code now}; returns the events created (to announce), usually none. */
    public List<HazardEvent> update(long now, HazardTiming timing) {
        List<HazardEvent> created = new ArrayList<>(0);
        if (event != null && now >= event.end) {
            lastEnd = event.end;
            event = null;
        }
        if (event == null) {
            long base = now - lastEnd > MAX_LATE_TICKS ? now : lastEnd;
            event = draw(base, timing);
            created.add(event);
        }
        return created;
    }

    /**
     * Starts an event now: after the normal warning lead, or at once with {@code skipWarning}. Supersedes the
     * current or upcoming event; the schedule resumes from the new event's end.
     */
    public HazardEvent trigger(long now, long durationTicks, boolean skipWarning, HazardTiming timing) {
        StormSchedule hash = hash(timing);
        long duration = durationTicks > 0L ? durationTicks : hash.stormLength(index);
        long start = skipWarning ? now : now + timing.warning;
        long warningStart = skipWarning ? start : now;
        index++;
        event = new HazardEvent(channel, index, warningStart, start, start + duration, timing.rampUp,
                timing.rampDown, seed(index), 1.0F, 0);
        return event;
    }

    /**
     * Ends the event: an active one through its ramp-down, an upcoming one at once (a fresh gap is drawn).
     * Returns the events to announce, empty if there was nothing to clear.
     */
    public List<HazardEvent> clear(long now, HazardTiming timing) {
        List<HazardEvent> out = new ArrayList<>(1);
        if (event == null || now >= event.end) {
            return out;
        }
        if (now >= event.start) {
            long newEnd = Math.min(event.end, now + Math.max(1, timing.rampDown));
            if (newEnd >= event.end) {
                return out;
            }
            index++;
            event = event.withEnd(index, newEnd, (int) (newEnd - now));
            out.add(event);
        } else {
            lastEnd = now;
            event = null;
            out.addAll(update(now, timing));
        }
        return out;
    }

    private HazardEvent draw(long base, HazardTiming timing) {
        StormSchedule hash = hash(timing);
        long gap = hash.clearLength(index);
        long duration = hash.stormLength(index);
        long start = base + gap;
        long warningStart = Math.max(base, start - timing.warning);
        index++;
        return new HazardEvent(channel, index, warningStart, start, start + duration, timing.rampUp,
                timing.rampDown, seed(index), 1.0F, 0);
    }

    private StormSchedule hash(HazardTiming t) {
        return new StormSchedule(key, t.clearMin, t.clearMax, t.durationMin, t.durationMax, 1, 0);
    }

    private long seed(long i) {
        long z = key + i * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
