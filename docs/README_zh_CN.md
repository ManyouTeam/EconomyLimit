# EconomyLimit

## 插件介绍

`EconomyLimit` 是一个基于 `Vault` 经济接口的收益限制插件。  
它通过字节码注入拦截经济插件的加钱方法，统计玩家获得的收益，并按照你配置的规则进行限制。

当玩家在某个规则周期内达到收益上限后，超出的金额不会直接消失，而是自动转入插件提供的虚拟银行。  
玩家可以之后手动提取虚拟银行中的金额，但提取行为依然会计入收益统计，如果当前规则已经超限，则无法提取。

插件支持：

- 多条收益规则同时生效
- 每条规则独立重置周期
- 条件化限额
- 虚拟银行存取
- 多语言显示
- SQLite / MySQL / PostgreSQL / H2 数据库存储
- Paper / Spigot 双平台文本兼容
- `display-name` 使用 `{lang:...}` 按玩家语言动态显示

## 核心机制

### 1. 收益统计

插件会拦截通过 `Vault` 发放给玩家的经济收入，并将其计入对应规则的累计收益。

### 2. 规则限制

每条规则都可以设置一个或多个限额。  
插件会根据玩家满足的条件，决定当前规则使用哪个上限。

### 3. 超限处理

当任意规则达到上限后，本次超出的金额会自动进入虚拟银行。

### 4. 银行提取

玩家执行提取时，插件会再次检查当前是否还能计入收益。  
如果提取后会超出限制，则本次提取会被拒绝。

## 命令

### 玩家命令

- `/economylimit`
  查看自己的银行余额和各规则进度
- `/economylimit status`
  查看自己的银行余额和各规则进度
- `/economylimit withdraw <金额>`
  从虚拟银行提取金额

### 管理员命令

- `/economylimit status <玩家>`
  查看指定玩家的银行余额和规则进度
- `/economylimit reload`
  重载插件配置和语言文件
- `/economylimit debug`
  查看 Vault 注入状态、桥接命中情况和错误信息

## 权限

- `economylimit.withdraw`
- `economylimit.admin.status`
- `economylimit.admin.reload`
- `economylimit.admin.debug`

## 配置说明

### 时区与自动保存

```yml
timezone: system
auto-save-minutes: 5
notify-on-bank-transfer: true
```

### 语言设置

```yml
config-files:
  language: zh_CN
  per-player-language: true
  force-parse-mini-message: true
```

### 数据库设置

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

## 规则配置示例

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

### 条件类型

- `ANY`
- `PERMISSION`
- `WORLD`
- `PLAYER`
- `OP`

## 语言文件中的规则名称

```yml
rules:
  daily:
    name: "每日收益"
  weekly:
    name: "每周收益"
```

如果规则中写了：

```yml
display-name: "{lang:rules.daily.name}"
```

那么不同语言玩家看到的规则名称会自动不同。

## 使用流程

1. 安装 `Vault`
2. 安装任意支持 `Vault` 的经济插件
3. 将 `EconomyLimit` 放入服务器插件目录
4. 启动服务器生成配置文件
5. 修改数据库、语言和规则配置
6. 重启服务器或执行 `/economylimit reload`
7. 使用 `/economylimit debug` 检查注入是否成功

## 调试建议

如果收益没有被统计，建议检查以下内容：

- `Vault` 是否正常挂钩到经济插件
- `/economylimit debug` 中的 `Vault provider`
- 注入状态是否为成功
- `Bridge hits` 是否增加
- 经济插件是否真的通过 `Vault.depositPlayer(...)` 发钱

## 适用场景

- 限制玩家每日/每周刷钱速度
- 将超额收益转入缓冲银行
- 配合权限组设置不同收益上限
- 多语言服务器中的动态规则展示
- 使用 SQL 数据库存储玩家收益和银行数据
