<template>
  <div class="app-container stats-page">
    <section class="panel toolbar-panel">
      <div class="toolbar-row">
        <div class="toolbar-main">
          <label class="field-label">账户</label>
          <el-select
            v-model="query.uid"
            clearable
            filterable
            size="small"
            placeholder="账户"
            class="account-select"
            @change="handleAccountChange"
          >
            <el-option v-for="item in accountList" :key="item.id" :label="item.idCardName" :value="item.uid" />
          </el-select>
          <label class="field-label">交易对</label>
          <el-select
            v-model="query.symbol"
            size="small"
            placeholder="交易对"
            class="symbol-select"
            @change="scheduleStats"
          >
            <el-option
              v-for="item in dict.invest_binance_symbol"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button
            size="small"
            type="success"
            icon="el-icon-search"
            :loading="statsLoading"
            :disabled="query.uid == null || !query.symbol"
            @click="doStats"
          >查询</el-button>
          <el-button
            size="small"
            plain
            type="success"
            icon="el-icon-refresh"
            :loading="syncLoading"
            :disabled="query.uid == null || !query.symbol"
            @click="syncSpotTradeInfo"
          >同步数据</el-button>
        </div>
        <div class="toolbar-actions">
          <el-popover placement="bottom-end" width="260" trigger="hover" :open-delay="150" :close-delay="200">
            <div class="account-popover-content">
              <div class="popover-heading">账户统计</div>
              <div v-for="item in accountSummaryItems" :key="item.label" class="popover-stat-row">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
            <el-button slot="reference" size="small" plain icon="el-icon-wallet">账户资产</el-button>
          </el-popover>
          <el-popover placement="bottom-end" width="660" trigger="hover" :open-delay="150" :close-delay="200">
            <div class="trade-summary-popover">
              <div class="popover-heading">交易汇总</div>
              <div class="summary-grid">
                <div v-for="item in tradeSummaryItems" :key="item.label" class="summary-item">
                  <span class="summary-label">{{ item.label }}</span>
                  <div class="summary-value-row">
                    <strong :class="['summary-value', item.tone]">{{ item.value }}</strong>
                    <el-tooltip v-if="item.delta" content="相对上次查询" placement="top">
                      <span :class="['summary-delta', item.deltaTone]">{{ item.delta }}</span>
                    </el-tooltip>
                  </div>
                </div>
              </div>
            </div>
            <el-button slot="reference" size="small" plain icon="el-icon-s-grid">全部汇总</el-button>
          </el-popover>
        </div>
      </div>
      <el-alert
        v-if="realtimeWarningText"
        :title="realtimeWarningText"
        type="warning"
        :closable="false"
        show-icon
        class="realtime-warning"
      />
    </section>

    <section v-loading="statsLoading" class="panel position-panel">
      <h2 class="position-heading">{{ query.symbol || '现货' }} 持仓买入分布</h2>
      <div class="position-metrics">
        <div v-for="item in positionMetrics" :key="item.label" class="metric-item">
          <span class="metric-label">{{ item.label }}</span>
          <strong :class="['metric-value', item.tone]">{{ item.value }}</strong>
        </div>
      </div>
      <trade-position-distribution-bar
        :row-data="spotStats.tradeList || []"
        :current-price="spotStats.currentSpotPrice"
        :average-price="spotStats.posAvgPrice"
        height="100%"
        @select-trade="openTradeCoreActions"
        @select-bucket="openBucketCoreActions"
      />
    </section>

    <el-drawer
      :title="coreActionTitle"
      :visible.sync="showCoreActions"
      direction="rtl"
      size="1180px"
      custom-class="core-action-drawer"
      @closed="handleCoreActionDrawerClosed"
    >
      <div class="core-action-content">
        <div v-if="selectedCoreRange" class="core-range-summary">
          <el-tag type="info">订单 {{ coreActionOrderGroups.length }} 个</el-tag>
          <el-tag type="info">成交 {{ coreActionRows.length }} 笔</el-tag>
          <el-tag type="primary">当前价 {{ moneyValue(spotStats.currentSpotPrice) }}</el-tag>
          <el-tag>底仓成交 {{ selectedCoreRange.coreCount || 0 }} 笔</el-tag>
          <el-tag>底仓 {{ decimalValue(selectedCoreRange.coreQty || 0, 8) }} BTC</el-tag>
          <el-tag type="success">可撮合 {{ decimalValue(selectedCoreRange.availableQty || 0, 8) }} BTC</el-tag>
          <el-popconfirm
            v-if="checkPer(['admin', 'binanceSpotCorePosition:add'])"
            title="确定将当前区间内所有未设置底仓的持仓数量全部设为底仓吗？"
            placement="bottom-end"
            :width="340"
            :disabled="coreActionUnlockedRows.length === 0 || batchLockLoading || batchReleaseLoading"
            confirm-button-text="全部设置"
            cancel-button-text="取消"
            icon="el-icon-warning"
            icon-color="#409eff"
            @confirm="lockAllCorePositions"
          >
            <el-button
              slot="reference"
              type="primary"
              size="mini"
              plain
              :disabled="coreActionUnlockedRows.length === 0 || batchReleaseLoading"
              :loading="batchLockLoading"
            >一键设置底仓</el-button>
          </el-popconfirm>
          <el-popconfirm
            title="确定解除当前区间内的全部底仓吗？解除后将重新参与后续 FIFO 撮合。"
            placement="bottom-end"
            :width="320"
            :disabled="coreActionCoreRows.length === 0 || batchReleaseLoading || batchLockLoading"
            confirm-button-text="全部解除"
            cancel-button-text="取消"
            icon="el-icon-warning"
            icon-color="#e6a23c"
            @confirm="releaseAllCorePositions"
          >
            <el-button
              slot="reference"
              type="warning"
              size="mini"
              plain
              :disabled="coreActionCoreRows.length === 0 || batchLockLoading"
              :loading="batchReleaseLoading"
            >一键解除</el-button>
          </el-popconfirm>
        </div>
        <el-alert
          :title="selectedCoreRange ? '默认按订单折叠；展开后对具体成交批次设置或调整底仓。' : '底仓只影响未来撮合。'"
          type="info"
          :closable="false"
          show-icon
          class="core-action-tip"
        />
        <el-radio-group
          v-if="selectedCoreRange"
          v-model="coreActionView"
          size="mini"
          class="core-action-view-switch"
          @change="handleCoreActionViewChange"
        >
          <el-radio-button label="order">按订单</el-radio-button>
          <el-radio-button label="trade">按成交</el-radio-button>
        </el-radio-group>
        <el-table
          :data="coreActionDisplayRows"
          :row-key="coreActionRowKey"
          :row-class-name="coreActionRowClassName"
          border
          stripe
          class="core-action-table"
        >
          <el-table-column v-if="isCoreOrderView" width="44" align="center">
            <template slot-scope="scope">
              <el-button
                v-if="scope.row._rowType === 'order'"
                type="text"
                class="core-order-toggle"
                :aria-label="isCoreOrderExpanded(scope.row) ? '收起订单成交明细' : '展开订单成交明细'"
                @click="toggleCoreOrder(scope.row)"
              >
                <span
                  :class="['core-order-chevron', { 'is-expanded': isCoreOrderExpanded(scope.row) }]"
                  aria-hidden="true"
                />
              </el-button>
            </template>
          </el-table-column>
          <el-table-column :label="isCoreOrderView ? '订单 / 买入时间' : '买入时间'" width="190">
            <template slot-scope="scope">
              <template v-if="scope.row._rowType === 'order'">
                <div class="core-cell-primary">订单 ID {{ scope.row.orderId || '--' }}</div>
                <small class="core-cell-meta">
                  {{ scope.row.tradeTime || '--' }} · {{ scope.row.tradeCount }} 个成交
                </small>
              </template>
              <template v-else>
                <div class="core-cell-primary">{{ scope.row.tradeTime || '--' }}</div>
                <small v-if="scope.row.tradeId" class="core-cell-meta" :title="String(scope.row.tradeId)">
                  成交 ID {{ scope.row.tradeId }}
                </small>
              </template>
            </template>
          </el-table-column>
          <el-table-column :label="isCoreOrderView ? '均价 / 金额' : '买入价格'" width="135">
            <template slot-scope="scope">
              <div class="core-cell-primary">{{ decimalValue(scope.row.price, 2) }}</div>
              <small class="core-cell-meta">金额 {{ moneyValue(scope.row.openAmount) }}</small>
            </template>
          </el-table-column>
          <el-table-column label="持仓数量" width="125">
            <template slot-scope="scope">{{ decimalValue(scope.row.remainingQty, 8) }}</template>
          </el-table-column>
          <el-table-column label="可撮合数量" width="125">
            <template slot-scope="scope">{{ decimalValue(coreAvailableQty(scope.row), 8) }}</template>
          </el-table-column>
          <el-table-column label="当前盈亏" width="145">
            <template slot-scope="scope">
              <strong :class="['core-cell-pnl', valueTone(scope.row.netPnl)]">
                {{ signedMoneyValue(scope.row.netPnl) }}
              </strong>
              <small class="core-cell-meta">收益率 {{ signedPercentValue(scope.row.roi) }}</small>
              <small class="core-cell-meta">保本价 {{ decimalValue(scope.row.breakEvenPrice, 2) }}</small>
            </template>
          </el-table-column>
          <el-table-column label="底仓" width="125" align="center">
            <template slot-scope="scope">
              <el-tag
                :type="scope.row._rowType === 'order' ? coreOrderStatusType(scope.row) : (hasCorePosition(scope.row) ? '' : 'info')"
                size="mini"
              >
                {{ scope.row._rowType === 'order' ? scope.row.coreStatus : (hasCorePosition(scope.row) ? '已设置' : '未设置') }}
              </el-tag>
              <small class="core-cell-meta">数量 {{ decimalValue(scope.row.coreQty || 0, 8) }}</small>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="170" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button
                v-if="scope.row._rowType === 'order'"
                type="primary"
                plain
                size="mini"
                @click="toggleCoreOrder(scope.row)"
              >{{ isCoreOrderExpanded(scope.row) ? '收起明细' : '展开明细' }}</el-button>
              <div v-else class="core-action-buttons">
                <el-button
                  v-if="!hasCorePosition(scope.row) && checkPer(['admin', 'binanceSpotCorePosition:add'])"
                  type="primary"
                  size="mini"
                  @click="lockCorePosition(scope.row)"
                >设为底仓</el-button>
                <template v-else-if="hasCorePosition(scope.row)">
                  <el-button
                    v-if="checkPer(['admin', 'binanceSpotCorePosition:edit'])"
                    type="primary"
                    size="mini"
                    @click="adjustCorePosition(scope.row)"
                  >调整</el-button>
                  <el-popconfirm
                    v-if="checkPer(['admin', 'binanceSpotCorePosition:edit'])"
                    title="解除后，该数量会按原买入时间重新参与后续 FIFO 撮合，是否继续？"
                    placement="top-end"
                    :width="320"
                    confirm-button-text="确定"
                    cancel-button-text="取消"
                    icon="el-icon-warning"
                    icon-color="#e6a23c"
                    @confirm="confirmDrawerReleaseCorePosition(scope.row)"
                  >
                    <el-button
                      slot="reference"
                      type="warning"
                      plain
                      size="mini"
                    >解除</el-button>
                  </el-popconfirm>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import crudBinanceTradeInfo from '@/api/binanceTradeInfo'
