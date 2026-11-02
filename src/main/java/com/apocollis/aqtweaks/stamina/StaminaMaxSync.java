package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.simpledifficulty.PacketTemperatureMax;
import com.elenai.elenaidodge2.api.FeathersHelper;
import com.elenai.elenaidodge2.util.Utils;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Pushes the player's current maximum feathers to the client without Elenai's full config packet.
 *
 * <p>{@code FeathersHelper.syncMaxFeathers} ends with {@code Utils.updateClientConfig}, which sends the whole
 * {@code CUpdateConfigMessage} including the armor weight list. Tweaks blanks that list while a player joins (see
 * {@code StaminaModule}), and the packet can overflow on this pack's list, so it must not be sent mid-game.
 * {@link PacketTemperatureMax} only sets the client's {@code ClientStorage.maxDodges}.
 *
 * <p>The maximum is never stored: {@code Utils.getMaxDodges} posts a {@code MaxFeathersEvent} every call, so
 * SolCarrot, Fortitude and the temperature penalty are recomputed from live state each time.
 */
public final class StaminaMaxSync {

    private StaminaMaxSync() {}

    /**
     * Lowers current feathers to the maximum if above it, then sends the maximum to the client. Does not refill
     * and does not raise current feathers when the maximum rises.
     *
     * <p>The clamp uses {@code decreaseFeathers}, a spend: the cost is paid from absorption first, a
     * {@code SpendFeatherEvent} listener can change or cancel it, and it does nothing in creative or spectator.
     * Callers that care can compare {@link FeathersHelper#getFeatherLevel} with the returned maximum afterwards.
     *
     * @return the maximum that was sent
     */
    public static int sync(EntityPlayerMP player) {
        int max = Utils.getMaxDodges(player);
        int level = FeathersHelper.getFeatherLevel(player);
        if (level > max) {
            FeathersHelper.decreaseFeathers(player, level - max);
        }
        ArcanaQuestTweaks.NETWORK.sendTo(new PacketTemperatureMax(max), player);
        return max;
    }
}
