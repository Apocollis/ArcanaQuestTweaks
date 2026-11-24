package com.apocollis.aqtweaks.thaumcraft.battlemage;

/** One escort entry of a variant (plan 18.7): entity registry id and how many of them arrive. */
public final class EscortDef {
    public final String entity;
    public final int count;

    public EscortDef(String entity, int count) {
        this.entity = entity;
        this.count = count;
    }
}
