package com.apocollis.aqtweaks.spawning;

import net.minecraft.world.biome.Biome;

public final class SpawnPackContext {

    private static final ThreadLocal<Boolean> IN_SPAWNER = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Biome.SpawnListEntry> ENTRY = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> FILLING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private SpawnPackContext() {}

    public static void enterSpawner() {
        IN_SPAWNER.set(Boolean.TRUE);
        ENTRY.remove();
        FILLING.set(Boolean.FALSE);
    }

    public static void leaveSpawner() {
        IN_SPAWNER.remove();
        ENTRY.remove();
        FILLING.remove();
    }

    public static boolean inSpawner() {
        return Boolean.TRUE.equals(IN_SPAWNER.get());
    }

    public static void storeEntry(Biome.SpawnListEntry entry) {
        if (inSpawner()) {
            ENTRY.set(entry);
        }
    }

    public static Biome.SpawnListEntry entry() {
        return ENTRY.get();
    }

    public static boolean filling() {
        return Boolean.TRUE.equals(FILLING.get());
    }

    public static void setFilling(boolean filling) {
        FILLING.set(filling);
    }
}
