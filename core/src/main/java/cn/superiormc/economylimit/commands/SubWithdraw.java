package cn.superiormc.economylimit.commands;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.service.WithdrawCheck;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

public final class SubWithdraw extends AbstractCommand {

    public SubWithdraw(EconomyLimitPlugin plugin) {
        super(plugin, "withdraw", "economylimit.withdraw", true, 2);
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            language().sendStringText(player, "error.invalid-number");
            return;
        }

        WithdrawCheck check = plugin.getEarningLimitService().checkWithdraw(player, amount);
        if (!check.allowed()) {
            String[] messageArgs = new String[check.replacements().length + 1];
            messageArgs[0] = check.messageKey();
            System.arraycopy(check.replacements(), 0, messageArgs, 1, check.replacements().length);
            language().sendStringText(player, messageArgs);
            return;
        }

        EconomyResponse response = plugin.depositToPlayer(player, amount);
        if (response == null) {
            language().sendStringText(player, "error.no-economy");
            return;
        }
        if (!response.transactionSuccess()) {
            language().sendStringText(player, "error.withdraw-failed", "reason", response.errorMessage == null ? "unknown" : response.errorMessage);
            return;
        }

        plugin.getEarningLimitService().completeWithdraw(player.getUniqueId(), amount);
        language().sendStringText(player, "bank.withdrew", "amount", plugin.getEarningLimitService().formatMoney(amount));
    }
}
