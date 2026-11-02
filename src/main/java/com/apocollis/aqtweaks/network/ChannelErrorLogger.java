package com.apocollis.aqtweaks.network;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logs any exception raised while decoding or handling a packet on the {@code aqtweaks} channel, then passes it on
 * unchanged. Forge turns such an exception into "A fatal error has occurred, this connection is terminated" and,
 * in this pack, the "critical exception handling a packet" line that should come with it was never written. This
 * handler changes no behaviour; it only makes the exception visible.
 */
public final class ChannelErrorLogger extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks");
    private static final String HANDLER_NAME = "aqtweaks-error-log";

    private final Side side;

    private ChannelErrorLogger(Side side) {
        this.side = side;
    }

    /** Adds the logger to the end of each side's channel pipeline. Call after the messages are registered. */
    public static void install() {
        for (Side side : Side.values()) {
            FMLEmbeddedChannel channel = NetworkRegistry.INSTANCE.getChannel(ArcanaQuestTweaks.MODID, side);
            if (channel == null || channel.pipeline().get(HANDLER_NAME) != null) continue;
            channel.pipeline().addLast(HANDLER_NAME, new ChannelErrorLogger(side));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Exception on the aqtweaks network channel ({} side)", side, cause);
        super.exceptionCaught(ctx, cause);
    }
}
