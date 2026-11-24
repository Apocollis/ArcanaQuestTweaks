package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.ForagerBlocks;
import com.apocollis.aqtweaks.reskillable.PerkAccess;
import com.elenai.elenaidodge2.api.FeathersHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Server side of hold-based mining stamina (plan 20261120-1700). Real blocks (hardness above 0) are billed by how long
 * the player keeps digging: {@link MiningHold} decides when a payment is due and this class spends it. Plants
 * (hardness exactly 0) cost a flat {@code Plant Break Cost} per break, free with the Forager perk. Creative players,
 * spectators and fake players pay nothing.
 */
public final class MiningBilling {

    private static final Map<UUID, MiningHold.Session> SESSIONS = new HashMap<>();

    private MiningBilling() {}

    public static void forget(UUID id) {
        SESSIONS.remove(id);
    }

    private static boolean skip(EntityPlayer player) {
        return player == null || player.world.isRemote || player.capabilities.isCreativeMode || player.isSpectator()
                || player instanceof FakePlayer || !(player instanceof EntityPlayerMP)
                || !ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.enableMiningCost;
    }

    private static boolean forager(EntityPlayer player) {
        return ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.forager.enable
                && StaminaPerks.unlocked(player, "aqtweaks:forager");
    }

    /** The player pressed attack on a block (server {@code LeftClickBlock}): the start of a dig, also for instant breaks. */
    public static void onStartDig(EntityPlayer player, BlockPos pos) {
        touch(player, pos);
    }

    /** One server tick of a dig in progress (from the mixin on {@code PlayerInteractionManager.updateBlockRemoving}). */
    public static void onDigTick(EntityPlayerMP player, BlockPos pos) {
        touch(player, pos);
    }

    private static void touch(EntityPlayer player, BlockPos pos) {
        if (skip(player) || pos == null) {
            return;
        }
        World world = player.world;
        IBlockState state = world.getBlockState(pos);
        float hardness = state.getBlockHardness(world, pos);
        if (!MiningHold.billedByHold(hardness)) {
            return;
        }
        if (forager(player) && ForagerBlocks.isPlant(state)) {
            return;
        }
        StaminaRegenPace.markMining(player);
        boolean forage = StaminaPerks.isForageBlock(world, state);
        int interval = StaminaPerks.holdInterval(player, forage,
                ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.holdIntervalTicks);
        MiningHold.Session session = SESSIONS.computeIfAbsent(player.getUniqueID(), id -> new MiningHold.Session());
        int due = MiningHold.touch(session, world.getTotalWorldTime(),
                ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.sessionGapTicks, interval);
        if (due > 0) {
            spend((EntityPlayerMP) player, ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.holdCost * due);
        }
    }

    /** A block was broken ({@code BlockEvent.BreakEvent}): plants pay the flat price; real blocks were billed by the hold. */
    public static void onBreak(EntityPlayer player, World world, IBlockState state, BlockPos pos) {
        if (skip(player) || state == null) {
            return;
        }
        float hardness = state.getBlockHardness(world, pos);
        if (!MiningHold.flatPlant(hardness)) {
            return;
        }
        if (forager(player) && ForagerBlocks.isPlant(state)) {
            return;
        }
        spend((EntityPlayerMP) player, ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.plantBreakCost);
    }

    /** Pays {@code cost} half-feathers; when the player cannot, the usable remainder is drained and the action goes on. */
    public static void spend(EntityPlayerMP player, int cost) {
        if (cost <= 0) {
            return;
        }
        if (StaminaFeathers.hasEnoughStamina(player, cost)) {
            StaminaFeathers.decreaseFeathers(player, cost);
            return;
        }
        int usable = FeathersHelper.getFeatherLevel(player) - StaminaFeathers.getWeight(player)
                + StaminaFeathers.getAbsorptionFeathers(player);
        if (usable > 0) {
            StaminaFeathers.decreaseFeathers(player, usable);
        }
    }

    /** Usable stamina of a player in half-feathers (the number the spend code uses). */
    public static int usableStamina(EntityPlayerMP player) {
        return MiningHold.usable(FeathersHelper.getFeatherLevel(player), StaminaFeathers.getWeight(player),
                StaminaFeathers.getAbsorptionFeathers(player));
    }
}
