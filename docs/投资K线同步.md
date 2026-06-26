# 投资 K 线同步

本文档记录投资模块中币安现货 K 线同步的业务目标、实现位置、运行参数、已验证结论和排障约定。

## 1. 模块目标

K 线同步用于把币安现货 K 线数据保存到本地表，供后续交易分析、复盘和行情查询使用。

当前已重点验证：

- 交易对：`BTCUSDT`
- 周期：`4h`
- 默认起始日期：`2017-08-16`

本功能当前不强依赖实时性。同步任务允许被锁跳过；只要后续任务能再次同步成功即可。

## 2. 代码位置

任务入口：

```text
eladmin/eladmin-invest/src/main/java/me/zhengjie/invest/task/SyncKlinesTask.java
```

核心同步逻辑：

```text
eladmin/eladmin-invest/src/main/java/me/zhengjie/invest/service/impl/InvestKlinesRecordServiceImpl.java
```

币安现货 K 线请求：

```text
eladmin/eladmin-invest/src/main/java/me/zhengjie/invest/util/BinanceSpotUtil.java
```

实体：

```text
eladmin/eladmin-invest/src/main/java/me/zhengjie/invest/domain/InvestKlinesRecord.java
```

## 3. 调用参数

`SyncKlinesTask#sync` 接收 JSON 数组字符串，示例：

```json
[
  {
    "symbol": "BTCUSDT",
    "intervalCodes": "4h",
    "defaultStartTime": "2017-08-16"
  }
]
```

字段说明：

- `symbol`：交易对，对应 `BinanceEnum.SYMBOL`。
- `intervalCodes`：K 线周期，可用逗号分隔多个周期，例如 `15m,1h,4h`。
- `defaultStartTime`：库中无历史数据时使用的同步起点，格式为 `yyyy-MM-dd`。

## 4. 同步规则

`InvestKlinesRecordServiceImpl#syncKlinesRecord` 的当前规则：

1. 使用 Redis key `SYNC_KLINES:{symbol}:{intervalCode}` 做同一交易对、同一周期的互斥锁。
2. 未拿到锁时直接返回，本次不执行同步。
3. 拿到锁后查询库中当前最新一根 K 线。
4. 如果库中已有数据，从最新一根 K 线的 `openTime` 开始重新拉取。
5. 为避免最新一根未收盘导致数据不准，会先删除库中最新一根，再重新保存。
6. 每次调用币安 `/api/v3/klines`，`limit=1000`。
7. 每批保存后，将下一次 `startTime` 推进到本批最后一根 K 线的 `closeTime`。
8. 币安返回空列表时结束同步。
9. 正常结束或异常退出方法时释放 Redis 锁。

说明：

- 币安 K 线按开盘时间识别，接口返回数据按时间升序。
- 当前实现会保存最新未收盘 K 线；下一次同步会删除最后一根再重新拉取，刷新最新数据。
- 当前不要求 K 线强实时，因此锁冲突时跳过一次同步可以接受。

## 5. 已验证结论

2026-06-26 使用本地开发环境实际执行 `BTCUSDT / 4h` 同步验证：

- 同步前本地已有 `7000` 条。
- 同步后本地共有 `19397` 条。
- 重复 `open_time` 数量为 `0`。
- 按严格 4 小时间隔检查发现 `8` 个历史断点。
- 进一步调用币安现货 K 线接口核对断点附近数据，币安原始接口同样缺失对应开盘时间。

结论：

- `BTCUSDT / 4h` 在当前实现下可以正确保存 K 线数据。
- 实测没有发现分页游标导致的重复保存或漏保存。
- 历史上的少量断点来自币安原始历史数据本身，不是当前同步逻辑漏写。

## 6. 排障约定

如果某个交易对/周期长期不更新，优先检查 Redis 锁：

```text
SYNC_KLINES:{symbol}:{intervalCode}
```

例如：

```text
SYNC_KLINES:BTCUSDT:4h
```

当前锁没有强实时要求。若确认没有任务正在同步，但该 key 长时间存在，可以删除该 key 后重新触发同步。

排查顺序建议：

1. 查看 Redis 锁是否残留。
2. 确认代理配置 `proxy.host`、`proxy.port` 可访问币安接口。
3. 查看数据库中该 `symbol + intervalCode` 最新 `open_time`。
4. 直接请求币安 `/api/v3/klines` 核对对应时间段是否有原始数据。
5. 检查是否存在重复 `open_time`。

## 7. 后续优化

当前可暂缓，但后续若提高稳定性或实时性，建议补充：

- 给 Redis 锁增加 TTL，避免进程异常退出后锁永久残留。
- 锁 value 使用唯一 token，释放锁时只删除自己持有的锁。
- 给数据库增加 `symbol + interval_code + open_time` 唯一约束，并使用 upsert 降低重复写风险。
- 调整“删除最后一根再拉取”的顺序，降低接口异常时短暂缺失最后一根 K 线的概率。
- 增加一个只检查连续性和重复数据的运维脚本或测试入口。
