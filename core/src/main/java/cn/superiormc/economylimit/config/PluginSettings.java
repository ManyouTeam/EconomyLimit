package cn.superiormc.economylimit.config;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.database.DatabaseSettings;
import org.bukkit.configuration.ConfigurationSection;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record PluginSettings(
        ZoneId zoneId,
        String timeFormat,
        long autoSaveMinutes,
        boolean notifyOnBankTransfer,
        String defaultLanguage,
        boolean perPlayerLanguage,
        DatabaseSettings databaseSettings,
        List<RuleDefinition> rules
) {

    public static PluginSettings load(EconomyLimitPlugin plugin) {
        String zoneValue = plugin.getConfig().getString("timezone", "system");
        ZoneId zoneId = "system".equalsIgnoreCase(zoneValue) ? ZoneId.systemDefault() : ZoneId.of(zoneValue);
        String timeFormat = plugin.getConfig().getString("time-format", "yyyy-MM-dd HH:mm");
        DateTimeFormatter.ofPattern(timeFormat);
        long autoSaveMinutes = Math.max(1L, plugin.getConfig().getLong("auto-save-minutes", 5L));
        boolean notifyOnBankTransfer = plugin.getConfig().getBoolean("notify-on-bank-transfer", true);
        String defaultLanguage = plugin.getConfig().getString("config-files.language", "zh_CN");
        boolean perPlayerLanguage = plugin.getConfig().getBoolean("config-files.per-player-language", true);
        DatabaseSettings databaseSettings = DatabaseSettings.load(plugin);

        ConfigurationSection rulesSection = plugin.getConfig().getConfigurationSection("rules");
        if (rulesSection == null || rulesSection.getKeys(false).isEmpty()) {
            throw new IllegalStateException("No rules configured in config.yml");
        }

        List<RuleDefinition> rules = new ArrayList<>();
        for (String ruleId : rulesSection.getKeys(false)) {
            ConfigurationSection ruleSection = rulesSection.getConfigurationSection(ruleId);
            if (ruleSection == null) {
                continue;
            }

            String displayName = ruleSection.getString("display-name", "{lang:rules." + ruleId + ".name}");
            ResetPolicy resetPolicy = ResetPolicy.from(ruleId, ruleSection.getConfigurationSection("reset"));
            List<RuleLimitEntry> limits = new ArrayList<>();
            List<Map<?, ?>> rawLimits = ruleSection.getMapList("limits");
            if (rawLimits.isEmpty()) {
                throw new IllegalStateException("Rule " + ruleId + " must define at least one limit.");
            }

            for (Map<?, ?> rawLimit : rawLimits) {
                Object limitObject = rawLimit.get("limit");
                if (!(limitObject instanceof Number number)) {
                    throw new IllegalStateException("Rule " + ruleId + " has a non-numeric limit.");
                }

                Map<?, ?> conditionMap = rawLimit.get("condition") instanceof Map<?, ?> map ? map : null;
                String conditionType = "ANY";
                String conditionValue = "";
                if (conditionMap != null) {
                    Object type = conditionMap.get("type");
                    Object value = conditionMap.get("value");
                    conditionType = type == null ? "ANY" : String.valueOf(type).trim().toUpperCase();
                    conditionValue = value == null ? "" : String.valueOf(value);
                }
                limits.add(new RuleLimitEntry(conditionType, conditionValue, number.doubleValue()));
            }

            rules.add(new RuleDefinition(ruleId, displayName, resetPolicy, limits));
        }

        return new PluginSettings(
                zoneId,
                timeFormat,
                autoSaveMinutes,
                notifyOnBankTransfer,
                defaultLanguage,
                perPlayerLanguage,
                databaseSettings,
                List.copyOf(rules)
        );
    }
}
