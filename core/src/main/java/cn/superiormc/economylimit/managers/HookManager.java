package cn.superiormc.economylimit.managers;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.inject.VaultInjectionManager;
import cn.superiormc.economylimit.papi.EconomyLimitExpansion;
import cn.superiormc.economylimit.utils.CommonUtil;

public final class HookManager {

    public static HookManager hookManager;

    private final EconomyLimitPlugin plugin;
    private VaultInjectionManager vaultInjectionManager;
    private EconomyLimitExpansion placeholderExpansion;

    public HookManager(EconomyLimitPlugin plugin) {
        hookManager = this;
        this.plugin = plugin;
    }

    public boolean initVaultHook() {
        if (vaultInjectionManager == null) {
            vaultInjectionManager = new VaultInjectionManager(plugin);
        }
        return vaultInjectionManager.start();
    }

    public void reloadPlaceholderHook() {
        if (!CommonUtil.checkPluginLoad("PlaceholderAPI") || !plugin.isFullVersion()) {
            if (placeholderExpansion != null) {
                placeholderExpansion.unregister();
                placeholderExpansion = null;
            }
            return;
        }

        if (placeholderExpansion == null) {
            placeholderExpansion = new EconomyLimitExpansion(plugin);
        } else {
            placeholderExpansion.unregister();
        }
        placeholderExpansion.register();
    }

    public void shutdown() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
    }

    public VaultInjectionManager getVaultInjectionManager() {
        return vaultInjectionManager;
    }
}
