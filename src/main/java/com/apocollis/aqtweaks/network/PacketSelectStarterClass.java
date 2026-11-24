package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.starter.StarterModule;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client to server: the player picked a class on the choice screen. */
public class PacketSelectStarterClass implements IMessage {
    public String id = "";

    public PacketSelectStarterClass() {}

    public PacketSelectStarterClass(String id) {
        this.id = id == null ? "" : id;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        id = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, id);
    }

    public static class Handler implements IMessageHandler<PacketSelectStarterClass, IMessage> {
        @Override
        public IMessage onMessage(PacketSelectStarterClass message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) return null;
            MinecraftServer server = player.getServer();
            if (server == null) return null;
            final String id = message.id;
            server.addScheduledTask(() -> StarterModule.onSelect(player, id));
            return null;
        }
    }
}
