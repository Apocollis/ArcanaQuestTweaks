package com.apocollis.aqtweaks.mixin.da;

import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(targets = {
        "com.dungeon_additions.da.world.aspect_forge.WorldGenAspectForge$Start",
        "com.dungeon_additions.da.world.blossom.WorldGenBlossomCave$Start",
        "com.dungeon_additions.da.world.cults_domain.WorldGenCultsDomain$Start",
        "com.dungeon_additions.da.world.dauntless.WorldGenDauntlessArena$Start",
        "com.dungeon_additions.da.world.forgotten_temple.WorldGenForgottenTemple$Start",
        "com.dungeon_additions.da.world.frozen_castle.WorldGenFrozenCastle$Start",
        "com.dungeon_additions.da.world.gaelon_sanctuary.WorldGenGaelonSanctuary$Start",
        "com.dungeon_additions.da.world.high_city.WorldGenHighCity$Start",
        "com.dungeon_additions.da.world.lich_tower.WorldGenLichTower$Start",
        "com.dungeon_additions.da.world.mysterious_trader.WorldGenMysteriousTraderPost$Start",
        "com.dungeon_additions.da.world.nether_arena.WorldGenNetherArena$Start",
        "com.dungeon_additions.da.world.obsidilith_arena.WorldGenObsidilithArena$Start",
        "com.dungeon_additions.da.world.outposts.WorldGenOutposts$Start",
        "com.dungeon_additions.da.world.rot_hold.WorldGenRotHold$Start"
}, remap = false)
public abstract class MixinDaStructureStart {

    @Shadow(remap = false)
    protected StructureBoundingBox field_75074_b;

    @Inject(method = "func_75068_a", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipDaOnVillage(World world, Random rand, StructureBoundingBox clip, CallbackInfo ci) {
        if (!StructureVillageOverlap.enabled()
                || world == null
                || world.provider == null
                || world.provider.getDimension() != 0) {
            return;
        }
        StructureBoundingBox box = this.field_75074_b;
        if (StructureVillageOverlap.overlapsVillage(world, box)) {
            VillageDebug.log("da %s skip village overlap box=%d..%d,%d..%d,%d..%d",
                    this.getClass().getEnclosingClass() != null
                            ? this.getClass().getEnclosingClass().getSimpleName()
                            : this.getClass().getSimpleName(),
                    box.minX, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ);
            ci.cancel();
        }
    }
}
