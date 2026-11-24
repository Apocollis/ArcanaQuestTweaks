package com.apocollis.aqtweaks.starter;

import codersafterdark.reskillable.api.ReskillableRegistries;
import com.apocollis.aqtweaks.reskillable.ClassPerkSupport;
import com.brandon3055.csg.DataManager;
import java.util.List;
import net.minecraft.util.ResourceLocation;

/** The real game behind the validator: Reskillable skills, CSG kits and the Tweaks class perks. */
final class StarterEnvironment implements StarterClassValidator.Environment {

    @Override
    public boolean skillExists(String skillId) {
        if (skillId == null || skillId.isEmpty()) return false;
        return ReskillableRegistries.SKILLS.getValue(new ResourceLocation(skillId)) != null;
    }

    @Override
    public boolean kitExists(String kit) {
        return kit != null && DataManager.kits != null && DataManager.kits.containsKey(kit);
    }

    @Override
    public boolean isClassPerk(String perkId) {
        if (perkId == null || perkId.isEmpty()) return false;
        return ClassPerkSupport.isClassPerk(new ResourceLocation(perkId));
    }

    @Override
    public List<String> kitDrift(String kit) {
        return StarterCsg.drift(kit);
    }
}
