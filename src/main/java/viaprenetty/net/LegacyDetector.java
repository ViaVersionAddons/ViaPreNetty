package viaprenetty.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

public final class LegacyDetector extends ChannelInboundHandlerAdapter {

    public static final String NAME = "viaprenetty-detector";

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (!(msg instanceof ByteBuf buf) || !buf.isReadable()) {
            ctx.fireChannelRead(msg);
            return;
        }
        final ChannelPipeline pipeline = ctx.pipeline();
        final int first = buf.getUnsignedByte(buf.readerIndex());
        if (first == 0x02) {
            removeLegacyQuery(pipeline);
            pipeline.replace(this, LegacyConnection.NAME, new LegacyConnection());
            pipeline.fireChannelRead(msg);
            return;
        }
        if (first == 0xFE && LegacyPing.looksLikePing(buf)) {
            removeLegacyQuery(pipeline);
            pipeline.replace(this, LegacyPing.NAME, new LegacyPing());
            pipeline.fireChannelRead(msg);
            return;
        }
        pipeline.remove(this);
        ctx.fireChannelRead(msg);
    }

    private static void removeLegacyQuery(final ChannelPipeline pipeline) {
        if (pipeline.get("legacy_query") != null) {
            pipeline.remove("legacy_query");
        }
    }

}
