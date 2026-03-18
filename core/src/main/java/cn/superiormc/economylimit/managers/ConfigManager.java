package cn.superiormc.economylimit.managers;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.config.PluginSettings;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigManager {

    public static ConfigManager configManager;

    private final EconomyLimitPlugin plugin;
    private PluginSettings settings;

    public ConfigManager(EconomyLimitPlugin plugin) {
        configManager = this;
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        settings = PluginSettings.load(plugin);
    }

    public PluginSettings getSettings() {
        return settings;
    }

    public FileConfiguration getSection() {
        return plugin.getConfig();
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return plugin.getConfig().getBoolean(path, defaultValue);
    }

    public String getString(String path, String defaultValue) {
        return plugin.getConfig().getString(path, defaultValue);
    }
}
