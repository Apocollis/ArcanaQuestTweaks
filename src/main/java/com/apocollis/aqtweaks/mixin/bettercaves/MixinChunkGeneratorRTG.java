package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.PrimerAccess;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rtg.world.gen.ChunkGeneratorRTG", remap = false)
public abstract class MixinChunkGeneratorRTG {

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

        IBlockState deepslateState = Reflect.getDeepslateState();
        IBlockState bedrockState = Reflect.getBedrockState();
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
