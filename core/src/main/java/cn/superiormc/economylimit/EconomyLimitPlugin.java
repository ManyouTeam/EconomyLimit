package cn.superiormc.economylimit;

import cn.superiormc.economylimit.config.PluginSettings;
import cn.superiormc.economylimit.inject.VaultInjectionManager;
import cn.superiormc.economylimit.managers.CommandManager;
import cn.superiormc.economylimit.managers.ConditionManager;
import cn.superiormc.economylimit.managers.ConfigManager;
import cn.superiormc.economylimit.managers.HookManager;
import cn.superiormc.economylimit.managers.InitManager;
import cn.superiormc.economylimit.managers.LanguageManager;
import cn.superiormc.economylimit.managers.TaskManager;
import cn.superiormc.economylimit.service.EarningLimitService;
import cn.superiormc.economylimit.storage.StorageService;
import cn.superiormc.economylimit.utils.CommonUtil;
import cn.superiormc.economylimit.utils.SpecialMethodUtil;
import cn.superiormc.economylimit.utils.TextUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyLimitPlugin extends JavaPlugin {

    private static EconomyLimitPlugin instance;

    private InitManager initManager;
    private ConfigManager configManager;
    private HookManager hookManager;
    private TaskManager taskManager;
    private ConditionManager conditionManager;
    private LanguageManager languageManager;
    private CommandManager commandManager;
    private SpecialMethodUtil methodUtil;
    private StorageService storageService;
    private EarningLimitService earningLimitService;
    private boolean fullVersion;

    public static EconomyLimitPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        initManager = new InitManager(this);
        methodUtil = createMethodUtil();

        initLicense(
                "cn.superiormc.ultimateshop.UltimateShop",
                "cn.superiormc.mythicprefixes.MythicPrefixes",
                "cn.superiormc.mythictotem.MythicTotem",
                "cn.superiormc.mythicchanger.MythicChanger"
        );

        configManager = new ConfigManager(this);
        hookManager = new HookManager(this);
        taskManager = new TaskManager(this);
        reloadPluginState();

        if (commandManager == null) {
            commandManager = new CommandManager(this);
        }

        if (!hookManager.initVaultHook()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " &fDatabase: &e" + getSettings().databaseSettings().jdbcUrl());
        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " &fRules loaded: &e" + getSettings().rules().size());
        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " &fPlugin is loaded. Author: PQguanfang.");
    }

    @Override
    public void onDisable() {
        if (taskManager != null) {
            taskManager.cancelTask();
        }
        if (hookManager != null) {
            hookManager.shutdown();
        }
        if (storageService != null && getSettings() != null) {
            storageService.save(getSettings());
            storageService.close();
        }
    }

    public void reloadPluginState() {
        if (taskManager != null) {
            taskManager.cancelTask();
        }
        if (storageService != null && getSettings() != null) {
            storageService.save(getSettings());
            storageService.close();
        }

        configManager.reload();
        conditionManager = new ConditionManager();
        languageManager = new LanguageManager(this, getSettings());
        storageService = new StorageService(this, getSettings().databaseSettings());
        storageService.load(getSettings());
        earningLimitService = new EarningLimitService(this, getSettings(), storageService);
        earningLimitService.initialize();

        if (isEnabled()) {
            taskManager.reload();
            hookManager.reloadPlaceholderHook();
        }
    }

    public PluginSettings getSettings() {
        return configManager == null ? null : configManager.getSettings();
    }

    public InitManager getInitManager() {
        return initManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public EarningLimitService getEarningLimitService() {
        return earningLimitService;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public ConditionManager getConditionManager() {
        return conditionManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public SpecialMethodUtil getMethodUtil() {
        return methodUtil;
    }

    public boolean isFullVersion() {
        return fullVersion;
    }

    public VaultInjectionManager getVaultInjectionManager() {
        return hookManager == null ? null : hookManager.getVaultInjectionManager();
    }

    public EconomyResponse depositToPlayer(Player player, double amount) {
        Economy economy = getEconomy();
        if (economy == null) {
            return null;
        }
        return economy.depositPlayer(player, amount);
    }

    public Economy getEconomy() {
        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        return registration == null ? null : registration.getProvider();
    }

    private SpecialMethodUtil createMethodUtil() {
        if (CommonUtil.getClass("net.kyori.adventure.text.minimessage.MiniMessage")) {
            try {
                Class<?> paperClass = Class.forName("cn.superiormc.economylimit.paper.PaperMethodUtil");
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " &fPaper is found, entering Paper plugin mode...");
                return (SpecialMethodUtil) paperClass.getDeclaredConstructor().newInstance();
            } catch (Throwable throwable) {
                throw new IllegalStateException("Failed to initialize Paper text mode.", throwable);
            }
        }

        try {
            Class<?> spigotClass = Class.forName("cn.superiormc.economylimit.spigot.SpigotMethodUtil");
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " &fSpigot is found, entering Spigot plugin mode...");
            return (SpecialMethodUtil) spigotClass.getDeclaredConstructor().newInstance();
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to initialize Spigot text mode.", throwable);
        }
    }

    public void initLicense(String... classNames) {
        fullVersion = false;
        for (String name : classNames) {
            try {
                Class<?> clazz = Class.forName(name);
                boolean value = clazz.getField("freeVersion").getBoolean(null);
                if (!value) {
                    fullVersion = true;
                    TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " &cFULL license active by " + clazz.getSimpleName() + " plugin, thanks for your support and we hope you have good experience with this plugin!");
                    break;
                }
            } catch (Throwable ignored) {
                // ignored
            }
        }
    }
}
