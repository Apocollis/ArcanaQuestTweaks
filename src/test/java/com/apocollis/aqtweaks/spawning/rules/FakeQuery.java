package com.apocollis.aqtweaks.spawning.rules;

import java.util.HashSet;
import java.util.Set;

/** A spawn situation described by fields, for tests. */
final class FakeQuery implements SpawnQuery {
    String mob = "minecraft:zombie";
    String mod = "minecraft";
    boolean hostile = true;
    Boolean spawner = Boolean.FALSE;
    int dimension = 0;
    int y = 64;
    int time = 6000;
    boolean raining;
    boolean thundering;
    boolean sky = true;
    String biome = "minecraft:plains";
    Set<String> structures = new HashSet<String>();
    boolean notColliding = true;
    Set<String> stages = new HashSet<String>();
    boolean playerNear;
    int count;
    int players = 1;
    float roll = 0.5f;

    @Override
    public String mobId() {
        return mob;
    }

    @Override
    public String modId() {
        return mob.substring(0, mob.indexOf(':'));
    }

    @Override
    public boolean hostile() {
        return hostile;
    }

    @Override
    public Boolean spawner() {
        return spawner;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public int y() {
        return y;
    }

    @Override
    public int timeOfDay() {
        return time;
    }

    @Override
    public boolean raining() {
        return raining || thundering;
    }

    @Override
    public boolean thundering() {
        return thundering;
    }

    @Override
    public boolean seesSky() {
        return sky;
    }

    @Override
    public String biomeId() {
        return biome;
    }

    @Override
    public boolean inStructure(String structure) {
        return structures.contains(structure);
    }

    @Override
    public boolean notColliding() {
        return notColliding;
    }

    @Override
    public boolean hasStage(String stage) {
        return stages.contains(stage);
    }

    @Override
    public boolean anyPlayerWithin(double blocks) {
        return playerNear;
    }

    @Override
    public int count(CountSpec spec) {
        return count;
    }

    @Override
    public int scaledAmount(CountSpec spec) {
        return spec.scale == CountSpec.Scale.PER_PLAYER ? spec.amount * players : spec.amount;
    }

    @Override
    public float roll() {
        return roll;
    }
}
