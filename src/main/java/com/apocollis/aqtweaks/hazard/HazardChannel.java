package com.apocollis.aqtweaks.hazard;

import net.minecraft.world.World;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** A registered hazard: id, name, which worlds it runs in, whether it is on, and its timing from config. */
public final class HazardChannel {
    public final int id;
    public final String name;
    private final Predicate<World> appliesTo;
    private final BooleanSupplier enabled;
    private final Supplier<HazardTiming> timing;

    public HazardChannel(int id, String name, Predicate<World> appliesTo, BooleanSupplier enabled,
                         Supplier<HazardTiming> timing) {
        this.id = id;
        this.name = name;
        this.appliesTo = appliesTo;
        this.enabled = enabled;
        this.timing = timing;
    }

    public boolean appliesTo(World world) {
        return world != null && appliesTo.test(world);
    }

    public boolean enabled() {
        return enabled.getAsBoolean();
    }

    public HazardTiming timing() {
        return timing.get();
    }
}
