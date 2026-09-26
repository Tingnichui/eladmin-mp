<template>
  <div class="position-chart">
    <div class="chart-toolbar">
      <div class="interval-control">
        <span class="control-label">价格间隔</span>
        <div class="interval-stepper">
          <el-button
            size="mini"
            class="interval-button interval-button-left"
            icon="el-icon-arrow-left"
            :disabled="!canDecreaseInterval"
            aria-label="减小价格间隔"
            @click="shiftPriceInterval(-1)"
          />
          <span class="interval-value">{{ priceInterval }} USDT</span>
          <el-button
            size="mini"
            class="interval-button interval-button-right"
            icon="el-icon-arrow-right"
            :disabled="!canIncreaseInterval"
            aria-label="增大价格间隔"
            @click="shiftPriceInterval(1)"
          />
        </div>
      </div>
      <el-radio-group v-model="profitFilter" size="small" class="profit-filter" @change="refreshChartLayout">
        <el-radio-button label="all">全部成交 {{ tradeCounts.all }}</el-radio-button>
        <el-radio-button label="profit">盈利成交 {{ tradeCounts.profit }}</el-radio-button>
        <el-radio-button label="loss">亏损成交 {{ tradeCounts.loss }}</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="coreFilter" size="small" class="core-filter" @change="refreshChartLayout">
        <el-radio-button label="all">底仓不限</el-radio-button>
        <el-radio-button label="core">含底仓 {{ coreTradeCounts.core }}</el-radio-button>
        <el-radio-button label="nonCore">不含底仓 {{ coreTradeCounts.nonCore }}</el-radio-button>
      </el-radio-group>
    </div>
    <div ref="chartContainer" :class="className" :style="{ height: chartHeight, width: width }" />
  </div>
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons')
import { debounce } from '@/utils'
import { addAmount, divAmount, formatPercent, formatQuantity, mulAmount, subAmount } from '@/utils/numberUtil'

const PRICE_INTERVALS = [500, 1000, 2500, 5000]

