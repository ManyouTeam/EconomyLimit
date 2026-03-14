package cn.superiormc.economylimit.database.sql;

public final class H2Dialect extends DatabaseDialect {

    @Override
    public boolean matches(String jdbcUrl) {
        return jdbcUrl.startsWith("jdbc:h2:");
    }

    @Override
    public int maxPoolSize() {
        return 5;
    }

    @Override
    public int minIdle() {
        return 1;
    }

    @Override
    public String createPlayerAccountTable() {
        return """
                CREATE TABLE IF NOT EXISTS economylimit_player_accounts (
                  uuid VARCHAR(36) PRIMARY KEY,
                  bank_balance DOUBLE NOT NULL DEFAULT 0
                )
                """;
    }

    @Override
    public String createRuleProgressTable() {
        return """
                CREATE TABLE IF NOT EXISTS economylimit_rule_progress (
                  uuid VARCHAR(36) NOT NULL,
                  rule_id VARCHAR(64) NOT NULL,
                  earned_amount DOUBLE NOT NULL DEFAULT 0,
                  PRIMARY KEY (uuid, rule_id)
                )
                """;
    }

    @Override
    public String createRuleResetTable() {
        return """
                CREATE TABLE IF NOT EXISTS economylimit_rule_resets (
                  rule_id VARCHAR(64) PRIMARY KEY,
                  next_reset VARCHAR
                )
                """;
    }

    @Override
    public void needExtraDownload(String jdbcUrl) {
        loadDriver("h2",
                "https://repo1.maven.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar",
                "org.h2.Driver");
    }
}
