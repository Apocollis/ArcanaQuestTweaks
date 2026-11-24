package com.apocollis.aqtweaks.reskillable;

import codersafterdark.reskillable.api.data.RequirementHolder;
import codersafterdark.reskillable.api.event.LockUnlockableEvent;
import codersafterdark.reskillable.api.unlockable.Unlockable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Class perks (starter classes, plan 12.9): Reskillable unlockables that cost nothing, cannot be bought (their only
 * requirement is an advancement that does not exist) and cannot be given back. The starter class grants them with
 * {@code PlayerSkillInfo.unlock}, which does not check requirements.
 *
 * <p>The requirement syntax {@code adv|<resource location>} was read from {@code ReskillableAPI}. An unknown
 * advancement makes {@code AdvancementRequirement.achievedByPlayer} false and its tooltip prints an empty name, so the
 * skill screen shows the perk locked.
 */
public final class ClassPerkSupport {

    public static final String IMPOSSIBLE_REQUIREMENT = "adv|aqtweaks:class_only";

    private static final Set<String> PATHS = new LinkedHashSet<>();

    private ClassPerkSupport() {}

    /** Registers {@code path} as a free, unobtainable class perk in the layout's cell. */
    public static void register(IForgeRegistry<Unlockable> registry, String path, ReskillablePerkLayout layout) {
        ReskillablePerkLayout copy = new ReskillablePerkLayout(layout.x, layout.y, 0, layout.parentSkill);
        copy.enable = layout.enable;
        registry.register(new AqtweaksTrait(path, copy, new String[] {IMPOSSIBLE_REQUIREMENT}));
        PATHS.add(path);
    }

    public static boolean isClassPerk(ResourceLocation id) {
        return id != null && "aqtweaks".equals(id.getNamespace()) && PATHS.contains(id.getPath());
    }

    public static Set<String> paths() {
        return Collections.unmodifiableSet(PATHS);
    }

    public static void preInit() {
        MinecraftForge.EVENT_BUS.register(new ClassPerkSupport());
    }

    /** Players never give a class perk back. */
    @SubscribeEvent
    public void onLock(LockUnlockableEvent.Pre e) {
        Unlockable unlockable = e.getUnlockable();
        if (unlockable != null && isClassPerk(unlockable.getRegistryName())) {
            e.setCanceled(true);
        }
    }

    /**
     * Reskillable's own config pass can rewrite cost and requirements after construction, so the class perks are
     * stamped again on the last registry pass (cost 0, the impossible requirement only).
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void restamp(RegistryEvent.Register<Unlockable> event) {
        IForgeRegistry<Unlockable> registry = event.getRegistry();
        for (String path : PATHS) {
            Unlockable perk = registry.getValue(new ResourceLocation("aqtweaks", path));
            if (perk == null) {
                continue;
            }
            perk.getUnlockableConfig().setCost(0);
            perk.getUnlockableConfig().setRequirementHolder(
                    RequirementHolder.fromStringList(new String[] {IMPOSSIBLE_REQUIREMENT}));
        }
    }
}
