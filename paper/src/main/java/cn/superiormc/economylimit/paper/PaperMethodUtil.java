package cn.superiormc.economylimit.paper;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.paper.utils.PaperTextUtil;
import cn.superiormc.economylimit.utils.SpecialMethodUtil;
import cn.superiormc.economylimit.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PaperMethodUtil implements SpecialMethodUtil {

    @Override
    public String methodID() {
        return "paper";
    }

    @Override
    public void sendChat(Player player, String text) {
        if (player == null) {
            Bukkit.getConsoleSender().sendMessage(PaperTextUtil.toLegacy(text));
            return;
        }
        player.sendMessage(PaperTextUtil.modernParse(text));
    }

    @Override
    public String legacyParse(String text) {
        EconomyLimitPlugin plugin = EconomyLimitPlugin.getInstance();
        if (plugin != null && !plugin.getConfig().getBoolean("config-files.force-parse-mini-message", true)) {
            return TextUtil.colorize(text);
        }
        return PaperTextUtil.toLegacy(text == null ? "" : text);
    }
}
