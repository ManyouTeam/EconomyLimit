package cn.superiormc.economylimit.config;

import cn.superiormc.economylimit.managers.ConditionManager;
import org.bukkit.OfflinePlayer;

import java.util.List;

public record RuleDefinition(
        String id,
        String displayName,
        ResetPolicy resetPolicy,
        List<RuleLimitEntry> limits
) {

    public double resolveLimit(OfflinePlayer player, ConditionManager conditionManager) {
        double fallback = -1D;
        for (RuleLimitEntry entry : limits) {
            if ("ANY".equalsIgnoreCase(entry.conditionType())) {
                fallback = entry.limit();
                continue;
            }
            if (conditionManager.matches(entry.conditionType(), entry.conditionValue(), player)) {
                return entry.limit();
            }
        }
        return fallback;
    }
}
