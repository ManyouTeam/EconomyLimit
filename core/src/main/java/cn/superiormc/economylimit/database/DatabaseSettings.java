package cn.superiormc.economylimit.database;

import cn.superiormc.economylimit.EconomyLimitPlugin;

public record DatabaseSettings(
        String jdbcUrl,
        String jdbcClass,
        String user,
        String password
) {

    public static DatabaseSettings load(EconomyLimitPlugin plugin) {
        String jdbcUrl = plugin.getConfig().getString("database.jdbc-url", "jdbc:sqlite:plugins/EconomyLimit/data/economylimit.db");
        String jdbcClass = plugin.getConfig().getString("database.jdbc-class", "org.sqlite.JDBC");
        String user = plugin.getConfig().getString("database.properties.user", "");
        String password = plugin.getConfig().getString("database.properties.password", "");
        return new DatabaseSettings(
                jdbcUrl == null || jdbcUrl.isBlank() ? "jdbc:sqlite:plugins/EconomyLimit/data/economylimit.db" : jdbcUrl,
                jdbcClass == null ? "" : jdbcClass,
                user == null ? "" : user,
                password == null ? "" : password
        );
    }
}
