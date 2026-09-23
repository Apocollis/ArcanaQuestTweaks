package com.apocollis.aqtweaks.mixin.astral;

import com.apocollis.aqtweaks.reskillable.MagicSchoolPresence;
import hellfirepvp.astralsorcery.common.crafting.altar.AbstractAltarRecipe;
import hellfirepvp.astralsorcery.common.crafting.altar.ActiveCraftingTask;
import hellfirepvp.astralsorcery.common.tile.TileAltar;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = ActiveCraftingTask.class, remap = false)
public abstract class MixinActiveCraftingTask {

    @Inject(method = "<init>", at = @At("HEAD"))
    private void aqtweaks$pushCtor(AbstractAltarRecipe recipe, int craftTime, UUID crafter, CallbackInfo ci) {
        MagicSchoolPresence.pushAstralCrafter(playerFromUuid(crafter));
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aqtweaks$popCtor(AbstractAltarRecipe recipe, int craftTime, UUID crafter, CallbackInfo ci) {
        MagicSchoolPresence.popAstralCrafter();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void aqtweaks$pushTick(TileAltar altar, CallbackInfoReturnable<Boolean> cir) {
        MagicSchoolPresence.pushAstralCrafter(((ActiveCraftingTask) (Object) this).tryGetCraftingPlayerServer());
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void aqtweaks$popTick(TileAltar altar, CallbackInfoReturnable<Boolean> cir) {
        MagicSchoolPresence.popAstralCrafter();
    }

    private static EntityPlayer playerFromUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return null;
        }
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(uuid);
        return player;
    }
}
