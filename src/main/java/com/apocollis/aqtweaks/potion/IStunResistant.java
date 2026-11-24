package com.apocollis.aqtweaks.potion;

/**
 * An entity that shortens or refuses Stun. {@link Stun#apply} asks it for the length to apply, so a mob can have
 * crowd-control resistance without the potion package knowing about it (no parent types here).
 */
public interface IStunResistant {

    /** @return the Stun length in ticks to apply, 0 to ignore the Stun entirely */
    int adjustStun(int ticks);
}
