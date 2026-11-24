package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.starter.StarterModule;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client to server: the reveal panel closed, so the lockdown can end. */
public class PacketStarterRevealDone implements IMessage {

    public PacketStarterRevealDone() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketStarterRevealDone, IMessage> {
        @Override
        public IMessage onMessage(PacketStarterRevealDone message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) return null;
            MinecraftServer server = player.getServer();
            if (server == null) return null;
            server.addScheduledTask(() -> StarterModule.onRevealDone(player));
            return null;
        }
    }
}
