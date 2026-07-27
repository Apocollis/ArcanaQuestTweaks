package com.apocollis.aqtweaks.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.apocollis.aqtweaks.GridStructureTracker;
import com.apocollis.aqtweaks.RoguelikeDungeonSavedData;

@Mixin(value = ChunkProviderServer.class, remap = false)
public class MixinChunkProviderServer {

    @Shadow
    public WorldServer field_73251_h; // Shadows MCP field 'world'

    @Inject(method = "func_193413_a", at = @At("HEAD"), cancellable = true)
    private void onIsInsideStructure(World worldIn, String structureName, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (structureName == null) return;
        
        String name = structureName.toLowerCase();
        if (name.startsWith("roguelikedungeon") || name.startsWith("roguelike")) {
            RoguelikeDungeonSavedData data = RoguelikeDungeonSavedData.get(worldIn);
            
            // Level-specific checks for InControl
            if (name.contains("floor_") || name.contains("floor1") || name.contains("floor2") || name.contains("floor3") || name.contains("floor4") || name.contains("floor5")) {
                int levelIndex = -2;
                if (name.contains("floor_1") || name.endsWith("floor1")) levelIndex = 0;
                else if (name.contains("floor_2") || name.endsWith("floor2")) levelIndex = 1;
                else if (name.contains("floor_3") || name.endsWith("floor3")) levelIndex = 2;
                else if (name.contains("floor_4") || name.endsWith("floor4")) levelIndex = 3;
                else if (name.contains("floor_5") || name.endsWith("floor5")) levelIndex = 4;
                
                if (levelIndex != -2 && data.getDungeonLevel(pos) == levelIndex) {
                    cir.setReturnValue(true);
                }
            } else if (name.contains("tower")) {
                if (data.getDungeonLevel(pos) == -1) {
                    cir.setReturnValue(true);
                }
            }
            // Standard check for generic "RoguelikeDungeon"
            else if ("roguelikedungeon".equals(name) || "roguelike".equals(name)) {
                if (data.isInside(pos)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "func_180513_a", at = @At("HEAD"), cancellable = true)
    private void onGetPossibleCreatureLocations(World worldIn, String structureName, BlockPos pos, boolean findUnexplored, CallbackInfoReturnable<BlockPos> cir) {
        if ("RoguelikeDungeon".equalsIgnoreCase(structureName) || "roguelike".equalsIgnoreCase(structureName)) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            ChunkPos nearest = GridStructureTracker.getNearestStructure(worldIn, chunkX, chunkZ);
            if (nearest != null) {
                cir.setReturnValue(new BlockPos((nearest.x << 4) + 8, 64, (nearest.z << 4) + 8));
            } else {
                cir.setReturnValue(null);
            }
        }
    }
}
