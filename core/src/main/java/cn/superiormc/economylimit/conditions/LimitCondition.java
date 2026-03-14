package cn.superiormc.economylimit.conditions;

import org.bukkit.OfflinePlayer;

public interface LimitCondition {

    String getType();

    boolean check(OfflinePlayer player, String value);
}
