package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Last comfort packet. Client only; the server never loads this class. */
@SideOnly(Side.CLIENT)
public final class ComfortClientState {

    public static final class CategoryScore {
        public String name = "";
        public float amount;
        public int seen;
        public int counted;
        public int limit;
    }

    public static boolean present;
    public static int state;
    public static int granted;
    public static int scoreTier;
    public static float effective;
    public static float cozy;
    public static float bonus;
    public static float penalty;
    public static float threshold1 = 24.0f;
    public static float threshold2 = 48.0f;
    public static float threshold3 = 66.0f;
    public static int promoteTicks = -1;
    public static boolean missingFurniture;
    public static int hurtTicks;
    public static int attackTicks;
    public static float insight;
    public static int vigorInterval;
    public static int halfHearts;
    public static int halfFeathers;
    public static int restedTicks;
    public static PacketComfortStatus.Line[] lines = new PacketComfortStatus.Line[0];
    public static final Map<String, int[]> categories = new HashMap<>();
    public static final List<CategoryScore> categoryScores = new ArrayList<>();
    public static final Map<String, PacketComfortCatalog.Row> catalog = new HashMap<>();
    public static long changedAt;

    private static int lastEvent = -1;
    private static long lastEventAt;
    private static long lastAlmostAt;

    private ComfortClientState() {}

    public static boolean messagesOn() {
        return ArcanaQuestTweaksConfig.ClientModuleConfig.comfort.comfortMessages;
    }

    public static void acceptCatalog(PacketComfortCatalog message) {
        if (message.index == 0) catalog.clear();
        for (PacketComfortCatalog.Row row : message.rows) {
            catalog.put(row.id, row);
        }
    }

    public static void acceptStatus(PacketComfortStatus message) {
        boolean wasPresent = present;
        int previousGranted = granted;
        present = message.state != 0;
        state = message.state;
        granted = message.granted;
        scoreTier = message.scoreTier;
        effective = message.effective;
        cozy = message.cozy;
        bonus = message.bonus;
        penalty = message.penalty;
        threshold1 = message.threshold1;
        threshold2 = message.threshold2;
        threshold3 = message.threshold3;
        promoteTicks = message.promoteTicks;
        missingFurniture = message.missingFurniture;
        hurtTicks = message.hurtTicks;
        attackTicks = message.attackTicks;
        insight = message.insight;
        vigorInterval = message.vigorInterval;
        halfHearts = message.halfHearts;
        halfFeathers = message.halfFeathers;
        restedTicks = message.restedTicks;
        lines = message.lines.toArray(new PacketComfortStatus.Line[0]);
        categories.clear();
        categoryScores.clear();
        for (PacketComfortStatus.Category category : message.categories) {
            categories.put(category.name, new int[] {category.seen, category.counted, category.limit});
            CategoryScore cs = new CategoryScore();
            cs.name = category.name;
            cs.amount = category.amount;
            cs.seen = category.seen;
            cs.counted = category.counted;
            cs.limit = category.limit;
            categoryScores.add(cs);
        }
        categoryScores.sort((a, b) -> Float.compare(b.amount, a.amount));
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (wasPresent != present || (present && previousGranted != granted)) {
            changedAt = player == null ? 0L : player.ticksExisted;
        }
        if (message.toastTier > 0 && messagesOn()) {
            Minecraft.getMinecraft().getToastGui().add(new ComfortToast(message.toastTier));
        }
        if (message.eventCode != 0) {
            actionBar(message.eventCode, player);
        }
    }

    public static void acceptPlaced(PacketComfortPlaced message) {
        if (!messagesOn()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;
        EnumParticleTypes type = message.addsScore ? EnumParticleTypes.VILLAGER_HAPPY : EnumParticleTypes.SMOKE_NORMAL;
        for (int i = 0; i < 6; i++) {
            double ox = (mc.world.rand.nextDouble() - 0.5) * 0.6;
            double oy = mc.world.rand.nextDouble() * 0.4;
            double oz = (mc.world.rand.nextDouble() - 0.5) * 0.6;
            mc.world.spawnParticle(type,
                    message.pos.getX() + 0.5 + ox,
                    message.pos.getY() + 0.5 + oy,
                    message.pos.getZ() + 0.5 + oz,
                    0, 0.02, 0);
        }
    }

    public static boolean panelVisible() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.gameSettings.hideGUI || mc.player.isSpectator()) return false;
        if (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) return false;
        if (!messagesOn()) return false;
        if (present) return true;
        return changedAt > 0L && mc.player.ticksExisted - changedAt < 160;
    }

    private static void actionBar(int eventCode, EntityPlayerSP player) {
        if (!messagesOn() || player == null) return;
        long now = player.ticksExisted;
        if (eventCode == ComfortFeedback.EVENT_ALMOST) {
            if (now - lastAlmostAt < 20L * 180L) return;
            lastAlmostAt = now;
        } else if (eventCode == lastEvent && now - lastEventAt < 20L * 60L) {
            return;
        }
        lastEvent = eventCode;
        lastEventAt = now;
        String key;
        int tier = Math.max(1, scoreTier);
        if (eventCode == ComfortFeedback.EVENT_ENTERED) key = "aqtweaks.comfort.entered";
        else if (eventCode == ComfortFeedback.EVENT_PROMOTED) key = "aqtweaks.comfort.promoted";
        else if (eventCode == ComfortFeedback.EVENT_DEMOTED) key = "aqtweaks.comfort.demoted";
        else if (eventCode == ComfortFeedback.EVENT_LOST_FURNITURE) key = "aqtweaks.comfort.lost_furniture";
        else if (eventCode == ComfortFeedback.EVENT_HURT) key = "aqtweaks.comfort.hurt";
        else if (eventCode == ComfortFeedback.EVENT_ATTACK) key = "aqtweaks.comfort.attack";
        else if (eventCode == ComfortFeedback.EVENT_ALMOST) key = "aqtweaks.comfort.almost";
        else return;
        Minecraft.getMinecraft().ingameGUI.setOverlayMessage(
                new TextComponentTranslation(key, roman(tier)).getUnformattedText(), false);
    }

    public static String roman(int tier) {
        if (tier >= 3) return "III";
        if (tier == 2) return "II";
        if (tier == 1) return "I";
        return "";
    }

    public static String lineLabel(PacketComfortStatus.Line line) {
        switch (line.kind) {
            case 0: return "Heat";
            case 1: return "Cold";
            case 2: return "Thirst";
            case 3: return "Hunger";
            case 4: return "Health";
            case 6: return "Resting";
            case 7:
            case 5:
                if (line.potion != null && !line.potion.isEmpty()) {
                    Potion potion = Potion.getPotionFromResourceLocation(line.potion);
                    if (potion != null) {
                        return net.minecraft.client.resources.I18n.format(potion.getName());
                    }
                    int colon = line.potion.indexOf(':');
                    String name = colon >= 0 ? line.potion.substring(colon + 1) : line.potion;
                    return Character.toUpperCase(name.charAt(0)) + name.substring(1).replace('_', ' ');
                }
                return line.kind == 7 ? "Bonus" : "Penalty";
            default: return "Effect";
        }
    }
}
