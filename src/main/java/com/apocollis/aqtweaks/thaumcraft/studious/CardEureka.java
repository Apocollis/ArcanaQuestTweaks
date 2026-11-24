package com.apocollis.aqtweaks.thaumcraft.studious;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/** Offered once one category leads: a bonus to that leading category, for a lower inspiration cost. */
public class CardEureka extends TheorycraftCard {

    @Override
    public boolean initialize(EntityPlayer player, ResearchTableData data) {
        return StudiousModule.isScholar(player)
                && StudiousMath.eurekaEligible(data.categoryTotals, StudiousModule.config().eurekaLeadThreshold);
    }

    @Override
    public boolean isAidOnly() {
        return true;
    }

    @Override
    public int getInspirationCost() {
        return StudiousModule.config().eurekaInspirationCost;
    }

    @Override
    public String getLocalizedName() {
        return new TextComponentTranslation("aqtweaks.card.eureka.name").getFormattedText();
    }

    @Override
    public String getLocalizedText() {
        return new TextComponentTranslation("aqtweaks.card.eureka.text", StudiousModule.config().eurekaBonus)
                .getFormattedText();
    }

    @Override
    public boolean activate(EntityPlayer player, ResearchTableData data) {
        String lead = StudiousMath.leadingCategory(data.categoryTotals);
        if (lead == null) {
            return false;
        }
        data.addTotal(lead, StudiousModule.config().eurekaBonus);
        return true;
    }
}
