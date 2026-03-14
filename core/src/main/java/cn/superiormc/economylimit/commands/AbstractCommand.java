package cn.superiormc.economylimit.commands;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.managers.LanguageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCommand {

    protected final EconomyLimitPlugin plugin;
    protected final String id;
    protected final String requiredPermission;
    protected final boolean onlyInGame;
    protected final Integer[] requiredArgLength;
    protected Integer[] requiredConsoleArgLength;

    protected AbstractCommand(EconomyLimitPlugin plugin, String id, String requiredPermission, boolean onlyInGame, Integer... requiredArgLength) {
        this.plugin = plugin;
        this.id = id;
        this.requiredPermission = requiredPermission;
        this.onlyInGame = onlyInGame;
        this.requiredArgLength = requiredArgLength;
    }

    public abstract void executeCommandInGame(String[] args, Player player);

    public void executeCommandInConsole(String[] args) {
        language().sendStringText((CommandSender) null, "error.in-game");
    }

    public List<String> getTabResult(String[] args, Player player) {
        return new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public boolean getOnlyInGame() {
        return onlyInGame;
    }

    public boolean getLengthCorrect(int length, CommandSender sender) {
        Integer[] acceptedLengths = sender instanceof Player || requiredConsoleArgLength == null || requiredConsoleArgLength.length == 0
                ? requiredArgLength
                : requiredConsoleArgLength;
        for (int acceptedLength : acceptedLengths) {
            if (acceptedLength == length) {
                return true;
            }
        }
        return false;
    }

    protected LanguageManager language() {
        return plugin.getLanguageManager();
    }
}
