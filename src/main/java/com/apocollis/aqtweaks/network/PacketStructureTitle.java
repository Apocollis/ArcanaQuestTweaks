package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.titles.StructureTitleClient;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client: the player entered a structure (or left it). {@code key} is the lower-case structure name
 * or {@code home}. {@code title} is the formatted home name for a home, otherwise the structure name in its
 * original case. {@code subtitle} is kept for later and unused.
 */
public class PacketStructureTitle implements IMessage {
    public String key = "";
    public String title = "";
    public String subtitle = "";
    public boolean enter;

    public PacketStructureTitle() {}

    public PacketStructureTitle(String key, String title, String subtitle, boolean enter) {
        this.key = key == null ? "" : key;
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.enter = enter;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        key = ByteBufUtils.readUTF8String(buf);
        title = ByteBufUtils.readUTF8String(buf);
        subtitle = ByteBufUtils.readUTF8String(buf);
        enter = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, key);
        ByteBufUtils.writeUTF8String(buf, title);
        ByteBufUtils.writeUTF8String(buf, subtitle);
        buf.writeBoolean(enter);
    }

    public static class Handler implements IMessageHandler<PacketStructureTitle, IMessage> {
        @Override
        public IMessage onMessage(PacketStructureTitle message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            final PacketStructureTitle m = message;
            Minecraft.getMinecraft().addScheduledTask(() -> StructureTitleClient.apply(m.key, m.title, m.subtitle, m.enter));
            return null;
        }
    }
}
