package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.aether.ClientBolts;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to client: an Aether lightning strike point and the ticks until the bolt lands (about 1.75 s). */
public class PacketAetherBoltTelegraph implements IMessage {
    public int x;
    public int y;
    public int z;
    public int ticks;

    public PacketAetherBoltTelegraph() {}

    public PacketAetherBoltTelegraph(int x, int y, int z, int ticks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.ticks = ticks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        ticks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(ticks);
    }

    public static class Handler implements IMessageHandler<PacketAetherBoltTelegraph, IMessage> {
        @Override
        public IMessage onMessage(PacketAetherBoltTelegraph m, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            Minecraft.getMinecraft().addScheduledTask(() -> ClientBolts.add(m.x, m.y, m.z, m.ticks));
            return null;
        }
    }
}