export default {
  name: 'TradePositionDistributionBar',
  props: {
    className: { type: String, default: 'chart' },
    width: { type: String, default: '100%' },
    height: { type: String, default: '420px' },
    rowData: { type: Array, default: () => [] },
    currentPrice: { type: [Number, String], default: null },
    averagePrice: { type: [Number, String], default: null }
  },
  data() {
    return {
      chart: null,
      isMobileViewport: false,
      profitFilter: 'all',
      coreFilter: 'all',
      priceInterval: 2500
    }
  },
  computed: {
    normalizedTrades() {
      return (this.rowData || []).map(trade => this.normalizeTrade(trade))
    },
    tradeCounts() {
      return this.normalizedTrades.reduce((result, trade) => {
        result.all++
        result[trade.profitable ? 'profit' : 'loss']++
        return result
      }, { all: 0, profit: 0, loss: 0 })
    },
    coreTradeCounts() {
      return this.normalizedTrades.reduce((result, trade) => {
        result[trade.coreQty > 0 ? 'core' : 'nonCore']++
        return result
      }, { core: 0, nonCore: 0 })
    },
    filteredTrades() {
      return this.normalizedTrades.filter(trade => {
        const profitMatched = this.profitFilter === 'all' || trade.profitable === (this.profitFilter === 'profit')
        const coreMatched = this.coreFilter === 'all' || (trade.coreQty > 0) === (this.coreFilter === 'core')
        return profitMatched && coreMatched
      })
    },
    canDecreaseInterval() {
      return PRICE_INTERVALS.indexOf(this.priceInterval) > 0
    },
    canIncreaseInterval() {
      const index = PRICE_INTERVALS.indexOf(this.priceInterval)
      return index >= 0 && index < PRICE_INTERVALS.length - 1
    },
    chartHeight() {
      if (!this.isMobileViewport || this.height !== '100%') return this.height
      const bucketCount = this.groupTradesByPrice(this.filteredTrades).length
      return `${Math.max(360, bucketCount * 38 + 76)}px`
    }
  },
  watch: {
    rowData: { deep: true, handler() { this.refreshChartLayout() } },
    currentPrice() { this.updateChart() },
    averagePrice() { this.updateChart() }
  },
  mounted() {
    this.updateViewportMode()
    this.initChart()
    this.__resizeHandler = debounce(() => {
      this.updateViewportMode()
      this.refreshChartLayout()
    }, 100)
    window.addEventListener('resize', this.__resizeHandler)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.__resizeHandler)
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    shiftPriceInterval(direction) {
      const currentIndex = PRICE_INTERVALS.indexOf(this.priceInterval)
      if (currentIndex < 0) return
      const nextIndex = currentIndex + direction
      if (nextIndex < 0 || nextIndex >= PRICE_INTERVALS.length) return
      this.priceInterval = PRICE_INTERVALS[nextIndex]
      this.refreshChartLayout()
    },
    updateViewportMode() {
      this.isMobileViewport = window.innerWidth <= 700
    },
    refreshChartLayout() {
      this.$nextTick(() => {
        if (this.chart) this.chart.resize()
        this.updateChart()
      })
    },
    initChart() {
      this.chart = echarts.init(this.$refs.chartContainer, 'macarons')
      this.chart.on('click', this.handleChartClick)
      this.updateChart()
    },
    handleChartClick(params) {
      if (!params.data || !params.data.trades) return
      this.hideTooltip()
      this.$emit('select-bucket', params.data)
    },
    hideTooltip() {
      if (this.chart) this.chart.dispatchAction({ type: 'hideTip' })
    },
    normalizeTrade(trade) {
      const openPrice = Number(trade.openPrice) || 0
      const qty = Number(trade.qty) || 0
      const currentPrice = Number(this.currentPrice) || 0
      const feeRate = Number(trade.feeRate) || 0
      const breakEvenPrice = Number(trade.breakEvenPrice) || openPrice * (1 + feeRate)
      const side = trade.side !== false
      const priceDiff = side ? currentPrice - breakEvenPrice : breakEvenPrice - currentPrice
      const calculatedProfit = mulAmount(priceDiff, qty, 8)
      const profit = trade.netPnl == null ? calculatedProfit : Number(trade.netPnl)
      const profitable = currentPrice > 0 && profit >= 0
      const coreQty = Number(trade.coreQty) || 0
      const availableQty = trade.availableQty == null ? Math.max(qty - coreQty, 0) : Number(trade.availableQty) || 0
      return {
        raw: trade,
        openPrice,
        qty,
        openAmount: Number(trade.openAmount) || mulAmount(openPrice, qty),
        profit,
        profitable,
        coreQty,
        availableQty
      }
    },
    updateChart() {
      if (!this.chart) return
      if (!this.rowData || !this.rowData.length) {
        this.renderEmptyChart()
      } else {
        this.renderBucketChart()
      }
    },
    renderEmptyChart() {
      this.chart.clear()
      this.chart.setOption({
        title: {
          text: '暂无持仓数据',
          left: 'center',
          top: 'middle',
          textStyle: { color: '#909399', fontSize: 14, fontWeight: 'normal' }
        }
      })
    },
    renderBucketChart() {
      const compact = this.isMobileViewport
      const buckets = this.groupTradesByPrice(this.filteredTrades)
      const currentPrice = Number(this.currentPrice) || 0
      const averagePrice = Number(this.averagePrice) || this.weightedAverage(this.normalizedTrades)
      const currentBucketKey = this.bucketKey(currentPrice)
      const averageBucketKey = this.bucketKey(averagePrice)
      const makeBucketData = type => buckets.map(item => {
        const isAverageBucket = item.range === averageBucketKey
        const matches = type === 'average' ? isAverageBucket : !isAverageBucket && item.profitable === (type === 'profit')
        if (!matches) return '-'
        const color = type === 'average' ? '#e6a23c' : type === 'profit' ? '#13ce8a' : '#f56c6c'
        return {
          ...item,
          value: item.totalQty,
          itemStyle: {
            color,
            borderRadius: [0, 4, 4, 0]
          }
        }
      })
      const makeBucketSeries = (name, type, color) => ({
        name,
        type: 'bar',
        barMaxWidth: compact ? 18 : 24,
        barGap: '-100%',
        data: makeBucketData(type),
        itemStyle: { color },
        label: {
          show: true,
          position: 'right',
          color: '#606266',
          fontSize: compact ? 11 : 12,
          formatter: params => {
            if (compact) return `${params.data.count}笔`
            const coreText = params.data.coreCount > 0 ? `  🔒${params.data.coreCount}笔` : ''
            return `${this.quantityNumber(params.data.totalQty)} BTC · ${params.data.count}笔${coreText}  均价 ${this.formatNumber(params.data.avgPrice, 2)}`
          }
        }
      })
      this.chart.clear()
      this.chart.setOption({
        animationDuration: 300,
        legend: { show: !compact, bottom: 0, data: ['盈利区间', '亏损区间', '持仓均价区间'] },
        toolbox: {
          show: !compact,
          right: 10,
          top: 0,
          feature: {
            restore: { title: '刷新图表' },
            saveAsImage: { title: '保存图片', pixelRatio: 2 }
          }
        },
        grid: compact
          ? { left: 72, right: 44, top: 18, bottom: 26 }
          : { left: 128, right: 170, top: 38, bottom: 58 },
        tooltip: { trigger: 'item', formatter: params => this.bucketTooltip(params.data) },
        xAxis: {
          name: compact ? '' : '持仓数量 (BTC)',
          type: 'value',
          axisLabel: { show: !compact },
          splitLine: { lineStyle: { color: '#eef1f6', type: 'dashed' }},
          axisLine: { lineStyle: { color: '#dcdfe6' }}
        },
        yAxis: {
          name: compact ? '' : '买入价格区间',
          type: 'category',
          data: buckets.map(item => item.range),
          axisLine: { lineStyle: { color: '#dcdfe6' }},
          axisLabel: {
            color: value => value === currentBucketKey ? '#409eff' : '#606266',
            fontWeight: value => value === currentBucketKey ? 'bold' : 'normal',
            fontSize: compact ? 11 : 12,
            formatter: value => compact ? this.compactBucketRange(value) : value
          }
        },
        series: [
          makeBucketSeries('盈利区间', 'profit', '#13ce8a'),
          makeBucketSeries('亏损区间', 'loss', '#f56c6c'),
          makeBucketSeries('持仓均价区间', 'average', '#e6a23c')
        ]
      }, true)
    },
    groupTradesByPrice(trades) {
      const buckets = {}
      trades.forEach(trade => {
        const key = this.bucketKey(trade.openPrice)
        if (!buckets[key]) {
          buckets[key] = {
            range: key,
            totalQty: 0,
            totalAmount: 0,
            profit: 0,
            count: 0,
            coreCount: 0,
            coreQty: 0,
            availableQty: 0,
            lower: 0,
            trades: []
          }
        }
        const bucket = buckets[key]
        bucket.totalQty = addAmount(bucket.totalQty, trade.qty)
        bucket.totalAmount = addAmount(bucket.totalAmount, trade.openAmount)
        bucket.profit = addAmount(bucket.profit, trade.profit)
        bucket.coreQty = addAmount(bucket.coreQty, trade.coreQty)
        bucket.availableQty = addAmount(bucket.availableQty, trade.availableQty)
        if (trade.coreQty > 0) bucket.coreCount++
        bucket.count++
        bucket.lower = Number(key.split('-')[0])
        bucket.trades.push(trade)
      })
      return Object.values(buckets).map(bucket => {
        const avgPrice = divAmount(bucket.totalAmount, bucket.totalQty, 2)
        return {
          ...bucket,
          avgPrice,
          profitRate: avgPrice > 0 ? divAmount(subAmount(Number(this.currentPrice) || 0, avgPrice), avgPrice) : 0,
          profitable: bucket.profit >= 0
        }
      }).sort((a, b) => a.lower - b.lower)
    },
    bucketKey(price) {
      const value = Number(price)
      if (!value || !this.priceInterval) return ''
      const lower = Math.floor(value / this.priceInterval) * this.priceInterval
      return `${lower}-${lower + this.priceInterval}`
    },
    compactBucketRange(value) {
      const parts = String(value).split('-')
      const prices = parts.map(part => Number(part))
      if (prices.length === 2 && prices.every(price => Number.isFinite(price) && price >= 1000)) {
        return `${Number((prices[0] / 1000).toFixed(1))}–${Number((prices[1] / 1000).toFixed(1))}k`
      }
      return parts.map(part => {
        const price = Number(part)
        return Number.isFinite(price) && price >= 1000 ? `${Number((price / 1000).toFixed(1))}k` : part
      }).join('-')
    },
    weightedAverage(trades) {
      const totalQty = trades.reduce((sum, trade) => addAmount(sum, trade.qty), 0)
      const totalAmount = trades.reduce((sum, trade) => addAmount(sum, trade.openAmount), 0)
      return totalQty > 0 ? divAmount(totalAmount, totalQty, 2) : 0
    },
    bucketTooltip(bucket) {
      return [
        `价格区间：${bucket.range}`,
        `成交笔数：${bucket.count}笔`,
        `持仓数量：${this.quantityNumber(bucket.totalQty)} BTC`,
        `底仓成交：${bucket.coreCount}笔`,
        `底仓数量：${this.quantityNumber(bucket.coreQty)} BTC`,
        `可撮合数量：${this.quantityNumber(bucket.availableQty)} BTC`,
        `加权均价：${this.formatNumber(bucket.avgPrice, 2)}`,
        `当前盈亏：${this.signedNumber(bucket.profit, 2)}`,
        `收益率：${this.signedPercent(bucket.profitRate)}`
      ].join('<br/>')
    },
    formatNumber(value, digits) {
      const number = Number(value)
      if (!Number.isFinite(number)) return '--'
      return number.toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
    },
    quantityNumber(value) {
      return formatQuantity(value)
    },
    signedNumber(value, digits) {
      const number = Number(value) || 0
      return `${number > 0 ? '+' : ''}${this.formatNumber(number, digits)}`
    },
    signedPercent(value) {
      const number = Number(value) || 0
      return `${number > 0 ? '+' : ''}${formatPercent(number)}`
    }
  }
}
</script>

