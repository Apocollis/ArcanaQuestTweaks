package com.apocollis.aqtweaks.twilightforest;

/** Client copy of the player's Ley-Rupture exposure factor. Plain static so any client class may read it. */
public final class ClientLey {
    private ClientLey() {}

    public static volatile float exposure;
}
