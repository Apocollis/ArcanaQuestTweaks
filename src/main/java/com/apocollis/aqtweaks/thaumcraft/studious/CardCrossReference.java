package com.apocollis.aqtweaks.thaumcraft.studious;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** Adds a small amount to every category that already carries progress (up to a cap): rewards broad theories. */
public class CardCrossReference extends TheorycraftCard {

    @Override
    public boolean initialize(EntityPlayer player, ResearchTableData data) {
        return StudiousModule.isScholar(player)
                && StudiousMath.crossReferenceEligible(data.categoryTotals,
                        StudiousModule.config().crossReferenceMinCategories);
    }

    @Override
    public boolean isAidOnly() {
        return true;
    }

    @Override
    public int getInspirationCost() {
        return StudiousModule.config().crossReferenceInspirationCost;
    }

    @Override
    public String getLocalizedName() {
        return new TextComponentTranslation("aqtweaks.card.cross_reference.name").getFormattedText();
    }

    @Override
    public String getLocalizedText() {
        return new TextComponentTranslation("aqtweaks.card.cross_reference.text",
                StudiousModule.config().crossReferenceBonus, StudiousModule.config().crossReferenceMaxCategories)
                .getFormattedText();
    }

    @Override
    public boolean activate(EntityPlayer player, ResearchTableData data) {
        List<String> targets = StudiousMath.crossReferenceTargets(data.categoryTotals,
                StudiousModule.config().crossReferenceMaxCategories);
        if (targets.isEmpty()) {
            return false;
        }
        for (String category : targets) {
            data.addTotal(category, StudiousModule.config().crossReferenceBonus);
        }
        return true;
    }
}
