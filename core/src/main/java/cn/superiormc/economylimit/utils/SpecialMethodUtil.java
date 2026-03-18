package cn.superiormc.economylimit.utils;

import org.bukkit.entity.Player;

public interface SpecialMethodUtil {

    String methodID();

    void sendChat(Player player, String text);

    void sendTitle(Player player, String title, String subTitle, int fadeIn, int stay, int fadeOut);

    void sendActionBar(Player player, String text);

    void sendBossBar(Player player, String title, float progress, String color, String style);

    String legacyParse(String text);
}
