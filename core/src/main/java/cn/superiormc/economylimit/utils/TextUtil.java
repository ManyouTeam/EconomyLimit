package cn.superiormc.economylimit.utils;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.Color;
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

    public static void sendMessage(CommandSender sender, String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return;
        }
        CommandSender actualSender = sender == null ? Bukkit.getConsoleSender() : sender;
        EconomyLimitPlugin plugin = EconomyLimitPlugin.getInstance();
        if (actualSender instanceof Player player && plugin != null && plugin.getMethodUtil() != null) {
            plugin.getMethodUtil().sendChat(player, rawText);
            return;
        }
        String parsed = plugin != null && plugin.getMethodUtil() != null
                ? plugin.getMethodUtil().legacyParse(rawText)
                : colorize(rawText);
        actualSender.sendMessage(parsed);
    }

    public static String parse(String text) {
        EconomyLimitPlugin plugin = EconomyLimitPlugin.getInstance();
        if (plugin == null || plugin.getMethodUtil() == null) {
            return colorize(text);
        }
        return plugin.getMethodUtil().legacyParse(text);
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
            float ratio = text.length() == 1 ? 0f : (float) i / (text.length() - 1);
            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            builder.append("§x").append(toMinecraftHex(String.format("%02x%02x%02x", red, green, blue))).append(text.charAt(i));
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
            float ratio = text.length() == 1 ? 0f : (float) i / (text.length() - 1);
            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            builder.append('§').append(getClosestLegacyColor(String.format("%02x%02x%02x", red, green, blue))).append(text.charAt(i));
        }
        return builder.toString();
    }

    private static String toMinecraftHex(String hex) {
        StringBuilder builder = new StringBuilder();
        for (char c : hex.toCharArray()) {
            builder.append('§').append(c);
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
}
