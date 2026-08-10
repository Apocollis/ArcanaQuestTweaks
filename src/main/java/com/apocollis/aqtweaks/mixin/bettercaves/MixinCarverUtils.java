package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CarverUtils.class, remap = false)
public abstract class MixinCarverUtils {

    /**
     * Overrides CarverUtils.canReplaceBlock to allow Better Caves to carve through ALL RTG
     * terrain blocks (terracotta, clay, sandstone, granite, diorite, andesite, basalt, etc.)
     * and modded stone/dirt types without stopping or fragmenting caves.
     */
    @Inject(method = "canReplaceBlock", at = @At("HEAD"), cancellable = true)
    private static void onCanReplaceBlock(IBlockState state, IBlockState stateAbove, CallbackInfoReturnable<Boolean> cir) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule) {
            if (state != null) {
                Block b = Reflect.getBlock(state);
                if (b != null) {
                    Material mat = Reflect.getMaterial(state);
                    Material air = Reflect.getMaterialAir();
                    Material water = Reflect.getMaterialWater();
                    Material lava = Reflect.getMaterialLava();
                    if (mat != null && mat != air && mat != water && mat != lava) {
                        // Allow all solid ground/rock/clay/sand/ice terrain materials
                        if (mat == Reflect.getMaterialRock()
                                || mat == Reflect.getMaterialGround()
                                || mat == Reflect.getMaterialClay()
                                || mat == Reflect.getMaterialSand()
                                || mat == Reflect.getMaterialGrass()
                                || mat == Reflect.getMaterialIce()
                                || mat == Reflect.getMaterialPackedIce()
                                || mat == Reflect.getMaterialCraftedSnow()) {
                            cir.setReturnValue(true);
                            return;
                        }

                        // Also allow by block name fallback (terracotta, slate, granite, etc.)
                        String name = b.getRegistryName() != null ? b.getRegistryName().toString().toLowerCase() : "";
                        if (name.contains("stone")
                                || name.contains("deepslate")
                                || name.contains("clay")
                                || name.contains("terracotta")
                                || name.contains("dirt")
                                || name.contains("sand")
                                || name.contains("rock")
                                || name.contains("granite")
                                || name.contains("diorite")
                                || name.contains("andesite")
                                || name.contains("basalt")
                                || name.contains("tuff")
                                || name.contains("slate")) {
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            }
        }
    }
}
