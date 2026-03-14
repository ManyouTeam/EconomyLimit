package cn.superiormc.economylimit.storage;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.config.PluginSettings;
import cn.superiormc.economylimit.database.DatabaseSettings;
import cn.superiormc.economylimit.database.SQLDatabase;
import cn.superiormc.economylimit.utils.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StorageService {

    private final EconomyLimitPlugin plugin;
    private final SQLDatabase database;
    private final File legacyDataFile;
    private final Map<UUID, PlayerAccount> accounts = new ConcurrentHashMap<>();
    private final Map<String, ZonedDateTime> nextResets = new ConcurrentHashMap<>();

    public StorageService(EconomyLimitPlugin plugin, DatabaseSettings databaseSettings) {
        this.plugin = plugin;
        this.database = new SQLDatabase(plugin, databaseSettings);
        this.legacyDataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load(PluginSettings settings) {
        try {
            database.onInit();
            if (database.isEmpty() && legacyDataFile.exists()) {
                loadLegacyYaml();
                save(settings);
                migrateLegacyFile();
                return;
            }
            database.load(accounts, nextResets);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load database: " + exception.getMessage(), exception);
        }
    }

    public void save(PluginSettings settings) {
        try {
            database.saveAll(accounts, nextResets);
        } catch (SQLException exception) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cFailed to save database: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    public void close() {
        database.onClose();
    }

    private void loadLegacyYaml() {
        accounts.clear();
        nextResets.clear();

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(legacyDataFile);
        ConfigurationSection playersSection = configuration.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidKey : playersSection.getKeys(false)) {
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidKey);
                if (playerSection == null) {
                    continue;
                }

                try {
                    UUID uuid = UUID.fromString(uuidKey);
                    PlayerAccount account = new PlayerAccount();
                    account.setBankBalance(playerSection.getDouble("bank", 0D));

                    ConfigurationSection rulesSection = playerSection.getConfigurationSection("rules");
                    if (rulesSection != null) {
                        for (String ruleId : rulesSection.getKeys(false)) {
                            account.setRuleProgress(ruleId, rulesSection.getDouble(ruleId, 0D));
                        }
                    }

                    accounts.put(uuid, account);
                } catch (IllegalArgumentException ignored) {
                    TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cSkipping invalid UUID in data.yml: " + uuidKey);
                }
            }
        }

        ConfigurationSection rulesSection = configuration.getConfigurationSection("rules");
        if (rulesSection != null) {
            for (String ruleId : rulesSection.getKeys(false)) {
                String rawNextReset = rulesSection.getString(ruleId + ".next-reset");
                if (rawNextReset != null && !rawNextReset.isBlank()) {
                    nextResets.put(ruleId, ZonedDateTime.parse(rawNextReset));
                }
            }
        }
    }

    private void migrateLegacyFile() {
        File migratedFile = new File(plugin.getDataFolder(), "data.yml.migrated");
        if (!legacyDataFile.renameTo(migratedFile)) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cLegacy data.yml was imported but could not be renamed.");
        }
    }

    public PlayerAccount getAccount(UUID playerId) {
        return accounts.computeIfAbsent(playerId, ignored -> new PlayerAccount());
    }

    public ZonedDateTime getNextReset(String ruleId) {
        return nextResets.get(ruleId);
    }

    public void setNextReset(String ruleId, ZonedDateTime nextReset) {
        if (nextReset == null) {
            nextResets.remove(ruleId);
            try {
                database.deleteNextReset(ruleId);
            } catch (SQLException exception) {
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cFailed to delete next reset for rule " + ruleId + ": " + exception.getMessage());
            }
        } else {
            nextResets.put(ruleId, nextReset);
            try {
                database.saveNextReset(ruleId, nextReset);
            } catch (SQLException exception) {
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cFailed to save next reset for rule " + ruleId + ": " + exception.getMessage());
            }
        }
    }

    public void resetRule(String ruleId) {
        for (PlayerAccount account : accounts.values()) {
            account.clearRuleProgress(ruleId);
        }
        try {
            database.resetRuleProgress(ruleId);
        } catch (SQLException exception) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cFailed to reset rule progress for " + ruleId + ": " + exception.getMessage());
        }
    }

    public void cleanup(UUID playerId) {
        PlayerAccount account = accounts.get(playerId);
        if (account == null) {
            return;
        }
        try {
            if (account.isEmpty()) {
                accounts.remove(playerId);
                database.deleteAccount(playerId);
            } else {
                database.saveAccount(playerId, account);
            }
        } catch (SQLException exception) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cFailed to persist account " + playerId + ": " + exception.getMessage());
        }
    }
}
