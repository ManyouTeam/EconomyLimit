package cn.superiormc.economylimit.utils;

import org.bukkit.entity.Player;

public interface SpecialMethodUtil {

    String methodID();

    void sendChat(Player player, String text);

    String legacyParse(String text);
}
