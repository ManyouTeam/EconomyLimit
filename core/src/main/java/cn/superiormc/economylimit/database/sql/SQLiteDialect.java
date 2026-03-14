package cn.superiormc.economylimit.database.sql;

public final class SQLiteDialect extends DatabaseDialect {

    @Override
    public boolean matches(String jdbcUrl) {
        return jdbcUrl.startsWith("jdbc:sqlite:");
    }

    @Override
    public int maxPoolSize() {
        return 1;
    }

    @Override
    public int minIdle() {
        return 1;
    }

    @Override
    public boolean forceSingleConnection() {
        return true;
    }

    @Override
    public String createPlayerAccountTable() {
        return """
                CREATE TABLE IF NOT EXISTS economylimit_player_accounts (
                  uuid TEXT PRIMARY KEY,
                  bank_balance REAL NOT NULL DEFAULT 0
                )
                """;
    }

    @Override
    public String createRuleProgressTable() {
        return """
                CREATE TABLE IF NOT EXISTS economylimit_rule_progress (
                  uuid TEXT NOT NULL,
                  rule_id TEXT NOT NULL,
                  earned_amount REAL NOT NULL DEFAULT 0,
                  PRIMARY KEY (uuid, rule_id)
                )
                """;
    }

    @Override
    public String createRuleResetTable() {
        return """
                CREATE TABLE IF NOT EXISTS economylimit_rule_resets (
                  rule_id TEXT PRIMARY KEY,
                  next_reset TEXT
                )
                """;
    }
}
