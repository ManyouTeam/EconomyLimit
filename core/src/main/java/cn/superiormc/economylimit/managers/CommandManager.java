package cn.superiormc.economylimit.managers;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.commands.AbstractCommand;
import cn.superiormc.economylimit.commands.MainCommand;
import cn.superiormc.economylimit.commands.MainCommandTab;
import cn.superiormc.economylimit.commands.SubDebug;
import cn.superiormc.economylimit.commands.SubReload;
import cn.superiormc.economylimit.commands.SubStatus;
import cn.superiormc.economylimit.commands.SubWithdraw;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CommandManager {

    private final EconomyLimitPlugin plugin;
    private final Map<String, AbstractCommand> registeredCommands = new LinkedHashMap<>();

    public CommandManager(EconomyLimitPlugin plugin) {
        this.plugin = plugin;
        registerBukkitCommands();
        registerObjectCommands();
    }

    private void registerBukkitCommands() {
        PluginCommand command = Objects.requireNonNull(Bukkit.getPluginCommand("economylimit"), "Missing economylimit command");
        command.setExecutor(new MainCommand(plugin));
        command.setTabCompleter(new MainCommandTab(plugin));
    }

    private void registerObjectCommands() {
        SubStatus status = new SubStatus(plugin);
        registerNewSubCommand(status);
        registerAlias("bank", status);
        registerNewSubCommand(new SubWithdraw(plugin));
        registerNewSubCommand(new SubReload(plugin));
        registerNewSubCommand(new SubDebug(plugin));
    }

    public Map<String, AbstractCommand> getSubCommandsMap() {
        return registeredCommands;
    }

    public Collection<AbstractCommand> getUniqueCommands() {
        return registeredCommands.values().stream().distinct().toList();
    }

    public void registerNewSubCommand(AbstractCommand command) {
        registeredCommands.put(command.getId(), command);
    }

    public void registerAlias(String alias, AbstractCommand command) {
        registeredCommands.put(alias, command);
    }
}
