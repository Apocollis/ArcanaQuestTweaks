package com.apocollis.aqtweaks.thaumcraft.studious;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.PerkAccess;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayer;
import thaumcraft.api.research.theorycraft.TheorycraftManager;

/**
 * The Scholar's Studious perk (starter-class plan section 13 and 13.8): three Scholar-only theorycraft cards
 * delivered by a virtual research aid, plus Steady Focus ({@link StudiousHooks}). Registered with Thaumcraft's
 * own {@link TheorycraftManager}; nothing here touches Thaumcraft code.
 */
public final class StudiousModule {

    public static final String PERK_ID = "aqtweaks:studious";

    private StudiousModule() {}

    public static ArcanaQuestTweaksConfig.ReskillableStarterScholarPerks config() {
        return ArcanaQuestTweaksConfig.ReskillableModuleConfig.starterScholar;
    }

    /** Registers the aid and its cards. Call from init when Thaumcraft is loaded. */
    public static void init() {
        TheorycraftManager.registerAid(new StudiousAid());
        TheorycraftManager.registerCard(CardEureka.class);
        TheorycraftManager.registerCard(CardCrossReference.class);
        TheorycraftManager.registerCard(CardSecondOpinion.class);
    }

    public static boolean isScholar(EntityPlayer player) {
        return PerkAccess.on(player, PERK_ID, ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.studious.enable);
    }

    /**
     * The aid keys for a theory the player is starting. A Scholar gets the Studious aid added to whatever aids the
     * table found, which is also the stock +1 starting inspiration an aid gives. Others get the set back unchanged.
     */
    public static Set<String> withAid(EntityPlayer player, Set<String> aids) {
        if (!isScholar(player)) {
            return aids;
        }
        Set<String> out = aids == null ? new HashSet<String>() : new HashSet<String>(aids);
        out.add(StudiousAid.KEY);
        return out;
    }
}
