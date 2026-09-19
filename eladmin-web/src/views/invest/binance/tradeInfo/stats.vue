<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div>
        <label class="el-form-item-label">账户</label>
        <el-select
          v-model="query.uid"
          clearable
          filterable
          size="small"
          placeholder="账户"
          class="filter-item"
          style="width: 185px"
          @change="handleAccountChange"
        >
          <el-option
            v-for="item in accountList"
            :key="item.id"
            :label="item.idCardName"
            :value="item.uid"
          />
        </el-select>
        <label class="el-form-item-label">交易对</label>
        <el-select
          v-model="query.symbol"
          size="small"
          placeholder="投资类型"
          class="filter-item"
          style="width: 185px"
          @change="scheduleStats"
        >
          <el-option
            v-for="item in dict.invest_binance_symbol"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <!--        <label class="el-form-item-label">撮合逻辑</label>-->
        <!--        <el-select-->
        <!--          v-model="query.tradePairingLogic"-->
        <!--          size="small"-->
        <!--          placeholder="投资类型"-->
        <!--          class="filter-item"-->
        <!--          style="width: 185px"-->
        <!--          @change="doStats"-->
        <!--        >-->
        <!--          <el-option-->
        <!--            v-for="item in dict.invest_binance_trade_pairing_logic"-->
        <!--            :key="item.value"-->
        <!--            :label="item.label"-->
        <!--            :value="item.value"-->
        <!--          />-->
        <!--        </el-select>-->
        <label class="el-form-item-label">成交截止日期</label>
        <el-date-picker
          v-model="query.endTime"
          align="right"
          type="date"
          placeholder="选择日期"
          class="date-item"
          value-format="yyyy-MM-dd"
          @change="scheduleStats"
        />
        <el-button
          slot="right"
          class="filter-item"
          size="mini"
          type="success"
          icon="el-icon-tickets"
          :loading="statsLoading"
          :disabled="query.uid == null || !query.symbol"
          @click="doStats"
        >查询</el-button>
        <el-button
          class="filter-item"
          size="mini"
          type="success"
          icon="el-icon-refresh"
          :loading="syncLoading"
          :disabled="query.uid == null || !query.symbol"
          @click="syncSpotTradeInfo"
        >同步</el-button>
      </div>
      <div>
        <el-alert
          v-if="realtimeWarningText"
          :title="realtimeWarningText"
          type="warning"
          :closable="false"
          show-icon
          style="margin-top: 16px"
        />
        <el-descriptions
          v-for="description in statsDescriptions"
          :key="description.title"
          :title="description.title"
          :column="3"
          border
          style="margin-bottom: 20px;margin-top: 20px"
        >
          <el-descriptions-item
            v-for="(item, index) in description.descriptionsItems"
            :key="index"
            :label="item.label"
          >
            <div v-if="item.showType === 'link'">
              <el-link type="primary" @click="showOpenTrades = true;tradeList = description.data.tradeList">
                {{ formatValue(description.data, item.key, item.type) }}
              </el-link>
            </div>
            <template v-else-if="item.showType === 'diff'">
              <span>
                {{ formatValue(description.data, item.key, item.type) }}
                <span
                  v-if="description.data.lastNetPnl != null"
                  :style="{
                    color: description.data[item.key] - description.data[item.diffKey] > 0 ? 'green' : description.data[item.key] - description.data[item.diffKey] < 0 ? 'red' : '#999',
                    fontSize: '12px',
                    marginLeft: '4px'
                  }"
                >
                  (
                  {{
                    (description.data[item.key] - description.data[item.diffKey] > 0 ? '+' : '') + (description.data[item.key] - description.data[item.diffKey]).toFixed(2)
                  }}
                  )
                </span>
              </span>
            </template>
            <div v-else> {{ formatValue(description.data, item.key, item.type) }}</div>
          </el-descriptions-item>
        </el-descriptions>
        <div>
          <trade-position-distribution-bar
            :row-data="statsInfo.spotFuturesStatsInfo.tradeList"
            :current-price="statsInfo.spotFuturesStatsInfo.currentSpotPrice"
            height="400px"
            style="margin-top: 20px"
          />
          <!--          <trade-profit-rate-scatter :row-data="statsInfo.matchedTradeInfoList" height="400px" style="margin-top: 20px" />-->
        </div>
      </div>
    </div>

    <!-- 🔹 弹窗 -->
    <el-dialog
      title="未平仓交易详情"
      :visible.sync="showOpenTrades"
      width="80%"
    >
      <el-form inline :model="filterForm" class="mb-2">
        <el-form-item label="方向">
          <el-select v-model="filterForm.side">
            <el-option label="全部" :value="null" />
            <el-option label="做多" :value="true" />
            <el-option label="做空" :value="false" />
          </el-select>
        </el-form-item>

        <el-form-item label="开仓价格">
          <el-input v-model="filterForm.openPrice" clearable />
        </el-form-item>

        <el-form-item label="价格范围">
          <el-input-number v-model="filterForm.priceRange" :step="100" />
        </el-form-item>

      </el-form>

      <el-table :data="filteredTrades" border stripe>
        <el-table-column
          v-for="(col, index) in tableColumns"
          :key="index"
          :prop="col.prop"
          :formatter="col.formatter"
          :label="col.label"
          :min-width="col.width || 100"
        />
      </el-table>
    </el-dialog>

  </div>
