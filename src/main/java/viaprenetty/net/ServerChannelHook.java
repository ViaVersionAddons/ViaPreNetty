package viaprenetty.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ServerChannelHook {

    private static final String NAME = "viaprenetty-acceptor";
    private static final List<Channel> HOOKED = new ArrayList<>();

    private ServerChannelHook() {
    }

    public static boolean install() throws ReflectiveOperationException {
        final List<ChannelFuture> futures = channelFutures();
        if (futures == null) {
            return false;
        }
        boolean any = false;
        synchronized (futures) {
            for (ChannelFuture future : futures) {
                final Channel channel = future.channel();
                if (channel.pipeline().get(NAME) == null) {
                    channel.pipeline().addFirst(NAME, new Acceptor());
                    HOOKED.add(channel);
                }
                any = true;
            }
        }
        return any;
    }

    public static void uninstall() {
        for (Channel channel : HOOKED) {
            try {
                if (channel.pipeline().get(NAME) != null) {
                    channel.pipeline().remove(NAME);
                }
            } catch (final RuntimeException ignored) {
            }
        }
        HOOKED.clear();
    }

    @SuppressWarnings("unchecked")
    private static List<ChannelFuture> channelFutures() throws ReflectiveOperationException {
        final Object craftServer = Bukkit.getServer();
        final Method getServer = craftServer.getClass().getMethod("getServer");
        final Object minecraftServer = getServer.invoke(craftServer);
        Object connection = null;
        for (Class<?> type = minecraftServer.getClass(); type != null && connection == null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && method.getReturnType().getSimpleName().equals("ServerConnection")) {
                    method.setAccessible(true);
                    connection = method.invoke(minecraftServer);
                    break;
                }
            }
        }
        if (connection == null) {
            return null;
        }
        for (Field field : connection.getClass().getDeclaredFields()) {
            if (!List.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            final List<?> list = (List<?>) field.get(connection);
            if (list == null) {
                continue;
            }
            synchronized (list) {
                if (!list.isEmpty() && list.get(0) instanceof ChannelFuture) {
                    return (List<ChannelFuture>) list;
                }
            }
        }
        return null;
    }

    @ChannelHandler.Sharable
    private static final class Acceptor extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            if (msg instanceof Channel child) {
                child.pipeline().addLast(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel channel) {
                        if (channel.pipeline().get(LegacyDetector.NAME) == null) {
                            channel.pipeline().addFirst(LegacyDetector.NAME, new LegacyDetector());
                        }
                    }
                });
            }
            super.channelRead(ctx, msg);
        }

    }

}
