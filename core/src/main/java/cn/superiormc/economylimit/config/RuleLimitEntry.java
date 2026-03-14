package cn.superiormc.economylimit.config;

public record RuleLimitEntry(
        String conditionType,
        String conditionValue,
        double limit
) {
}
