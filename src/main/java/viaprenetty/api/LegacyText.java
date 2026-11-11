package viaprenetty.api;

import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.StringComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.TranslationComponent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.translation.BasicTranslator;
import com.viaversion.viaversion.libs.mcstructs.text.translation.Translator;
import com.viaversion.viaversion.libs.mcstructs.text.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;

public final class LegacyText {

    private static final BasicTranslator TRANSLATIONS = new BasicTranslator();
    private static final BasicTranslator SERVER_LANGUAGE = new BasicTranslator();
    private static final Translator TRANSLATOR = key -> {
        String value = TRANSLATIONS.translate(key);
        if (value != null) {
            return value;
        }
        value = SERVER_LANGUAGE.translate(key);
        if (value != null) {
            return value;
        }
        if (key.startsWith("entity.") && key.endsWith(".name") && key.length() > 12) {
            return key.substring(7, key.length() - 5);
        }
        return null;
    };

    static {
        t("chat.type.text", "<%s> %s");
        t("chat.type.emote", "* %s %s");
        t("chat.type.announcement", "[%s] %s");
        t("chat.type.admin", "[%s: %s]");
        t("chat.type.achievement", "%s has just earned the achievement %s");
        t("multiplayer.player.joined", "%s joined the game");
        t("multiplayer.player.left", "%s left the game");
        t("commands.generic.notFound", "Unknown command. Try /help for a list of commands");
        t("commands.generic.permission", "You do not have permission to use this command");
        t("commands.generic.usage", "Usage: %s");
        t("commands.generic.exception", "An unknown error occurred while attempting to perform this command");
        t("commands.generic.player.notFound", "That player cannot be found");
        t("death.attack.inFire", "%1$s went up in flames");
        t("death.attack.onFire", "%1$s burned to death");
        t("death.attack.lava", "%1$s tried to swim in lava");
        t("death.attack.inWall", "%1$s suffocated in a wall");
        t("death.attack.drown", "%1$s drowned");
        t("death.attack.starve", "%1$s starved to death");
        t("death.attack.cactus", "%1$s was pricked to death");
        t("death.attack.generic", "%1$s died");
        t("death.attack.explosion", "%1$s blew up");
        t("death.attack.explosion.player", "%1$s was blown up by %2$s");
        t("death.attack.magic", "%1$s was killed by magic");
        t("death.attack.wither", "%1$s withered away");
        t("death.attack.anvil", "%1$s was squashed by a falling anvil");
        t("death.attack.fallingBlock", "%1$s was squashed by a falling block");
        t("death.attack.mob", "%1$s was slain by %2$s");
        t("death.attack.player", "%1$s was slain by %2$s");
        t("death.attack.player.item", "%1$s was slain by %2$s using %3$s");
        t("death.attack.arrow", "%1$s was shot by %2$s");
        t("death.attack.arrow.item", "%1$s was shot by %2$s using %3$s");
        t("death.attack.fireball", "%1$s was fireballed by %2$s");
        t("death.attack.thrown", "%1$s was pummeled by %2$s");
        t("death.attack.indirectMagic", "%1$s was killed by %2$s using magic");
        t("death.attack.thorns", "%1$s was killed trying to hurt %2$s");
        t("death.attack.fall", "%1$s hit the ground too hard");
        t("death.attack.outOfWorld", "%1$s fell out of the world");
        t("death.fell.accident.generic", "%1$s fell from a high place");
        t("entity.Creeper.name", "Creeper");
        t("entity.Skeleton.name", "Skeleton");
        t("entity.Spider.name", "Spider");
        t("entity.Giant.name", "Giant");
        t("entity.Zombie.name", "Zombie");
        t("entity.Slime.name", "Slime");
        t("entity.Ghast.name", "Ghast");
        t("entity.PigZombie.name", "Zombie Pigman");
        t("entity.Enderman.name", "Enderman");
        t("entity.CaveSpider.name", "Cave Spider");
        t("entity.Silverfish.name", "Silverfish");
        t("entity.Blaze.name", "Blaze");
        t("entity.LavaSlime.name", "Magma Cube");
        t("entity.EnderDragon.name", "Ender Dragon");
        t("entity.WitherBoss.name", "Wither");
        t("entity.Bat.name", "Bat");
        t("entity.Witch.name", "Witch");
        t("entity.Pig.name", "Pig");
        t("entity.Sheep.name", "Sheep");
        t("entity.Cow.name", "Cow");
        t("entity.Chicken.name", "Chicken");
        t("entity.Squid.name", "Squid");
        t("entity.Wolf.name", "Wolf");
        t("entity.MushroomCow.name", "Mooshroom");
        t("entity.SnowMan.name", "Snow Golem");
        t("entity.Ozelot.name", "Ocelot");
        t("entity.VillagerGolem.name", "Iron Golem");
        t("entity.EntityHorse.name", "Horse");
        t("entity.Villager.name", "Villager");
        t("entity.Arrow.name", "Arrow");
        t("entity.Fireball.name", "Fireball");
        t("entity.SmallFireball.name", "Small Fireball");
        t("entity.Snowball.name", "Snowball");
        t("entity.ThrownPotion.name", "Potion");
        t("entity.PrimedTnt.name", "Block of TNT");
        t("entity.generic.name", "unknown");
    }

