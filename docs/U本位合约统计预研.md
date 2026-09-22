# U 本位合约统计预研

本文档记录 U 本位合约统计功能的当前代码状态、已确定口径和后续实施边界。该功能目前只完成后端接口基础和前端请求封装，尚未进入页面开发，不应作为已经交付的完整业务功能使用。

## 1. 当前状态

截至 2026-09-22，仓库已经具备以下基础能力：

- 按账户、交易对和持仓方向查询 U 本位合约统计。
- 按指定账户和交易对增量同步 U 本位成交。
- 查询币安实时持仓、账户资产和交易对杠杆配置。
- 使用本地 `binance_futures_trade_info` 成交计算已实现盈亏、手续费和净盈亏。
- 对双向持仓模式中的 `LONG`、`SHORT` 成交按 FIFO 重建未平仓批次，供后续页面绘图。
- 提供对应的 Vue 请求封装。

当前明确暂停以下工作：

- U 本位统计页面及菜单配置。
- U 本位下单、挂单查询和撤单。
- 资金费流水同步与统计。
- WebSocket 实时推送。
- 单向持仓模式 `BOTH` 的逐笔未平仓分布。
- 更多交易对的产品配置和页面选择逻辑。

当前 U 本位仍处于预研和接口基础阶段，手续费暂按现有数值直接汇总，不处理 `commissionAsset` 不同导致的币种换算问题。恢复 U 本位正式开发时，再结合账户保证金模式补充手续费资产过滤、告警或汇率换算；在此之前不作为当前缺陷处理。

如后续重新启用开发，应先确认需求确实为 U 本位，而不是币本位；两类合约的币安接口、交易对、保证金资产和统计口径不能混用。

## 2. 已实现接口

### 2.1 查询统计

```text
GET /api/binanceFuturesTradeInfo/stats
```

请求参数：

- `uid`：账户编号。
- `symbol`：U 本位交易对，例如 `BTCUSDT`。
- `positionSide`：持仓方向，只允许 `LONG`、`SHORT` 或 `BOTH`。

权限：

```text
binanceFuturesTradeInfo:list
```

响应主要包含：

- `positionInfo`：实时持仓、标记价格、开仓均价、盈亏平衡价、未实现盈亏、强平价、杠杆和保证金模式。
- `tradeSummary`：本地成交的已实现盈亏、手续费、净盈亏和成交数量。
- `accountInfo`：钱包余额、保证金余额、可用余额和保证金要求。
- `tradeList`：重建后的未平仓成交批次。
- `warnings`：实时持仓缺失或当前统计模式不支持时的提示。

### 2.2 同步当前交易对

```text
PUT /api/binanceFuturesTradeInfo/syncSelected
```

请求参数：

- `uid`：账户编号。
- `symbol`：U 本位交易对，例如 `BTCUSDT`。

权限：

```text
binanceTradeInfo:sync
```

响应字段 `tradeCount` 表示本次新增的成交数量。同步继续使用币安成交 `id` 作为现有表主键，本阶段不调整数据库主键设计。

## 3. 数据口径

### 3.1 当前持仓

当前持仓以币安实时数据为准：

- `/fapi/v3/positionRisk`：持仓数量、标记价格、开仓均价、盈亏平衡价、未实现盈亏、强平价和保证金。
- `/fapi/v1/symbolConfig`：杠杆、保证金模式和最大名义价值。
- `/fapi/v3/account`：U 本位账户余额和保证金汇总。

页面开发时，顶部持仓指标必须使用实时持仓数据，不能用本地成交重建结果替代。

### 3.2 历史成交

本地成交来自 `binance_futures_trade_info`：

- 已实现盈亏直接汇总币安返回的 `realized_pnl`。
- 手续费汇总 `commission`，展示为正的成本金额。
- 当前净盈亏暂按 `realizedPnl - commission` 计算。
- 资金费当前固定为零，不能在页面中标记为已经完整统计。

### 3.3 未平仓分布

`LONG` 和 `SHORT` 按成交时间和成交 ID 执行展示层 FIFO 重建。该结果用于价格区间图和逐笔开仓图，不作为币安实际持仓均价、保证金或风险指标的权威来源。

`BOTH` 表示单向持仓模式。当前接口返回实时持仓和成交汇总，但不重建逐笔未平仓分布，并通过 `warnings` 提示这一边界。

## 4. 后续页面建议

如果恢复页面开发，建议在“投资管理”下新增独立的“U 本位统计”，不要与现货统计或币本位统计混在同一页面。

页面第一阶段建议包含：

1. 筛选栏：账户、交易对、持仓方向、查询和同步数据。
2. 核心指标：标记价格、开仓均价、持仓数量、名义价值、未实现盈亏和强平价格。
3. 风险指标：杠杆、保证金模式、盈亏平衡价、仓位保证金和维持保证金。
4. 图表：价格区间聚合和逐笔开仓两种视图。
5. 汇总：已实现盈亏、手续费、资金费、净盈亏、钱包余额和可用余额。

合约页面不应复用现货专属的底仓、可撮合数量、快捷卖出来源关联和现货 FIFO 固化语义。

## 5. 代码位置

- 控制器：`eladmin/eladmin-invest/src/main/java/me/zhengjie/invest/rest/BinanceFuturesTradeInfoController.java`
- 服务接口：`eladmin/eladmin-invest/src/main/java/me/zhengjie/invest/service/BinanceFuturesTradeInfoService.java`
- 服务实现：`eladmin/eladmin-invest/src/main/java/me/zhengjie/invest/service/impl/BinanceFuturesTradeInfoServiceImpl.java`
- 币安请求封装：`eladmin/eladmin-invest/src/main/java/me/zhengjie/invest/util/BinanceUsdFuturesUtil.java`
- 响应对象：`eladmin/eladmin-invest/src/main/java/me/zhengjie/invest/domain/dto/BinanceUsdFuturesStatsInfoVO.java`
- 前端请求封装：`eladmin-web/src/api/binanceFuturesTradeInfo.js`
- 控制器测试：`eladmin/eladmin-invest/src/test/java/me/zhengjie/invest/rest/BinanceFuturesTradeInfoControllerTest.java`
- 服务测试：`eladmin/eladmin-invest/src/test/java/me/zhengjie/invest/service/impl/BinanceFuturesTradeInfoServiceImplTest.java`

## 6. 恢复开发检查清单

恢复开发前应依次确认：

1. 产品目标是 U 本位还是币本位。
2. 需要支持 `LONG/SHORT` 双向持仓、`BOTH` 单向持仓，还是只支持当前账户的实际模式。
3. 第一阶段支持哪些交易对。
4. 是否需要同步资金费；如需要，应单独设计流水落库和增量同步。
5. 是否需要下单和挂单管理；如需要，应单独确认权限、幂等和风险控制。
6. 页面展示口径以币安实时持仓为主，本地逐笔重建只用于分析图表。

相关修改完成后至少执行：

```text
cd eladmin
mvn -pl eladmin-invest -DskipTests=false test

cd eladmin-web
npm run lint
npm run test:unit
```

本机运行或重启后端时，仍须遵守 `docs/项目启动流程.md` 中的 Java 8、Maven、启动脚本和 `JASYPT_ENCRYPTOR_PASSWORD` 要求。
