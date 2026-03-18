package cn.superiormc.economylimit.managers;

import cn.superiormc.economylimit.EconomyLimitPlugin;

import java.io.File;

public final class InitManager {

    public static InitManager initManager;

    private final EconomyLimitPlugin plugin;

    public InitManager(EconomyLimitPlugin plugin) {
        initManager = this;
        this.plugin = plugin;
        init();
    }

    public void init() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            return;
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }

        resourceOutput("languages/en_US.yml");
        resourceOutput("languages/zh_CN.yml");
    }

    private void resourceOutput(String fileName) {
        File target = new File(plugin.getDataFolder(), fileName);
        if (!target.exists()) {
            plugin.saveResource(fileName, false);
        }
    }
}
