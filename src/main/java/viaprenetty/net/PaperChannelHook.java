package viaprenetty.net;

import io.netty.channel.Channel;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class PaperChannelHook {

    private static final String NAMESPACE = "viaprenetty";
    private static final String KEY = "detector";

    private PaperChannelHook() {
    }

    public static boolean isSupported() {
        try {
            Class.forName("io.papermc.paper.network.ChannelInitializeListenerHolder");
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    public static void install() throws ReflectiveOperationException {
        final Class<?> listenerClass = Class.forName("io.papermc.paper.network.ChannelInitializeListener");
        final Object listener = Proxy.newProxyInstance(PaperChannelHook.class.getClassLoader(), new Class[]{listenerClass}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "afterInitChannel" -> {
                    inject((Channel) args[0]);
                    return null;
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "equals" -> {
                    return proxy == args[0];
                }
                case "toString" -> {
                    return "ViaPreNettyChannelListener";
                }
                default -> {
                    return null;
                }
            }
        });
        final Class<?> holderClass = Class.forName("io.papermc.paper.network.ChannelInitializeListenerHolder");
        final Method add = holderClass.getDeclaredMethod("addListener", keyClass(), listenerClass);
        add.invoke(null, key(), listener);
    }

    public static void uninstall() {
        try {
            final Class<?> holderClass = Class.forName("io.papermc.paper.network.ChannelInitializeListenerHolder");
            final Method remove = holderClass.getDeclaredMethod("removeListener", keyClass());
            remove.invoke(null, key());
        } catch (final ReflectiveOperationException ignored) {
        }
    }

    private static Class<?> keyClass() throws ClassNotFoundException {
        return Class.forName("net.kyori.adventure.key.Key");
    }

    private static Object key() throws ReflectiveOperationException {
        return keyClass().getMethod("key", String.class, String.class).invoke(null, NAMESPACE, KEY);
    }

    private static void inject(final Channel channel) {
        if (channel.pipeline().get(LegacyDetector.NAME) == null) {
            channel.pipeline().addFirst(LegacyDetector.NAME, new LegacyDetector());
        }
    }

}
