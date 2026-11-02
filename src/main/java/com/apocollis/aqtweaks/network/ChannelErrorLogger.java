package com.apocollis.aqtweaks.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logs any exception raised while decoding or handling a packet on a mod network channel, then passes it on
 * unchanged. Forge turns such an exception into "A fatal error has occurred, this connection is terminated" and,
 * in this pack, the "critical exception handling a packet" line that should come with it was never written, so the
 * failing channel was unknown. This handler changes no behaviour; it only makes the exception and the channel name
 * visible.
 *
 * <p>It is added to the end of every registered channel on both sides from {@code FMLLoadCompleteEvent}, after every
 * mod has registered its channels. A channel registered later is not covered.
 */
public final class ChannelErrorLogger extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks");
    private static final String HANDLER_NAME = "aqtweaks-error-log";

    private final String channel;
    private final Side side;

    private ChannelErrorLogger(String channel, Side side) {
        this.channel = channel;
        this.side = side;
    }

    /** Adds the logger to the end of every registered channel's pipeline, on both sides. */
    public static void installAll() {
        int installed = 0;
        List<String> names = new ArrayList<>();
        for (Side side : Side.values()) {
            for (String name : NetworkRegistry.INSTANCE.channelNamesFor(side)) {
                FMLEmbeddedChannel channel = NetworkRegistry.INSTANCE.getChannel(name, side);
                if (channel == null || channel.pipeline().get(HANDLER_NAME) != null) continue;
                channel.pipeline().addLast(HANDLER_NAME, new ChannelErrorLogger(name, side));
                installed++;
                if (!names.contains(name)) names.add(name);
            }
        }
        LOGGER.info("Network error logger installed on {} channel endpoints ({} channels)", installed, names.size());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Exception on network channel '{}' ({} side)", channel, side, cause);
        super.exceptionCaught(ctx, cause);
    }
}