    private LegacyText() {
    }

    public static void loadServerLanguage(final ClassLoader loader) {
        if (loader == null) {
            return;
        }
        try (java.io.InputStream in = loader.getResourceAsStream("assets/minecraft/lang/en_us.json")) {
            if (in == null) {
                return;
            }
            final com.viaversion.viaversion.libs.gson.JsonObject object = com.viaversion.viaversion.libs.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (java.util.Map.Entry<String, com.viaversion.viaversion.libs.gson.JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    SERVER_LANGUAGE.addTranslation(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (final Exception ignored) {
        }
    }

    public static boolean isDeliveryFallback(final String json) {
        if (json == null || !json.contains("deliver")) {
            return false;
        }
        try {
            final com.viaversion.viaversion.libs.gson.JsonElement element = com.viaversion.viaversion.libs.gson.JsonParser.parseString(json);
            if (!element.isJsonObject() || !element.getAsJsonObject().has("translate")) {
                return false;
            }
            final String key = element.getAsJsonObject().get("translate").getAsString();
            return key.equals("multiplayer.message_not_delivered") || key.startsWith("Can't deliver chat message");
        } catch (final Exception e) {
            return false;
        }
    }

    private static void t(final String key, final String value) {
        TRANSLATIONS.addTranslation(key, value);
    }

    private static TextComponent parse17(final String json) {
        try {
            return TextComponentSerializer.V1_7.deserialize(json);
        } catch (final Throwable e) {
            return new StringComponent(json == null ? "" : json);
        }
    }

    private static TextComponent parse16(final String json) {
        try {
            return TextComponentSerializer.V1_6.deserialize(json);
        } catch (final Throwable e) {
            return new StringComponent(json == null ? "" : json);
        }
    }

    private static String toLegacy(final TextComponent component) {
        TextUtils.iterateAll(component, c -> {
            if (c instanceof TranslationComponent translation) {
                translation.setTranslator(TRANSLATOR);
            }
        });
        try {
            return component.asLegacyFormatString();
        } catch (final Throwable e) {
            return component.asUnformattedString();
        }
    }

    public static String json17To16(final String json) {
        return TextComponentSerializer.V1_6.serialize(new StringComponent(toLegacy(parse17(json))));
    }

    public static String json17ToLegacy(final String json) {
        return toLegacy(parse17(json));
    }

    public static String json16ToLegacy(final String json) {
        return toLegacy(parse16(json));
    }

    public static List<String> splitLegacy(final String text, final int max) {
        final List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text;
        String carry = "";
        while (!remaining.isEmpty()) {
            final String line = carry + remaining;
            if (line.length() <= max) {
                lines.add(line);
                break;
            }
            int cut = max;
            if (line.charAt(cut - 1) == '§') {
                cut--;
            }
            final int space = line.lastIndexOf(' ', cut);
            if (space > max / 2) {
                cut = space;
            }
            final String part = line.substring(0, cut);
            lines.add(part);
            carry = lastFormat(part);
            remaining = line.substring(cut).stripLeading();
            if (carry.length() >= max) {
                carry = "";
            }
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private static String lastFormat(final String text) {
        String color = "";
        final StringBuilder formats = new StringBuilder();
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') {
                final char code = Character.toLowerCase(text.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || code == 'r') {
                    color = code == 'r' ? "" : "§" + code;
                    formats.setLength(0);
                } else if (code >= 'k' && code <= 'o') {
                    formats.append('§').append(code);
                }
                i++;
            }
        }
        return color + formats;
    }

}
