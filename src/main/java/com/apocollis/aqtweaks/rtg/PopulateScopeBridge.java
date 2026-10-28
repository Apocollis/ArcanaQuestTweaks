package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.world.PopulateRelight;
import com.apocollis.chunksteward.api.PopulateScope;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Chunk Steward staged populate finishes a chunk's populate after the populate call returned (the decorations, animals,
 * snow, mod generators). This opens {@link PopulateRelight}'s recording scope for that part, so it records light work
 * the same way an unstaged populate does. Loaded only after {@link PipelineContext} read API version 4.
 */
public final class PopulateScopeBridge implements PopulateScope {

    @Override
    public void begin(World world, Chunk chunk) {
        PopulateRelight.beginResume(world, chunk);
    }

    @Override
    public void end(World world, Chunk chunk) {
        PopulateRelight.end(chunk);
    }

    /** Chunk Steward API version 5: the chunk's light was computed on a worker and installed. */
    @Override
    public void lightInstalled(World world, Chunk chunk) {
        PopulateRelight.dropPending(chunk);
    }
}
