package cn.superiormc.economylimit.managers;

import cn.superiormc.economylimit.conditions.LimitCondition;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ConditionManager {

    private final Map<String, LimitCondition> registeredConditions = new HashMap<>();

    public ConditionManager() {
        registerBuiltInConditions();
    }

    private void registerBuiltInConditions() {
        registerCondition(new LimitCondition() {
            @Override
            public String getType() {
                return "ANY";
            }

            @Override
            public boolean check(OfflinePlayer player, String value) {
                return true;
            }
        });
        registerCondition(new LimitCondition() {
            @Override
            public String getType() {
                return "PERMISSION";
            }

            @Override
            public boolean check(OfflinePlayer player, String value) {
                return player instanceof Player online && online.hasPermission(value);
            }
        });
        registerCondition(new LimitCondition() {
            @Override
            public String getType() {
                return "WORLD";
            }

            @Override
            public boolean check(OfflinePlayer player, String value) {
                return player instanceof Player online && online.getWorld().getName().equalsIgnoreCase(value);
            }
        });
        registerCondition(new LimitCondition() {
            @Override
            public String getType() {
                return "PLAYER";
            }

            @Override
            public boolean check(OfflinePlayer player, String value) {
                return player.getName() != null && player.getName().equalsIgnoreCase(value);
            }
        });
        registerCondition(new LimitCondition() {
            @Override
            public String getType() {
                return "OP";
            }

            @Override
            public boolean check(OfflinePlayer player, String value) {
                return player.isOp();
            }
        });
    }

    public void registerCondition(LimitCondition condition) {
        registeredConditions.put(condition.getType().toUpperCase(Locale.ROOT), condition);
    }

    public boolean matches(String type, String value, OfflinePlayer player) {
        LimitCondition condition = registeredConditions.getOrDefault(type.toUpperCase(Locale.ROOT), registeredConditions.get("ANY"));
        return condition != null && condition.check(player, value);
    }
}
