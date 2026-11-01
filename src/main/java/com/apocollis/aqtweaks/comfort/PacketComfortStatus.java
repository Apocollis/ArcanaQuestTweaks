package com.apocollis.aqtweaks.comfort;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Home-context snapshot. Sent when the content changes, at most every 10 ticks. */
public class PacketComfortStatus implements IMessage {

    public int state;
    public int granted;
    public int scoreTier;
    public float effective;
    public float cozy;
    public float bonus;
    public float penalty;
    public float threshold1;
    public float threshold2;
    public float threshold3;
    public int promoteTicks;
    public boolean missingFurniture;
    public int hurtTicks;
    public int attackTicks;
    public float insight;
    public int vigorInterval;
    public int halfHearts;
    public int halfFeathers;
    public int restedTicks;
    public int eventCode;
    public int toastTier;
    public final List<Line> lines = new ArrayList<>();
    public final List<Category> categories = new ArrayList<>();

    public PacketComfortStatus() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        state = buf.readByte();
        granted = buf.readByte();
        scoreTier = buf.readByte();
        effective = buf.readFloat();
        cozy = buf.readFloat();
        bonus = buf.readFloat();
        penalty = buf.readFloat();
        threshold1 = buf.readFloat();
        threshold2 = buf.readFloat();
        threshold3 = buf.readFloat();
        promoteTicks = buf.readInt();
        missingFurniture = buf.readBoolean();
        hurtTicks = buf.readInt();
        attackTicks = buf.readInt();
        insight = buf.readFloat();
        vigorInterval = buf.readInt();
        halfHearts = buf.readByte();
        halfFeathers = buf.readByte();
        restedTicks = buf.readInt();
        eventCode = buf.readByte();
        toastTier = buf.readByte();
        lines.clear();
        int lineCount = buf.readUnsignedByte();
        for (int i = 0; i < lineCount; i++) {
            Line line = new Line();
            line.kind = buf.readByte();
            line.amount = buf.readFloat();
            line.potion = ByteBufUtils.readUTF8String(buf);
            lines.add(line);
        }
        categories.clear();
        int categoryCount = buf.readUnsignedByte();
        for (int i = 0; i < categoryCount; i++) {
            Category category = new Category();
            category.name = ByteBufUtils.readUTF8String(buf);
            category.seen = buf.readUnsignedShort();
            category.counted = buf.readUnsignedShort();
            category.limit = buf.readUnsignedShort();
            categories.add(category);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(state);
        buf.writeByte(granted);
        buf.writeByte(scoreTier);
        buf.writeFloat(effective);
        buf.writeFloat(cozy);
        buf.writeFloat(bonus);
        buf.writeFloat(penalty);
        buf.writeFloat(threshold1);
        buf.writeFloat(threshold2);
        buf.writeFloat(threshold3);
        buf.writeInt(promoteTicks);
        buf.writeBoolean(missingFurniture);
        buf.writeInt(hurtTicks);
        buf.writeInt(attackTicks);
        buf.writeFloat(insight);
        buf.writeInt(vigorInterval);
        buf.writeByte(halfHearts);
        buf.writeByte(halfFeathers);
        buf.writeInt(restedTicks);
        buf.writeByte(eventCode);
        buf.writeByte(toastTier);
        buf.writeByte(Math.min(255, lines.size()));
        int written = 0;
        for (Line line : lines) {
            if (written++ >= 255) break;
            buf.writeByte(line.kind);
            buf.writeFloat(line.amount);
            ByteBufUtils.writeUTF8String(buf, line.potion == null ? "" : line.potion);
        }
        buf.writeByte(Math.min(255, categories.size()));
        written = 0;
        for (Category category : categories) {
            if (written++ >= 255) break;
            ByteBufUtils.writeUTF8String(buf, category.name == null ? "" : category.name);
            buf.writeShort(category.seen);
            buf.writeShort(category.counted);
            buf.writeShort(category.limit);
        }
    }

    public static final class Line {
        public int kind;
        public float amount;
        public String potion = "";
    }

    public static final class Category {
        public String name = "";
        public int seen;
        public int counted;
        public int limit;
    }

    public static class Handler implements IMessageHandler<PacketComfortStatus, IMessage> {
        @Override
        public IMessage onMessage(PacketComfortStatus message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            Minecraft.getMinecraft().addScheduledTask(() -> ComfortClientState.acceptStatus(message));
            return null;
        }
    }
}
