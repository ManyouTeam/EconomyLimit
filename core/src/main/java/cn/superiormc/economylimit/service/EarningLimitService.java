package cn.superiormc.economylimit.service;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.config.PluginSettings;
import cn.superiormc.economylimit.config.RuleDefinition;
import cn.superiormc.economylimit.managers.LanguageManager;
import cn.superiormc.economylimit.storage.PlayerAccount;
import cn.superiormc.economylimit.storage.StorageService;
import cn.superiormc.economylimit.utils.TextUtil;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class EarningLimitService {

    private static final double EPSILON = 0.000001D;

    private final EconomyLimitPlugin plugin;
    private final PluginSettings settings;
    private final StorageService storageService;
    private final DecimalFormat moneyFormat = new DecimalFormat("0.##");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public EarningLimitService(EconomyLimitPlugin plugin, PluginSettings settings, StorageService storageService) {
        this.plugin = plugin;
        this.settings = settings;
        this.storageService = storageService;
    }

    public void initialize() {
        checkResets();
        for (RuleDefinition rule : settings.rules()) {
            if (rule.resetPolicy().resettable() && storageService.getNextReset(rule.id()) == null) {
                storageService.setNextReset(rule.id(), rule.resetPolicy().nextResetAfter(ZonedDateTime.now(settings.zoneId())));
            }
        }
    }

    public synchronized DepositContext prepareDeposit(OfflinePlayer player, double amount) {
        checkResets();
        if (amount <= EPSILON) {
            return null;
        }

        PlayerAccount account = storageService.getAccount(player.getUniqueId());
        List<String> trackedRules = new ArrayList<>();
        double directAmount = amount;

        for (RuleDefinition rule : settings.rules()) {
            double limit = rule.resolveLimit(player, plugin.getConditionManager());
            if (limit < 0D) {
                continue;
            }

            trackedRules.add(rule.id());
            double current = account.getRuleProgress(rule.id());
            double remaining = Math.max(0D, limit - current);
            directAmount = Math.min(directAmount, remaining);
        }

        if (trackedRules.isEmpty()) {
            return null;
        }

        directAmount = normalize(directAmount);
        double bankedAmount = normalize(Math.max(0D, amount - directAmount));
        return new DepositContext(
                player.getUniqueId(),
                player.getName() == null ? player.getUniqueId().toString() : player.getName(),
                List.copyOf(trackedRules),
                directAmount,
                bankedAmount
        );
    }

    public synchronized DepositContext inspectDeposit(OfflinePlayer player, double amount) {
        return prepareDeposit(player, amount);
    }

    public synchronized void completeSkippedDeposit(DepositContext context) {
        commit(context, true);
    }

    public synchronized void completeDeposit(DepositContext context, EconomyResponse response) {
        if (response.transactionSuccess()) {
            commit(context, true);
        }
    }

    private void commit(DepositContext context, boolean countEarnedAmount) {
        PlayerAccount account = storageService.getAccount(context.playerId());
        double earnedAmount = context.directAmount() + context.bankedAmount();
        if (countEarnedAmount && earnedAmount > EPSILON) {
            for (String ruleId : context.trackedRules()) {
                account.addRuleProgress(ruleId, earnedAmount);
            }
        }

        if (context.bankedAmount() > EPSILON) {
            account.addBankBalance(context.bankedAmount());
            if (settings.notifyOnBankTransfer()) {
                Player onlinePlayer = Bukkit.getPlayer(context.playerId());
                if (onlinePlayer != null) {
                    plugin.getLanguageManager().sendStringText(
                            onlinePlayer,
                            "bank.transferred",
                            "amount",
                            formatMoney(context.bankedAmount())
                    );
                }
            }
        }
        storageService.cleanup(context.playerId());
    }

    public synchronized WithdrawCheck checkWithdraw(Player player, double amount) {
        if (amount <= EPSILON) {
            return new WithdrawCheck(false, "error.withdraw-positive", new String[0]);
        }

        PlayerAccount account = storageService.getAccount(player.getUniqueId());
        if (account.getBankBalance() + EPSILON < amount) {
            return new WithdrawCheck(false, "error.bank-insufficient", new String[0]);
        }

        DepositContext context = inspectDeposit(player, amount);
        if (context != null && context.directAmount() + EPSILON < amount) {
            return new WithdrawCheck(false, "error.withdraw-limited", new String[0]);
        }

        return new WithdrawCheck(true, "", new String[0]);
    }

    public synchronized void completeWithdraw(UUID playerId, double amount) {
        PlayerAccount account = storageService.getAccount(playerId);
        account.addBankBalance(-amount);
        storageService.cleanup(playerId);
    }

    public synchronized List<String> buildStatusLines(CommandSender sender, OfflinePlayer player) {
        checkResets();
        PlayerAccount account = storageService.getAccount(player.getUniqueId());
        List<String> lines = new ArrayList<>();
        LanguageManager languageManager = plugin.getLanguageManager();
        lines.add(languageManager.getStringText(sender, "status.bank", "amount", formatMoney(account.getBankBalance())));

        for (RuleDefinition rule : settings.rules()) {
            String ruleDisplayName = languageManager.resolveText(sender, rule.displayName());
            double limit = rule.resolveLimit(player, plugin.getConditionManager());
            if (limit < 0D) {
                lines.add(languageManager.getStringText(sender, "status.rule-unlimited", "rule", ruleDisplayName));
                continue;
            }

            double current = account.getRuleProgress(rule.id());
            double remaining = Math.max(0D, limit - current);
            ZonedDateTime nextReset = storageService.getNextReset(rule.id());
            String nextResetText = nextReset == null
                    ? languageManager.getStringText(sender, "status.never")
                    : nextReset.withZoneSameInstant(settings.zoneId()).format(timeFormatter);
            lines.add(languageManager.getStringText(
                    sender,
                    "status.rule",
                    "rule",
                    ruleDisplayName,
                    "current",
                    formatMoney(current),
                    "limit",
                    formatMoney(limit),
                    "remaining",
                    formatMoney(remaining),
                    "next_reset",
                    nextResetText
            ));
        }

        return lines;
    }

    public synchronized void checkResets() {
        ZonedDateTime now = ZonedDateTime.now(settings.zoneId());
        for (RuleDefinition rule : settings.rules()) {
            if (!rule.resetPolicy().resettable()) {
                continue;
            }

            ZonedDateTime nextReset = storageService.getNextReset(rule.id());
            if (nextReset == null) {
                nextReset = rule.resetPolicy().nextResetAfter(now);
            }

            while (nextReset != null && !nextReset.isAfter(now)) {
                storageService.resetRule(rule.id());
                nextReset = rule.resetPolicy().nextResetAfter(nextReset);
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cRule " + rule.id() + " has been reset.");
            }

            storageService.setNextReset(rule.id(), nextReset);
        }
    }

    public String formatMoney(double value) {
        return moneyFormat.format(normalize(value));
    }

    private double normalize(double value) {
        return Double.parseDouble(String.format(Locale.US, "%.2f", value));
    }
}
