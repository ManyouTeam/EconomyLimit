package cn.superiormc.economylimit.managers;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.config.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public final class TaskManager {

    public static TaskManager taskManager;

    private final EconomyLimitPlugin plugin;
    private BukkitTask resetTask;
    private BukkitTask autoSaveTask;

    public TaskManager(EconomyLimitPlugin plugin) {
        taskManager = this;
        this.plugin = plugin;
    }

    public void reload() {
        cancelTask();

        PluginSettings settings = plugin.getSettings();
        if (settings == null) {
            return;
        }

        long autosaveTicks = settings.autoSaveMinutes() * 60L * 20L;
        resetTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (plugin.getEarningLimitService() != null) {
                plugin.getEarningLimitService().checkResets();
            }
        }, 20L, 20L * 60L);

        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (plugin.getStorageService() != null && plugin.getSettings() != null) {
                plugin.getStorageService().save(plugin.getSettings());
            }
        }, autosaveTicks, autosaveTicks);
    }

    public void cancelTask() {
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
