<template>
  <div class="position-chart">
    <div class="chart-toolbar">
      <el-radio-group v-model="viewMode" size="small" @change="updateChart">
        <el-radio-button label="trades">逐笔买入</el-radio-button>
        <el-radio-button label="buckets">价格区间聚合</el-radio-button>
      </el-radio-group>
      <div v-if="viewMode === 'buckets'" class="interval-control">
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
      <el-radio-group v-model="profitFilter" size="small" class="profit-filter" @change="updateChart">
        <el-radio-button label="all">全部成交 {{ tradeCounts.all }}</el-radio-button>
        <el-radio-button label="profit">盈利成交 {{ tradeCounts.profit }}</el-radio-button>
        <el-radio-button label="loss">亏损成交 {{ tradeCounts.loss }}</el-radio-button>
      </el-radio-group>
      <span v-if="coreTradeCount" class="core-legend">
        <i class="el-icon-lock" />底仓成交 {{ coreTradeCount }} 笔
      </span>
    </div>
    <div ref="chartContainer" :class="className" :style="{ height: height, width: width }" />
  </div>
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons')
import { debounce } from '@/utils'
import { addAmount, divAmount, formatPercent, mulAmount, subAmount } from '@/utils/numberUtil'

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
      viewMode: 'buckets',
      profitFilter: 'all',
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
    coreTradeCount() {
      return this.normalizedTrades.filter(trade => trade.coreQty > 0).length
    },
    filteredTrades() {
      if (this.profitFilter === 'all') return this.normalizedTrades
      const profitable = this.profitFilter === 'profit'
      return this.normalizedTrades.filter(trade => trade.profitable === profitable)
    },
    canDecreaseInterval() {
      return PRICE_INTERVALS.indexOf(this.priceInterval) > 0
    },
    canIncreaseInterval() {
      const index = PRICE_INTERVALS.indexOf(this.priceInterval)
      return index >= 0 && index < PRICE_INTERVALS.length - 1
    }
  },
  watch: {
    rowData: { deep: true, handler() { this.updateChart() } },
    currentPrice() { this.updateChart() },
    averagePrice() { this.updateChart() }
  },
  mounted() {
    this.initChart()
    this.__resizeHandler = debounce(() => {
      if (this.chart) this.chart.resize()
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
      this.updateChart()
    },
    initChart() {
      this.chart = echarts.init(this.$refs.chartContainer, 'macarons')
      this.chart.on('click', params => {
        if (params.data && params.data.trade) {
          this.$emit('select-trade', params.data.trade.raw)
        } else if (params.data && params.data.trades) {
          this.$emit('select-bucket', params.data)
        }
      })
      this.updateChart()
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
      const calculatedRoi = openPrice > 0 ? divAmount(profit, mulAmount(openPrice, qty)) : 0
      const roi = trade.roi == null ? calculatedRoi : Number(trade.roi)
      const openTimeValue = trade.openTime || trade.tradeTime
      const parsedOpenTime = openTimeValue ? new Date(openTimeValue).getTime() : NaN
      const openTime = Number.isFinite(parsedOpenTime) ? parsedOpenTime : null
      const profitable = currentPrice > 0 && profit >= 0
      const coreQty = Number(trade.coreQty) || 0
      const availableQty = trade.availableQty == null ? Math.max(qty - coreQty, 0) : Number(trade.availableQty) || 0
      return {
        raw: trade,
        openPrice,
        qty,
        openAmount: Number(trade.openAmount) || mulAmount(openPrice, qty),
        openTime,
        breakEvenPrice,
        profit,
        roi,
        profitable,
        coreQty,
        availableQty,
        corePositionId: trade.corePositionId || null
      }
    },
    updateChart() {
      if (!this.chart) return
      if (!this.rowData || !this.rowData.length) {
        this.renderEmptyChart()
      } else if (this.viewMode === 'buckets') {
        this.renderBucketChart()
      } else {
        this.renderTradeChart()
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
    renderTradeChart() {
      const currentPrice = Number(this.currentPrice) || 0
      const averagePrice = Number(this.averagePrice) || this.weightedAverage(this.normalizedTrades)
      const visibleTrades = this.filteredTrades
      const allPrices = this.normalizedTrades.map(item => item.openPrice).concat([currentPrice, averagePrice]).filter(Boolean)
      if (!allPrices.length) {
        this.renderEmptyChart()
        return
      }
      const minPrice = Math.min(...allPrices)
      const maxPrice = Math.max(...allPrices)
      const padding = Math.max((maxPrice - minPrice) * 0.12, 100)
      const maxQty = Math.max(...this.normalizedTrades.map(item => item.qty), 0.00000001)
      const makeData = profitable => visibleTrades
        .filter(item => item.profitable === profitable && item.openTime != null)
        .map(item => ({
          value: [item.openTime, item.openPrice, item.qty],
          trade: item,
          symbolSize: 9 + Math.sqrt(item.qty / maxQty) * 18,
          itemStyle: item.coreQty > 0 ? {
            borderColor: '#409eff',
            borderWidth: 3,
            shadowBlur: 12,
            shadowColor: '#409eff'
          } : undefined,
          label: item.coreQty > 0 ? {
            show: true,
            position: 'top',
            color: '#409eff',
            fontSize: 13,
            formatter: '🔒'
          } : undefined
        }))
      const series = [
        this.createScatterSeries('盈利买入', '#13ce8a', makeData(true)),
        this.createScatterSeries('亏损买入', '#f56c6c', makeData(false))
      ]
      series[0].markLine = {
        silent: true,
        symbol: 'none',
        label: { position: 'insideEndTop' },
        data: [
          {
            name: '当前价格',
            yAxis: currentPrice,
            lineStyle: { color: '#409eff', type: 'dashed', width: 2 },
            label: { formatter: `当前价格 ${this.formatNumber(currentPrice, 2)}`, color: '#409eff' }
          },
          {
            name: '持仓均价',
            yAxis: averagePrice,
            lineStyle: { color: '#e6a23c', type: 'dashed', width: 2 },
            label: { formatter: `持仓均价 ${this.formatNumber(averagePrice, 2)}`, color: '#e6a23c' }
          }
        ]
      }
      if (currentPrice) {
        series[0].markArea = {
          silent: true,
          data: [
            [
              { yAxis: minPrice - padding, itemStyle: { color: 'rgba(19, 206, 138, 0.06)' }},
              { yAxis: currentPrice }
            ],
            [
              { yAxis: currentPrice, itemStyle: { color: 'rgba(245, 108, 108, 0.06)' }},
              { yAxis: maxPrice + padding }
            ]
          ]
        }
      }
      this.chart.clear()
      this.chart.setOption({
        animationDuration: 300,
        color: ['#13ce8a', '#f56c6c'],
        legend: { bottom: 0, data: ['盈利买入', '亏损买入'] },
        toolbox: {
          right: 10,
          top: 0,
          feature: {
            restore: { title: '刷新图表' },
            saveAsImage: { title: '保存图片', pixelRatio: 2 }
          }
        },
        grid: { left: 78, right: 36, top: 38, bottom: 58 },
        tooltip: { trigger: 'item', formatter: params => this.tradeTooltip(params.data.trade) },
        xAxis: {
          name: '买入时间',
          type: 'time',
          axisLine: { lineStyle: { color: '#dcdfe6' }},
          splitLine: { show: true, lineStyle: { color: '#eef1f6', type: 'dashed' }},
          axisLabel: { color: '#606266' }
        },
        yAxis: {
          name: '买入价格 (USDT)',
          type: 'value',
          min: Math.max(0, minPrice - padding),
          max: maxPrice + padding,
          scale: true,
          axisLine: { lineStyle: { color: '#dcdfe6' }},
          splitLine: { lineStyle: { color: '#eef1f6', type: 'dashed' }},
          axisLabel: { color: '#606266', formatter: value => this.formatNumber(value, 0) }
        },
        series
      }, true)
    },
    createScatterSeries(name, color, data) {
      return {
        name,
        type: 'scatter',
        data,
        itemStyle: {
          color,
          borderColor: '#fff',
          borderWidth: 1,
          shadowBlur: 8,
          shadowColor: color
        },
        emphasis: { itemStyle: { borderWidth: 2, shadowBlur: 14 }}
      }
    },
    renderBucketChart() {
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
            borderColor: item.coreCount > 0 || item.range === currentBucketKey ? '#409eff' : color,
            borderWidth: item.coreCount > 0 || item.range === currentBucketKey ? 2 : 0,
            borderRadius: [0, 4, 4, 0]
          }
        }
      })
      const makeBucketSeries = (name, type, color) => ({
        name,
        type: 'bar',
        barMaxWidth: 24,
        barGap: '-100%',
        data: makeBucketData(type),
        itemStyle: { color },
        label: {
          show: true,
          position: 'right',
          color: '#606266',
          formatter: params => {
            const coreText = params.data.coreCount > 0 ? `  🔒${params.data.coreCount}笔` : ''
            return `${this.formatNumber(params.data.totalQty, 6)} BTC · ${params.data.count}笔${coreText}  均价 ${this.formatNumber(params.data.avgPrice, 2)}`
          }
        }
      })
      this.chart.clear()
      this.chart.setOption({
        animationDuration: 300,
        legend: { bottom: 0, data: ['盈利区间', '亏损区间', '持仓均价区间'] },
        toolbox: {
          right: 10,
          top: 0,
          feature: {
            restore: { title: '刷新图表' },
            saveAsImage: { title: '保存图片', pixelRatio: 2 }
          }
        },
        grid: { left: 128, right: 170, top: 38, bottom: 58 },
        tooltip: { trigger: 'item', formatter: params => this.bucketTooltip(params.data) },
        xAxis: {
          name: '持仓数量 (BTC)',
          type: 'value',
          splitLine: { lineStyle: { color: '#eef1f6', type: 'dashed' }},
          axisLine: { lineStyle: { color: '#dcdfe6' }}
        },
        yAxis: {
          name: '买入价格区间',
          type: 'category',
          data: buckets.map(item => item.range),
          axisLine: { lineStyle: { color: '#dcdfe6' }},
          axisLabel: {
            color: value => value === currentBucketKey ? '#409eff' : '#606266',
            fontWeight: value => value === currentBucketKey ? 'bold' : 'normal'
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
    weightedAverage(trades) {
      const totalQty = trades.reduce((sum, trade) => addAmount(sum, trade.qty), 0)
      const totalAmount = trades.reduce((sum, trade) => addAmount(sum, trade.openAmount), 0)
      return totalQty > 0 ? divAmount(totalAmount, totalQty, 2) : 0
    },
    tradeTooltip(trade) {
      return [
        `买入时间：${this.formatDateTime(trade.openTime)}`,
        `买入价格：${this.formatNumber(trade.openPrice, 2)}`,
        `剩余数量：${this.formatNumber(trade.qty, 8)}`,
        `盈亏平衡价：${this.formatNumber(trade.breakEvenPrice, 2)}`,
        `当前盈亏：${this.signedNumber(trade.profit, 2)}`,
        `收益率：${this.signedPercent(trade.roi)}`
      ].join('<br/>')
    },
    bucketTooltip(bucket) {
      return [
        `价格区间：${bucket.range}`,
        `成交笔数：${bucket.count}笔`,
        `持仓数量：${this.formatNumber(bucket.totalQty, 8)} BTC`,
        `底仓成交：${bucket.coreCount}笔`,
        `底仓数量：${this.formatNumber(bucket.coreQty, 8)} BTC`,
        `可撮合数量：${this.formatNumber(bucket.availableQty, 8)} BTC`,
        `加权均价：${this.formatNumber(bucket.avgPrice, 2)}`,
        `当前盈亏：${this.signedNumber(bucket.profit, 2)}`,
        `收益率：${this.signedPercent(bucket.profitRate)}`
      ].join('<br/>')
    },
    formatDateTime(value) {
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '--'
      const pad = number => String(number).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
    },
    formatNumber(value, digits) {
      const number = Number(value)
      if (!Number.isFinite(number)) return '--'
      return number.toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })
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
.core-legend { color: #409eff; font-size: 13px; white-space: nowrap; }
.core-legend i { margin-right: 4px; }
</style>
