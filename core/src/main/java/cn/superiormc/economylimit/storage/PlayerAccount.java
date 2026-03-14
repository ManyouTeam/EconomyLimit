package cn.superiormc.economylimit.storage;

import java.util.HashMap;
import java.util.Map;

public final class PlayerAccount {

    private double bankBalance;
    private final Map<String, Double> ruleProgress = new HashMap<>();

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }

    public void addBankBalance(double delta) {
        this.bankBalance += delta;
        if (this.bankBalance < 0D) {
            this.bankBalance = 0D;
        }
    }

    public double getRuleProgress(String ruleId) {
        return ruleProgress.getOrDefault(ruleId, 0D);
    }

    public void setRuleProgress(String ruleId, double value) {
        ruleProgress.put(ruleId, Math.max(0D, value));
    }

    public void addRuleProgress(String ruleId, double delta) {
        ruleProgress.put(ruleId, Math.max(0D, getRuleProgress(ruleId) + delta));
    }

    public void clearRuleProgress(String ruleId) {
        ruleProgress.remove(ruleId);
    }

    public Map<String, Double> getRuleProgress() {
        return ruleProgress;
    }

    public boolean isEmpty() {
        return bankBalance <= 0D && ruleProgress.isEmpty();
    }
}
