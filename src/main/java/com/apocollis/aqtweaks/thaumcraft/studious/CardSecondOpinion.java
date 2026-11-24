package com.apocollis.aqtweaks.thaumcraft.studious;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/**
 * A free card (no inspiration) that widens the next draw: the next round offers extra cards, and the leading
 * category gets a small push. Thaumcraft's table marks the picked slot selected right after {@code activate}, so
 * redrawing the current set from inside the card would corrupt that selection; the extra choice is queued through
 * {@code bonusDraws} for the next draw instead (see the starter-class plan, section 13.8).
 */
public class CardSecondOpinion extends TheorycraftCard {

    @Override
    public boolean initialize(EntityPlayer player, ResearchTableData data) {
        return StudiousModule.isScholar(player);
    }

    @Override
    public boolean isAidOnly() {
        return true;
    }

    @Override
    public int getInspirationCost() {
        return StudiousModule.config().secondOpinionInspirationCost;
    }

    @Override
    public String getLocalizedName() {
        return new TextComponentTranslation("aqtweaks.card.second_opinion.name").getFormattedText();
    }

    @Override
    public String getLocalizedText() {
        return new TextComponentTranslation("aqtweaks.card.second_opinion.text",
                StudiousModule.config().secondOpinionProgress).getFormattedText();
    }

    @Override
    public boolean activate(EntityPlayer player, ResearchTableData data) {
        data.bonusDraws += Math.max(0, StudiousModule.config().secondOpinionExtraDraws);
        String lead = StudiousMath.leadingCategory(data.categoryTotals);
        int progress = StudiousModule.config().secondOpinionProgress;
        if (lead != null && progress > 0) {
            data.addTotal(lead, progress);
        }
        return true;
    }
}
