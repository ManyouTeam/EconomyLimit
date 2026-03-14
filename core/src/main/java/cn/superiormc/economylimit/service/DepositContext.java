package cn.superiormc.economylimit.service;

import java.util.List;
import java.util.UUID;

public record DepositContext(
        UUID playerId,
        String playerName,
        List<String> trackedRules,
        double directAmount,
        double bankedAmount
) {

    public boolean shouldSkipOriginal() {
        return directAmount <= 0D && bankedAmount > 0D;
    }
}
