package cn.superiormc.economylimit;

import cn.superiormc.economylimit.config.PluginSettings;
import cn.superiormc.economylimit.inject.VaultInjectionManager;
import cn.superiormc.economylimit.managers.CommandManager;
import cn.superiormc.economylimit.managers.ConditionManager;
import cn.superiormc.economylimit.managers.LanguageManager;
import cn.superiormc.economylimit.papi.EconomyLimitExpansion;
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
import org.bukkit.scheduler.BukkitTask;

public final class EconomyLimitPlugin extends JavaPlugin {

    private static EconomyLimitPlugin instance;

    private PluginSettings settings;
    private ConditionManager conditionManager;
    private LanguageManager languageManager;
    private CommandManager commandManager;
    private SpecialMethodUtil methodUtil;
    private StorageService storageService;
    private EarningLimitService earningLimitService;
    private VaultInjectionManager vaultInjectionManager;
    private EconomyLimitExpansion placeholderExpansion;
    private BukkitTask resetTask;
    private BukkitTask autoSaveTask;
    private boolean fullVersion;

    public static EconomyLimitPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        methodUtil = createMethodUtil();

        initLicense("cn.superiormc.ultimateshop.UltimateShop",
                "cn.superiormc.mythicprefixes.MythicPrefixes",
                "cn.superiormc.mythictotem.MythicTotem",
                "cn.superiormc.mythicchanger.MythicChanger");

        reloadPluginState();
        if (commandManager == null) {
            commandManager = new CommandManager(this);
        }

        vaultInjectionManager = new VaultInjectionManager(this);
        if (!vaultInjectionManager.start()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " &fDatabase: &e" + settings.databaseSettings().jdbcUrl());
        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " &fRules loaded: &e" + settings.rules().size());
        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " &fPlugin is loaded. Author: PQguanfang.");
    }

    @Override
    public void onDisable() {
        stopTasks();
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        if (storageService != null && settings != null) {
            storageService.save(settings);
            storageService.close();
        }
    }

    public void reloadPluginState() {
        stopTasks();
        if (storageService != null && settings != null) {
            storageService.save(settings);
            storageService.close();
        }

        reloadConfig();
        settings = PluginSettings.load(this);
        conditionManager = new ConditionManager();
        languageManager = new LanguageManager(this, settings);
        storageService = new StorageService(this, settings.databaseSettings());
        storageService.load(settings);
        earningLimitService = new EarningLimitService(this, settings, storageService);
        earningLimitService.initialize();

        if (isEnabled()) {
            startTasks();
            registerPlaceholderExpansion();
        }
    }

    public PluginSettings getSettings() {
        return settings;
    }

    public EarningLimitService getEarningLimitService() {
        return earningLimitService;
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

    public VaultInjectionManager getVaultInjectionManager() {
        return vaultInjectionManager;
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

    private void startTasks() {
        stopTasks();
        long autosaveTicks = settings.autoSaveMinutes() * 60L * 20L;
        resetTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (earningLimitService != null) {
                earningLimitService.checkResets();
            }
        }, 20L, 20L * 60L);
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (storageService != null && settings != null) {
                storageService.save(settings);
            }
        }, autosaveTicks, autosaveTicks);
    }

    private void stopTasks() {
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        Bukkit.getScheduler().cancelTasks(this);
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
        for (String name : classNames) {
            try {
                Class<?> clazz = Class.forName(name);
                boolean value = clazz.getField("freeVersion").getBoolean(null);
                if (!value) {
                    fullVersion = true;
                    TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cFULL license active by " + clazz.getSimpleName() + " plugin, thanks for your support and we hope you have good experience with this plugin!");
                    break;
                }
            } catch (Throwable e) {
                // ignored
            }
        }
    }

    private void registerPlaceholderExpansion() {
        if (!CommonUtil.checkPluginLoad("PlaceholderAPI")) {
            return;
        }
        if (!fullVersion) {
            return;
        }
        if (placeholderExpansion == null) {
            placeholderExpansion = new EconomyLimitExpansion(this);
        } else {
            placeholderExpansion.unregister();
        }
        placeholderExpansion.register();
    }
}