import { listAllAccount } from '@/api/binanceAccountInfo'
import TradePositionDistributionBar from '@/views/invest/binance/tradeInfo/TradePositionDistributionBar.vue'
import CRUD from '@crud/crud'
import * as numberUtil from '@/utils/numberUtil'
import { add as addCorePosition, edit as editCorePosition, lockAll as lockAllCorePositionsApi, release as releaseCorePositionApi, releaseAll as releaseAllCorePositionsApi } from '@/api/binanceSpotCorePosition'

const ACCOUNT_STORAGE_KEY = 'binanceTradeInfoStats.uid'

export default {
  name: 'BinanceTradeInfoStats',
  components: { TradePositionDistributionBar },
  dicts: ['invest_binance_trade_pairing_logic', 'invest_binance_symbol'],
  data() {
    return {
      statsInfo: {
        spotFuturesStatsInfo: {},
        realtimeStatus: {},
        warnings: []
      },
      accountList: [],
      statsLoading: false,
      syncLoading: false,
      statsDebounceTimer: null,
      statsRequestId: 0,
      showCoreActions: false,
      coreActionRows: [],
      coreActionView: 'order',
      expandedCoreOrderKeys: [],
      selectedCoreRange: null,
      coreActionsDirty: false,
      batchLockLoading: false,
      batchReleaseLoading: false,
      query: {
        symbol: 'BTCUSDT',
        uid: null
      }
    }
  },
  computed: {
    spotStats() {
      return (this.statsInfo && this.statsInfo.spotFuturesStatsInfo) || {}
    },
    accountStats() {
      return (this.statsInfo && this.statsInfo.accountInfo) || {}
    },
    coreActionTitle() {
      return this.selectedCoreRange ? `${this.selectedCoreRange.range} 区间持仓` : '底仓操作'
    },
    isCoreOrderView() {
      return Boolean(this.selectedCoreRange) && this.coreActionView === 'order'
    },
    coreActionOrderGroups() {
      const groupedRows = new Map()
      this.coreActionRows.forEach(row => {
        const orderKey = this.coreActionOrderKey(row)
        if (!groupedRows.has(orderKey)) groupedRows.set(orderKey, [])
        groupedRows.get(orderKey).push(row)
      })
      return Array.from(groupedRows.entries()).map(([orderKey, rows]) => this.createCoreOrderGroup(orderKey, rows))
    },
    coreActionDisplayRows() {
      if (!this.isCoreOrderView) return this.coreActionRows
      return this.coreActionOrderGroups.reduce((rows, order) => {
        rows.push(order)
        if (this.isCoreOrderExpanded(order)) rows.push(...order.trades)
        return rows
      }, [])
    },
    coreActionCoreRows() {
      return this.coreActionRows.filter(row => this.hasCorePosition(row))
    },
    coreActionUnlockedRows() {
      return this.coreActionRows.filter(row => !this.hasCorePosition(row) && this.coreAvailableQty(row) > 0)
    },
    realtimeWarningText() {
      const warnings = ((this.statsInfo && this.statsInfo.warnings) || [])
        .filter(message => !/U本位|币本位|合约|对冲/.test(message))
      const accountStatus = this.statsInfo && this.statsInfo.realtimeStatus && this.statsInfo.realtimeStatus.accountInfo
      if (accountStatus && accountStatus.status === 'CACHE' && accountStatus.updatedAt) {
        warnings.push(`账户资产缓存时间：${new Date(accountStatus.updatedAt).toLocaleString('zh-CN')}`)
      }
      return warnings.join('；')
    },
    positionMetrics() {
      return [
        { label: '当前价格', value: this.moneyValue(this.spotStats.currentSpotPrice), tone: 'primary' },
        { label: '持仓均价', value: this.moneyValue(this.spotStats.posAvgPrice), tone: 'warning' },
        { label: '持仓数量', value: this.quantityValue(this.spotStats.posQty), tone: '' },
        { label: '持仓盈亏', value: this.signedMoneyValue(this.spotStats.holdingProfitLoss), tone: this.valueTone(this.spotStats.holdingProfitLoss) }
      ]
    },
    accountSummaryItems() {
      return [
        { label: '汇率', value: this.decimalValue(this.accountStats.rmbToUsdRate, 4) },
        { label: 'RMB总额', value: this.currencyValue(this.accountStats.rmbAmount, '¥') },
        { label: 'USD总额', value: this.currencyValue(this.accountStats.usdAmount, '$') }
      ]
    },
    tradeSummaryItems() {
      const netPnlDelta = this.netPnlDelta
      return [
        { label: '买入总额', value: this.moneyValue(this.spotStats.totalBuyAmount) },
        { label: '卖出总额', value: this.moneyValue(this.spotStats.totalSellAmount) },
        { label: '收益率', value: this.signedPercentValue(this.spotStats.roi), tone: this.valueTone(this.spotStats.roi) },
        { label: '盈亏', value: this.signedMoneyValue(this.spotStats.pnl), tone: this.valueTone(this.spotStats.pnl) },
        { label: '手续费', value: this.moneyValue(this.spotStats.fee) },
        {
          label: '净盈亏',
          value: this.signedMoneyValue(this.spotStats.netPnl),
          tone: this.valueTone(this.spotStats.netPnl),
          delta: netPnlDelta == null || Number(netPnlDelta) === 0 ? '' : this.signedMoneyValue(netPnlDelta),
          deltaTone: this.valueTone(netPnlDelta)
        },
        { label: '未匹配卖出', value: this.quantityValue(this.spotStats.unmatchedSellQty, false) },
        { label: '持仓总额', value: this.moneyValue(this.spotStats.posAmount) },
        { label: '持仓盈利', value: this.signedMoneyValue(this.spotStats.holdingProfit), tone: 'positive' },
        { label: '持仓亏损', value: this.signedMoneyValue(this.spotStats.holdingLoss), tone: 'negative' }
      ]
    },
    netPnlDelta() {
      if (this.spotStats.netPnl == null || this.spotStats.lastNetPnl == null) return null
      return numberUtil.subAmount(this.spotStats.netPnl, this.spotStats.lastNetPnl, 2)
    }
  },
  mounted() {
    this.refreshAccountList()
  },
  beforeDestroy() {
    this.clearStatsDebounce()
    this.statsRequestId++
  },
  methods: {
    scheduleStats() {
      this.clearStatsDebounce()
      const requestId = ++this.statsRequestId
      this.statsDebounceTimer = setTimeout(() => {
        this.statsDebounceTimer = null
        this.executeStats(requestId)
      }, 300)
    },
    clearStatsDebounce() {
      if (this.statsDebounceTimer) {
        clearTimeout(this.statsDebounceTimer)
        this.statsDebounceTimer = null
      }
    },
    doStats() {
      this.clearStatsDebounce()
      this.executeStats(++this.statsRequestId)
    },
    executeStats(requestId) {
      if (this.query.uid == null || !this.query.symbol) {
        if (requestId === this.statsRequestId) this.statsLoading = false
        return
      }
      this.statsLoading = true
      crudBinanceTradeInfo.stats({ ...this.query }).then(res => {
        if (requestId === this.statsRequestId) {
          this.statsInfo = res
        }
      }).catch(() => {
        // 请求错误由全局拦截器提示；过期请求不改变当前页面状态
      }).then(() => {
        if (requestId === this.statsRequestId) this.statsLoading = false
      })
    },
    handleAccountChange(uid) {
      if (uid == null) {
        window.localStorage.removeItem(ACCOUNT_STORAGE_KEY)
      } else {
        window.localStorage.setItem(ACCOUNT_STORAGE_KEY, String(uid))
      }
      this.scheduleStats()
    },
    refreshAccountList() {
      return listAllAccount().then(data => {
        this.accountList = (data && data.content) || []
        const storedUid = window.localStorage.getItem(ACCOUNT_STORAGE_KEY)
        const selectedAccount = this.accountList.find(item => String(item.uid) === storedUid) || this.accountList[0]
        this.query.uid = selectedAccount ? selectedAccount.uid : null
        if (this.query.uid != null) {
          window.localStorage.setItem(ACCOUNT_STORAGE_KEY, String(this.query.uid))
          this.doStats()
        }
      })
    },
    syncSpotTradeInfo() {
      this.syncLoading = true
      crudBinanceTradeInfo.syncSelected({ uid: this.query.uid, symbol: this.query.symbol }).then(res => {
        this.doStats()
        this.$notify({
          title: `同步成功：现货 ${res.spotCount || 0} 条`,
          type: CRUD.NOTIFICATION_TYPE.SUCCESS,
          duration: 2500
        })
        this.syncLoading = false
      }).catch(() => {
        this.syncLoading = false
      })
    },
    openTradeCoreActions(trade) {
      this.selectedCoreRange = null
      this.coreActionRows = [this.toCoreActionRow(trade)]
      this.coreActionView = 'trade'
      this.expandedCoreOrderKeys = []
      this.coreActionsDirty = false
      this.showCoreActions = true
    },
    openBucketCoreActions(bucket) {
      this.selectedCoreRange = { ...bucket }
      this.coreActionRows = (bucket.trades || []).map(trade => this.toCoreActionRow(trade.raw || trade))
      this.coreActionView = 'order'
      this.expandedCoreOrderKeys = []
      this.coreActionsDirty = false
      this.showCoreActions = true
    },
    toCoreActionRow(row) {
      const remainingQty = row.remainingQty == null ? row.qty : row.remainingQty
      const coreQty = Number(row.coreQty) || 0
      const availableQty = row.availableQty == null
        ? Math.max((Number(remainingQty) || 0) - coreQty, 0)
        : row.availableQty
      return {
        ...row,
        _rowType: 'trade',
        _rowKey: `trade:${row.tradeId}`,
        uid: row.uid == null ? this.query.uid : row.uid,
        symbol: row.symbol || this.query.symbol,
        tradeTime: row.tradeTime || row.openTime,
        price: row.price == null ? row.openPrice : row.price,
        remainingQty,
        coreQty,
        availableQty,
        lockedAt: row.lockedAt || row.coreLockedAt
      }
    },
    coreActionOrderKey(row) {
      if (row.orderId !== null && row.orderId !== undefined && row.orderId !== '') {
        return `order:${row.orderId}`
      }
      return `trade:${row.tradeId}`
    },
    createCoreOrderGroup(orderKey, rows) {
      const summary = rows.reduce((result, row) => {
        const remainingQty = Number(row.remainingQty) || 0
        const openAmount = row.openAmount == null
          ? numberUtil.mulAmount(row.price, remainingQty)
          : Number(row.openAmount) || 0
        const breakEvenAmount = numberUtil.mulAmount(row.breakEvenPrice || row.price, remainingQty)
        result.remainingQty = numberUtil.addAmount(result.remainingQty, remainingQty, 8)
        result.availableQty = numberUtil.addAmount(result.availableQty, this.coreAvailableQty(row), 8)
        result.openAmount = numberUtil.addAmount(result.openAmount, openAmount, 8)
        result.netPnl = numberUtil.addAmount(result.netPnl, row.netPnl, 8)
        result.coreQty = numberUtil.addAmount(result.coreQty, row.coreQty, 8)
        result.breakEvenAmount = numberUtil.addAmount(result.breakEvenAmount, breakEvenAmount, 8)
        if (this.hasCorePosition(row)) result.coreCount++
        return result
      }, {
        remainingQty: 0,
        availableQty: 0,
        openAmount: 0,
        netPnl: 0,
        coreQty: 0,
        breakEvenAmount: 0,
        coreCount: 0
      })
      const firstRow = rows[0] || {}
      return {
        _rowType: 'order',
        _rowKey: `group:${orderKey}`,
        _orderKey: orderKey,
        orderId: firstRow.orderId,
        tradeTime: firstRow.tradeTime,
        tradeCount: rows.length,
        price: numberUtil.divAmount(summary.openAmount, summary.remainingQty, 8),
        openAmount: summary.openAmount,
        remainingQty: summary.remainingQty,
        availableQty: summary.availableQty,
        netPnl: summary.netPnl,
        roi: numberUtil.divAmount(summary.netPnl, summary.openAmount, 8),
        breakEvenPrice: numberUtil.divAmount(summary.breakEvenAmount, summary.remainingQty, 8),
        coreQty: summary.coreQty,
        coreCount: summary.coreCount,
        coreStatus: summary.coreCount === 0 ? '未设置' : (summary.coreCount === rows.length ? '全部设置' : '部分设置'),
        trades: rows
      }
    },
    handleCoreActionViewChange() {
      this.expandedCoreOrderKeys = []
    },
    toggleCoreOrder(order) {
      const index = this.expandedCoreOrderKeys.indexOf(order._orderKey)
      if (index >= 0) {
        this.expandedCoreOrderKeys.splice(index, 1)
      } else {
        this.expandedCoreOrderKeys.push(order._orderKey)
      }
    },
    isCoreOrderExpanded(order) {
      return this.expandedCoreOrderKeys.includes(order._orderKey)
    },
    coreActionRowKey(row) {
      return row._rowKey
    },
    coreActionRowClassName({ row }) {
      if (!this.isCoreOrderView) return ''
      return row._rowType === 'order' ? 'core-order-row' : 'core-trade-child-row'
    },
    coreOrderStatusType(order) {
      if (order.coreStatus === '未设置') return 'info'
      if (order.coreStatus === '部分设置') return 'warning'
      return ''
    },
    hasCorePosition(row) {
      return Boolean(row && row.corePositionId && Number(row.coreQty) > 0)
    },
    coreAvailableQty(row) {
      if (row.availableQty != null) return row.availableQty
      return Math.max((Number(row.qty || row.remainingQty) || 0) - (Number(row.coreQty) || 0), 0)
    },
    decimalValue(value, digits) {
      if (value === null || value === undefined || value === '') return '--'
      const number = Number(value)
      if (!Number.isFinite(number)) return '--'
      return number.toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
    },
    currencyValue(value, prefix) {
      if (value === null || value === undefined || value === '') return '--'
      const number = Number(value)
      if (!Number.isFinite(number)) return '--'
      const formatted = this.decimalValue(Math.abs(number), 2)
      return `${number < 0 ? '-' : ''}${prefix}${formatted}`
    },
    moneyValue(value) {
      return this.currencyValue(value, '$')
    },
    signedMoneyValue(value) {
      if (value === null || value === undefined || value === '') return '--'
      const number = Number(value)
      if (!Number.isFinite(number)) return '--'
      return `${number > 0 ? '+' : number < 0 ? '-' : ''}$${this.decimalValue(Math.abs(number), 2)}`
    },
    signedPercentValue(value) {
      if (value === null || value === undefined || value === '') return '--'
      const number = Number(value)
      if (!Number.isFinite(number)) return '--'
      return `${number > 0 ? '+' : ''}${numberUtil.formatPercent(number)}`
    },
    quantityValue(value, withSymbol = true) {
      if (value === null || value === undefined || value === '') return '--'
      const number = Number(value)
      if (!Number.isFinite(number)) return '--'
      const formatted = number.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 8 })
      if (formatted === '--' || !withSymbol || !this.query.symbol) return formatted
      const asset = this.query.symbol.replace(/USDT$|BUSD$|USDC$/, '')
      return `${formatted} ${asset}`
    },
    valueTone(value) {
      const number = Number(value)
      if (number > 0) return 'positive'
      if (number < 0) return 'negative'
      return ''
    },
    lockCorePosition(row) {
      this.promptCoreQty('设置底仓数量', row.remainingQty, row.remainingQty).then(value => {
        return addCorePosition({
          uid: row.uid,
          symbol: row.symbol,
          tradeId: row.tradeId,
          coreQty: value,
          remark: null
        })
      }).then(resource => {
        this.$message.success('底仓设置成功')
        return this.handleCorePositionMutation(row, resource, 'lock')
      }).catch(() => {})
    },
    adjustCorePosition(row) {
      this.promptCoreQty('调整底仓数量', row.coreQty, row.remainingQty).then(value => {
        return editCorePosition({ id: row.corePositionId, coreQty: value, remark: row.remark })
      }).then(resource => {
        this.$message.success('底仓调整成功')
        return this.handleCorePositionMutation(row, resource, 'adjust')
      }).catch(() => {})
    },
    confirmDrawerReleaseCorePosition(row) {
      return releaseCorePositionApi(row.corePositionId).then(resource => {
        this.$message.success('底仓已解除')
        return this.handleCorePositionMutation(row, resource, 'release')
      }).catch(() => {})
    },
    lockAllCorePositions() {
      const rows = this.coreActionUnlockedRows.slice()
      if (rows.length === 0) return Promise.resolve()
      this.batchLockLoading = true
      return lockAllCorePositionsApi({
        uid: this.query.uid,
        symbol: this.query.symbol,
        tradeIds: rows.map(row => row.tradeId)
      }).then(resources => {
        const resourcesByTradeId = new Map((resources || []).map(resource => [String(resource.tradeId), resource]))
        rows.forEach(row => {
          const resource = resourcesByTradeId.get(String(row.tradeId))
          if (resource) this.handleCorePositionMutation(row, resource, 'lock')
        })
        this.$message.success(`已设置 ${rows.length} 笔底仓`)
      }).catch(() => {}).finally(() => {
        this.batchLockLoading = false
      })
    },
    releaseAllCorePositions() {
      const rows = this.coreActionCoreRows.slice()
      if (rows.length === 0) return Promise.resolve()
      this.batchReleaseLoading = true
      return releaseAllCorePositionsApi(rows.map(row => row.corePositionId)).then(() => {
        rows.forEach(row => this.handleCorePositionMutation(row, null, 'release'))
        this.$message.success(`已解除 ${rows.length} 笔底仓`)
      }).catch(() => {}).finally(() => {
        this.batchReleaseLoading = false
      })
    },
    handleCorePositionMutation(row, resource, action) {
      if (this.showCoreActions) {
        if (action === 'release') {
          Object.assign(row, {
            corePositionId: null,
            coreQty: 0,
            availableQty: row.remainingQty,
            lockedAt: null
          })
        } else {
          const coreQty = resource && resource.coreQty != null ? resource.coreQty : row.coreQty
          Object.assign(row, {
            corePositionId: resource && resource.id ? resource.id : row.corePositionId,
            coreQty,
            availableQty: numberUtil.subAmount(row.remainingQty, coreQty, 8),
            lockedAt: resource && resource.lockedAt ? resource.lockedAt : row.lockedAt,
            remark: resource && resource.remark != null ? resource.remark : row.remark
          })
        }
        this.coreActionsDirty = true
        this.refreshSelectedCoreRangeSummary()
        return Promise.resolve()
      }
      this.doStats()
      return Promise.resolve()
    },
    refreshSelectedCoreRangeSummary() {
      if (!this.selectedCoreRange) return
      const summary = this.coreActionRows.reduce((result, row) => {
        const coreQty = Number(row.coreQty) || 0
        if (coreQty > 0) result.coreCount++
        result.coreQty = numberUtil.addAmount(result.coreQty, coreQty)
        result.availableQty = numberUtil.addAmount(result.availableQty, this.coreAvailableQty(row))
        return result
      }, { coreCount: 0, coreQty: 0, availableQty: 0 })
      Object.assign(this.selectedCoreRange, summary)
    },
    handleCoreActionDrawerClosed() {
      if (!this.coreActionsDirty) return
      this.coreActionsDirty = false
      this.doStats()
    },
    promptCoreQty(title, value, maxQty) {
      return this.$prompt(`数量必须大于 0，且不能超过当前剩余数量 ${maxQty}`, title, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: String(value),
        inputValidator: input => {
          const qty = Number(input)
          return (!Number.isNaN(qty) && qty > 0 && qty <= Number(maxQty)) || '请输入有效的底仓数量'
        }
      }).then(({ value: input }) => input)
    }
  }
}
</script>

