package cn.superiormc.economylimit.database.sql;

public final class MySQLDialect extends DatabaseDialect {

    @Override
    public boolean matches(String jdbcUrl) {
        return jdbcUrl.startsWith("jdbc:mysql:") || jdbcUrl.startsWith("jdbc:mariadb:");
    }

    @Override
    public int maxPoolSize() {
        return 10;
    }

    @Override
    public int minIdle() {
        return 2;
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
                  next_reset TEXT
                )
                """;
    }

    @Override
    public void needExtraDownload(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:mariadb:")) {
            loadDriver("mariadb-java-client",
                    "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.1.4/mariadb-java-client-3.1.4.jar",
                    "org.mariadb.jdbc.Driver");
        }
    }
}
