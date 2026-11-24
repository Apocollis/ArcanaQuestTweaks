package com.apocollis.aqtweaks.starter;

import codersafterdark.reskillable.api.ReskillableRegistries;
import codersafterdark.reskillable.api.data.PlayerData;
import codersafterdark.reskillable.api.data.PlayerDataHandler;
import codersafterdark.reskillable.api.data.PlayerSkillInfo;
import codersafterdark.reskillable.api.skill.Skill;
import codersafterdark.reskillable.base.ExperienceHelper;
import com.apocollis.aqtweaks.comfort.home.HomeRegistry;
import com.apocollis.aqtweaks.reskillable.PerkToggleEvents;
import com.apocollis.aqtweaks.reskillable.PerkToggleState;
import com.apocollis.aqtweaks.reskillable.ReskillableBonuses;
import com.apocollis.aqtweaks.reskillable.ReskillableModule;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraft.util.EnumParticleTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Tabula Rasa draught on the server: refunds the XP of purchased skill levels, clears skills, perks and
 * class-tied state, then reopens the class menu without a second kit.
 */
public final class StarterResetService {

    private static final Logger LOG = LogManager.getLogger("aqtweaks");

    private StarterResetService() {}

    /** True when the draught may be started: no menu in progress and at least one valid class. */
    public static boolean canStartUse(EntityPlayerMP player) {
        if (StarterLockdown.isLocked(player) || StarterModule.isChoosing(player.getUniqueID())) return false;
        StarterClassRegistry.ensureLoaded();
        if (!StarterClassRegistry.hasValid()) {
            player.sendStatusMessage(new TextComponentTranslation("aqtweaks.starter.nomenu"), true);
            return false;
        }
        return PlayerDataHandler.get(player) != null;
    }

    /** @return true when the reset ran, so the caller may consume the item */
    public static boolean applyOnDrinkFinish(EntityPlayerMP player) {
        if (!canStartUse(player)) return false;
        try {
            PlayerData data = PlayerDataHandler.get(player);
            StarterClassDef previous = StarterClassRegistry.find(StarterState.classId(player));

            int refund = 0;
            for (PlayerSkillInfo info : data.getAllSkillInfo()) {
                Skill skill = info.skill;
                int target = previous == null ? 1 : previous.skills.getOrDefault(skill.getRegistryName().toString(), 1);
                int base = StarterResetMath.baseLevel(target, skill.getCap());
                refund = StarterResetMath.addClamped(refund, StarterResetMath.refundXpForSkill(
                        info.getLevel(), base, skill::getLevelUpCost, ExperienceHelper::getExperienceForLevel));
            }
            for (PlayerSkillInfo info : data.getAllSkillInfo()) {
                info.setLevel(1);
                info.respec();
            }

            HomeRegistry.setHearthkeeper(player.getUniqueID(), false);
            PerkToggleState.clearAll(player);
            ReskillableBonuses.invalidateLevels(player);
            data.saveAndSync();
            ReskillableModule.applyAttributes(player);
            PerkToggleEvents.sync(player);

            if (!player.capabilities.isCreativeMode && refund > 0) {
                ExperienceHelper.addPlayerXP(player, refund);
            }

            StarterState.resetForReawakening(player);
            effects(player);
            player.sendStatusMessage(new TextComponentTranslation("aqtweaks.starter.reset_used")
                    .setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.AQUA)), true);
            StarterModule.reawaken(player);
            return true;
        } catch (RuntimeException e) {
            LOG.error("[starter classes] Tabula Rasa failed for {}", player.getName(), e);
            return false;
        }
    }

    private static void effects(EntityPlayerMP player) {
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 0.4F, 1.6F);
        if (player.world instanceof WorldServer ws) {
            ws.spawnParticle(EnumParticleTypes.END_ROD, player.posX, player.posY + 1.0, player.posZ,
                    40, 0.4, 0.8, 0.4, 0.05);
        }
    }
}
