<template>
  <div class="app-container coin-stats-page">
    <section class="panel toolbar-panel">
      <div class="toolbar-row">
        <div class="toolbar-main">
          <label>账户</label>
          <el-select v-model="query.uid" filterable size="small" placeholder="账户" class="account-select" @change="handleAccountChange">
            <el-option v-for="item in accountList" :key="item.id" :label="item.idCardName" :value="item.uid" />
          </el-select>
          <label>合约</label>
          <el-select v-model="query.symbol" size="small" class="symbol-select" @change="scheduleStats">
            <el-option label="BTCUSD 永续" value="BTCUSD_PERP" />
          </el-select>
          <label>方向</label>
          <el-select v-model="query.positionSide" size="small" class="side-select" @change="scheduleStats">
            <el-option label="做空 SHORT" value="SHORT" />
            <el-option label="做多 LONG" value="LONG" />
            <el-option label="单向 BOTH" value="BOTH" />
          </el-select>
          <el-button size="small" type="success" icon="el-icon-search" :loading="statsLoading" :disabled="!canQuery" @click="doStats">查询</el-button>
          <el-button v-if="checkPer(['admin', 'binanceTradeInfo:sync'])" size="small" plain type="success" icon="el-icon-refresh" :loading="syncLoading" :disabled="!canQuery" @click="syncData">同步数据</el-button>
          <template v-if="checkPer(['admin', 'binanceCoinFuturesTradeInfo:order'])">
            <el-button-group class="order-buttons">
              <el-button size="small" type="success" :disabled="!canPlaceOrder" @click="openOrderDialog('OPEN', 'LONG')">开多</el-button>
              <el-button size="small" type="danger" :disabled="!canPlaceOrder" @click="openOrderDialog('OPEN', 'SHORT')">开空</el-button>
              <el-button size="small" plain type="success" :disabled="!canPlaceOrder" @click="openOrderDialog('CLOSE', 'LONG')">平多</el-button>
              <el-button size="small" plain type="danger" :disabled="!canPlaceOrder" @click="openOrderDialog('CLOSE', 'SHORT')">平空</el-button>
            </el-button-group>
            <el-button size="small" plain icon="el-icon-tickets" :disabled="!canQuery" @click="showOpenOrders">当前挂单</el-button>
          </template>
        </div>
        <div class="toolbar-actions">
          <el-popover placement="bottom-end" width="300" trigger="hover">
            <div class="popover-title">账户资产（{{ marginAsset }}）</div>
            <div v-for="item in accountItems" :key="item.label" class="popover-row"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div>
            <el-button slot="reference" size="small" plain icon="el-icon-wallet">账户资产</el-button>
          </el-popover>
          <el-popover placement="bottom-end" width="360" trigger="hover">
            <div class="popover-title">当前持仓周期汇总</div>
            <div v-for="item in summaryItems" :key="item.label" class="popover-row"><span>{{ item.label }}</span><strong :class="item.tone">{{ item.value }}</strong></div>
            <el-button slot="reference" size="small" plain icon="el-icon-s-grid">全部汇总</el-button>
          </el-popover>
        </div>
      </div>
      <el-alert v-if="warningText" :title="warningText" type="warning" :closable="false" show-icon class="warning" />
    </section>

    <section v-loading="statsLoading" class="panel position-panel">
      <div class="position-metrics">
        <div v-for="item in positionMetrics" :key="item.label" class="metric-item">
          <span>{{ item.label }}</span>
          <strong :class="item.tone">{{ item.value }}</strong>
        </div>
      </div>
      <div class="risk-strip">
        <span>杠杆 <strong>{{ position.leverage || '--' }}x</strong></span>
        <span>保证金模式 <strong>{{ marginTypeText }}</strong></span>
        <span>盈亏平衡价 <strong>{{ price(position.breakEvenPrice) }}</strong></span>
        <span>合约面值 <strong>{{ decimal(contract.contractSize) }} USD/张</strong></span>
        <span>每张折合 <strong>{{ btcValue(markSingleContractBase) }}</strong></span>
      </div>
      <coin-futures-position-chart
        :row-data="statsInfo.tradeList || []"
        :current-price="position.markPrice"
        :average-price="position.entryPrice"
      />
    </section>

    <el-dialog :title="orderDialogTitle" :visible.sync="orderDialogVisible" width="520px" append-to-body @closed="resetOrderForm">
      <el-alert title="币本位数量单位为合约张数；提交后将直接发送至币安账户" type="warning" :closable="false" show-icon class="order-alert" />
      <el-form ref="orderForm" :model="orderForm" :rules="orderRules" label-width="100px">
        <el-form-item label="账户"><span>{{ selectedAccountName }}</span></el-form-item>
        <el-form-item label="操作">
          <el-tag :type="orderForm.positionSide === 'LONG' ? 'success' : 'danger'">{{ orderDialogTitle }}</el-tag>
        </el-form-item>
        <el-form-item label="订单类型" prop="type">
          <el-radio-group v-model="orderForm.type">
            <el-radio-button label="MARKET">市价</el-radio-button>
            <el-radio-button label="LIMIT">限价</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量（张）" prop="quantity">
          <el-input-number v-model="orderForm.quantity" :min="1" :precision="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="orderForm.type === 'LIMIT'" label="委托价" prop="price">
          <el-input-number v-model="orderForm.price" :min="0.01" :precision="2" :step="100" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="orderForm.type === 'LIMIT'" label="有效方式" prop="timeInForce">
          <el-select v-model="orderForm.timeInForce">
            <el-option label="一直有效 GTC" value="GTC" />
            <el-option label="立即成交或取消 IOC" value="IOC" />
            <el-option label="全部成交或取消 FOK" value="FOK" />
            <el-option label="只做 Maker GTX" value="GTX" />
          </el-select>
        </el-form-item>
        <el-form-item label="换算价格"><span>{{ price(orderReferencePrice) }}</span></el-form-item>
        <el-form-item label="每张折合"><strong>{{ btcValue(orderSingleContractBase) }}</strong></el-form-item>
        <el-form-item label="委托价值"><span>{{ decimal(orderNotionalUsd, 2) }} USD</span></el-form-item>
        <el-form-item label="预计数量"><strong class="primary">{{ btcValue(estimatedOrderBase) }}</strong></el-form-item>
        <el-form-item label="预估保证金">
          <strong>{{ btcValue(estimatedOrderMarginBase) }}</strong>
          <span class="estimate-note">（{{ position.leverage || 1 }}x，未含手续费，最终以币安为准）</span>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="orderSubmitting" @click="submitOrder">确认下单</el-button>
      </span>
    </el-dialog>

    <el-dialog title="币本位当前挂单" :visible.sync="openOrdersVisible" width="900px" append-to-body>
      <el-table v-loading="openOrdersLoading" :data="openOrderList" empty-text="暂无挂单" size="small">
        <el-table-column prop="orderId" label="订单 ID" min-width="145" />
        <el-table-column label="方向" width="90">
          <template slot-scope="scope"><el-tag size="mini" :type="scope.row.side === 'BUY' ? 'success' : 'danger'">{{ scope.row.side }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="positionSide" label="持仓方向" width="95" />
        <el-table-column prop="type" label="类型" width="90" />
        <el-table-column label="委托数量" min-width="145">
          <template slot-scope="scope">
            <div class="quantity-cell"><strong>{{ decimal(scope.row.origQty, 0) }} 张</strong><small>≈ {{ btcValue(orderEstimatedBase(scope.row, scope.row.origQty)) }}</small></div>
          </template>
        </el-table-column>
        <el-table-column label="已成交" min-width="145">
          <template slot-scope="scope">
            <div class="quantity-cell"><strong>{{ decimal(scope.row.executedQty, 0) }} 张</strong><small>{{ filledBaseText(scope.row) }}</small></div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="委托价" min-width="110" />
        <el-table-column prop="status" label="状态" width="85" />
        <el-table-column label="操作" width="80" fixed="right">
          <template slot-scope="scope"><el-button type="text" class="cancel-order" @click="cancelOpenOrder(scope.row)">撤单</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import coinFuturesApi from '@/api/binanceCoinFuturesTradeInfo'
import { listAllAccount } from '@/api/binanceAccountInfo'
import { calculateCoinQuantity, calculateInitialMarginCoin } from '@/utils/coinFuturesOrder'
import { formatQuantity } from '@/utils/numberUtil'
import CoinFuturesPositionChart from './CoinFuturesPositionChart.vue'

const ACCOUNT_STORAGE_KEY = 'invest.binance.stats.uid'

function emptyStatsInfo(warnings = []) {
  return { positionInfo: {}, tradeSummary: {}, accountInfo: {}, contractInfo: {}, tradeList: [], warnings }
}

export default {
  name: 'BinanceCoinFuturesStats',
  components: { CoinFuturesPositionChart },
  data() {
    return {
      query: { uid: null, symbol: 'BTCUSD_PERP', positionSide: 'SHORT' },
      accountList: [],
      statsInfo: emptyStatsInfo(),
      statsLoading: false,
      syncLoading: false,
      orderDialogVisible: false,
      orderSubmitting: false,
      openOrdersVisible: false,
      openOrdersLoading: false,
      openOrderList: [],
      orderForm: { action: 'OPEN', positionSide: 'LONG', type: 'LIMIT', quantity: 1, price: null, timeInForce: 'GTC' },
      orderRules: {
        quantity: [{ required: true, message: '请输入下单张数', trigger: 'blur' }],
        price: [{ validator: (rule, value, callback) => this.validateOrderPrice(value, callback), trigger: 'blur' }]
      },
      requestId: 0,
      debounceTimer: null
    }
  },
  computed: {
    canQuery() { return this.query.uid != null && this.query.symbol && this.query.positionSide },
    statsReady() { return Number(this.contract.contractSize) > 0 && Number(this.position.markPrice) > 0 },
    canPlaceOrder() { return this.canQuery && this.statsReady && !this.statsLoading },
    position() { return this.statsInfo.positionInfo || {} },
    summary() { return this.statsInfo.tradeSummary || {} },
    account() { return this.statsInfo.accountInfo || {} },
    contract() { return this.statsInfo.contractInfo || {} },
    marginAsset() { return this.contract.marginAsset || this.account.asset || 'BTC' },
    warningText() { return (this.statsInfo.warnings || []).join('；') },
    selectedAccountName() {
      const account = this.accountList.find(item => item.uid === this.query.uid)
      return account ? account.idCardName : '--'
    },
    orderDialogTitle() {
      const action = this.orderForm.action === 'OPEN' ? '开' : '平'
      const side = this.orderForm.positionSide === 'LONG' ? '多' : '空'
      return `${action}${side}`
    },
    orderReferencePrice() {
      return this.orderForm.type === 'LIMIT' ? Number(this.orderForm.price) : Number(this.position.markPrice)
    },
    markSingleContractBase() { return calculateCoinQuantity(1, this.contract.contractSize, this.position.markPrice) },
    orderSingleContractBase() { return calculateCoinQuantity(1, this.contract.contractSize, this.orderReferencePrice) },
    estimatedOrderBase() { return calculateCoinQuantity(this.orderForm.quantity, this.contract.contractSize, this.orderReferencePrice) },
    orderNotionalUsd() {
      const value = Number(this.orderForm.quantity) * Number(this.contract.contractSize)
      return Number.isFinite(value) && value > 0 ? value : null
    },
    estimatedOrderMarginBase() {
      return calculateInitialMarginCoin(this.orderForm.quantity, this.contract.contractSize,
        this.orderReferencePrice, this.position.leverage || 1)
    },
    marginTypeText() {
      const value = String(this.position.marginType || '').toLowerCase()
      if (value === 'cross' || value === 'crossed') return '全仓'
      if (value === 'isolated') return '逐仓'
      return '--'
    },
    positionMetrics() {
      const pnl = Number(this.position.unrealizedPnl)
      const usdPnl = pnl * Number(this.position.markPrice || 0)
      return [
        { label: '标记价格', value: this.price(this.position.markPrice), tone: 'primary' },
        { label: '开仓均价', value: this.price(this.position.entryPrice), tone: 'warning-tone' },
        { label: '持仓张数', value: `${this.decimal(Math.abs(Number(this.position.positionAmt || 0)))} 张` },
        { label: '未实现盈亏', value: `${this.signed(pnl)} ${this.marginAsset}`, tone: this.tone(pnl) },
        { label: '折合盈亏', value: `${this.signed(usdPnl)} USD`, tone: this.tone(usdPnl) },
        { label: '强平价格', value: this.price(this.position.liquidationPrice), tone: 'danger-tone' }
      ]
    },
    accountItems() {
      return [
        { label: '钱包余额', value: this.assetValue(this.account.walletBalance) },
        { label: '保证金余额', value: this.assetValue(this.account.marginBalance) },
        { label: '可用余额', value: this.assetValue(this.account.availableBalance) },
        { label: '初始保证金', value: this.assetValue(this.account.initialMargin) },
        { label: '维持保证金', value: this.assetValue(this.account.maintMargin) }
      ]
    },
    summaryItems() {
      return [
        { label: '已实现盈亏', value: this.assetValue(this.summary.realizedPnl), tone: this.tone(this.summary.realizedPnl) },
        { label: '手续费', value: this.assetValue(this.summary.commission) },
        { label: '资金费', value: this.assetValue(this.summary.fundingFee), tone: this.tone(this.summary.fundingFee) },
        { label: '净盈亏', value: this.assetValue(this.summary.netPnl), tone: this.tone(this.summary.netPnl) },
        { label: '成交笔数', value: this.summary.totalTradeCount == null ? '--' : this.summary.totalTradeCount },
        { label: '未平仓批次', value: this.summary.openTradeCount == null ? '--' : this.summary.openTradeCount }
      ]
    }
  },
  created() {
    this.loadAccounts()
  },
  beforeDestroy() {
    clearTimeout(this.debounceTimer)
    this.requestId += 1
  },
  methods: {
    loadAccounts() {
      return listAllAccount().then(data => {
        this.accountList = (data && data.content) || []
        const storedUid = window.localStorage.getItem(ACCOUNT_STORAGE_KEY)
        const selected = this.accountList.find(item => String(item.uid) === storedUid) || this.accountList[0]
        this.query.uid = selected ? selected.uid : null
        if (this.canQuery) this.doStats()
      })
    },
    handleAccountChange(uid) {
      if (uid != null) window.localStorage.setItem(ACCOUNT_STORAGE_KEY, String(uid))
      this.scheduleStats()
    },
    scheduleStats() {
      clearTimeout(this.debounceTimer)
      this.debounceTimer = setTimeout(() => this.doStats(), 180)
    },
    doStats() {
      if (!this.canQuery) return Promise.resolve()
      const requestId = ++this.requestId
      this.statsLoading = true
      this.statsInfo = emptyStatsInfo()
      return coinFuturesApi.stats({ ...this.query }).then(data => {
        if (requestId === this.requestId) this.statsInfo = data || {}
      }).catch(() => {
        if (requestId === this.requestId) this.statsInfo = emptyStatsInfo(['实时合约数据加载失败，已禁止下单，请重新查询'])
      }).finally(() => {
        if (requestId === this.requestId) this.statsLoading = false
      })
    },
    syncData() {
      if (!this.canQuery || this.syncLoading) return
      this.syncLoading = true
      coinFuturesApi.syncSelected({ uid: this.query.uid, symbol: this.query.symbol }).then(data => {
        this.$notify.success({ title: `币本位同步完成，新增 ${data.tradeCount || 0} 条成交`, duration: 2500 })
        return this.doStats()
      }).finally(() => { this.syncLoading = false })
    },
    openOrderDialog(action, positionSide) {
      if (!this.canPlaceOrder) {
        this.$message.warning('实时标记价格和合约面值尚未加载，暂不能下单')
        return
      }
      this.orderForm = {
        action,
        positionSide,
        type: 'LIMIT',
        quantity: action === 'CLOSE' && this.query.positionSide === positionSide
          ? Math.max(1, Math.abs(Number(this.position.positionAmt || 0))) : 1,
        price: Number(this.position.markPrice) || null,
        timeInForce: 'GTC'
      }
      this.orderDialogVisible = true
      this.$nextTick(() => this.$refs.orderForm && this.$refs.orderForm.clearValidate())
    },
    resetOrderForm() {
      this.orderSubmitting = false
      if (this.$refs.orderForm) this.$refs.orderForm.clearValidate()
    },
    validateOrderPrice(value, callback) {
      if (this.orderForm.type === 'LIMIT' && !(Number(value) > 0)) callback(new Error('请输入大于 0 的委托价'))
      else callback()
    },
    submitOrder() {
      if (!this.canPlaceOrder) {
        this.$message.warning('实时合约数据已失效，请重新查询后再下单')
        return
      }
      this.$refs.orderForm.validate(valid => {
        if (!valid || this.orderSubmitting) return
        const summary = `${this.selectedAccountName}：${this.orderDialogTitle} ${this.orderForm.quantity} 张，${this.orderForm.type === 'MARKET' ? '市价单' : `限价 ${this.orderForm.price} USD`}`
        this.$confirm(`${summary}。订单将直接发送到币安，是否继续？`, '确认币本位下单', {
          confirmButtonText: '确认下单',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.orderSubmitting = true
          const data = {
            uid: this.query.uid,
            symbol: this.query.symbol,
            action: this.orderForm.action,
            positionSide: this.orderForm.positionSide,
            type: this.orderForm.type,
            quantity: this.orderForm.quantity,
            timeInForce: this.orderForm.type === 'LIMIT' ? this.orderForm.timeInForce : null,
            price: this.orderForm.type === 'LIMIT' ? this.orderForm.price : null
          }
          return coinFuturesApi.placeOrder(data).then(order => {
            const actualBase = Number(order.cumBase)
            const baseText = actualBase > 0
              ? `实际成交 ${this.btcValue(actualBase)}`
              : `预计数量 ${this.btcValue(this.estimatedOrderBase)}`
            this.$notify.success({ title: '下单成功', message: `订单 ID：${order.orderId}；${baseText}`, duration: 4500 })
            this.orderDialogVisible = false
            return Promise.all([this.doStats(), this.loadOpenOrders(false)])
          }).finally(() => { this.orderSubmitting = false })
        }).catch(() => {})
      })
    },
    showOpenOrders() {
      this.openOrdersVisible = true
      this.loadOpenOrders(true)
    },
    loadOpenOrders(showLoading = true) {
      if (!this.canQuery) return Promise.resolve()
      if (showLoading) this.openOrdersLoading = true
      return coinFuturesApi.openOrders({ uid: this.query.uid, symbol: this.query.symbol }).then(data => {
        this.openOrderList = data || []
      }).finally(() => { this.openOrdersLoading = false })
    },
    cancelOpenOrder(order) {
      this.$confirm(`确认撤销订单 ${order.orderId}？已成交部分不会回退。`, '确认撤单', { type: 'warning' }).then(() => {
        return coinFuturesApi.cancelOrder({ uid: this.query.uid, symbol: this.query.symbol, orderId: order.orderId })
      }).then(() => {
        this.$notify.success({ title: `订单 ${order.orderId} 已撤销`, duration: 2500 })
        return this.loadOpenOrders(true)
      }).catch(() => {})
    },
    orderEstimatedBase(order, quantity) {
      const price = Number(order.price) > 0 ? order.price : this.position.markPrice
      return calculateCoinQuantity(quantity, this.contract.contractSize, price)
    },
    filledBaseText(order) {
      const actualBase = Number(order.cumBase)
      if (Number.isFinite(actualBase) && actualBase > 0) return this.btcValue(actualBase)
      const estimated = this.orderEstimatedBase(order, order.executedQty)
      return estimated == null ? '--' : `≈ ${this.btcValue(estimated)}`
    },
    decimal(value, digits = 8) {
      const number = Number(value)
      return Number.isFinite(number) ? number.toLocaleString('en-US', { maximumFractionDigits: digits }) : '--'
    },
    signed(value) {
      const number = Number(value)
      if (!Number.isFinite(number)) return '--'
      return `${number > 0 ? '+' : ''}${this.decimal(number)}`
    },
    price(value) {
      const number = Number(value)
      return Number.isFinite(number) && number > 0 ? `${this.decimal(number, 2)} USD` : '--'
    },
    assetValue(value) {
      return `${this.decimal(value)} ${this.marginAsset}`
    },
    btcValue(value) {
      if (value == null || value === '') return '--'
      const number = Number(value)
      if (!Number.isFinite(number) || number < 0) return '--'
      return `${formatQuantity(number)} BTC`
    },
    tone(value) {
      const number = Number(value)
      if (number > 0) return 'profit'
      if (number < 0) return 'loss'
      return ''
    }
  }
}
</script>

<style scoped>
.coin-stats-page { display: flex; flex-direction: column; gap: 12px; box-sizing: border-box; height: calc(100vh - 117px); overflow: hidden; background: #f5f7fa; padding: 12px 16px; }
.panel { background: #fff; border: 1px solid #ebeef5; border-radius: 10px; box-shadow: 0 4px 14px rgba(31, 45, 61, 0.05); }
.toolbar-panel { flex: 0 0 auto; padding: 12px 16px; }
.toolbar-row, .toolbar-main, .toolbar-actions { display: flex; align-items: center; gap: 10px; }
.toolbar-row { justify-content: space-between; }
.toolbar-main { flex-wrap: wrap; }
.account-select { width: 190px; }
.symbol-select { width: 170px; }
.side-select { width: 145px; }
.order-buttons { margin-left: 2px; }
.order-alert { margin-bottom: 18px; }
.estimate-note { margin-left: 6px; color: #909399; font-size: 12px; }
.quantity-cell { display: flex; flex-direction: column; line-height: 20px; }
.quantity-cell small { color: #909399; white-space: nowrap; }
.cancel-order { color: #f56c6c; }
.warning { margin-top: 12px; }
.position-panel { display: flex; flex: 1 1 auto; flex-direction: column; min-height: 0; padding: 12px 18px 6px; overflow: hidden; }
.position-metrics { display: grid; grid-template-columns: repeat(6, 1fr); border-bottom: 1px solid #ebeef5; }
.metric-item { min-width: 0; padding: 2px 16px 12px; border-right: 1px solid #ebeef5; }
.metric-item:last-child { border-right: 0; }
.metric-item span { display: block; margin-bottom: 6px; color: #909399; font-size: 12px; }
.metric-item strong { color: #303133; font-size: 18px; white-space: nowrap; }
.risk-strip { display: flex; flex-wrap: wrap; gap: 28px; padding: 10px 16px; color: #909399; font-size: 12px; }
.risk-strip strong { margin-left: 5px; color: #606266; }
.popover-title { margin-bottom: 10px; color: #303133; font-weight: 600; }
.popover-row { display: flex; justify-content: space-between; padding: 6px 0; color: #606266; }
.popover-row strong { color: #303133; }
.primary { color: #409eff !important; }
.warning-tone { color: #e6a23c !important; }
.danger-tone, .loss { color: #f56c6c !important; }
.profit { color: #13ce66 !important; }
@media (max-width: 1400px) { .position-metrics { grid-template-columns: repeat(3, 1fr); row-gap: 12px; } }
@media (max-width: 1200px), (max-height: 760px) {
  .coin-stats-page { height: auto; min-height: calc(100vh - 117px); overflow: visible; }
  .toolbar-row { align-items: flex-start; flex-direction: column; }
  .toolbar-actions { align-self: flex-end; }
  .position-panel { min-height: 600px; }
}
</style>
