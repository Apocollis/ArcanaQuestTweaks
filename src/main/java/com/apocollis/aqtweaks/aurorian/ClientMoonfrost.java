package com.apocollis.aqtweaks.aurorian;

/**
 * Client copy of the player's own rime stack, sent by the server ({@code PacketMoonfrostState}). The storm
 * itself is read through the hazard engine, not from here. Plain statics so any client class may read it.
 */
public final class ClientMoonfrost {
    private ClientMoonfrost() {}

    /** Own rime stack 0..15, from the server. */
    public static volatile int rime;

    public static void reset() {
        rime = 0;
    }
}
