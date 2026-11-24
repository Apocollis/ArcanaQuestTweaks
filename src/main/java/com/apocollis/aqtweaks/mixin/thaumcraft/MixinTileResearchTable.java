package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.studious.StudiousModule;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.common.tiles.crafting.TileResearchTable;

/**
 * Studious: a Scholar starting a theory gets the virtual Studious aid added to the aids the table found
 * (that also raises the starting inspiration by one, as any aid does), and optionally a few extra-choice rounds.
 */
@Mixin(value = TileResearchTable.class, remap = false)
public abstract class MixinTileResearchTable {

    @Shadow
    public ResearchTableData data;

    @ModifyVariable(method = "startNewTheory", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Set<String> aqtweaks$studiousAid(Set<String> aids, EntityPlayer player, Set<String> aidsArg) {
        return StudiousModule.withAid(player, aids);
    }

    @Inject(method = "startNewTheory", at = @At("RETURN"))
    private void aqtweaks$studiousExtraChoice(EntityPlayer player, Set<String> aids, CallbackInfo ci) {
        if (data != null && StudiousModule.isScholar(player) && StudiousModule.config().extraChoice) {
            data.bonusDraws += Math.max(0, StudiousModule.config().extraChoiceRounds);
        }
    }
}
