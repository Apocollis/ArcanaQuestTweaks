package com.apocollis.aqtweaks.starter;

/** Small pure decisions of the starter module, kept apart so they can be tested (plan 12.4, 12.10, 13.2). */
public final class StarterRules {

    /** Ticks after the pending flag is set before the open packet is first sent. */
    public static final int FIRST_SEND_TICKS = 40;
    /** Ticks between repeats of the open packet while the player is still choosing. */
    public static final int RESEND_TICKS = 100;
    /** Seconds the server waits past the reveal hold before it releases the player. */
    public static final int RELEASE_GRACE_SECONDS = 5;

    private StarterRules() {}

    /**
     * Whether a logging-in player gets the menu. A chosen class never prompts again; a pending flag keeps prompting
     * (a crash or a failed screen left it set); otherwise only a character that has not yet received the CSG
     * inventory is new. A character that already had the CSG flag when the module was installed is never asked.
     */
    public static boolean shouldPrompt(boolean pending, boolean chosen, boolean csgReceivedInventory) {
        if (chosen) return false;
        if (pending) return true;
        return !csgReceivedInventory;
    }

    /** True on the tick the open packet is due: once at {@link #FIRST_SEND_TICKS}, then every {@link #RESEND_TICKS}. */
    public static boolean shouldSendOpen(int ticksSincePending) {
        if (ticksSincePending < FIRST_SEND_TICKS) return false;
        return (ticksSincePending - FIRST_SEND_TICKS) % RESEND_TICKS == 0;
    }

    /** Server tick at which a player in the reveal is released when the client never answers. */
    public static long revealDeadline(long nowTick, int holdSeconds) {
        return nowTick + (Math.max(0, holdSeconds) + RELEASE_GRACE_SECONDS) * 20L;
    }

    public static boolean shouldRelease(long nowTick, long deadlineTick, boolean revealDoneReceived) {
        return revealDoneReceived || nowTick >= deadlineTick;
    }

    /** The reawakening text when {@code reset} is set, otherwise the first-time text. */
    public static String pickText(boolean reset, String initial, String resetText) {
        return reset ? resetText : initial;
    }

    /**
     * The research key of {@code tc research <player> add <KEY>} (already substituted and without a slash), or null
     * when the command is anything else.
     */
    public static String tcResearchAddKey(String command) {
        if (command == null) return null;
        String[] parts = command.trim().split("\\s+");
        if (parts.length != 5) return null;
        if (!"tc".equalsIgnoreCase(parts[0]) || !"research".equalsIgnoreCase(parts[1])) return null;
        if (!"add".equalsIgnoreCase(parts[3])) return null;
        return parts[4];
    }

    /** Replaces {@code %player%} and drops one leading slash. */
    public static String substitute(String command, String playerName) {
        if (command == null) return "";
        String c = command.trim();
        if (c.startsWith("/")) c = c.substring(1);
        return c.replace("%player%", playerName == null ? "" : playerName);
    }
}
