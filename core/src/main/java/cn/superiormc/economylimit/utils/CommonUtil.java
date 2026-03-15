package cn.superiormc.economylimit.utils;

import cn.superiormc.economylimit.EconomyLimitPlugin;

import java.util.HashMap;
import java.util.Map;

public final class CommonUtil {

    private CommonUtil() {
    }

    public static Map<String, Boolean> loadedPlugins = new HashMap<>();

    public static boolean checkPluginLoad(String pluginName) {
        if (loadedPlugins.containsKey(pluginName)) {
            return loadedPlugins.get(pluginName);
        }
        loadedPlugins.put(pluginName, EconomyLimitPlugin.getInstance().getServer().getPluginManager().isPluginEnabled(pluginName));
        return EconomyLimitPlugin.getInstance().getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    public static boolean getClass(String className) {
        try {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
}
