package com.apocollis.aqtweaks.starter;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.network.PacketOpenStarterClassGui;
import com.apocollis.aqtweaks.network.PacketStarterClassResult;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Presents the starter class menu to a new character, locks them down until they choose, applies the class and ends
 * the lockdown after the reveal (plan 12.3 to 12.10). Server side only; registered when Reskillable and
 * CustomStartingGear are both present.
 */
public final class StarterModule {

    private static final Logger LOG = LogManager.getLogger("aqtweaks");

    /** One player in the menu flow. Not saved: the persisted pending flag restarts it on the next login. */
    private static final class Session {
        int ticks;
        boolean reveal;
        long deadline;
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new StarterModule());
        MinecraftForge.EVENT_BUS.register(new StarterLockdown.Events());
    }

    // ---- events -----------------------------------------------------------------------------------------

    /** Highest priority, so the CSG flag is read before CSG's own handler sets it on the first login. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP) || event.player.world.isRemote) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        try {
            StarterClassRegistry.ensureLoaded();
            if (StarterState.isChosen(player)) {
                StarterReward.regrantIfMissing(player);
                return;
            }
            if (!StarterRules.shouldPrompt(StarterState.isPending(player), false, StarterState.csgReceived(player))) {
                return;
            }
            StarterState.setPending(player, true);
            if (!StarterClassRegistry.hasValid()) {
                warnOperator(player);
                return;
            }
            beginSelection(player);
        } catch (RuntimeException e) {
            // Never trap a player because of a starter bug.
            LOG.error("[starter classes] login handling failed for {}", player.getName(), e);
            release(player, false);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.player.getUniqueID();
        StarterLockdown.unlock(id);
        SESSIONS.remove(id);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        resend(event.player);
    }

    @SubscribeEvent
    public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        resend(event.player);
    }

    private static void resend(net.minecraft.entity.player.EntityPlayer player) {
        Session s = SESSIONS.get(player.getUniqueID());
        if (s == null || s.reveal) return;
        StarterLockdown.lock(player);
        s.ticks = StarterRules.FIRST_SEND_TICKS - 1;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        long now = server.getTickCounter();
        for (Map.Entry<UUID, Session> e : SESSIONS.entrySet()) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(e.getKey());
            if (player == null) {
                StarterLockdown.unlock(e.getKey());
                SESSIONS.remove(e.getKey());
                continue;
            }
            Session s = e.getValue();
            if (!StarterClassRegistry.hasValid()) {
                // A reload left no valid class: do not keep anyone locked.
                release(player, true);
                continue;
            }
            if (s.reveal) {
                if (StarterRules.shouldRelease(now, s.deadline, false)) {
                    release(player, true);
                }
            } else {
                s.ticks++;
                if (StarterRules.shouldSendOpen(s.ticks)) {
                    sendOpen(player);
                }
            }
        }
    }

    // ---- flow -------------------------------------------------------------------------------------------

    /** Locks the player and starts presenting the menu. The pending flag must already be set. */
    static void beginSelection(EntityPlayerMP player) {
        StarterLockdown.lock(player);
        SESSIONS.put(player.getUniqueID(), new Session());
    }

    private static void sendOpen(EntityPlayerMP player) {
        StarterSettings s = StarterClassRegistry.settings();
        boolean again = StarterState.isNoKit(player);
        ArcanaQuestTweaks.NETWORK.sendTo(new PacketOpenStarterClassGui(
                StarterRules.pickText(again, s.intro, s.resetIntro),
                StarterRules.pickText(again, s.prompt, s.resetPrompt),
                StarterClassRegistry.views()), player);
    }

    /** Tabula Rasa: locks the player where they stand and opens the menu now. The reawakening flags are already set. */
    static void reawaken(EntityPlayerMP player) {
        beginSelection(player);
        Session s = SESSIONS.get(player.getUniqueID());
        if (s != null) s.ticks = StarterRules.FIRST_SEND_TICKS;
        sendOpen(player);
    }

    /** Called on the server thread from {@code PacketSelectStarterClass}. */
    public static void onSelect(EntityPlayerMP player, String classId) {
        Session s = SESSIONS.get(player.getUniqueID());
        if (s == null || s.reveal || !StarterLockdown.isLocked(player)) return;
        StarterClassDef def = StarterClassRegistry.find(classId);
        if (def == null) {
            player.sendMessage(new TextComponentTranslation("aqtweaks.starter.failed").setStyle(
                    new net.minecraft.util.text.Style().setColor(TextFormatting.RED)));
            LOG.error("[starter classes] {} chose unknown class '{}'", player.getName(), classId);
            return;
        }
        StarterResult result;
        try {
            result = StarterReward.apply(player, def);
        } catch (RuntimeException e) {
            LOG.error("[starter classes] applying class '{}' to {} failed", def.id, player.getName(), e);
            player.sendMessage(new TextComponentTranslation("aqtweaks.starter.failed").setStyle(
                    new net.minecraft.util.text.Style().setColor(TextFormatting.RED)));
            return;
        }
        MinecraftServer server = player.getServer();
        long now = server == null ? 0L : server.getTickCounter();
        s.reveal = true;
        s.deadline = StarterRules.revealDeadline(now, StarterClassRegistry.settings().revealHoldSeconds);
        ArcanaQuestTweaks.NETWORK.sendTo(new PacketStarterClassResult(result), player);
        player.sendMessage(new TextComponentTranslation("aqtweaks.starter.chosen", def.name));
    }

    /** Called on the server thread from {@code PacketStarterRevealDone}. */
    public static void onRevealDone(EntityPlayerMP player) {
        Session s = SESSIONS.get(player.getUniqueID());
        if (s != null && s.reveal) {
            release(player, false);
        }
    }

    /** Ends the lockdown. {@code closeClient} also tells the client to close any starter screen. */
    static void release(EntityPlayerMP player, boolean closeClient) {
        StarterLockdown.unlock(player.getUniqueID());
        SESSIONS.remove(player.getUniqueID());
        if (closeClient) {
            ArcanaQuestTweaks.NETWORK.sendTo(
                    new PacketOpenStarterClassGui("", "", new ArrayList<StarterClassView>()), player);
        }
    }

    /** {@code /aqclass skip}: clears the pending flag without choosing and lets the player go. */
    public static void skip(EntityPlayerMP player) {
        StarterState.setPending(player, false);
        StarterState.setNoKit(player, false);
        release(player, true);
    }

    /** {@code /aqclass reset}: a new character again. Shows the menu when classes are available. */
    public static boolean reset(EntityPlayerMP player) {
        StarterClassRegistry.ensureLoaded();
        StarterState.reset(player);
        release(player, false);
        if (!StarterClassRegistry.hasValid()) return false;
        beginSelection(player);
        return true;
    }

    public static boolean isChoosing(UUID id) {
        return SESSIONS.containsKey(id);
    }

    private static void warnOperator(EntityPlayerMP player) {
        MinecraftServer server = player.getServer();
        LOG.error("[starter classes] {} is a new character but there is no valid class; the menu is off", player.getName());
        if (server != null && server.getPlayerList().canSendCommands(player.getGameProfile())) {
            player.sendMessage(new TextComponentTranslation("aqtweaks.starter.nomenu").setStyle(
                    new net.minecraft.util.text.Style().setColor(TextFormatting.RED)));
        }
    }
}
