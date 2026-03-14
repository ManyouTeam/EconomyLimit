# 💰 Welcoome to EconomyLimit 

## Introduction ✨

`EconomyLimit` is an income limit plugin built for `Vault`-based economy systems.  
It intercepts money deposits through bytecode injection, tracks how much players earn, and applies configurable earning limits based on your rules.

When a player reaches the cap of any active rule, the extra money is not lost.  
Instead, it is automatically moved into a virtual bank 🏦 managed by the plugin.

Players can later withdraw money from the bank, but withdrawals still count as earned income.  
If a withdrawal would exceed the current earning limit, it will be denied.

## Core Features 🚀

- Supports multiple earning limit rules
- Each rule can have its own reset time
- Supports daily, weekly, monthly, and custom reset behavior
- Dynamic earning caps based on conditions
- Overflow money is automatically moved into a virtual bank
- Players can manually withdraw banked money
- Supports SQLite, MySQL, PostgreSQL, and H2
- Multi-language support 🌍
- Rule names can use `{lang:xxx}` for per-language display
- Compatible with both Paper and Spigot text systems

## How It Works ⚙️

The plugin monitors income added through `Vault`.  
Whenever a player receives money, `EconomyLimit` checks the active rules:

1. Whether this income would exceed any configured earning cap
2. If not, the money is given normally
3. If yes, the exceeded amount is redirected into the virtual bank
4. When the player withdraws from the bank later, the same earning checks are applied again

## Use Cases 🎯

- Limiting how much money players can farm per day
- Controlling income from jobs, shops, quests, or farms
- Giving different earning caps to different permission groups
- Showing localized rule names on multi-language servers
- Storing overflow income in a buffer bank instead of deleting it

## Summary 📦

`EconomyLimit` is designed for servers that need tighter control over economic growth.  
Instead of simply blocking player income, it combines earning caps with a virtual bank system, making economy control more flexible, balanced, and practical for long-term servers.
