package cn.superiormc.economylimit.service;

public record WithdrawCheck(boolean allowed, String messageKey, String[] replacements) {
}
