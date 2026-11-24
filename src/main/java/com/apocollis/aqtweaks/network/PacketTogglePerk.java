package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.reskillable.PerkToggleEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client to server: the player clicked an unlocked toggleable perk in the Reskillable skill screen. */
public class PacketTogglePerk implements IMessage {
    public String perkId = "";

    public PacketTogglePerk() {}

    public PacketTogglePerk(String perkId) {
        this.perkId = perkId == null ? "" : perkId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        perkId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, perkId);
    }

    public static class Handler implements IMessageHandler<PacketTogglePerk, IMessage> {
        @Override
        public IMessage onMessage(PacketTogglePerk message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) return null;
            MinecraftServer server = player.getServer();
            if (server == null) return null;
            final String id = message.perkId;
            server.addScheduledTask(() -> PerkToggleEvents.handleToggle(player, id));
            return null;
        }
    }
}
