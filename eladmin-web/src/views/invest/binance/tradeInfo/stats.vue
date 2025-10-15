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
          @change="doStats"
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
          @change="doStats"
        >
          <el-option
            v-for="item in dict.invest_binance_symbol"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">撮合逻辑</label>
        <el-select
          v-model="query.tradePairingLogic"
          size="small"
          placeholder="投资类型"
          class="filter-item"
          style="width: 185px"
          @change="doStats"
        >
          <el-option
            v-for="item in dict.invest_binance_trade_pairing_logic"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">截止日期</label>
        <el-date-picker
          v-model="query.endTime"
          align="right"
          type="date"
          placeholder="选择日期"
          class="date-item"
          @change="doStats"
        />
        <el-button
          slot="right"
          class="filter-item"
          size="mini"
          type="success"
          icon="el-icon-tickets"
          @click="doStats"
        >查询</el-button>
        <el-button
          class="filter-item"
          size="mini"
          type="success"
          icon="el-icon-refresh"
          :loading="syncLoading"
          @click="syncSpotTradeInfo"
        >同步</el-button>
        <el-button
          class="filter-item"
          size="mini"
          type="success"
          icon="el-icon-refresh"
          :loading="syncLoading"
          @click="syncFuturesHedge"
        >锁仓</el-button>
      </div>
      <div>
        <el-descriptions :column="3" border class="stats-descriptions">
          <el-descriptions-item
            v-for="(item, index) in statItems"
            :key="index"
            :label="item.label"
          >
            {{ formatValue(item.key, item.type) }}
          </el-descriptions-item>
        </el-descriptions>
        <div>
          <trade-position-distribution-bar :row-data="statsInfo.waitSellTradeInfoList" :symbol="query.symbol" height="400px" style="margin-top: 20px" />
          <trade-profit-rate-scatter :row-data="statsInfo.matchedTradeInfoList" height="400px" style="margin-top: 20px" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import crudBinanceTradeInfo from '@/api/binanceTradeInfo'
import { formatDuration } from '@/utils/dateUtil'
import TradeProfitRateScatter from '@/views/invest/binance/tradeInfo/TradeProfitRateScatter.vue'
import { listAllAccount } from '@/api/binanceAccountInfo'
import TradePositionDistributionBar from '@/views/invest/binance/tradeInfo/TradePositionDistributionBar.vue'
import CRUD from '@crud/crud'

export default {
  name: 'BinanceTradeInfoStats',
  components: { TradePositionDistributionBar, TradeProfitRateScatter },
  dicts: ['invest_binance_trade_pairing_logic', 'invest_binance_symbol'],
  data() {
    return {
      statsInfo: {},
      accountList: [],
      syncLoading: false,
      query: {
        symbol: 'BTCUSDT',
        tradePairingLogic: 'FIFO'
      },
      statItems: [
        { label: '买入均价', key: 'avgBuyPrice' },
        { label: '卖出均价', key: 'avgSellPrice' },
        { label: '收益率', key: 'profitPct', type: 'percent' },
        { label: '买入总金额', key: 'totalBuyAmount' },
        { label: '卖出总金额', key: 'totalSellAmount' },
        { label: '利润', key: 'profit', type: 'profit' },
        { label: '未平仓均价', key: 'totalWaitAvgSellPrice' },
        { label: '未平仓数量', key: 'totalWaitSellQty' },
        { label: '未平仓总额', key: 'totalWaitSellAmount' },
        { label: '锁仓均价', key: 'hedgedAvgPrice' },
        { label: '锁仓数量', key: 'hedgedQty' },
        { label: '锁仓总额', key: 'hedgedAmount' },
        { label: '最短持仓', key: 'minHoldTimeMs', type: 'duration' },
        { label: '最长持仓', key: 'maxHoldTimeMs', type: 'duration' },
        { label: '平均持仓', key: 'avgHoldTimeMs', type: 'duration' }
      ]
    }
  },
  mounted() {
    this.doStats()
    this.refreshAccountList()
  },
  methods: {
    formatDuration,
    // 显示汇总
    doStats() {
      crudBinanceTradeInfo.stats(this.query).then(res => {
        this.statsInfo = res
      })
    },
    formatValue(key, type) {
      const value = this.statsInfo[key]
      if (type === 'percent') return this.formatPercent(value)
      if (type === 'duration') return this.formatDuration(value)
      return this.formatDecimal(value)
    },
    formatDecimal(val) {
      return val != null ? Number(val).toFixed(4) : '--'
    },
    formatPercent(val) {
      return val != null ? (val * 100).toFixed(2) + '%' : '--'
    },
    refreshAccountList() {
      listAllAccount().then(data => {
        this.accountList = data.content
      })
    },
    syncSpotTradeInfo() {
      this.syncLoading = true
      crudBinanceTradeInfo.syncSpotTradeInfo().then(() => {
        this.doStats()
        this.$notify({
          title: '同步成功',
          type: CRUD.NOTIFICATION_TYPE.SUCCESS,
          duration: 2500
        })
        this.syncLoading = false
      }).catch(() => {
        this.syncLoading = false
      })
    },
    syncFuturesHedge() {
      this.syncLoading = true
      crudBinanceTradeInfo.syncFuturesHedge().then(() => {
        this.doStats()
        this.$notify({
          title: '同步成功',
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
