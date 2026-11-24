package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.reskillable.client.PerkToggleClient;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to client: the ids of the toggleable perks this player has switched off. */
public class PacketSyncPerkToggles implements IMessage {
    public List<String> disabled = new ArrayList<>();

    public PacketSyncPerkToggles() {}

    public PacketSyncPerkToggles(List<String> disabled) {
        this.disabled = disabled == null ? new ArrayList<String>() : disabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        disabled = new ArrayList<>();
        for (int i = 0; i < count && i < 256; i++) disabled.add(ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(disabled.size());
        for (String id : disabled) ByteBufUtils.writeUTF8String(buf, id);
    }

    public static class Handler implements IMessageHandler<PacketSyncPerkToggles, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncPerkToggles message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            PerkToggleClient.apply(new ArrayList<>(message.disabled));
            return null;
        }
    }
}
