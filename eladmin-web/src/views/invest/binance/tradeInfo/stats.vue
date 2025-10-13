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
        <label class="el-form-item-label">成交时间</label>
        <date-range-picker v-model="query.time" class="date-item" @change="doStats" />
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
          <el-descriptions-item label="买入均价">
            {{ formatDecimal(statsInfo.avgBuyPrice) }}
          </el-descriptions-item>
          <el-descriptions-item label="卖出均价">
            {{ formatDecimal(statsInfo.avgSellPrice) }}
          </el-descriptions-item>
          <el-descriptions-item label="收益率">
            {{ formatPercent(statsInfo.profitPct) }}
          </el-descriptions-item>
          <el-descriptions-item label="买入总金额">
            {{ formatDecimal(statsInfo.totalBuyAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="卖出总金额">
            {{ formatDecimal(statsInfo.totalSellAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="利润">
            {{ formatDecimal(statsInfo.profit) }}
          </el-descriptions-item>
          <el-descriptions-item label="未平仓均价">
            {{ formatDecimal(statsInfo.totalWaitAvgSellPrice) }}
          </el-descriptions-item>
          <el-descriptions-item label="未平仓数量">
            {{ formatDecimal(statsInfo.totalWaitSellQty) }}
          </el-descriptions-item>
          <el-descriptions-item label="未平仓总额">
            {{ formatDecimal(statsInfo.totalWaitSellAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="锁仓均价">
            {{ formatDecimal(statsInfo.hedgedAvgPrice) }}
          </el-descriptions-item>
          <el-descriptions-item label="锁仓数量">
            {{ formatDecimal(statsInfo.hedgedQty) }}
          </el-descriptions-item>
          <el-descriptions-item label="锁仓总额">
            {{ formatDecimal(statsInfo.hedgedAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="最短持仓">
            {{ formatDuration(statsInfo.minHoldTimeMs) }}
          </el-descriptions-item>
          <el-descriptions-item label="最长持仓">
            {{ formatDuration(statsInfo.maxHoldTimeMs) }}
          </el-descriptions-item>
          <el-descriptions-item label="平均持仓">
            {{ formatDuration(statsInfo.avgHoldTimeMs) }}
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
import DateRangePicker from '@/components/DateRangePicker/index.vue'
import { formatDuration } from '@/utils/dateUtil'
import TradeProfitRateScatter from '@/views/invest/binance/tradeInfo/TradeProfitRateScatter.vue'
import { listAllAccount } from '@/api/binanceAccountInfo'
import TradePositionDistributionBar from '@/views/invest/binance/tradeInfo/TradePositionDistributionBar.vue'
import CRUD from '@crud/crud'

export default {
  name: 'BinanceTradeInfoStats',
  components: { TradePositionDistributionBar, TradeProfitRateScatter, DateRangePicker },
  dicts: ['invest_binance_trade_pairing_logic', 'invest_binance_symbol'],
  data() {
    return {
      statsInfo: {},
      accountList: [],
      syncLoading: false,
      query: {
        symbol: 'BTCUSDT',
        tradePairingLogic: 'FIFO'
      }
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
