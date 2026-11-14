package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.nether.ClientSpouts;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to client: where a spout's thrown lava is about to land, and how many ticks until it does. */
public class PacketSplashTelegraph implements IMessage {
    public int[] coords = new int[0];
    public int ticks;

    public PacketSplashTelegraph() {}

    /** {@code coords} is x, y, z for each landing, back to back. */
    public PacketSplashTelegraph(int[] coords, int ticks) {
        this.coords = coords;
        this.ticks = ticks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        ticks = buf.readInt();
        int count = Math.max(0, Math.min(buf.readUnsignedByte(), 16));
        coords = new int[count * 3];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = buf.readInt();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(ticks);
        int count = Math.min(coords.length / 3, 16);
        buf.writeByte(count);
        for (int i = 0; i < count * 3; i++) {
            buf.writeInt(coords[i]);
        }
    }

    public static class Handler implements IMessageHandler<PacketSplashTelegraph, IMessage> {
        @Override
        public IMessage onMessage(PacketSplashTelegraph message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            final PacketSplashTelegraph m = message;
            Minecraft.getMinecraft().addScheduledTask(() -> ClientSpouts.addLandings(m.coords, m.ticks));
            return null;
        }
    }
}
