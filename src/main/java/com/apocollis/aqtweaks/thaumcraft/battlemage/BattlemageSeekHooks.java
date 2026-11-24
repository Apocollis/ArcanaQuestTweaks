package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.common.entities.monster.EntitySpellBat;
import thaumcraft.common.entities.projectile.EntityFocusCloud;
import thaumcraft.common.entities.projectile.EntityFocusProjectile;
import thaumcraft.common.lib.utils.EntityUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * World-facing half of the Battlemage target rules (plan 2.2). The ordering and filtering decisions are in the
 * pure {@link BattlemageSeekPicker}; these methods map entities to it and back.
 */
public final class BattlemageSeekHooks {

    private static final double BAT_SEARCH_RANGE = 12.0D;

    private BattlemageSeekHooks() {}

    private static boolean active(Entity owner) {
        return owner instanceof EntityCultistBattlemage && ArcanaQuestTweaksConfig.BattlemageConfig.seekPlayersOnly;
    }

    public static boolean validPlayer(Entity e) {
        if (!(e instanceof EntityPlayer) || e.isDead) {
            return false;
        }
        EntityPlayer p = (EntityPlayer) e;
        return !p.isCreative() && !p.isSpectator();
    }

    /** True when {@code target} is a valid player and {@code caster} a Battlemage: never friendly, whatever PvP says. */
    public static boolean playerNeverFriendly(Entity caster, Entity target) {
        return active(caster) && validPlayer(target);
    }

    private static BattlemageSeekPicker.Priority priority() {
        return BattlemageSeekPicker.Priority.parse(ArcanaQuestTweaksConfig.BattlemageConfig.seekPriority);
    }

    private static EntityLivingBase attackTarget(Entity owner) {
        return owner instanceof net.minecraft.entity.EntityLiving ? ((net.minecraft.entity.EntityLiving) owner).getAttackTarget() : null;
    }

    private static List<BattlemageSeekPicker.Candidate> candidates(List<? extends Entity> players, Entity from, Entity owner) {
        EntityLivingBase wanted = attackTarget(owner);
        List<BattlemageSeekPicker.Candidate> out = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            Entity e = players.get(i);
            float health = e instanceof EntityLivingBase ? ((EntityLivingBase) e).getHealth() : 0.0F;
            out.add(new BattlemageSeekPicker.Candidate(i, e.getDistanceSq(from), health, e == wanted));
        }
        return out;
    }

    /** Seek Hostiles scan list for a Battlemage-owned projectile: valid players only, in priority order. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List seekList(EntityFocusProjectile projectile, List raw) {
        EntityLivingBase owner = projectile.getThrower();
        if (projectile.getSpecial() != 2 || !active(owner)) {
            return raw;
        }
        List<EntityPlayer> players = new ArrayList<>();
        for (Object o : raw) {
            if (validPlayer((Entity) o)) {
                players.add((EntityPlayer) o);
            }
        }
        List<EntityPlayer> out = new ArrayList<>();
        for (int idx : BattlemageSeekPicker.order(candidates(players, projectile, owner), priority())) {
            out.add(players.get(idx));
        }
        return out;
    }

    /** True when a Battlemage's hostile bat should use the player-only search. */
    public static boolean handlesBat(EntitySpellBat bat) {
        return active(bat.getOwner()) && !bat.getIsFriendly();
    }

    /** The player a new bat search takes: attack target first, within 12 blocks of the bat; null when none. */
    public static EntityLivingBase pickBat(EntitySpellBat bat) {
        List<EntityPlayer> players = new ArrayList<>();
        for (EntityPlayer p : bat.world.getEntitiesWithinAABB(EntityPlayer.class,
                bat.getEntityBoundingBox().grow(BAT_SEARCH_RANGE))) {
            if (validPlayer(p) && p.getDistance(bat) <= BAT_SEARCH_RANGE) {
                players.add(p);
            }
        }
        int pick = BattlemageSeekPicker.pickOne(candidates(players, bat, bat.getOwner()), priority());
        return pick < 0 ? null : players.get(pick);
    }

    public static BattlemageSeekPicker.CloudKind cloudKind(FocusPackage pkg) {
        boolean heal = false;
        boolean other = false;
        if (pkg != null) {
            for (FocusEffect fx : pkg.getFocusEffects()) {
                if ("thaumcraft.HEAL".equals(fx.getKey())) {
                    heal = true;
                } else {
                    other = true;
                }
            }
        }
        return BattlemageSeekPicker.classifyCloud(heal, other);
    }

    /** The entity list a Battlemage-owned cloud iterates (plan 2.2 "Cloud mixin"). */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List cloudTargets(EntityFocusCloud cloud, BattlemageSeekPicker.CloudKind kind, List raw) {
        EntityLivingBase owner = cloud.getOwner();
        if (!active(owner)) {
            return raw;
        }
        List out = new ArrayList();
        for (Object o : raw) {
            Entity e = (Entity) o;
            boolean friendlyNonPlayer = !(e instanceof EntityPlayer) && owner != null && EntityUtils.isFriendly(owner, e);
            if (BattlemageSeekPicker.keepInCloud(kind, e instanceof EntityFocusCloud, validPlayer(e), e == owner,
                    friendlyNonPlayer)) {
                out.add(e);
            }
        }
        return out;
    }
}
