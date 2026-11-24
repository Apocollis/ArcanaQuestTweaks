package com.apocollis.aqtweaks.starter;

import codersafterdark.reskillable.api.ReskillableRegistries;
import codersafterdark.reskillable.api.data.PlayerData;
import codersafterdark.reskillable.api.data.PlayerDataHandler;
import codersafterdark.reskillable.api.data.PlayerSkillInfo;
import codersafterdark.reskillable.api.skill.Skill;
import codersafterdark.reskillable.api.unlockable.Unlockable;
import com.apocollis.aqtweaks.comfort.home.HomeRegistry;
import com.brandon3055.csg.DataManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Applies a chosen class on the server (plan 12.6, 12.9 and 13.2): skills, perk, kit, flags, commands. */
final class StarterReward {

    private static final Logger LOG = LogManager.getLogger("aqtweaks");
    private static final int GEAR_CAP = 12;

    private StarterReward() {}

    /**
     * Everything is checked first, so a bad class changes nothing. The order is skills, perk, kit, then the flags
     * and the commands. An exception before the flags are written leaves the player pending and locked.
     */
    static StarterResult apply(EntityPlayerMP player, StarterClassDef def) {
        if (DataManager.kits == null || !DataManager.kits.containsKey(def.csgKit)) {
            throw new IllegalStateException("CSG kit '" + def.csgKit + "' does not exist");
        }
        PlayerData data = PlayerDataHandler.get(player);
        if (data == null) {
            throw new IllegalStateException("no Reskillable data for " + player.getName());
        }
        for (String skillId : def.skills.keySet()) {
            if (ReskillableRegistries.SKILLS.getValue(new ResourceLocation(skillId)) == null) {
                throw new IllegalStateException("unknown skill " + skillId);
            }
        }

        List<StarterResult.SkillGain> gains = new ArrayList<>();
        for (Map.Entry<String, Integer> e : def.skills.entrySet()) {
            Skill skill = ReskillableRegistries.SKILLS.getValue(new ResourceLocation(e.getKey()));
            PlayerSkillInfo info = data.getSkillInfo(skill);
            int target = Math.min(e.getValue(), skill.getCap());
            if (info.getLevel() < target) {
                info.setLevel(target);
            }
            gains.add(new StarterResult.SkillGain(e.getKey(), displayName(e.getKey()), info.getLevel()));
        }

        grantPerk(player, data, def.perk);

        boolean noKit = StarterState.isNoKit(player);
        List<ItemStack> gear = Collections.emptyList();
        if (!noKit) {
            DataManager.givePlayerKit(player, def.csgKit);
            gear = StarterCsg.gear(def.csgKit, GEAR_CAP);
        }

        data.saveAndSync();
        StarterState.setChosen(player, def.id);
        StarterState.setNoKit(player, false);
        afterPerk(player, def.perkKey());
        runCommands(player, def, noKit);
        return new StarterResult(def.id, def.name, def.perkKey(), gains, gear);
    }

    /** Gives the stored class perk back when it is missing (login check, {@code /aqclass regrant}). */
    static boolean regrantIfMissing(EntityPlayerMP player) {
        if (!StarterState.isChosen(player)) return false;
        StarterClassDef def = StarterClassRegistry.find(StarterState.classId(player));
        if (def == null || def.perk == null || def.perk.isEmpty()) return false;
        PlayerData data = PlayerDataHandler.get(player);
        if (data == null) return false;
        boolean granted = grantPerk(player, data, def.perk);
        if (granted) {
            data.saveAndSync();
        }
        afterPerk(player, def.perkKey());
        return granted;
    }

    /** @return true when the perk was newly unlocked */
    private static boolean grantPerk(EntityPlayerMP player, PlayerData data, String perk) {
        if (perk == null || perk.isEmpty()) return false;
        Unlockable unlockable = ReskillableRegistries.UNLOCKABLES.getValue(new ResourceLocation(perk));
        if (unlockable == null) {
            LOG.error("[starter classes] perk {} is not registered, not granted to {}", perk, player.getName());
            return false;
        }
        PlayerSkillInfo info = data.getSkillInfo(unlockable.getParentSkill());
        if (info == null) {
            LOG.error("[starter classes] skill of perk {} has no info for {}", perk, player.getName());
            return false;
        }
        if (info.isUnlocked(unlockable)) return false;
        info.unlock(unlockable, player);
        return true;
    }

    /** Tells other systems what this perk means (the Homesteader's home record reads the Hearthkeeper flag). */
    private static void afterPerk(EntityPlayerMP player, String perkKey) {
        if ("hearthkeeper".equals(perkKey)) {
            HomeRegistry.setHearthkeeper(player.getUniqueID(), true);
        }
    }

    /**
     * @param noKit a Tabula Rasa reawakening: one-time commands are skipped, except {@code tc research ... add KEY},
     *              which runs only while Thaumcraft is loaded and the player does not know that research yet
     */
    private static void runCommands(EntityPlayerMP player, StarterClassDef def, boolean noKit) {
        MinecraftServer server = player.getServer();
        if (server == null || def.commands == null) return;
        for (String raw : def.commands) {
            String command = StarterRules.substitute(raw, player.getName());
            if (command.isEmpty()) continue;
            if (noKit) {
                String key = StarterRules.tcResearchAddKey(command);
                if (key == null || !Loader.isModLoaded("thaumcraft")
                        || StarterThaumcraftHelper.isResearchComplete(player, key)) {
                    continue;
                }
            }
            try {
                int ok = server.getCommandManager().executeCommand(server, command);
                if (ok <= 0) {
                    LOG.warn("[starter classes] class '{}' command did nothing: {}", def.id, command);
                }
            } catch (RuntimeException e) {
                LOG.error("[starter classes] class '{}' command failed: {}", def.id, command, e);
            }
        }
    }

    /** {@code reskillable:attack} to {@code Attack}. */
    static String displayName(String skillId) {
        int colon = skillId.indexOf(':');
        String path = colon < 0 ? skillId : skillId.substring(colon + 1);
        if (path.isEmpty()) return skillId;
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }
}
