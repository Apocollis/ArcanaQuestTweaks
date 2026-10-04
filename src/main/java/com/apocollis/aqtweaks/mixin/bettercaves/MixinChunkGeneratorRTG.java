package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.depths.DepthsBlocks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.PrimerAccess;
import com.apocollis.aqtweaks.depths.SeamReinforcer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "rtg.world.gen.ChunkGeneratorRTG", remap = false)
public abstract class MixinChunkGeneratorRTG {

    @Shadow
    private World world;

    /**
     * Y 0-4 seam reinforcement, once per newly generated chunk (RETURN of
     * {@code IChunkGenerator.generateChunk}, {@code func_185932_a}). Order inside that method:
     * {@link MixinChunkGeneratorRTGVillage} flatten (HEAD / landscape) -> RTG terrain and caves ->
     * this pass. Never inject {@code ChunkProviderServer.provideChunk}: it runs on every chunk fetch.
     */
    @Inject(method = "func_185932_a", at = @At("RETURN"))
    private void aqtweaks$reinforceSeams(int chunkX, int chunkZ, CallbackInfoReturnable<Chunk> cir) {
        SeamReinforcer.reinforce(this.world, cir.getReturnValue(), chunkX, chunkZ);
    }

    /**
     * Injects into RTG's generateTerrain(ChunkPrimer, float[]).
     *
     * RTG only fills Y = 0 to 255, leaving sub-zero columns as void. Depths Update only mixined
     * ChunkGeneratorOverworld, so RTG worlds had no Deepslate below Y = 0.
     *
     * This mixin writes bedrock at minY (-64) and Deepslate from minY+1 through -1. It does
     * <em>not</em> write Y = 0 — that sealed Better Caves mouths.
     *
     * <p>Ordering against {@link MixinChunkGeneratorRTGVillage} is fixed by injection points, not
     * by the json list and not by {@code @Mixin(priority)}: flatten runs before
     * {@code generateTerrain}, this fill runs at its TAIL.
     *
     * <p>Writes go through {@link PrimerAccess}: this class is {@code remap = false}, so a direct
     * {@code primer.setBlockState} would not be remapped, and {@code Reflect} invoke is too
     * expensive for 1,008 writes per chunk.
     */
    @Inject(method = "generateTerrain", at = @At("TAIL"))
    private void onGenerateTerrain(ChunkPrimer primer, float[] noise, CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.DepthsModuleConfig.general.enableDepthsModule) return;

        int minY = ArcanaQuestTweaksConfig.DepthsModuleConfig.general.minWorldY;
        if (minY >= 0 || primer == null) return;

        IBlockState deepslateState = DepthsBlocks.deepslateState();
        IBlockState bedrockState = DepthsBlocks.BEDROCK_STATE;
        if (deepslateState == null && bedrockState == null) return;

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                // Bedrock floor at minY (-64)
                if (bedrockState != null) {
                    PrimerAccess.setBlockState(primer, x, minY, z, bedrockState);
                }

                // Fill solid Deepslate from minY+1 to -1 only.
                // Do NOT overwrite Y=0 — that created a solid deepslate lid blocking breach mouths into +Y caves.
                if (deepslateState != null) {
                    for (int y = minY + 1; y <= -1; ++y) {
                        PrimerAccess.setBlockState(primer, x, y, z, deepslateState);
                    }
                }
            }
        }
    }
}
