package cn.superiormc.economylimit.config;

import org.bukkit.configuration.ConfigurationSection;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

public record ResetPolicy(
        ResetMode mode,
        LocalTime time,
        DayOfWeek dayOfWeek,
        int dayOfMonth
) {

    public static ResetPolicy from(String ruleId, ConfigurationSection section) {
        if (section == null) {
            throw new IllegalStateException("Rule " + ruleId + " is missing the reset section.");
        }

        ResetMode mode = ResetMode.valueOf(section.getString("mode", "DAILY").toUpperCase());
        LocalTime time = LocalTime.parse(section.getString("time", "00:00"));
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(section.getString("day-of-week", "MONDAY").toUpperCase());
        int dayOfMonth = Math.max(1, section.getInt("day-of-month", 1));
        return new ResetPolicy(mode, time, dayOfWeek, dayOfMonth);
    }

    public boolean resettable() {
        return mode != ResetMode.NEVER;
    }

    public ZonedDateTime nextResetAfter(ZonedDateTime reference) {
        return switch (mode) {
            case NEVER -> null;
            case DAILY -> {
                ZonedDateTime candidate = reference.with(time);
                if (!candidate.isAfter(reference)) {
                    candidate = candidate.plusDays(1);
                }
                yield candidate;
            }
            case WEEKLY -> {
                ZonedDateTime candidate = reference.with(dayOfWeek).with(time);
                if (!candidate.isAfter(reference)) {
                    candidate = candidate.plusWeeks(1).with(dayOfWeek).with(time);
                }
                yield candidate;
            }
            case MONTHLY -> {
                LocalDate baseDate = reference.toLocalDate();
                int currentDay = Math.min(dayOfMonth, baseDate.lengthOfMonth());
                LocalDateTime dateTime = LocalDateTime.of(baseDate.withDayOfMonth(currentDay), time);
                ZonedDateTime candidate = ZonedDateTime.of(dateTime, reference.getZone());
                if (!candidate.isAfter(reference)) {
                    LocalDate nextMonth = baseDate.plusMonths(1);
                    int nextDay = Math.min(dayOfMonth, nextMonth.lengthOfMonth());
                    candidate = ZonedDateTime.of(nextMonth.withDayOfMonth(nextDay), time, reference.getZone());
                }
                yield candidate;
            }
        };
    }
}
