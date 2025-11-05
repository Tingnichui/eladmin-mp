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
          :loading="syncLoading"
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
      </div>
      <div>
        <el-descriptions :column="3" border class="stats-descriptions">
          <el-descriptions-item
            v-for="(item, index) in spotStatsItems"
            :key="index"
            :label="item.label"
          >
            <div v-if="item.showType === 'link'">
              <el-link type="primary" @click="showOpenTrades = true;openTradeList = statsInfo.openTradeList">
                {{ formatValue(statsInfo, item.key, item.type) }}
              </el-link>
            </div>
            <div v-else> {{ formatValue(statsInfo, item.key, item.type) }}</div>
          </el-descriptions-item>
          <!-- 分割线 -->
          <el-descriptions-item :span="3" label-class-name="no-border" content-class-name="no-border">
            <div class="divider" />
          </el-descriptions-item>
          <el-descriptions-item
            v-for="(item, index) in futuresStatsItems"
            :key="index"
            :label="item.label"
          >
            <div v-if="item.showType === 'link'">
              <el-link type="primary" @click="showOpenTrades = true;openTradeList = statsInfo.futuresTradeStatsInfo.openTradeList">
                {{ formatValue(statsInfo.futuresTradeStatsInfo, item.key, item.type) }}
              </el-link>
            </div>
            <div v-else> {{ formatValue(statsInfo.futuresTradeStatsInfo, item.key, item.type) }}</div>
          </el-descriptions-item>
        </el-descriptions>
        <div>
          <trade-position-distribution-bar :row-data="statsInfo.waitSellTradeInfoList" :symbol="query.symbol" height="400px" style="margin-top: 20px" />
          <!--          <trade-profit-rate-scatter :row-data="statsInfo.matchedTradeInfoList" height="400px" style="margin-top: 20px" />-->
        </div>
      </div>
    </div>

    <!-- 🔹 弹窗 -->
    <el-dialog
      title="未平仓交易详情"
      :visible.sync="showOpenTrades"
      width="80%"
      :close-on-click-modal="false"
    >
      <el-table :data="openTradeList" border stripe>
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

export default {
  name: 'BinanceTradeInfoStats',
  components: { TradePositionDistributionBar },
  dicts: ['invest_binance_trade_pairing_logic', 'invest_binance_symbol'],
  data() {
    return {
      statsInfo: {},
      accountList: [],
      syncLoading: false,
      showOpenTrades: false,
      openTradeList: [],
      query: {
        symbol: 'BTCUSDT'
      },
      spotStatsItems: [
        { label: '买入总额', key: 'totalBuyAmount' },
        { label: '卖出总额', key: 'totalSellAmount' },
        { label: '收益率', key: 'profitPct', type: 'percent' },
        { label: '盈亏', key: 'pnl' },
        { label: '手续费', key: 'fee' },
        { label: '净盈亏', key: 'netPnl' },
        { label: '持仓均价', key: 'totalWaitAvgSellPrice' },
        { label: '持仓数量', key: 'totalWaitSellQty' },
        { label: '持仓总额', key: 'totalWaitSellAmount' },
        { label: '持仓盈利', key: 'holdingProfit' },
        { label: '持仓亏损', key: 'holdingLoss' },
        { label: '持仓盈亏', key: 'holdingProfitLoss' },
        { label: '', key: '' },
        { label: '', key: '' },
        { label: '持仓订单', key: 'openTradeList', type: 'length', showType: 'link' }
      ],
      futuresStatsItems: [
        { label: '持仓均价', key: 'posAvgPrice' },
        { label: '持仓数量', key: 'posQty' },
        { label: '持仓金额', key: 'posAmount' },
        { label: '锁仓均价', key: 'hedgedAvgPrice' },
        { label: '锁仓数量', key: 'hedgedQty' },
        { label: '锁仓总额', key: 'hedgedAmount' },
        { label: '盈亏', key: 'pnl' },
        { label: '手续费', key: 'fee' },
        { label: '净盈亏', key: 'netPnl' },
        { label: '', key: '' },
        { label: '', key: '' },
        { label: '对冲止损', key: 'stopLossAmount' },
        { label: '', key: '' },
        { label: '', key: '' },
        { label: '持仓订单', key: 'openTradeList', type: 'length', showType: 'link' }
      ],
      tableColumns: [
        { prop: 'side', label: '方向', formatter: (row) => (row.side ? '做多' : '做空') },
        { prop: 'qty', label: '成交数量' },
        { prop: 'openPrice', label: '开仓价' },
        { prop: 'closePrice', label: '平仓价' },
        { prop: 'breakEvenPrice', label: '盈亏平衡价' },
        { prop: 'pnl', label: '盈亏' },
        { prop: 'fee', label: '手续费' },
        { prop: 'netPnl', label: '净盈亏' },
        { prop: 'roi', label: '回报率', formatter: (row) => (this.formatNum(row.roi, 'percent')) }
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
      if (this.query.endTime) {
        const date = new Date(this.query.endTime)
        this.query.endTime = date.toLocaleString('zh-CN', {
          timeZone: 'Asia/Shanghai',
          hour12: false
        }).replace(/\//g, '-')
      } else {
        delete this.query.endTime // 避免传空字符串
      }
      this.syncLoading = true
      crudBinanceTradeInfo.stats(this.query).then(res => {
        this.statsInfo = res
        this.syncLoading = false
      }).catch(() => {
        this.syncLoading = false
      })
    },
    formatValue(info, key, type) {
      return this.formatNum(info[key], type)
    },
    formatNum(value, type) {
      if (type === 'percent') return this.formatPercent(value)
      if (type === 'duration') return this.formatDuration(value)
      if (type === 'length') return value.length
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
    }
  }
}
</script>

<style scoped>

</style>
