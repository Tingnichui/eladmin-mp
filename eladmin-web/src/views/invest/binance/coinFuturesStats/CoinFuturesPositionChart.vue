<template>
  <div class="coin-position-chart">
    <div class="chart-toolbar">
      <el-radio-group v-model="viewMode" size="small" @change="renderChart">
        <el-radio-button label="buckets">价格区间聚合</el-radio-button>
        <el-radio-button label="trades">逐笔开仓</el-radio-button>
      </el-radio-group>
      <div v-if="viewMode === 'buckets'" class="interval-control">
        <span>价格间隔</span>
        <el-button size="mini" icon="el-icon-arrow-left" :disabled="intervalIndex === 0" @click="shiftInterval(-1)" />
        <strong>{{ priceInterval }} USD</strong>
        <el-button size="mini" icon="el-icon-arrow-right" :disabled="intervalIndex === intervals.length - 1" @click="shiftInterval(1)" />
      </div>
      <el-radio-group v-model="profitFilter" size="small" @change="renderChart">
        <el-radio-button label="all">全部 {{ counts.all }}</el-radio-button>
        <el-radio-button label="profit">盈利 {{ counts.profit }}</el-radio-button>
        <el-radio-button label="loss">亏损 {{ counts.loss }}</el-radio-button>
      </el-radio-group>
    </div>
    <div ref="chart" class="chart" />
  </div>
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons')
import { debounce } from '@/utils'

