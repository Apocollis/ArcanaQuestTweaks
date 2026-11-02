package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;

/** Builds and throttles the comfort packets. Client classes are not referenced here. */
public final class ComfortFeedback {

    public static final int EVENT_NONE = 0;
    public static final int EVENT_ENTERED = 1;
    public static final int EVENT_PROMOTED = 2;
    public static final int EVENT_DEMOTED = 3;
    public static final int EVENT_LOST_FURNITURE = 4;
    public static final int EVENT_HURT = 5;
    public static final int EVENT_ATTACK = 6;
    public static final int EVENT_ALMOST = 7;

    private static final String TOASTED = "AQTComfortToasted";
    private static final Map<UUID, Integer> LAST_HASH = new HashMap<>();
    private static final Map<UUID, Long> LAST_TICK = new HashMap<>();
    private static final Map<UUID, Boolean> WAS_HOME = new HashMap<>();
    private static final Map<UUID, Integer> LAST_RESTED = new HashMap<>();

    private ComfortFeedback() {}

    public static void clearPlayer(UUID id) {
        LAST_HASH.remove(id);
        LAST_TICK.remove(id);
        WAS_HOME.remove(id);
        LAST_RESTED.remove(id);
    }

    public static void sendLogin(EntityPlayerMP player) {
        sendCatalog(player);
        ComfortSystemHandler.ComfortEval eval = ComfortSystemHandler.evaluate(player);
        send(player, eval, EVENT_NONE, true);
    }

    public static void sendCatalog(EntityPlayerMP player) {
        List<PacketComfortCatalog.Row> rows = catalogRows();
        int chunk = 120;
        if (rows.isEmpty()) {
            ArcanaQuestTweaks.NETWORK.sendTo(new PacketComfortCatalog(0, true, rows), player);
            return;
        }
        for (int i = 0; i < rows.size(); i += chunk) {
            int end = Math.min(rows.size(), i + chunk);
            boolean last = end >= rows.size();
            ArcanaQuestTweaks.NETWORK.sendTo(
                    new PacketComfortCatalog(i, last, rows.subList(i, end)), player);
        }
    }

    public static void sendPlaced(EntityPlayerMP player, BlockPos pos, boolean addsScore) {
        ArcanaQuestTweaks.NETWORK.sendTo(new PacketComfortPlaced(pos, addsScore), player);
    }

    public static void send(EntityPlayer player, ComfortSystemHandler.ComfortEval eval, int eventCode, boolean force) {
        if (!(player instanceof EntityPlayerMP mp) || eval == null) return;
        if (player.world == null || player.world.isRemote) return;
        UUID id = player.getUniqueID();
        boolean home = eval.furniture || eval.currentlyResting;
        if (!home) {
            // Away from home the client still needs the Rested values for the effect tooltip.
            int restedHash = restedHash(eval);
            Integer lastRested = LAST_RESTED.get(id);
            boolean transition = Boolean.TRUE.equals(WAS_HOME.remove(id));
            if (transition || eventCode != EVENT_NONE || force
                    || lastRested == null || lastRested.intValue() != restedHash) {
                PacketComfortStatus packet = cleared(eventCode);
                packet.insight = eval.insight;
                packet.vigorInterval = eval.vigorInterval;
                packet.halfHearts = eval.halfHearts;
                packet.halfFeathers = eval.halfFeathers;
                packet.restedTicks = eval.restedTicks;
                ArcanaQuestTweaks.NETWORK.sendTo(packet, mp);
                LAST_RESTED.put(id, restedHash);
                LAST_HASH.put(id, 0);
                LAST_TICK.put(id, player.world.getTotalWorldTime());
            }
            return;
        }
        LAST_RESTED.remove(id);
        int toast = toastTier(player, eventCode, eval.grantedBand);
        int almost = eventCode;
        if (almost == EVENT_NONE && isAlmost(eval)) {
            almost = EVENT_ALMOST;
        }
        PacketComfortStatus packet = fromEval(eval, almost, toast);
        int hash = hash(packet);
        long now = player.world.getTotalWorldTime();
        Integer previous = LAST_HASH.get(id);
        Long lastTick = LAST_TICK.get(id);
        boolean changed = previous == null || previous.intValue() != hash;
        if (!force && !changed) return;
        if (!force && almost == EVENT_NONE && lastTick != null && now - lastTick < 10L) return;
        LAST_HASH.put(id, hash);
        LAST_TICK.put(id, now);
        WAS_HOME.put(id, Boolean.TRUE);
        ArcanaQuestTweaks.NETWORK.sendTo(packet, mp);
    }

    private static int restedHash(ComfortSystemHandler.ComfortEval eval) {
        int hash = 17;
        hash = 31 * hash + Float.floatToIntBits(eval.insight);
        hash = 31 * hash + eval.vigorInterval;
        hash = 31 * hash + eval.halfHearts;
        hash = 31 * hash + eval.halfFeathers;
        return hash;
    }

    private static int toastTier(EntityPlayer player, int eventCode, int granted) {
        if (granted < 1) return 0;
        if (eventCode != EVENT_ENTERED && eventCode != EVENT_PROMOTED) return 0;
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        int toasted = persisted.getInteger(TOASTED);
        if (toasted >= granted) return 0;
        persisted.setInteger(TOASTED, granted);
        return granted;
    }

    private static boolean isAlmost(ComfortSystemHandler.ComfortEval eval) {
        float next = eval.scoreBand <= 0 ? ComfortSystemHandler.THRESHOLD_HOMESTEAD_1
                : eval.scoreBand == 1 ? ComfortSystemHandler.THRESHOLD_HOMESTEAD_2
                : eval.scoreBand == 2 ? ComfortSystemHandler.THRESHOLD_HOMESTEAD_3
                : -1.0f;
        if (next < 0.0f) return false;
        float gap = next - eval.effective;
        return gap > 0.0f && gap <= 5.0f && eval.furniture;
    }

