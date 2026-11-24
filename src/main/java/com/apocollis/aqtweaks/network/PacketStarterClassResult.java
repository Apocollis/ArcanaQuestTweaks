package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.starter.StarterResult;
import com.apocollis.aqtweaks.starter.client.StarterClientHooks;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to client: what the chosen class gave, for the "what you gained" panel. */
public class PacketStarterClassResult implements IMessage {
    public StarterResult result;

    public PacketStarterClassResult() {}

    public PacketStarterClassResult(StarterResult result) {
        this.result = result;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        String classId = ByteBufUtils.readUTF8String(buf);
        String className = ByteBufUtils.readUTF8String(buf);
        String perkKey = ByteBufUtils.readUTF8String(buf);
        int skillCount = buf.readInt();
        List<StarterResult.SkillGain> skills = new ArrayList<>();
        for (int i = 0; i < skillCount && i < 64; i++) {
            skills.add(new StarterResult.SkillGain(ByteBufUtils.readUTF8String(buf),
                    ByteBufUtils.readUTF8String(buf), buf.readInt()));
        }
        int gearCount = buf.readInt();
        List<ItemStack> gear = new ArrayList<>();
        for (int i = 0; i < gearCount && i < 64; i++) {
            gear.add(ByteBufUtils.readItemStack(buf));
        }
        result = new StarterResult(classId, className, perkKey, skills, gear);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, result.classId);
        ByteBufUtils.writeUTF8String(buf, result.className);
        ByteBufUtils.writeUTF8String(buf, result.perkKey);
        buf.writeInt(result.skills.size());
        for (StarterResult.SkillGain g : result.skills) {
            ByteBufUtils.writeUTF8String(buf, g.skillId);
            ByteBufUtils.writeUTF8String(buf, g.skillName);
            buf.writeInt(g.level);
        }
        buf.writeInt(result.gear.size());
        for (ItemStack stack : result.gear) {
            ByteBufUtils.writeItemStack(buf, stack);
        }
    }

    public static class Handler implements IMessageHandler<PacketStarterClassResult, IMessage> {
        @Override
        public IMessage onMessage(PacketStarterClassResult message, MessageContext ctx) {
            if (!ctx.side.isClient()) return null;
            final StarterResult r = message.result;
            Minecraft.getMinecraft().addScheduledTask(() -> StarterClientHooks.showResult(r));
            return null;
        }
    }
}
