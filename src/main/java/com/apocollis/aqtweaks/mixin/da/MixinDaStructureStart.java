package com.apocollis.aqtweaks.mixin.da;

import com.apocollis.aqtweaks.perf.StageTimer;
import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
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
    protected List<StructureComponent> field_75075_a;

    @Shadow(remap = false)
    protected StructureBoundingBox field_75074_b;

    @Unique
    private static final ThreadLocal<Long> AQTWEAKS$DA_PASTE_NS = ThreadLocal.withInitial(() -> 0L);

    @Unique
    private String aqtweaks$daStartName() {
        Class<?> enclosing = this.getClass().getEnclosingClass();
        return enclosing != null ? enclosing.getSimpleName() : this.getClass().getSimpleName();
    }

    @Inject(method = "create", at = @At("HEAD"))
    private void aqtweaks$timeCreateHead(World world, Random rand, int chunkX, int chunkZ, CallbackInfo ci) {
        AQTWEAKS$DA_PASTE_NS.set(StageTimer.start());
    }

    @Inject(method = "create", at = @At("RETURN"))
    private void aqtweaks$timeCreateReturn(World world, Random rand, int chunkX, int chunkZ, CallbackInfo ci) {
        long t0 = AQTWEAKS$DA_PASTE_NS.get();
        AQTWEAKS$DA_PASTE_NS.set(0L);
        if (t0 != 0L) {
            int comps = this.field_75075_a != null ? this.field_75075_a.size() : 0;
            StageTimer.end("da.init", t0, aqtweaks$daStartName() + ":comps=" + comps);
        }
    }

    @Inject(method = "func_75068_a", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipDaOnVillage(World world, Random rand, StructureBoundingBox clip, CallbackInfo ci) {
        AQTWEAKS$DA_PASTE_NS.set(StageTimer.start());
        if (!StructureVillageOverlap.enabled()
                || world == null
                || world.provider == null
                || world.provider.getDimension() != 0) {
            return;
        }
        StructureBoundingBox box = this.field_75074_b;
        if (StructureVillageOverlap.overlapsVillage(world, box)) {
            VillageDebug.log("da %s skip village overlap box=%d..%d,%d..%d,%d..%d",
                    aqtweaks$daStartName(),
                    box.minX, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ);
            AQTWEAKS$DA_PASTE_NS.set(0L);
            ci.cancel();
        }
    }

    @Inject(method = "func_75068_a", at = @At("RETURN"))
    private void aqtweaks$timePasteReturn(World world, Random rand, StructureBoundingBox clip, CallbackInfo ci) {
        long t0 = AQTWEAKS$DA_PASTE_NS.get();
        AQTWEAKS$DA_PASTE_NS.set(0L);
        if (t0 != 0L) {
            int comps = this.field_75075_a != null ? this.field_75075_a.size() : 0;
            StageTimer.end("da.paste", t0, aqtweaks$daStartName() + ":comps=" + comps);
        }
    }
}
