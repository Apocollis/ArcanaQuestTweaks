package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.client.PacketProspectorMarks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Ore positions counted by one prospecting-pick click. Server thread only. */
public final class ProspectorSample {

    private static final int CAP = 8192;
    private static final List<BlockPos> marks = new ArrayList<>();
    private static EntityPlayer player;
    private static BlockPos pos;

    private ProspectorSample() {}

    public static void begin(EntityPlayer clicker) {
        player = clicker;
        pos = null;
        marks.clear();
    }

    public static EntityPlayer player() {
        return player;
    }

    public static void pos(BlockPos scanned) {
        pos = scanned;
    }

    public static void note() {
        if (pos == null || marks.size() >= CAP) return;
        marks.add(pos.toImmutable());
    }

    public static void finish() {
        EntityPlayer clicker = player;
        player = null;
        pos = null;
        if (clicker == null || clicker.world.isRemote || !(clicker instanceof EntityPlayerMP)) {
            marks.clear();
            return;
        }
        if (!PerkAccess.on(clicker, "aqtweaks:prospector",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.prospector.enable)) {
            marks.clear();
            return;
        }
        List<BlockPos> copy = new ArrayList<>(marks);
        marks.clear();
        ArcanaQuestTweaks.NETWORK.sendTo(new PacketProspectorMarks(copy), (EntityPlayerMP) clicker);
    }
}
