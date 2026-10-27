package com.apocollis.aqtweaks.rtg;

/**
 * Whether the current work is a Chunk Steward pipeline job (a terrain worker, or a pipeline generator being
 * constructed). Worker generators are built on the server thread, so a thread check cannot tell them apart; the
 * signal has to come from Chunk Steward (API v3). Until that integration (plan T2) this is a constant {@code false},
 * so every guard that reads it is inert and behaviour is unchanged.
 *
 * <p>Guards that read it: the generator stash in {@code MixinChunkGeneratorRTGVillage} (a worker generator must never
 * become the stashed one), and {@code registerVillages} (the layout runs in {@code prepare()} for a pipeline job).
 */
public final class PipelineContext {

    private PipelineContext() {}

    public static boolean active() {
        return false;
    }
}
