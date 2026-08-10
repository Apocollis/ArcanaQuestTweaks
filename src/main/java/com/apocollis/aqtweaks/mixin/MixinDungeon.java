package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import greymerk.roguelike.dungeon.Dungeon;
import greymerk.roguelike.dungeon.settings.DungeonSettings;
import greymerk.roguelike.worldgen.Coord;
import greymerk.roguelike.worldgen.WorldEditor;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.apocollis.aqtweaks.roguelike.GridStructureTracker;
import com.apocollis.aqtweaks.roguelike.RoguelikeDungeonSavedData;
import com.apocollis.aqtweaks.util.Reflect;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = Dungeon.class, remap = false)
public abstract class MixinDungeon {

    @Shadow
    private WorldEditor editor;

    @Shadow
    private Coord origin;

    @Shadow
    private List<greymerk.roguelike.dungeon.DungeonLevel> levels;

    @Shadow
    public abstract boolean canGenerateDungeonHere(Coord pos);

    @Inject(method = "isDungeonChunk", at = @At("HEAD"), cancellable = true)
    private static void onIsDungeonChunk(WorldEditor editor, int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        World world = null;
        if (editor instanceof com.github.fnar.minecraft.WorldEditor1_12) {
            try {
                Field field = com.github.fnar.minecraft.WorldEditor1_12.class.getDeclaredField("world");
                field.setAccessible(true);
                world = (World) field.get(editor);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        boolean fitsGrid = GridStructureTracker.shouldSpawnAt(world, chunkX, chunkZ);
        cir.setReturnValue(fitsGrid);
    }

    @Inject(method = "selectLocation", at = @At("HEAD"), cancellable = true)
    private void onSelectLocation(java.util.Random rand, int startX, int startZ, CallbackInfoReturnable<java.util.Optional<Coord>> cir) {
        if (com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.roguelikeModule.enableGridSpawning) {
            int centerX = startX + 8;
            int centerZ = startZ + 8;
            Coord coord = new Coord(centerX, 0, centerZ);
            if (this.canGenerateDungeonHere(coord)) {
                cir.setReturnValue(java.util.Optional.of(coord));
            } else {
                cir.setReturnValue(java.util.Optional.empty());
            }
        }
    }

    @Inject(method = "generate", at = @At("RETURN"))
    private void onGenerate(DungeonSettings settings, Coord origin, CallbackInfo ci) {
        if (this.editor instanceof com.github.fnar.minecraft.WorldEditor1_12 && this.origin != null) {
            try {
                Field field = com.github.fnar.minecraft.WorldEditor1_12.class.getDeclaredField("world");
                field.setAccessible(true);
                World world = (World) field.get(this.editor);
                if (world != null && !Reflect.isRemote(world)) {
                    Coord base = greymerk.roguelike.dungeon.towers.TowerType.getBaseCoord(this.editor, this.origin);
                    if (base != null) {
                        List<RoguelikeDungeonSavedData.DungeonBoundingBox> boxes = new ArrayList<>();
                        
                        // 1. Entrance tower box on the surface (level = -1)
                        int towerRadius = 16;
                        boxes.add(new RoguelikeDungeonSavedData.DungeonBoundingBox(
                            base.getX() - towerRadius, base.getY() - 30, base.getZ() - towerRadius,
                            base.getX() + towerRadius, base.getY() + 30, base.getZ() + towerRadius,
                            -1
                        ));
                        
                        // 2. Loop through each level and add all room/corridor bounding boxes (level = 0 to 4)
                        if (this.levels != null) {
                            for (int i = 0; i < this.levels.size(); i++) {
                                greymerk.roguelike.dungeon.DungeonLevel level = this.levels.get(i);
                                if (level != null && level.getLayout() != null) {
                                    List<greymerk.roguelike.worldgen.Bounded> layoutBoxes = level.getLayout().getBoundingBoxes();
                                    if (layoutBoxes != null) {
                                        for (greymerk.roguelike.worldgen.Bounded box : layoutBoxes) {
                                            if (box != null && box.getStart() != null && box.getEnd() != null) {
                                                int minX = Math.min(box.getStart().getX(), box.getEnd().getX());
                                                int maxX = Math.max(box.getStart().getX(), box.getEnd().getX());
                                                int minY = Math.min(box.getStart().getY(), box.getEnd().getY()) - 1;
                                                int maxY = Math.max(box.getStart().getY(), box.getEnd().getY()) + 4;
                                                int minZ = Math.min(box.getStart().getZ(), box.getEnd().getZ());
                                                int maxZ = Math.max(box.getStart().getZ(), box.getEnd().getZ());
                                                
                                                boxes.add(new RoguelikeDungeonSavedData.DungeonBoundingBox(
                                                    minX, minY, minZ, maxX, maxY, maxZ,
                                                    i // level index: 0 for Floor 1, 1 for Floor 2, etc.
                                                ));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        RoguelikeDungeonSavedData.get(world).addDungeonBoxes(boxes);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
