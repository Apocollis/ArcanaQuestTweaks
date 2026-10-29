package com.apocollis.aqtweaks.spawning.rules;

/**
 * Everything a condition row can ask about a spawn: the mob and where and when it appears. The game supplies one for
 * a spawn check and one for a join; tests supply fakes. Answers are computed only when a row asks.
 */
public interface SpawnQuery {
    /** The mob's registry id, for example {@code minecraft:zombie}. */
    String mobId();

    /** The mod (namespace) the mob comes from. */
    String modId();

    /** True for a hostile mob (a monster). */
    boolean hostile();

    /** Whether a spawner made the mob; null when this is not a spawn check, so a 'spawner' row cannot match. */
    Boolean spawner();

    int dimension();

    /** Block Y of the spawn position. */
    int y();

    /** The time of day, 0 to 23999. */
    int timeOfDay();

    boolean raining();

    boolean thundering();

    boolean seesSky();

    /** The registry id of the biome at the spawn position. */
    String biomeId();

    boolean inStructure(String structure);

    /** True when the mob does not collide with blocks where it is. */
    boolean notColliding();

    /** Whether the nearest player (within the pack's player search range) has the stage. */
    boolean hasStage(String stage);

    /** True when any player is within this many blocks of the spawn position. */
    boolean anyPlayerWithin(double blocks);

    /** The count a count condition compares: the matching mobs now alive. */
    int count(CountSpec spec);

    /** The amount a count condition compares against, after per-player or per-chunk scaling. */
    int scaledAmount(CountSpec spec);

    /** A random number in [0, 1). */
    float roll();
}
