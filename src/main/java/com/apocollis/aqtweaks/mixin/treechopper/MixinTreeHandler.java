package com.apocollis.aqtweaks.mixin.treechopper;

import com.apocollis.aqtweaks.reskillable.HarvestActor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import treechopper.common.handler.TreeHandler;

/**
 * Tree Chopper fells a tree with {@code World.destroyBlock(pos, true)} for every log and leaf, and that
 * path raises {@code HarvestDropsEvent} with no harvester. Record the felling player for the
 * duration of {@code DestroyTree} so the harvest perks can still apply (see {@link HarvestActor}).
 */
@Mixin(value = TreeHandler.class, remap = false)
public abstract class MixinTreeHandler {

    @Inject(method = "DestroyTree", at = @At("HEAD"))
    private void aqtweaks$beginFell(World world, EntityPlayer player, CallbackInfo ci) {
        HarvestActor.beginFell(player);
    }

    @Inject(method = "DestroyTree", at = @At("RETURN"))
    private void aqtweaks$endFell(World world, EntityPlayer player, CallbackInfo ci) {
        HarvestActor.endFell();
    }
}
