package com.apocollis.aqtweaks.comfort;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to client. One row chunk of the comfort block table. */
public class PacketComfortCatalog implements IMessage {

    public int index;
    public boolean last;
    public final List<Row> rows = new ArrayList<>();

    public PacketComfortCatalog() {}

    public PacketComfortCatalog(int index, boolean last, List<Row> rows) {
        this.index = index;
        this.last = last;
        this.rows.addAll(rows);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        index = buf.readInt();
        last = buf.readBoolean();
        int count = buf.readUnsignedShort();
        rows.clear();
        for (int i = 0; i < count; i++) {
            Row row = new Row();
            row.id = ByteBufUtils.readUTF8String(buf);
            row.category = ByteBufUtils.readUTF8String(buf);
            row.family = ByteBufUtils.readUTF8String(buf);
            row.weight = buf.readFloat();
            row.limit = buf.readUnsignedShort();
            rows.add(row);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(index);
        buf.writeBoolean(last);
        buf.writeShort(rows.size());
        for (Row row : rows) {
            ByteBufUtils.writeUTF8String(buf, row.id);
            ByteBufUtils.writeUTF8String(buf, row.category);
            ByteBufUtils.writeUTF8String(buf, row.family);
            buf.writeFloat(row.weight);
            buf.writeShort(row.limit);
        }
    }

    public static final class Row {
        public String id = "";
        public String category = "";
        public String family = "";
        public float weight;
        public int limit;
    }

    public static class Handler implements IMessageHandler<PacketComfortCatalog, IMessage> {
        @Override
        public IMessage onMessage(PacketComfortCatalog message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            Minecraft.getMinecraft().addScheduledTask(() -> ComfortClientState.acceptCatalog(message));
            return null;
        }
    }
}
