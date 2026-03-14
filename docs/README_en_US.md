# EconomyLimit

## Introduction

`EconomyLimit` is an earning limit plugin built around the `Vault` economy API.  
It injects into supported economy providers, tracks player income, and applies configurable earning caps based on your rules.

When a player reaches the cap of any active rule during its reset period, the extra money is not lost.  
Instead, it is redirected into a virtual bank managed by the plugin.

Players can withdraw money from the virtual bank later, but withdrawals still count as earned money.  
If a withdrawal would exceed the current earning limit, it will be denied.

The plugin supports:

- Multiple earning rules at the same time
- Independent reset schedules per rule
- Condition-based limits
- Virtual bank deposit and withdrawal
- Multi-language display
- SQLite / MySQL / PostgreSQL / H2 storage
- Paper / Spigot text compatibility
- `{lang:...}` rule names based on player language

## Core Features

### 1. Income Tracking

The plugin intercepts money added to players through `Vault` and records it for the matching rules.

### 2. Rule Limits

Each rule can contain multiple limits.  
The plugin checks conditions and decides which limit applies to the current player.

### 3. Overflow Handling

If any rule reaches its cap, the exceeded amount is automatically stored in the virtual bank.

### 4. Withdraw Control

When a player withdraws from the virtual bank, the plugin checks the earning limits again.  
If the withdrawal would push the player over the cap, it is rejected.

## Commands

### Player Commands

- `/economylimit`
  View your bank balance and rule progress
- `/economylimit status`
  View your bank balance and rule progress
- `/economylimit withdraw <amount>`
  Withdraw money from the virtual bank

### Admin Commands

- `/economylimit status <player>`
  View another player's bank balance and progress
- `/economylimit reload`
  Reload config and language files
- `/economylimit debug`
  View Vault injection status, bridge hits, and errors

## Permissions

- `economylimit.withdraw`
- `economylimit.admin.status`
- `economylimit.admin.reload`
- `economylimit.admin.debug`

## Configuration

### Timezone and Auto Save

```yml
timezone: system
auto-save-minutes: 5
notify-on-bank-transfer: true
```

### Language Settings

```yml
config-files:
  language: en_US
  per-player-language: true
  force-parse-mini-message: true
```

### Database Settings

#### SQLite

```yml
database:
  jdbc-url: "jdbc:sqlite:plugins/EconomyLimit/data/economylimit.db"
  jdbc-class: "org.sqlite.JDBC"
  properties:
    user: ""
    password: ""
```

#### MySQL

```yml
database:
  jdbc-url: "jdbc:mysql://127.0.0.1:3306/economylimit?useSSL=false&autoReconnect=true"
  jdbc-class: "com.mysql.cj.jdbc.Driver"
  properties:
    user: "root"
    password: "123456"
```

#### PostgreSQL

```yml
database:
  jdbc-url: "jdbc:postgresql://127.0.0.1:5432/economylimit"
  jdbc-class: "org.postgresql.Driver"
  properties:
    user: "postgres"
    password: "123456"
```

#### H2

```yml
database:
  jdbc-url: "jdbc:h2:./plugins/EconomyLimit/data/economylimit"
  jdbc-class: "org.h2.Driver"
  properties:
    user: "sa"
    password: ""
```

## Rule Example

```yml
rules:
  daily:
    display-name: "{lang:rules.daily.name}"
    reset:
      mode: DAILY
      time: "00:00"
    limits:
      - limit: 50000
      - condition:
          type: PERMISSION
          value: economylimit.rule.daily.vip
        limit: 100000
      - condition:
          type: PERMISSION
          value: economylimit.rule.daily.bypass
        limit: -1
```

### Condition Types

- `ANY`
- `PERMISSION`
- `WORLD`
- `PLAYER`
- `OP`

## Language-Based Rule Names

```yml
rules:
  daily:
    name: "Daily earnings"
  weekly:
    name: "Weekly earnings"
```

If your rule uses:

```yml
display-name: "{lang:rules.daily.name}"
```

then different players can see different rule names depending on their selected language.

## Setup Steps

1. Install `Vault`
2. Install any `Vault`-compatible economy plugin
3. Put `EconomyLimit` into your plugins folder
4. Start the server once to generate files
5. Edit database, language, and rule settings
6. Restart the server or run `/economylimit reload`
7. Use `/economylimit debug` to confirm injection works

## Debug Suggestions

If income is not being tracked, check:

- Whether `Vault` is hooked correctly
- The `Vault provider` shown in `/economylimit debug`
- Whether injection status is successful
- Whether `Bridge hits` increases
- Whether the economy plugin really pays players through `Vault.depositPlayer(...)`

## Use Cases

- Limit daily or weekly money farming
- Redirect overflow income into a buffer bank
- Give different earning caps to different permission groups
- Show rule names in different languages
- Store earnings and virtual bank data in SQL