</template>

<script>
import crudBinanceTradeInfo from '@/api/binanceTradeInfo'
import { formatDuration } from '@/utils/dateUtil'
import { listAllAccount } from '@/api/binanceAccountInfo'
import TradePositionDistributionBar from '@/views/invest/binance/tradeInfo/TradePositionDistributionBar.vue'
import CRUD from '@crud/crud'
import * as numberUtil from '@/utils/numberUtil'

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
      showOpenTrades: false,
      tradeList: [],
      query: {
        symbol: 'BTCUSDT',
        uid: null
      },
      tableColumns: [
        { prop: 'side', label: '方向', formatter: (row) => (row.side ? '做多' : '做空') },
        { prop: 'qty', label: '成交数量' },
        { prop: 'openPrice', label: '开仓价' },
        { prop: 'closePrice', label: '平仓价' },
        { prop: 'breakEvenPrice', label: '盈亏平衡价' },
        { prop: 'pnl', label: '盈亏' },
        { prop: 'fee', label: '手续费' },
        { prop: 'netPnl', label: '净盈亏' },
        { prop: 'roi', label: '回报率', formatter: (row) => (numberUtil.formatByType(row.roi, 'percent')) }
      ],
      filterForm: {
        side: null,
        openPrice: null,
        priceRange: 500
      }
    }
  },
  computed: {
    realtimeWarningText() {
      const warnings = (this.statsInfo && this.statsInfo.warnings) || []
      const statuses = (this.statsInfo && this.statsInfo.realtimeStatus) || {}
      const labels = {
        usdFuturesPrice: 'U本位价格',
        coinFuturesPrice: '币本位价格',
        coinFundingFee: '币本位资金费',
        accountInfo: '账户资产'
      }
      const cacheTimes = Object.keys(statuses)
        .filter(key => statuses[key] && statuses[key].status === 'CACHE' && statuses[key].updatedAt)
        .map(key => `${labels[key] || key}缓存时间：${new Date(statuses[key].updatedAt).toLocaleString('zh-CN')}`)
      return warnings.concat(cacheTimes).join('；')
    },
    statsDescriptions() {
      return [
        {
          title: '账户统计',
          data: (this.statsInfo && this.statsInfo.accountInfo) || {},
          descriptionsItems: [
            { label: '汇率', key: 'rmbToUsdRate' },
            { label: 'RMB总额', key: 'rmbAmount' },
            { label: 'USD总额', key: 'usdAmount' }
          ]
        },
        {
          title: '现货统计',
          data: (this.statsInfo && this.statsInfo.spotFuturesStatsInfo) || {},
          descriptionsItems: [
            { label: '买入总额', key: 'totalBuyAmount' },
            { label: '卖出总额', key: 'totalSellAmount' },
            { label: '收益率', key: 'roi', type: 'percent' },
            { label: '盈亏', key: 'pnl' },
            { label: '手续费', key: 'fee' },
            { label: '净盈亏', key: 'netPnl', showType: 'diff', diffKey: 'lastNetPnl' },
            { label: '持仓均价', key: 'posAvgPrice' },
            { label: '持仓数量', key: 'posQty' },
            { label: '持仓总额', key: 'posAmount' },
            { label: '持仓盈利', key: 'holdingProfit' },
            { label: '持仓亏损', key: 'holdingLoss' },
            { label: '持仓盈亏', key: 'holdingProfitLoss' },
            { label: '', key: '' },
            { label: '', key: '' },
            { label: '持仓订单', key: 'tradeList', type: 'length', showType: 'link' }
          ]
        },
        {
          title: 'U本位-合约统计',
          data: (this.statsInfo && this.statsInfo.usdFuturesStatsInfo) || {},
          descriptionsItems: [
            { label: '持仓均价', key: 'posAvgPrice' },
            { label: '持仓数量', key: 'posQty' },
            { label: '持仓金额', key: 'posAmount' },
            { label: '盈亏', key: 'pnl' },
            { label: '手续费', key: 'fee' },
            { label: '净盈亏', key: 'netPnl' },
            { label: '', key: '' },
            { label: '', key: '' },
            { label: '对冲止损', key: 'stopLossAmount' },
            { label: '', key: '' },
            { label: '', key: '' },
            { label: '持仓订单', key: 'tradeList', type: 'length', showType: 'link' }
          ]
        },
        {
          title: '币本位-合约统计',
          data: (this.statsInfo && this.statsInfo.coinFuturesStatsInfo) || {},
          descriptionsItems: [
            { label: '持仓均价', key: 'posAvgPrice' },
            { label: '持仓数量', key: 'posQty' },
            { label: '持仓金额', key: 'posAmount' },
            { label: '盈亏', key: 'pnl' },
            { label: '手续费', key: 'fee' },
            { label: '净盈亏', key: 'netPnl' },
            { label: '', key: '' },
            { label: '资金费', key: 'fundingFee' },
            { label: '对冲止损', key: 'stopLossAmount' },
            { label: '', key: '' },
            { label: '', key: '' },
            { label: '持仓订单', key: 'tradeList', type: 'length', showType: 'link' }
          ]
        },
        {
          title: '对冲统计',
          data: (this.statsInfo && this.statsInfo.spotHedgedFuturesStatsInfo) || {},
          descriptionsItems: [
            { label: '锁仓均价', key: 'posAvgPrice' },
            { label: '锁仓数量', key: 'posQty' },
            { label: '锁仓总额', key: 'posAmount' }
          ]
        }
      ]
    },
    filteredTrades() {
      return this.tradeList.filter(item => {
        // 按方向筛选
        if (this.filterForm.side != null && item.side !== this.filterForm.side) {
          return false
        }

        const base = Number(this.filterForm.openPrice)
        const range = Number(this.filterForm.priceRange) || 500 // 默认 500

        // 只有输入了 openPrice 时才筛选
        if (!isNaN(base) && this.filterForm.openPrice) {
          let min, max

          if (item.side === true) {
            // 做多 → openPrice + range
            min = base
            max = base + range
          } else {
            // 做空 → openPrice - range
            min = base - range
            max = base
          }

          if (!(item.openPrice >= min && item.openPrice <= max)) {
            return false
          }
        }

        return true
      })
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
    formatDuration,
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
    // 显示汇总，手动查询不等待防抖
    doStats() {
      this.clearStatsDebounce()
      this.executeStats(++this.statsRequestId)
    },
    executeStats(requestId) {
      if (this.query.uid == null || !this.query.symbol) {
        if (requestId === this.statsRequestId) {
          this.statsLoading = false
        }
        return
      }
      const params = this.buildStatsParams()
      this.statsLoading = true
      crudBinanceTradeInfo.stats(params).then(res => {
        if (requestId === this.statsRequestId) {
          this.statsInfo = res
        }
      }).catch(() => {
        // 请求错误由全局拦截器提示；过期请求不改变当前页面状态
      }).then(() => {
        if (requestId === this.statsRequestId) {
          this.statsLoading = false
        }
      })
    },
    buildStatsParams() {
      const params = { ...this.query }
      if (params.endTime) {
        params.endTime = `${params.endTime} 23:59:59`
      } else {
        delete params.endTime
      }
      return params
    },
    formatValue(info, key, type) {
      if (!info || info[key] === null || info[key] === undefined) {
        return '--'
      }
      return numberUtil.formatByType(info[key], type)
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
      crudBinanceTradeInfo.syncSelected({
        uid: this.query.uid,
        symbol: this.query.symbol
      }).then((res) => {
        this.doStats()
        this.$notify({
          title: `同步成功：现货 ${res.spotCount || 0} 条，U 本位 ${res.usdFuturesCount || 0} 条，币本位 ${res.coinFuturesCount || 0} 条`,
          type: CRUD.NOTIFICATION_TYPE.SUCCESS,
          duration: 2500
        })
        this.syncLoading = false
      }).catch(() => {
        this.syncLoading = false
      })
    }
  }
}
</script>

<style scoped>

</style>