    private static PacketComfortStatus cleared(int eventCode) {
        PacketComfortStatus packet = new PacketComfortStatus();
        packet.eventCode = eventCode;
        return packet;
    }

    private static PacketComfortStatus fromEval(ComfortSystemHandler.ComfortEval eval, int eventCode, int toast) {
        PacketComfortStatus packet = new PacketComfortStatus();
        packet.state = eval.currentlyResting ? 2 : (eval.furniture ? 1 : 0);
        packet.granted = eval.grantedBand;
        packet.scoreTier = eval.scoreBand;
        packet.effective = eval.effective;
        packet.cozy = eval.cozy;
        packet.bonus = eval.bonuses;
        packet.penalty = eval.penalties;
        packet.threshold1 = ComfortSystemHandler.THRESHOLD_HOMESTEAD_1;
        packet.threshold2 = ComfortSystemHandler.THRESHOLD_HOMESTEAD_2;
        packet.threshold3 = ComfortSystemHandler.THRESHOLD_HOMESTEAD_3;
        packet.promoteTicks = eval.promoteInTicks < 0L ? -1 : (int) eval.promoteInTicks;
        packet.missingFurniture = !eval.furniture;
        packet.hurtTicks = (int) eval.hurtRemain;
        packet.attackTicks = (int) eval.attackRemain;
        packet.insight = eval.insight;
        packet.vigorInterval = eval.vigorInterval;
        packet.halfHearts = eval.halfHearts;
        packet.halfFeathers = eval.halfFeathers;
        packet.restedTicks = eval.restedTicks;
        packet.eventCode = eventCode;
        packet.toastTier = toast;
        addPenalty(packet, eval.heatPenalty ? 0 : 1, eval.tempPenalty, "");
        addPenalty(packet, 2, eval.thirstPenalty, "");
        addPenalty(packet, 3, eval.hungerPenalty, "");
        addPenalty(packet, 4, eval.healthPenalty, "");
        if (eval.activePenaltyEffects != null) {
            for (ComfortSettings.PotionModifier modifier : eval.activePenaltyEffects) {
                if (modifier == null) continue;
                addPenalty(packet, 5, modifier.amount, modifier.potion);
            }
        }
        if (eval.restBonus > 0.0f) {
            addPenalty(packet, 6, eval.restBonus, "");
        }
        if (eval.categorySeen != null) {
            for (Map.Entry<String, Integer> entry : eval.categorySeen.entrySet()) {
                PacketComfortStatus.Category category = new PacketComfortStatus.Category();
                category.name = entry.getKey();
                category.seen = entry.getValue() == null ? 0 : entry.getValue();
                Integer counted = eval.categoryCounted.get(entry.getKey());
                category.counted = counted == null ? 0 : counted;
                category.limit = ComfortSystemHandler.CATEGORY_LIMITS.getOrDefault(entry.getKey(), 1);
                packet.categories.add(category);
            }
        }
        return packet;
    }

    private static void addPenalty(PacketComfortStatus packet, int kind, float amount, String potion) {
        if (amount == 0.0f || packet.lines.size() >= 8) return;
        PacketComfortStatus.Line line = new PacketComfortStatus.Line();
        line.kind = kind;
        line.amount = amount;
        line.potion = potion == null ? "" : potion;
        packet.lines.add(line);
    }

    private static int hash(PacketComfortStatus packet) {
        int hash = 17;
        hash = 31 * hash + packet.state;
        hash = 31 * hash + packet.granted;
        hash = 31 * hash + packet.scoreTier;
        hash = 31 * hash + Float.floatToIntBits(packet.effective);
        hash = 31 * hash + Float.floatToIntBits(packet.bonus);
        hash = 31 * hash + Float.floatToIntBits(packet.penalty);
        hash = 31 * hash + packet.promoteTicks;
        hash = 31 * hash + (packet.missingFurniture ? 1 : 0);
        hash = 31 * hash + packet.hurtTicks / 20;
        hash = 31 * hash + packet.attackTicks / 20;
        hash = 31 * hash + packet.restedTicks / 20;
        hash = 31 * hash + packet.eventCode;
        hash = 31 * hash + packet.toastTier;
        for (PacketComfortStatus.Line line : packet.lines) {
            hash = 31 * hash + line.kind;
            hash = 31 * hash + Float.floatToIntBits(line.amount);
        }
        return hash;
    }

    private static List<PacketComfortCatalog.Row> catalogRows() {
        List<PacketComfortCatalog.Row> rows = new ArrayList<>();
        for (Map.Entry<String, ComfortSystemHandler.CozyConfig> entry : ComfortSystemHandler.COZY_BLOCKS.entrySet()) {
            ComfortSystemHandler.CozyConfig config = entry.getValue();
            if (config == null) continue;
            PacketComfortCatalog.Row row = new PacketComfortCatalog.Row();
            row.id = entry.getKey();
            row.category = config.category;
            row.family = ComfortFamilies.family(entry.getKey());
            row.weight = config.weight;
            row.limit = ComfortSystemHandler.CATEGORY_LIMITS.getOrDefault(config.category, 1);
            rows.add(row);
        }
        return rows;
    }

    static int restedTicks(EntityPlayer player) {
        PotionEffect effect = player.getActivePotionEffect(PotionHomestead.INSTANCE);
        return effect == null ? 0 : effect.getDuration();
    }
}
