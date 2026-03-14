package cn.superiormc.economylimit.spigot;

import cn.superiormc.economylimit.utils.SpecialMethodUtil;
import cn.superiormc.economylimit.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SpigotMethodUtil implements SpecialMethodUtil {

    @Override
    public String methodID() {
        return "spigot";
    }

    @Override
    public void sendChat(Player player, String text) {
        String parsed = TextUtil.colorize(text);
        if (player == null) {
            Bukkit.getConsoleSender().sendMessage(parsed);
            return;
        }
        player.sendMessage(parsed);
    }

    @Override
    public String legacyParse(String text) {
        return TextUtil.colorize(text);
    }
}
