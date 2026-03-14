package cn.superiormc.economylimit.commands;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.managers.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainCommandTab implements TabCompleter {

    private final EconomyLimitPlugin plugin;

    public MainCommandTab(EconomyLimitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        CommandManager commandManager = plugin.getCommandManager();
        if (args.length == 1) {
            List<String> results = new ArrayList<>();
            for (AbstractCommand subCommand : commandManager.getUniqueCommands()) {
                if (subCommand.getRequiredPermission() == null || subCommand.getRequiredPermission().isEmpty()
                        || sender.hasPermission(subCommand.getRequiredPermission())) {
                    results.add(subCommand.getId());
                }
            }
            return filter(results, args[0]);
        }

        AbstractCommand subCommand = commandManager.getSubCommandsMap().get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null || !(sender instanceof Player player)) {
            return List.of();
        }
        return filter(subCommand.getTabResult(args, player), args[args.length - 1]);
    }

    private List<String> filter(List<String> values, String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowerInput))
                .toList();
    }
}
