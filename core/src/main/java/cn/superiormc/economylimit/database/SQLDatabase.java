package cn.superiormc.economylimit.database;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.database.sql.DatabaseDialect;
import cn.superiormc.economylimit.database.sql.H2Dialect;
import cn.superiormc.economylimit.database.sql.MySQLDialect;
import cn.superiormc.economylimit.database.sql.PostgreSQLDialect;
import cn.superiormc.economylimit.database.sql.SQLiteDialect;
import cn.superiormc.economylimit.storage.PlayerAccount;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SQLDatabase extends AbstractDatabase {

    private final EconomyLimitPlugin plugin;
    private final DatabaseSettings settings;
    private HikariDataSource dataSource;
    private DatabaseDialect dialect;

    public SQLDatabase(EconomyLimitPlugin plugin, DatabaseSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @Override
    public void onInit() throws SQLException {
        onClose();
        initDialect(settings.jdbcUrl());
        dialect.needExtraDownload(settings.jdbcUrl());
        loadConfiguredDriver();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl());
        if (!settings.user().isEmpty()) {
            config.setUsername(settings.user());
            config.setPassword(settings.password());
        }
        config.setPoolName("EconomyLimit-Hikari");
        config.setMaximumPoolSize(dialect.maxPoolSize());
        config.setMinimumIdle(dialect.minIdle());
        if (dialect.forceSingleConnection()) {
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
        }
        dataSource = new HikariDataSource(config);
        createTables();
    }

    @Override
    public void onClose() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public boolean isEmpty() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(1) FROM economylimit_player_accounts")) {
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return false;
            }
        }
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(1) FROM economylimit_rule_resets")) {
            return !resultSet.next() || resultSet.getInt(1) == 0;
        }
    }

    @Override
    public void load(Map<UUID, PlayerAccount> accounts, Map<String, ZonedDateTime> nextResets) throws SQLException {
        accounts.clear();
        nextResets.clear();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT uuid, bank_balance FROM economylimit_player_accounts");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                PlayerAccount account = new PlayerAccount();
                account.setBankBalance(resultSet.getDouble("bank_balance"));
                accounts.put(uuid, account);
            }
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT uuid, rule_id, earned_amount FROM economylimit_rule_progress");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                PlayerAccount account = accounts.computeIfAbsent(uuid, ignored -> new PlayerAccount());
                account.setRuleProgress(resultSet.getString("rule_id"), resultSet.getDouble("earned_amount"));
            }
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT rule_id, next_reset FROM economylimit_rule_resets");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String nextReset = resultSet.getString("next_reset");
                if (nextReset != null && !nextReset.isBlank()) {
                    nextResets.put(resultSet.getString("rule_id"), ZonedDateTime.parse(nextReset));
                }
            }
        }
    }

    @Override
    public void saveAll(Map<UUID, PlayerAccount> accounts, Map<String, ZonedDateTime> nextResets) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM economylimit_rule_progress");
                statement.executeUpdate("DELETE FROM economylimit_player_accounts");
                statement.executeUpdate("DELETE FROM economylimit_rule_resets");
            }

            try (PreparedStatement accountStatement = connection.prepareStatement(
                    "INSERT INTO economylimit_player_accounts(uuid, bank_balance) VALUES(?, ?)");
                 PreparedStatement progressStatement = connection.prepareStatement(
                         "INSERT INTO economylimit_rule_progress(uuid, rule_id, earned_amount) VALUES(?, ?, ?)");
                 PreparedStatement resetStatement = connection.prepareStatement(
                         "INSERT INTO economylimit_rule_resets(rule_id, next_reset) VALUES(?, ?)")) {

                for (Map.Entry<UUID, PlayerAccount> entry : accounts.entrySet()) {
                    if (entry.getValue().isEmpty()) {
                        continue;
                    }
                    accountStatement.setString(1, entry.getKey().toString());
                    accountStatement.setDouble(2, entry.getValue().getBankBalance());
                    accountStatement.addBatch();

                    for (Map.Entry<String, Double> ruleEntry : entry.getValue().getRuleProgress().entrySet()) {
                        progressStatement.setString(1, entry.getKey().toString());
                        progressStatement.setString(2, ruleEntry.getKey());
                        progressStatement.setDouble(3, ruleEntry.getValue());
                        progressStatement.addBatch();
                    }
                }

                for (Map.Entry<String, ZonedDateTime> entry : nextResets.entrySet()) {
                    resetStatement.setString(1, entry.getKey());
                    resetStatement.setString(2, entry.getValue() == null ? null : entry.getValue().toString());
                    resetStatement.addBatch();
                }

                accountStatement.executeBatch();
                progressStatement.executeBatch();
                resetStatement.executeBatch();
            }
            connection.commit();
        }
    }

    @Override
    public void saveAccount(UUID playerId, PlayerAccount account) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            deleteAccount(connection, playerId);
            if (!account.isEmpty()) {
                try (PreparedStatement accountStatement = connection.prepareStatement(
                        "INSERT INTO economylimit_player_accounts(uuid, bank_balance) VALUES(?, ?)");
                     PreparedStatement progressStatement = connection.prepareStatement(
                             "INSERT INTO economylimit_rule_progress(uuid, rule_id, earned_amount) VALUES(?, ?, ?)")) {
                    accountStatement.setString(1, playerId.toString());
                    accountStatement.setDouble(2, account.getBankBalance());
                    accountStatement.executeUpdate();
                    for (Map.Entry<String, Double> ruleEntry : account.getRuleProgress().entrySet()) {
                        progressStatement.setString(1, playerId.toString());
                        progressStatement.setString(2, ruleEntry.getKey());
                        progressStatement.setDouble(3, ruleEntry.getValue());
                        progressStatement.addBatch();
                    }
                    progressStatement.executeBatch();
                }
            }
            connection.commit();
        }
    }

    @Override
    public void deleteAccount(UUID playerId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            deleteAccount(connection, playerId);
        }
    }

    @Override
    public void saveNextReset(String ruleId, ZonedDateTime nextReset) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            deleteNextReset(connection, ruleId);
            if (nextReset != null) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO economylimit_rule_resets(rule_id, next_reset) VALUES(?, ?)")) {
                    statement.setString(1, ruleId);
                    statement.setString(2, nextReset.toString());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    @Override
    public void deleteNextReset(String ruleId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            deleteNextReset(connection, ruleId);
        }
    }

    @Override
    public void resetRuleProgress(String ruleId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM economylimit_rule_progress WHERE rule_id = ?")) {
            statement.setString(1, ruleId);
            statement.executeUpdate();
        }
    }

    private void initDialect(String jdbcUrl) {
        List<DatabaseDialect> dialects = List.of(
                new MySQLDialect(),
                new PostgreSQLDialect(),
                new H2Dialect(),
                new SQLiteDialect()
        );
        this.dialect = dialects.stream().filter(singleDialect -> singleDialect.matches(jdbcUrl)).findFirst().orElse(new SQLiteDialect());
    }

    private void loadConfiguredDriver() throws SQLException {
        if (!settings.jdbcClass().isEmpty()) {
            try {
                Class.forName(settings.jdbcClass());
            } catch (ClassNotFoundException exception) {
                throw new SQLException("Can not load JDBC driver: " + settings.jdbcClass(), exception);
            }
        }
    }

    private void createTables() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(dialect.createPlayerAccountTable());
            statement.execute(dialect.createRuleProgressTable());
            statement.execute(dialect.createRuleResetTable());
        }
    }

    private void deleteAccount(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement deleteAccount = connection.prepareStatement("DELETE FROM economylimit_player_accounts WHERE uuid = ?");
             PreparedStatement deleteProgress = connection.prepareStatement("DELETE FROM economylimit_rule_progress WHERE uuid = ?")) {
            deleteAccount.setString(1, playerId.toString());
            deleteAccount.executeUpdate();
            deleteProgress.setString(1, playerId.toString());
            deleteProgress.executeUpdate();
        }
    }

    private void deleteNextReset(Connection connection, String ruleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM economylimit_rule_resets WHERE rule_id = ?")) {
            statement.setString(1, ruleId);
            statement.executeUpdate();
        }
    }
}
