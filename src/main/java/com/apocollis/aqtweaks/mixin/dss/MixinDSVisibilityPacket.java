package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.dynamicstealth.StealthPerf;
import com.apocollis.aqtweaks.perf.StageTimer;
import com.fantasticsource.dynamicstealth.server.senses.sight.Sight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.LinkedHashMap;

/**
 * {@code Network.VisibilityPacket} recomputes {@code Sight.seenEntities(player)} every tick for every player
 * (5.2 s in a 296 s capture). The map is only read when the packet is written, so a short-lived copy per player
 * is reused for the configured number of ticks. See {@link StealthPerf}.
 */
@Mixin(targets = "com.fantasticsource.dynamicstealth.common.Network$VisibilityPacket", remap = false)
public abstract class MixinDSVisibilityPacket {

    @Redirect(method = "<init>(Lnet/minecraft/entity/player/EntityPlayerMP;)V", at = @At(value = "INVOKE",
            target = "Lcom/fantasticsource/dynamicstealth/server/senses/sight/Sight;seenEntities(Lnet/minecraft/entity/player/EntityPlayerMP;)Ljava/util/LinkedHashMap;"))
    private static LinkedHashMap<Entity, Double> aqtweaks$memoSeen(EntityPlayerMP player) {
        LinkedHashMap<Entity, Double> memo = StealthPerf.seenLookup(player);
        if (memo != null) {
            StageTimer.recordNs("ds.seenMemoHit", 0L, null);
            return memo;
        }
        long t0 = StageTimer.start();
        LinkedHashMap<Entity, Double> fresh = Sight.seenEntities(player);
        StageTimer.end("ds.seenEntities", t0);
        StealthPerf.seenStore(player, fresh);
        return fresh;
    }
}
