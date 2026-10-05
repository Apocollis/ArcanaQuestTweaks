package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.portal.EntityArcaneRift;
import com.fantasticsource.dynamicstealth.server.entitytracker.DSEntityTrackerEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dynamic Stealth stops tracking any entity the player cannot see, which removes the client rift (mesh and
 * particles) whenever the player looks away. Its full-bypass list fixes that but then tracks the entity
 * without vanilla's "player is watching this chunk" test, so the single spawn packet can reach the client
 * before the chunk and be dropped (the rift never renders after a dimension change). For the rift only:
 * tracked whenever in range and the player's client has the chunk, no sight test.
 */
@Mixin(value = DSEntityTrackerEntry.class, remap = false)
public abstract class MixinDSEntityTrackerEntry {

    @Shadow(remap = false)
    @Final
    protected Entity entity;

    @Shadow(remap = false)
    protected int trackerEntryRange;

    @Shadow(remap = false)
    protected int entityTrackerMaxRange;

    @Inject(method = "func_180233_c", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$riftAlwaysTracked(EntityPlayerMP player, CallbackInfoReturnable<Boolean> cir) {
        if (!(this.entity instanceof EntityArcaneRift)) return;
        int range = Math.min(this.trackerEntryRange, this.entityTrackerMaxRange);
        double dx = player.posX - this.entity.posX;
        double dz = player.posZ - this.entity.posZ;
        boolean inRange = dx >= -range && dx <= range && dz >= -range && dz <= range;
        boolean watching = inRange && player.getServerWorld().getPlayerChunkMap()
                .isPlayerWatchingChunk(player, this.entity.chunkCoordX, this.entity.chunkCoordZ);
        if (!aqtweaks$logged) {
            aqtweaks$logged = true;
            LogManager.getLogger("AQTweaks-Portal")
                    .info("[AQ-PORTAL] DS tracker override: rift tracked without the sight test");
        }
        cir.setReturnValue(watching);
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean aqtweaks$logged;
}
