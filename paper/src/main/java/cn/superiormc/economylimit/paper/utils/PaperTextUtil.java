package cn.superiormc.economylimit.paper.utils;

import cn.superiormc.economylimit.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class PaperTextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private PaperTextUtil() {
    }

    public static Component modernParse(String text) {
        String actualText = text == null ? "" : text;
        if (TextUtil.containsLegacyCodes(actualText)) {
            actualText = TextUtil.convertToMiniMessage(actualText);
        }
        return MINI_MESSAGE.deserialize(actualText);
    }

    public static String toLegacy(String text) {
        return LEGACY_SERIALIZER.serialize(modernParse(text));
    }
}
