package cn.superiormc.economylimit.database;

import cn.superiormc.economylimit.storage.PlayerAccount;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractDatabase {

    public void onInit() throws SQLException {
    }

    public void onClose() {
    }

    public abstract boolean isEmpty() throws SQLException;

    public abstract void load(Map<UUID, PlayerAccount> accounts, Map<String, ZonedDateTime> nextResets) throws SQLException;

    public abstract void saveAll(Map<UUID, PlayerAccount> accounts, Map<String, ZonedDateTime> nextResets) throws SQLException;

    public abstract void saveAccount(UUID playerId, PlayerAccount account) throws SQLException;

    public abstract void deleteAccount(UUID playerId) throws SQLException;

    public abstract void saveNextReset(String ruleId, ZonedDateTime nextReset) throws SQLException;

    public abstract void deleteNextReset(String ruleId) throws SQLException;

    public abstract void resetRuleProgress(String ruleId) throws SQLException;
}
