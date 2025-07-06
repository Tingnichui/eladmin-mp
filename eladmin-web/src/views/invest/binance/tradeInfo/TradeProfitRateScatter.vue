<template>
  <div :class="className" :style="{ height: height, width: width }" />
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import { debounce } from '@/utils'

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
      chart: null
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
      this.chart = echarts.init(this.$el, 'macarons')
      this.updateChart()
    },
    updateChart() {
      if (!this.chart || !this.rowData.length) return

      const chartData = this.rowData.map(d => [d.holdHours, d.profitRate])
      console.log(JSON.stringify(chartData))
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
            return `持仓时间：${params.data[0]} 小时<br/>收益率：${(params.data[1] * 100).toFixed(2)}%`
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
            formatter: val => `${(val * 100).toFixed(2)}%`
          },
          splitLine: { lineStyle: { type: 'dashed' }}
        },
        series: [
          {
            name: '收益点',
            type: 'scatter',
            data: chartData,
            symbolSize: 8,
            itemStyle: {
              color: function(params) {
                return params.value[1] >= 0 ? '#67C23A' : '#F56C6C'
              }
            }
          }
        ]
      })
    }
  }
}
</script>
