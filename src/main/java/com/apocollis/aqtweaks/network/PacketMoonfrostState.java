package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.aurorian.ClientMoonfrost;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client: the player's rime stack (0..15), sent only when it changes. The client cannot see the
 * server's counters. Own channel ({@code aqtweaks}); never touches Elenai's config packet.
 */
public class PacketMoonfrostState implements IMessage {
    public int rime;

    public PacketMoonfrostState() {}

    public PacketMoonfrostState(int rime) {
        this.rime = rime;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        rime = buf.readUnsignedByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(Math.max(0, Math.min(255, rime)));
    }

    public static class Handler implements IMessageHandler<PacketMoonfrostState, IMessage> {
        @Override
        public IMessage onMessage(PacketMoonfrostState message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            final int rime = message.rime;
            Minecraft.getMinecraft().addScheduledTask(() -> ClientMoonfrost.rime = rime);
            return null;
        }
    }
}
