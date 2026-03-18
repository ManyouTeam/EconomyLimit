package cn.superiormc.economylimit.utils;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {

    public static final Pattern SINGLE_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    public static final Pattern GRADIENT_PATTERN = Pattern.compile("&<#([A-Fa-f0-9]{6})>(.*?)&<#([A-Fa-f0-9]{6})>");
    public static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("[&§]([0-9a-frlomnk])", Pattern.CASE_INSENSITIVE);
    public static final Pattern SECTION_HEX_PATTERN = Pattern.compile("§x(§[A-Fa-f0-9]){6}");

    private static final Map<Character, Color> LEGACY_COLORS = Map.ofEntries(
            Map.entry('0', new Color(0, 0, 0)),
            Map.entry('1', new Color(0, 0, 170)),
            Map.entry('2', new Color(0, 170, 0)),
            Map.entry('3', new Color(0, 170, 170)),
            Map.entry('4', new Color(170, 0, 0)),
            Map.entry('5', new Color(170, 0, 170)),
            Map.entry('6', new Color(255, 170, 0)),
            Map.entry('7', new Color(170, 170, 170)),
            Map.entry('8', new Color(85, 85, 85)),
            Map.entry('9', new Color(85, 85, 255)),
            Map.entry('a', new Color(85, 255, 85)),
            Map.entry('b', new Color(85, 255, 255)),
            Map.entry('c', new Color(255, 85, 85)),
            Map.entry('d', new Color(255, 85, 255)),
            Map.entry('e', new Color(255, 255, 85)),
            Map.entry('f', new Color(255, 255, 255))
    );

    private static final Map<Character, String> LEGACY_MINI_MESSAGE = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white"),
            Map.entry('r', "reset"),
            Map.entry('l', "bold"),
            Map.entry('o', "italic"),
            Map.entry('n', "underlined"),
            Map.entry('m', "strikethrough"),
            Map.entry('k', "obfuscated")
    );

    private TextUtil() {
    }

    public static String pluginPrefix() {
        return "§x§9§8§F§B§9§8[EconomyLimit]";
    }

    public static String parse(String text) {
        EconomyLimitPlugin plugin = EconomyLimitPlugin.getInstance();
        if (plugin == null || plugin.getMethodUtil() == null) {
            return colorize(text);
        }
        return plugin.getMethodUtil().legacyParse(text);
    }

    public static String parse(String text, Player player) {
        return parse(withPAPI(text, player));
    }

    public static String withPAPI(String text, Player player) {
        if (text == null) {
            return "";
        }
        if (player != null && text.contains("%") && CommonUtil.checkPluginLoad("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }

    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        input = applyGradients(input, true);

        Matcher hexMatcher = SINGLE_HEX_PATTERN.matcher(input);
        StringBuilder hexBuffer = new StringBuilder();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexBuffer, Matcher.quoteReplacement("§x" + toMinecraftHex(hexMatcher.group(1))));
        }
        hexMatcher.appendTail(hexBuffer);
        input = hexBuffer.toString();

        Matcher legacyMatcher = LEGACY_COLOR_PATTERN.matcher(input);
        StringBuilder legacyBuffer = new StringBuilder();
        while (legacyMatcher.find()) {
            legacyMatcher.appendReplacement(legacyBuffer, "§" + legacyMatcher.group(1).toLowerCase());
        }
        legacyMatcher.appendTail(legacyBuffer);
        return legacyBuffer.toString();
    }

    public static String convertToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        Matcher sectionHexMatcher = SECTION_HEX_PATTERN.matcher(input);
        StringBuilder sectionHexBuffer = new StringBuilder();
        while (sectionHexMatcher.find()) {
            String hex = sectionHexMatcher.group().replace("§x", "").replace("§", "");
            sectionHexMatcher.appendReplacement(sectionHexBuffer, "<#" + hex + ">");
        }
        sectionHexMatcher.appendTail(sectionHexBuffer);

        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(sectionHexBuffer.toString());
        StringBuilder gradientBuffer = new StringBuilder();
        while (gradientMatcher.find() && gradientMatcher.groupCount() >= 3) {
            String replacement = "<gradient:#" + gradientMatcher.group(1) + ":#" + gradientMatcher.group(3) + ">"
                    + gradientMatcher.group(2) + "</gradient>";
            gradientMatcher.appendReplacement(gradientBuffer, Matcher.quoteReplacement(replacement));
        }
        gradientMatcher.appendTail(gradientBuffer);

        Matcher hexMatcher = SINGLE_HEX_PATTERN.matcher(gradientBuffer.toString());
        StringBuilder hexBuffer = new StringBuilder();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexBuffer, "<#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(hexBuffer);

        Matcher legacyMatcher = LEGACY_COLOR_PATTERN.matcher(hexBuffer.toString());
        StringBuilder finalBuffer = new StringBuilder();
        while (legacyMatcher.find()) {
            char code = Character.toLowerCase(legacyMatcher.group(1).charAt(0));
            String tag = LEGACY_MINI_MESSAGE.getOrDefault(code, "");
            legacyMatcher.appendReplacement(finalBuffer, tag.isEmpty() ? "" : "<" + tag + ">");
        }
        legacyMatcher.appendTail(finalBuffer);
        return finalBuffer.toString().replace("\n", "<newline>");
    }

    public static boolean containsLegacyCodes(String text) {
        return text != null && (
                LEGACY_COLOR_PATTERN.matcher(text).find()
                        || SINGLE_HEX_PATTERN.matcher(text).find()
                        || GRADIENT_PATTERN.matcher(text).find()
                        || SECTION_HEX_PATTERN.matcher(text).find()
        );
    }

    public static void sendMessage(CommandSender sender, String rawText) {

        EconomyLimitPlugin plugin = EconomyLimitPlugin.getInstance();
        if (plugin == null || plugin.getMethodUtil() == null) {
            return;
        }

        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (!rawText.contains("[")) {
            plugin.getMethodUtil().sendChat(player, rawText);
            return;
        }

        boolean sentAny = false;

        for (String message : parseSimpleTag(rawText, "message")) {
            plugin.getMethodUtil().sendChat(player, withPAPI(message, player));
            sentAny = true;
        }

        for (TagResult tag : parseArgTag(rawText, "title")) {
            TitleData data = parseTitle(tag);
            plugin.getMethodUtil().sendTitle(
                    player,
                    withPAPI(data.title(), player),
                    withPAPI(data.subTitle(), player),
                    data.fadeIn(),
                    data.stay(),
                    data.fadeOut()
            );
            sentAny = true;
        }

        for (String message : parseSimpleTag(rawText, "actionbar")) {
            plugin.getMethodUtil().sendActionBar(player, withPAPI(message, player));
            sentAny = true;
        }

        for (TagResult tag : parseArgTag(rawText, "bossbar")) {
            BossBarData data = parseBossBar(tag);
            plugin.getMethodUtil().sendBossBar(
                    player,
                    withPAPI(data.title(), player),
                    data.progress(),
                    data.color(),
                    data.style()
            );
            sentAny = true;
        }

        for (TagResult tag : parseArgTag(rawText, "sound")) {
            SoundData data = parseSound(tag);
            if (data.sound() != null) {
                player.playSound(player.getLocation(), data.sound(), data.volume(), data.pitch());
                sentAny = true;
            }
        }

        // 兜底
        if (!sentAny) {
            plugin.getMethodUtil().sendChat(player, rawText);
        }
    }

    private static List<String> parseSimpleTag(String text, String tag) {
        List<String> list = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[" + tag + "]([\\s\\S]*?)\\[/" + tag + "]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            list.add(matcher.group(1).trim());
        }
        return list;
    }

    private static List<TagResult> parseArgTag(String text, String tag) {
        List<TagResult> list = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[" + tag + "(?:=([^\\]]+))?]([\\s\\S]*?)\\[/" + tag + "]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            list.add(new TagResult(matcher.group(1), matcher.group(2).trim()));
        }
        return list;
    }

    private static TitleData parseTitle(TagResult tag) {
        int fadeIn = 10;
        int stay = 70;
        int fadeOut = 20;

        if (tag.args() != null) {
            String[] args = tag.args().split(",");
            if (args.length == 3) {
                fadeIn = parseInt(args[0], fadeIn);
                stay = parseInt(args[1], stay);
                fadeOut = parseInt(args[2], fadeOut);
            }
        }

        String[] parts = tag.content().split(";;", 2);
        String title = parts[0];
        String subTitle = parts.length > 1 ? parts[1] : "";
        return new TitleData(title, subTitle, fadeIn, stay, fadeOut);
    }

    private static BossBarData parseBossBar(TagResult tag) {
        String color = "WHITE";
        String style = "SOLID";
        float progress = 1.0F;

        if (tag.args() != null) {
            String[] args = tag.args().split(",");
            if (args.length > 0 && !args[0].isBlank()) {
                color = args[0].trim();
            }
            if (args.length > 1 && !args[1].isBlank()) {
                style = args[1].trim();
            }
            if (args.length > 2) {
                progress = parseFloat(args[2], progress);
            }
        }

        return new BossBarData(tag.content(), progress, color, style);
    }

    private static SoundData parseSound(TagResult tag) {
        Sound sound = null;
        float volume = 1F;
        float pitch = 1F;

        if (tag.args() != null) {
            String[] args = tag.args().split(",");
            if (args.length > 0) {
                try {
                    sound = Sound.valueOf(args[0].trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    sound = null;
                }
            }
            if (args.length > 1) {
                volume = parseFloat(args[1], volume);
            }
            if (args.length > 2) {
                pitch = parseFloat(args[2], pitch);
            }
        }

        return new SoundData(sound, volume, pitch);
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static float parseFloat(String value, float defaultValue) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static String applyGradients(String input, boolean supportHex) {
        Matcher matcher = GRADIENT_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String gradientText = supportHex
                    ? applyGradient(matcher.group(1), matcher.group(3), matcher.group(2))
                    : applyLegacyGradient(matcher.group(1), matcher.group(3), matcher.group(2));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(gradientText));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String applyGradient(String startHex, String endHex, String text) {
        if (text.isEmpty()) {
            return "";
        }
        Color start = Color.decode("#" + startHex);
        Color end = Color.decode("#" + endHex);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            float ratio = text.length() == 1 ? 0F : (float) i / (text.length() - 1);
            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            builder.append("\u00A7x").append(toMinecraftHex(String.format("%02x%02x%02x", red, green, blue))).append(text.charAt(i));
        }
        return builder.toString();
    }

    private static String applyLegacyGradient(String startHex, String endHex, String text) {
        if (text.isEmpty()) {
            return "";
        }
        Color start = Color.decode("#" + startHex);
        Color end = Color.decode("#" + endHex);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            float ratio = text.length() == 1 ? 0F : (float) i / (text.length() - 1);
            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            builder.append('\u00A7').append(getClosestLegacyColor(String.format("%02x%02x%02x", red, green, blue))).append(text.charAt(i));
        }
        return builder.toString();
    }

    private static String toMinecraftHex(String hex) {
        StringBuilder builder = new StringBuilder();
        for (char c : hex.toCharArray()) {
            builder.append('\u00A7').append(c);
        }
        return builder.toString();
    }

    private static char getClosestLegacyColor(String hex) {
        Color target = Color.decode("#" + hex);
        double minDistance = Double.MAX_VALUE;
        char closest = 'f';
        for (Map.Entry<Character, Color> entry : LEGACY_COLORS.entrySet()) {
            double distance = colorDistance(target, entry.getValue());
            if (distance < minDistance) {
                minDistance = distance;
                closest = entry.getKey();
            }
        }
        return closest;
    }

    private static double colorDistance(Color c1, Color c2) {
        int r = c1.getRed() - c2.getRed();
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();
        return 0.3 * r * r + 0.59 * g * g + 0.11 * b * b;
    }

    private record TagResult(String args, String content) {
    }

    private record TitleData(String title, String subTitle, int fadeIn, int stay, int fadeOut) {
    }

    private record BossBarData(String title, float progress, String color, String style) {
    }

    private record SoundData(Sound sound, float volume, float pitch) {
    }
}
