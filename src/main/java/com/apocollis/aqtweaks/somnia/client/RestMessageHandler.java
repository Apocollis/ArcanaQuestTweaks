package com.apocollis.aqtweaks.somnia.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.kingrunes.somnia.common.SomniaConfig;
import com.kingrunes.somnia.common.util.SomniaUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * While {@code Rest on Beds When Not Tired} is on, Somnia's "You're not tired yet" action-bar line is dropped: the
 * seat's own line ({@code MixinNetHandlerPlayClientBedSeat}) says it together with the dismount hint, so the two
 * no longer draw over each other. It is kept when no seat will follow because armour blocks sleep.
 */
@SideOnly(Side.CLIENT)
public final class RestMessageHandler {

    private static final String KEY = "somnia.status.cooldown";

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.getType() != ChatType.GAME_INFO
                || !ArcanaQuestTweaksConfig.SomniaModuleConfig.enableSomniaModule
                || !ArcanaQuestTweaksConfig.SomniaModuleConfig.restOnBedsWhenNotTired) {
            return;
        }
        ITextComponent message = event.getMessage();
        boolean notTired = message instanceof TextComponentTranslation
                ? KEY.equals(((TextComponentTranslation) message).getKey())
                : message != null && message.getUnformattedText().equals(I18n.format(KEY));
        if (!notTired) {
            return;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null && SomniaConfig.OPTIONS != null && !SomniaConfig.OPTIONS.sleepWithArmor
                && !player.capabilities.isCreativeMode && SomniaUtil.doesPlayHaveAnyArmor(player)) {
            return;
        }
        event.setCanceled(true);
    }
}