<style scoped>
.stats-page { display: flex; flex-direction: column; gap: 12px; box-sizing: border-box; height: calc(100vh - 117px); overflow: hidden; background: #f5f7fa; padding: 12px 16px; }
.panel { background: #fff; border: 1px solid #ebeef5; border-radius: 10px; box-shadow: 0 4px 14px rgba(31, 45, 61, 0.05); }
.toolbar-panel { flex: 0 0 auto; padding: 12px 16px; }
.toolbar-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.toolbar-main { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; min-width: 0; }
.toolbar-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 10px; }
.toolbar-actions .el-button + .el-button { margin-left: 0; }
.field-label { color: #303133; font-weight: 500; }
.account-select, .symbol-select { width: 210px; }
.toolbar-main .el-button + .el-button { margin-left: 0; }
.realtime-warning { margin-top: 12px; }
.position-panel { display: flex; flex: 1 1 auto; flex-direction: column; min-height: 0; padding: 12px 18px 6px; overflow: hidden; }
.position-panel .position-chart { flex: 1 1 auto; min-height: 0; }
.position-heading { position: absolute; width: 1px; height: 1px; overflow: hidden; margin: -1px; padding: 0; border: 0; clip: rect(0 0 0 0); }
.position-metrics { display: grid; flex: 0 0 auto; grid-template-columns: repeat(4, minmax(150px, 1fr)); }
.metric-item { padding: 4px 20px; border-right: 1px solid #ebeef5; }
.metric-item:first-child { padding-left: 0; }
.metric-item:last-child { border-right: 0; }
.metric-label, .summary-label { display: block; margin-bottom: 6px; color: #8492a6; font-size: 13px; }
.metric-value { display: block; color: #17233d; font-size: 21px; line-height: 1.2; }
.primary { color: #409eff !important; }
.warning { color: #e6a23c !important; }
.positive { color: #13a76f !important; }
.negative { color: #f56c6c !important; }
.popover-heading { margin-bottom: 6px; color: #17233d; font-weight: 600; }
.popover-stat-row { display: flex; align-items: center; justify-content: space-between; padding: 9px 2px; border-top: 1px solid #ebeef5; }
.popover-stat-row span { color: #8492a6; }
.popover-stat-row strong { color: #17233d; }
.trade-summary-popover .summary-grid { display: grid; grid-template-columns: repeat(5, minmax(110px, 1fr)); }
.summary-item { min-width: 0; padding: 9px 10px; border-right: 1px solid #ebeef5; border-top: 1px solid #f1f3f7; }
.summary-item:nth-child(5n) { border-right: 0; }
.summary-value-row { display: flex; align-items: center; gap: 7px; min-width: 0; }
.summary-value { display: block; color: #17233d; font-size: 16px; white-space: nowrap; }
.summary-delta { flex: 0 0 auto; padding: 1px 6px; background: #f4f4f5; border-radius: 9px; font-size: 12px; line-height: 18px; white-space: nowrap; }
.summary-delta.positive { background: #ecf8f3; }
.summary-delta.negative { background: #fef0f0; }
.core-action-content { padding: 0 20px 24px; }
.core-range-summary { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 12px; }
.core-action-tip { margin-bottom: 14px; }
.core-action-view-switch { margin-bottom: 12px; }
.core-action-table { width: 100%; }
.core-cell-primary { color: #303133; white-space: nowrap; }
.core-cell-meta { display: block; overflow: hidden; margin-top: 4px; color: #909399; font-size: 12px; line-height: 1.25; text-overflow: ellipsis; white-space: nowrap; }
.core-cell-pnl { display: block; white-space: nowrap; }
.core-action-buttons { display: flex; align-items: center; justify-content: center; gap: 8px; white-space: nowrap; }
.core-action-buttons .el-button + .el-button { margin-left: 0; }
.core-order-toggle { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; overflow: hidden; padding: 0; }
.core-order-chevron { display: block; width: 7px; height: 7px; border-right: 1px solid #409eff; border-bottom: 1px solid #409eff; transform: rotate(-45deg); transition: transform 0.2s; }
.core-order-chevron.is-expanded { transform: rotate(45deg); }
::v-deep .core-order-row > td { background: #f7faff !important; }
::v-deep .core-order-row .core-cell-primary { font-weight: 600; }
::v-deep .core-trade-child-row > td { background: #fff !important; }
::v-deep .core-trade-child-row > td:nth-child(2) { padding-left: 18px; }
@media (max-width: 1200px) {
  .stats-page { height: auto; min-height: calc(100vh - 117px); overflow: visible; }
  .toolbar-row { align-items: flex-start; flex-direction: column; }
  .toolbar-actions { align-self: flex-end; }
  .position-panel .position-chart { min-height: 430px; }
  .position-metrics { grid-template-columns: repeat(3, 1fr); row-gap: 12px; }
  ::v-deep .core-action-drawer { width: 94% !important; }
}
@media (max-height: 760px) {
  .stats-page { height: auto; min-height: calc(100vh - 117px); overflow: visible; }
  .position-panel .position-chart { min-height: 430px; }
}
</style>