<style scoped>
.position-chart { display: flex; flex-direction: column; width: 100%; height: 100%; }
.position-chart > .chart { flex: 1 1 auto; min-height: 250px; }
.chart-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; padding: 14px 0 6px; border-top: 1px solid #ebeef5; }
.interval-control { display: flex; align-items: center; gap: 8px; }
.control-label { color: #606266; white-space: nowrap; }
.interval-stepper { display: flex; align-items: stretch; }
.interval-stepper .el-button { margin: 0; border-radius: 0; }
.interval-stepper .interval-button-left { border-radius: 4px 0 0 4px; }
.interval-stepper .interval-button-right { border-radius: 0 4px 4px 0; }
.interval-value { display: flex; align-items: center; justify-content: center; min-width: 90px; margin: 0 -1px; padding: 0 8px; border: 1px solid #dcdfe6; color: #303133; font-size: 13px; white-space: nowrap; background: #fff; }
.profit-filter { margin-left: 4px; }
.core-filter { white-space: nowrap; }
@media (max-width: 700px) {
  .chart-toolbar { gap: 8px; padding-top: 12px; }
  .profit-filter, .core-filter { display: flex; width: 100%; margin-left: 0; }
  .interval-control { width: 100%; }
  .interval-stepper { flex: 1 1 auto; }
  .interval-value { flex: 1 1 auto; min-width: 0; }
  ::v-deep .profit-filter .el-radio-button,
  ::v-deep .core-filter .el-radio-button { flex: 1 1 0; min-width: 0; }
  ::v-deep .profit-filter .el-radio-button__inner,
  ::v-deep .core-filter .el-radio-button__inner { width: 100%; padding-right: 4px; padding-left: 4px; }
}
</style>
