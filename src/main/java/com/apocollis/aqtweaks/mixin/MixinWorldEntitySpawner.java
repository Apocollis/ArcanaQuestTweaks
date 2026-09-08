package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.spawning.SpawnPackContext;
import com.apocollis.aqtweaks.spawning.SpawnPackFiller;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldEntitySpawner.class)
public class MixinWorldEntitySpawner {

    @Inject(method = "findChunksForSpawning", at = @At("HEAD"))
    private void aqtweaks$enterSpawner(WorldServer world, boolean spawnHostileMobs, boolean spawnPeacefulMobs,
                                      boolean spawnOnSetTickRate, CallbackInfoReturnable<Integer> cir) {
        SpawnPackContext.enterSpawner();
    }

    @Inject(method = "findChunksForSpawning", at = @At("RETURN"))
    private void aqtweaks$leaveSpawner(WorldServer world, boolean spawnHostileMobs, boolean spawnPeacefulMobs,
                                      boolean spawnOnSetTickRate, CallbackInfoReturnable<Integer> cir) {
        SpawnPackContext.leaveSpawner();
    }

    @Redirect(method = "findChunksForSpawning", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldServer;getSpawnListEntryForTypeAt(Lnet/minecraft/entity/EnumCreatureType;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$SpawnListEntry;"))
    private Biome.SpawnListEntry aqtweaks$capturePackEntry(WorldServer world, EnumCreatureType type, BlockPos pos) {
        Biome.SpawnListEntry entry = world.getSpawnListEntryForTypeAt(type, pos);
        SpawnPackContext.storeEntry(entry);
        return entry;
    }

    @Redirect(method = "findChunksForSpawning", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldServer;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean aqtweaks$spawnAndFill(WorldServer world, Entity entity) {
        boolean spawned = world.spawnEntity(entity);
        if (spawned && entity instanceof EntityLiving living) {
            SpawnPackFiller.onVanillaSpawned(living);
        }
        return spawned;
    }

    @Redirect(method = "findChunksForSpawning", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/event/ForgeEventFactory;getMaxSpawnPackSize(Lnet/minecraft/entity/EntityLiving;)I"))
    private int aqtweaks$ownPackRemainder(EntityLiving entity) {
        if (SpawnPackFiller.ownsVanillaRemainder(entity)) {
            return 1;
        }
        return ForgeEventFactory.getMaxSpawnPackSize(entity);
    }
}
