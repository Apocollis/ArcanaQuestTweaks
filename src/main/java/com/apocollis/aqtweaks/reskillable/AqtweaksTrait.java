package com.apocollis.aqtweaks.reskillable;

import codersafterdark.reskillable.api.data.RequirementHolder;
import codersafterdark.reskillable.api.unlockable.Trait;
import codersafterdark.reskillable.api.unlockable.UnlockableConfig;
import net.minecraft.util.ResourceLocation;

public class AqtweaksTrait extends Trait {

    public AqtweaksTrait(String path, ReskillablePerkLayout layout) {
        super(new ResourceLocation("aqtweaks", path), layout.x, layout.y,
                new ResourceLocation(parentOf(layout)), layout.cost, reqsOf(layout));
        UnlockableConfig cfg = getUnlockableConfig();
        cfg.setEnabled(layout.enable);
        cfg.setX(layout.x);
        cfg.setY(layout.y);
        cfg.setCost(layout.cost);
        cfg.setRequirementHolder(RequirementHolder.fromStringList(reqsOf(layout)));
    }

    private static String parentOf(ReskillablePerkLayout layout) {
        if (layout.parentSkill == null || layout.parentSkill.isEmpty()) {
            return "reskillable:attack";
        }
        return layout.parentSkill;
    }

    private static String[] reqsOf(ReskillablePerkLayout layout) {
        return layout.requirements != null ? layout.requirements : new String[0];
    }
}
