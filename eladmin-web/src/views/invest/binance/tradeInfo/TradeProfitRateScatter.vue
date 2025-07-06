<template>
  <div>
    <div style="display: flex; justify-content: center; align-items: center; margin-bottom: 10px;">
      <label style="margin-right: 10px;">最小收益率：</label>
      <input
        v-model.number="minProfitRate"
        type="range"
        min="0"
        max="0.1"
        step="0.001"
        style="width: 300px;"
        @input="updateChart"
      >
      <span style="margin-left: 10px;">{{ (minProfitRate * 100).toFixed(2) }}%</span>
    </div>
    <div ref="chartContainer" :class="className" :style="{ height: height, width: width }" />
  </div>
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import { debounce } from '@/utils'
import { formatPercent } from '@/utils/numberUtil'
import { formatDuration } from '@/utils/dateUtil'

export default {
  props: {
    className: {
      type: String,
      default: 'chart'
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '300px'
    },
    rowData: {
      type: Array,
      default: () => [] // 避免未定义时出错
    }
  },
  data() {
    return {
      chart: null,
      minProfitRate: 0
    }
  },
  watch: {
    rowData: {
      deep: true,
      handler() {
        this.updateChart()
      }
    }
  },
  mounted() {
    this.initChart()
    this.__resizeHandler = debounce(() => {
      if (this.chart) {
        this.chart.resize()
      }
    }, 100)
    window.addEventListener('resize', this.__resizeHandler)
  },
  beforeDestroy() {
    if (this.chart) {
      window.removeEventListener('resize', this.__resizeHandler)
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$refs.chartContainer, 'macarons')
      this.updateChart()
    },
    updateChart() {
      if (!this.chart || !this.rowData.length) return

      const chartData = this.rowData
        .filter(d => d.profitRate >= this.minProfitRate)
        .map(d => ({
          value: [Math.floor(d.holdMillis / 1000 / 60 / 60), d.profitRate],
          ...d
        }))
      this.chart.setOption({
        title: {
          text: '持仓时间 vs 收益率',
          left: 'center',
          top: 10,
          textStyle: {
            fontSize: 16
          }
        },
        tooltip: {
          formatter: function(params) {
            const d = params.data
            return `
              买入价格：${d.buyPrice}<br/>
              卖出价格：${d.sellPrice}<br/>
              成交数量：${d.qty}<br/>
              买入时间：${d.buyTime}<br/>
              卖出时间：${d.sellTime}<br/>
              持仓时间：${formatDuration(d.holdMillis)}<br/>
              收益：${d.profit}<br/>
              收益率：${formatPercent(d.profitRate)}<br/>
            `
          }
        },
        grid: {
          left: '10%',
          right: '10%',
          bottom: '10%',
          containLabel: true
        },
        xAxis: {
          name: '持仓时间（小时）',
          type: 'value',
          splitLine: { lineStyle: { type: 'dashed' }}
        },
        yAxis: {
          name: '收益率',
          type: 'value',
          axisLabel: {
            formatter: val => formatPercent(val)
          },
          splitLine: { lineStyle: { type: 'dashed' }}
        },
        series: [
          {
            name: '收益点',
            type: 'scatter',
            data: chartData,
            symbolSize: function(value, param) {
              return 8 + Math.min(param.data.profit / 2, 20)
            },
            itemStyle: {
              color: function(params) {
                // 最大收益假设为 10，映射到颜色深浅（0 到 1）
                const intensity = Math.min(params.data.profit / 10, 1)
                // 绿色从浅到深
                return `rgba(103, 194, 58, ${0.3 + 0.7 * intensity})` // 最低透明度 0.3，最高 1
              }
            }
          }
        ]
      })
    }
  }
}
</script>
