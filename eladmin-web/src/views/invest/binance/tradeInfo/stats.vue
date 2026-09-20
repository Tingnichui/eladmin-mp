<template>
  <div class="app-container stats-page">
    <section class="panel toolbar-panel">
      <div class="toolbar-row">
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
        <el-button
          v-if="checkPer(['admin', 'binanceSpotCorePosition:list'])"
          size="small"
          type="primary"
          icon="el-icon-lock"
          :disabled="query.uid == null || !query.symbol"
          @click="openCorePositionDialog"
        >底仓管理</el-button>
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
      <div class="panel-title-row">
        <h3>{{ query.symbol || '现货' }} 持仓买入分布</h3>
      </div>
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

    <div class="summary-layout">
      <section class="panel account-panel">
        <h3>账户统计</h3>
        <div v-for="item in accountSummaryItems" :key="item.label" class="account-row">
          <span><i :class="item.icon" />{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>
      <section class="panel trade-summary-panel">
        <div class="panel-title-row">
          <h3>交易汇总</h3>
        </div>
        <div class="summary-grid">
          <div v-for="item in tradeSummaryItems" :key="item.label" class="summary-item">
            <span class="summary-icon"><i :class="item.icon" /></span>
            <div>
              <span class="summary-label">{{ item.label }}</span>
              <strong :class="['summary-value', item.tone]">{{ item.value }}</strong>
              <small v-if="item.note">{{ item.note }}</small>
            </div>
          </div>
        </div>
      </section>
    </div>

    <el-dialog title="现货底仓管理" :visible.sync="showCorePositions" width="88%">
      <el-alert
        title="底仓只影响未来撮合；已经固化的历史撮合不会重排。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-table v-loading="corePositionLoading" :data="corePositionCandidates" border stripe>
        <el-table-column prop="tradeId" label="成交 ID" min-width="150" />
        <el-table-column prop="tradeTime" label="买入时间" min-width="160" />
        <el-table-column prop="price" label="买入价格" min-width="120" />
        <el-table-column prop="remainingQty" label="剩余数量" min-width="120" />
        <el-table-column prop="coreQty" label="底仓数量" min-width="120" />
        <el-table-column prop="availableQty" label="可撮合数量" min-width="120" />
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column label="操作" min-width="210" fixed="right">
          <template slot-scope="scope">
            <el-button
              v-if="!scope.row.corePositionId && checkPer(['admin', 'binanceSpotCorePosition:add'])"
              type="primary"
              size="mini"
              @click="lockCorePosition(scope.row)"
            >设为底仓</el-button>
            <template v-else-if="scope.row.corePositionId">
              <el-button
                v-if="checkPer(['admin', 'binanceSpotCorePosition:edit'])"
                type="primary"
                size="mini"
                @click="adjustCorePosition(scope.row)"
              >调整</el-button>
              <el-button
                v-if="checkPer(['admin', 'binanceSpotCorePosition:edit'])"
                type="warning"
                size="mini"
                @click="releaseCorePosition(scope.row)"
              >解除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-drawer
      :title="coreActionTitle"
      :visible.sync="showCoreActions"
      direction="rtl"
      size="1050px"
      custom-class="core-action-drawer"
      @closed="handleCoreActionDrawerClosed"
    >
      <div class="core-action-content">
        <div v-if="selectedCoreRange" class="core-range-summary">
          <el-tag type="info">共 {{ coreActionRows.length }} 笔</el-tag>
          <el-tag type="primary">当前价 {{ moneyValue(spotStats.currentSpotPrice) }}</el-tag>
          <el-tag>底仓 {{ selectedCoreRange.coreCount || 0 }} 笔</el-tag>
          <el-tag>底仓 {{ decimalValue(selectedCoreRange.coreQty || 0, 8) }} BTC</el-tag>
          <el-tag type="success">可撮合 {{ decimalValue(selectedCoreRange.availableQty || 0, 8) }} BTC</el-tag>
          <el-popconfirm
            title="确定解除当前区间内的全部底仓吗？解除后将重新参与后续 FIFO 撮合。"
            placement="bottom-end"
            :width="320"
            :disabled="coreActionCoreRows.length === 0 || batchReleaseLoading"
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
              :disabled="coreActionCoreRows.length === 0"
              :loading="batchReleaseLoading"
            >一键解除</el-button>
          </el-popconfirm>
        </div>
        <el-alert
          title="底仓只影响未来撮合；聚合区间需选择具体买入批次进行操作。"
          type="info"
          :closable="false"
          show-icon
          class="core-action-tip"
        />
        <el-table :data="coreActionRows" border stripe class="core-action-table">
          <el-table-column label="买入时间" width="165">
            <template slot-scope="scope">
              <div class="core-cell-primary">{{ scope.row.tradeTime || '--' }}</div>
              <small v-if="scope.row.tradeId" class="core-cell-meta" :title="String(scope.row.tradeId)">
                成交 ID {{ scope.row.tradeId }}
              </small>
            </template>
          </el-table-column>
          <el-table-column label="买入价格" width="135">
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
              <el-tag :type="hasCorePosition(scope.row) ? '' : 'info'" size="mini">
                {{ hasCorePosition(scope.row) ? '已设置' : '未设置' }}
              </el-tag>
              <small class="core-cell-meta">数量 {{ decimalValue(scope.row.coreQty || 0, 8) }}</small>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="170" align="center" fixed="right">
            <template slot-scope="scope">
              <div class="core-action-buttons">
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
import { add as addCorePosition, edit as editCorePosition, getCandidates, release as releaseCorePositionApi, releaseAll as releaseAllCorePositionsApi } from '@/api/binanceSpotCorePosition'

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
      showCorePositions: false,
      showCoreActions: false,
      corePositionLoading: false,
      corePositionCandidates: [],
      coreActionRows: [],
      selectedCoreRange: null,
      coreActionsDirty: false,
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
    coreActionCoreRows() {
      return this.coreActionRows.filter(row => this.hasCorePosition(row))
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
        { label: '持仓盈亏', value: this.signedMoneyValue(this.spotStats.holdingProfitLoss), tone: this.valueTone(this.spotStats.holdingProfitLoss) },
        { label: '收益率', value: this.signedPercentValue(this.spotStats.roi), tone: this.valueTone(this.spotStats.roi) }
      ]
    },
    accountSummaryItems() {
      return [
        { label: '汇率', value: this.decimalValue(this.accountStats.rmbToUsdRate, 4), icon: 'el-icon-sort' },
        { label: 'RMB总额', value: this.currencyValue(this.accountStats.rmbAmount, '¥'), icon: 'el-icon-money' },
        { label: 'USD总额', value: this.currencyValue(this.accountStats.usdAmount, '$'), icon: 'el-icon-coin' }
      ]
    },
    tradeSummaryItems() {
      const netPnlDelta = this.netPnlDelta
      return [
        { label: '买入总额', value: this.moneyValue(this.spotStats.totalBuyAmount), icon: 'el-icon-shopping-cart-2' },
        { label: '卖出总额', value: this.moneyValue(this.spotStats.totalSellAmount), icon: 'el-icon-sold-out' },
        { label: '收益率', value: this.signedPercentValue(this.spotStats.roi), tone: this.valueTone(this.spotStats.roi), icon: 'el-icon-data-analysis' },
        { label: '盈亏', value: this.signedMoneyValue(this.spotStats.pnl), tone: this.valueTone(this.spotStats.pnl), icon: 'el-icon-s-data' },
        { label: '手续费', value: this.moneyValue(this.spotStats.fee), icon: 'el-icon-coin' },
        {
          label: '净盈亏',
          value: this.signedMoneyValue(this.spotStats.netPnl),
          tone: this.valueTone(this.spotStats.netPnl),
          icon: 'el-icon-s-marketing',
          note: netPnlDelta == null ? '' : `较上次 ${this.signedMoneyValue(netPnlDelta)}`
        },
        { label: '未匹配卖出', value: this.quantityValue(this.spotStats.unmatchedSellQty, false), icon: 'el-icon-document' },
        { label: '持仓总额', value: this.moneyValue(this.spotStats.posAmount), icon: 'el-icon-pie-chart' },
        { label: '持仓盈利', value: this.signedMoneyValue(this.spotStats.holdingProfit), tone: 'positive', icon: 'el-icon-top-right' },
        { label: '持仓亏损', value: this.signedMoneyValue(this.spotStats.holdingLoss), tone: 'negative', icon: 'el-icon-bottom-right' }
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
      this.coreActionsDirty = false
      this.showCoreActions = true
    },
    openBucketCoreActions(bucket) {
      this.selectedCoreRange = { ...bucket }
      this.coreActionRows = (bucket.trades || []).map(trade => this.toCoreActionRow(trade.raw || trade))
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
    openCorePositionDialog() {
      this.showCorePositions = true
      this.loadCorePositionCandidates()
    },
    loadCorePositionCandidates() {
      this.corePositionLoading = true
      return getCandidates({ uid: this.query.uid, symbol: this.query.symbol }).then(data => {
        this.corePositionCandidates = data || []
      }).finally(() => {
        this.corePositionLoading = false
      })
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
    releaseCorePosition(row) {
      this.$confirm('解除后，该数量会按原买入时间重新参与后续 FIFO 撮合，是否继续？', '解除底仓', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => releaseCorePositionApi(row.corePositionId)).then(resource => {
        this.$message.success('底仓已解除')
        return this.handleCorePositionMutation(row, resource, 'release')
      }).catch(() => {})
    },
    confirmDrawerReleaseCorePosition(row) {
      return releaseCorePositionApi(row.corePositionId).then(resource => {
        this.$message.success('底仓已解除')
        return this.handleCorePositionMutation(row, resource, 'release')
      }).catch(() => {})
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
      if (this.showCorePositions) return this.loadCorePositionCandidates()
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
      if (this.showCorePositions) this.loadCorePositionCandidates()
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
.toolbar-row { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.field-label { color: #303133; font-weight: 500; }
.account-select, .symbol-select { width: 210px; }
.toolbar-row .el-button + .el-button { margin-left: 0; }
.realtime-warning { margin-top: 12px; }
.position-panel { display: flex; flex: 1 1 auto; flex-direction: column; min-height: 0; padding: 12px 18px 6px; overflow: hidden; }
.position-panel .position-chart { flex: 1 1 auto; min-height: 0; }
.panel-title-row { display: flex; align-items: center; justify-content: space-between; }
.panel-title-row h3, .account-panel h3 { margin: 0; color: #17233d; font-size: 18px; }
.position-metrics { display: grid; flex: 0 0 auto; grid-template-columns: repeat(5, minmax(150px, 1fr)); margin-top: 8px; }
.metric-item { padding: 4px 20px; border-right: 1px solid #ebeef5; }
.metric-item:first-child { padding-left: 0; }
.metric-item:last-child { border-right: 0; }
.metric-label, .summary-label { display: block; margin-bottom: 6px; color: #8492a6; font-size: 13px; }
.metric-value { display: block; color: #17233d; font-size: 21px; line-height: 1.2; }
.primary { color: #409eff !important; }
.warning { color: #e6a23c !important; }
.positive { color: #13a76f !important; }
.negative { color: #f56c6c !important; }
.summary-layout { display: grid; flex: 0 0 166px; grid-template-columns: minmax(260px, 0.8fr) minmax(720px, 3.2fr); gap: 12px; min-height: 0; }
.account-panel, .trade-summary-panel { padding: 10px 14px; overflow: hidden; }
.account-row { display: flex; justify-content: space-between; align-items: center; box-sizing: border-box; height: 32px; margin-top: 6px; padding: 4px 9px; background: #f8fafc; border-radius: 6px; }
.account-row span { color: #637083; }
.account-row i { margin-right: 8px; color: #409eff; }
.account-row strong { color: #17233d; }
.summary-grid { display: grid; grid-auto-rows: 48px; grid-template-columns: repeat(5, minmax(125px, 1fr)); margin-top: 4px; }
.summary-item { display: flex; align-items: center; min-height: 0; padding: 4px 12px; border-right: 1px solid #ebeef5; border-top: 1px solid #f1f3f7; }
.summary-item:nth-child(5n) { border-right: 0; }
.summary-icon { display: flex; align-items: center; justify-content: center; flex: 0 0 34px; height: 34px; margin-right: 10px; color: #409eff; background: #ecf5ff; border-radius: 50%; }
.summary-value { display: block; color: #17233d; font-size: 16px; white-space: nowrap; }
.summary-item small { display: block; margin-top: 3px; color: #8492a6; white-space: nowrap; }
.core-action-content { padding: 0 20px 24px; }
.core-range-summary { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 12px; }
.core-action-tip { margin-bottom: 14px; }
.core-action-table { width: 100%; }
.core-cell-primary { color: #303133; white-space: nowrap; }
.core-cell-meta { display: block; overflow: hidden; margin-top: 4px; color: #909399; font-size: 12px; line-height: 1.25; text-overflow: ellipsis; white-space: nowrap; }
.core-cell-pnl { display: block; white-space: nowrap; }
.core-action-buttons { display: flex; align-items: center; justify-content: center; gap: 8px; white-space: nowrap; }
.core-action-buttons .el-button + .el-button { margin-left: 0; }
@media (max-width: 1200px) {
  .stats-page { height: auto; min-height: calc(100vh - 117px); overflow: visible; }
  .position-panel .position-chart { min-height: 430px; }
  .position-metrics { grid-template-columns: repeat(3, 1fr); row-gap: 12px; }
  .summary-layout { flex-basis: auto; grid-template-columns: 1fr; }
  .summary-grid { grid-template-columns: repeat(3, 1fr); }
  .summary-item:nth-child(5n) { border-right: 1px solid #ebeef5; }
  .summary-item:nth-child(3n) { border-right: 0; }
  ::v-deep .core-action-drawer { width: 94% !important; }
}
@media (max-height: 760px) {
  .stats-page { height: auto; min-height: calc(100vh - 117px); overflow: visible; }
  .position-panel .position-chart { min-height: 360px; }
  .summary-layout { flex-basis: auto; }
}
</style>
