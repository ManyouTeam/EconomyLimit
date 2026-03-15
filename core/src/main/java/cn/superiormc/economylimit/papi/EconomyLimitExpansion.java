package cn.superiormc.economylimit.papi;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.config.RuleDefinition;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class EconomyLimitExpansion extends PlaceholderExpansion {

    private final EconomyLimitPlugin plugin;

    public EconomyLimitExpansion(EconomyLimitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "economylimit";
    }

    @Override
    public String getAuthor() {
        return "PQguanfang";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || params == null || params.isBlank()) {
            return "";
        }

        String normalized = params.toLowerCase();
        if (normalized.equals("bank_balance") || normalized.equals("bank")) {
            return plugin.getEarningLimitService().getBankBalance(player);
        }

        if (!normalized.startsWith("rule_")) {
            return null;
        }

        String payload = params.substring("rule_".length());
        String[] suffixes = {
                "_next_reset",
                "_remaining",
                "_progress",
                "_current",
                "_earned",
                "_limit",
                "_reset",
                "_name"
        };

        String ruleId = null;
        String type = null;
        for (String suffix : suffixes) {
            if (normalized.substring("rule_".length()).endsWith(suffix)) {
                ruleId = payload.substring(0, payload.length() - suffix.length());
                type = suffix.substring(1);
                break;
            }
        }

        if (ruleId == null || ruleId.isBlank() || type == null) {
            return null;
        }

        RuleDefinition rule = plugin.getEarningLimitService().findRule(ruleId);
        if (rule == null) {
            return null;
        }

        if (!(player instanceof Player onlinePlayer)) {
            return null;
        }

        return switch (type) {
            case "name" -> plugin.getEarningLimitService().getRuleDisplayName(onlinePlayer, ruleId);
            case "earned", "progress", "current" -> plugin.getEarningLimitService().getRuleProgress(player, ruleId);
            case "limit" -> plugin.getEarningLimitService().getRuleLimit(player, ruleId);
            case "remaining" -> plugin.getEarningLimitService().getRuleRemaining(player, ruleId);
            case "next_reset", "reset" -> plugin.getEarningLimitService().getRuleNextReset(onlinePlayer, ruleId);
            default -> null;
        };
    }
}
