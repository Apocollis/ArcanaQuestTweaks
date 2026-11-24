package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.starter.StarterClassView;
import com.apocollis.aqtweaks.starter.client.StarterClientHooks;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client: open the class choice screen with the story text and the valid classes. An empty class list
 * means "close every starter screen" (used by {@code /aqclass skip}).
 */
public class PacketOpenStarterClassGui implements IMessage {
    public String intro = "";
    public String prompt = "";
    public List<StarterClassView> classes = new ArrayList<>();

    public PacketOpenStarterClassGui() {}

    public PacketOpenStarterClassGui(String intro, String prompt, List<StarterClassView> classes) {
        this.intro = intro == null ? "" : intro;
        this.prompt = prompt == null ? "" : prompt;
        this.classes = classes == null ? new ArrayList<StarterClassView>() : classes;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        intro = ByteBufUtils.readUTF8String(buf);
        prompt = ByteBufUtils.readUTF8String(buf);
        int count = buf.readInt();
        classes = new ArrayList<>();
        for (int i = 0; i < count && i < 64; i++) {
            classes.add(new StarterClassView(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf),
                    ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf)));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, intro);
        ByteBufUtils.writeUTF8String(buf, prompt);
        buf.writeInt(classes.size());
        for (StarterClassView c : classes) {
            ByteBufUtils.writeUTF8String(buf, c.id);
            ByteBufUtils.writeUTF8String(buf, c.name);
            ByteBufUtils.writeUTF8String(buf, c.flavor);
            ByteBufUtils.writeUTF8String(buf, c.icon);
        }
    }

    public static class Handler implements IMessageHandler<PacketOpenStarterClassGui, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenStarterClassGui message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            final PacketOpenStarterClassGui m = message;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (m.classes.isEmpty()) {
                    StarterClientHooks.closeAll();
                } else {
                    StarterClientHooks.open(m.intro, m.prompt, m.classes);
                }
            });
            return null;
        }
    }
}