export default {
  name: 'CoinFuturesPositionChart',
  props: {
    rowData: { type: Array, default: () => [] },
    currentPrice: { type: [Number, String], default: null },
    averagePrice: { type: [Number, String], default: null }
  },
  data() {
    return {
      chart: null,
      viewMode: 'buckets',
      profitFilter: 'all',
      intervals: [500, 1000, 2500, 5000],
      intervalIndex: 2
    }
  },
  computed: {
    priceInterval() {
      return this.intervals[this.intervalIndex]
    },
    normalizedTrades() {
      return (this.rowData || []).map(item => ({
        ...item,
        contractQty: Number(item.contractQty) || 0,
        baseQty: Number(item.baseQty) || 0,
        openPrice: Number(item.openPrice) || 0,
        unrealizedPnl: Number(item.unrealizedPnl) || 0,
        openTimeValue: item.openTime ? new Date(item.openTime).getTime() : null
      })).filter(item => item.contractQty > 0 && item.openPrice > 0)
    },
    counts() {
      return {
        all: this.normalizedTrades.length,
        profit: this.normalizedTrades.filter(item => item.unrealizedPnl >= 0).length,
        loss: this.normalizedTrades.filter(item => item.unrealizedPnl < 0).length
      }
    },
    filteredTrades() {
      if (this.profitFilter === 'profit') return this.normalizedTrades.filter(item => item.unrealizedPnl >= 0)
      if (this.profitFilter === 'loss') return this.normalizedTrades.filter(item => item.unrealizedPnl < 0)
      return this.normalizedTrades
    }
  },
  watch: {
    rowData: { deep: true, handler() { this.renderChart() } },
    currentPrice() { this.renderChart() },
    averagePrice() { this.renderChart() }
  },
  mounted() {
    this.chart = echarts.init(this.$refs.chart, 'macarons')
    this.resizeHandler = debounce(() => this.chart && this.chart.resize(), 100)
    window.addEventListener('resize', this.resizeHandler)
    this.renderChart()
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeHandler)
    if (this.chart) this.chart.dispose()
  },
  methods: {
    shiftInterval(step) {
      this.intervalIndex += step
      this.renderChart()
    },
    renderChart() {
      if (!this.chart) return
      if (!this.filteredTrades.length) {
        this.chart.clear()
        this.chart.setOption({ title: { text: '暂无未平仓批次', left: 'center', top: '45%', textStyle: { color: '#909399', fontSize: 14 }}})
        return
      }
      if (this.viewMode === 'trades') this.renderTrades()
      else this.renderBuckets()
    },
    renderTrades() {
      const data = this.filteredTrades.filter(item => item.openTimeValue).map(item => ({
        value: [item.openTimeValue, item.openPrice, item.contractQty],
        item,
        itemStyle: { color: item.unrealizedPnl >= 0 ? '#13ce66' : '#ff6b6b' }
      }))
      this.chart.clear()
      this.chart.setOption({
        grid: { left: 70, right: 40, top: 28, bottom: 58 },
        tooltip: { formatter: params => this.tradeTooltip(params.data.item) },
        xAxis: { type: 'time', name: '开仓时间' },
        yAxis: { type: 'value', name: '开仓价格 (USD)', scale: true },
        series: [{
          type: 'scatter',
          data,
          symbolSize: value => Math.max(10, Math.min(32, 9 + Math.sqrt(value[2]) * 2)),
          markLine: this.markLines()
        }]
      })
    },
    renderBuckets() {
      const buckets = new Map()
      this.filteredTrades.forEach(item => {
        const start = Math.floor(item.openPrice / this.priceInterval) * this.priceInterval
        const key = String(start)
        const bucket = buckets.get(key) || { start, contractQty: 0, baseQty: 0, pnl: 0, count: 0 }
        bucket.contractQty += item.contractQty
        bucket.baseQty += item.baseQty
        bucket.pnl += item.unrealizedPnl
        bucket.count += 1
        buckets.set(key, bucket)
      })
      const rows = Array.from(buckets.values()).sort((a, b) => b.start - a.start)
      this.chart.clear()
      this.chart.setOption({
        grid: { left: 110, right: 80, top: 24, bottom: 48 },
        tooltip: { formatter: params => {
          const row = params.data.row
          return [`价格区间：${row.start}-${row.start + this.priceInterval}`, `持仓：${this.number(row.contractQty)} 张`, `折合：${this.number(row.baseQty)} BTC`, `未实现盈亏：${this.number(row.pnl)} BTC`, `开仓成交：${row.count} 笔`].join('<br>')
        } },
        xAxis: { type: 'value', name: '持仓张数' },
        yAxis: { type: 'category', data: rows.map(row => `${row.start}-${row.start + this.priceInterval}`) },
        series: [{
          type: 'bar',
          barMaxWidth: 26,
          data: rows.map(row => ({ value: row.contractQty, row, itemStyle: { color: row.pnl >= 0 ? '#13ce66' : '#ff6b6b' }})),
          label: { show: true, position: 'right', formatter: params => `${this.number(params.data.row.contractQty)} 张 · ${params.data.row.count} 笔` }
        }]
      })
    },
    markLines() {
      const data = []
      if (Number(this.currentPrice) > 0) data.push({ name: '标记价格', yAxis: Number(this.currentPrice), lineStyle: { color: '#409eff' }})
      if (Number(this.averagePrice) > 0) data.push({ name: '开仓均价', yAxis: Number(this.averagePrice), lineStyle: { color: '#e6a23c' }})
      return { symbol: 'none', label: { formatter: params => `${params.name} ${this.number(params.value)}` }, data }
    },
    tradeTooltip(item) {
      return [`开仓时间：${this.time(item.openTime)}`, `开仓价格：${this.number(item.openPrice)} USD`, `持仓：${this.number(item.contractQty)} 张`, `折合：${this.number(item.baseQty)} BTC`, `未实现盈亏：${this.number(item.unrealizedPnl)} BTC`].join('<br>')
    },
    number(value) {
      const number = Number(value)
      return Number.isFinite(number) ? number.toLocaleString('en-US', { maximumFractionDigits: 8 }) : '--'
    },
    time(value) {
      const date = value ? new Date(value) : null
      return date && !Number.isNaN(date.getTime()) ? date.toLocaleString() : '--'
    }
  }
}
</script>

<style scoped>
.coin-position-chart { display: flex; flex: 1 1 auto; flex-direction: column; min-height: 0; }
.chart-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; padding-bottom: 8px; }
.interval-control { display: flex; align-items: center; gap: 6px; color: #606266; font-size: 13px; }
.chart { flex: 1 1 auto; min-height: 420px; width: 100%; }
</style>
