<template>
  <div>
    <div style="display: flex; justify-content: center; align-items: center; margin-bottom: 10px;">
      <label style="margin-right: 10px;">价格区间间隔：</label>
      <input
        v-model.number="priceInterval"
        type="range"
        min="100"
        max="2000"
        step="100"
        style="width: 300px;"
        @input="updateChart"
      >
      <span style="margin-left: 10px;">{{ priceInterval }}</span>
    </div>
    <div ref="chartContainer" :class="className" :style="{ height: height, width: width }" />
  </div>
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import { debounce } from '@/utils'
import { price } from '@/api/investKlinesRecord'
import { addAmount, divAmount, mulAmount } from '@/utils/numberUtil'

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
    },
    symbol: {
      type: String,
      default: () => '' // 避免未定义时出错
    }
  },
  data() {
    return {
      chart: null,
      priceInterval: 1000,
      currentPrice: ''
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
    groupTradesByPrice(trades) {
      if (!trades || trades.length === 0) return []

      const maxPrice = Math.ceil(Math.max(...trades.map(t => t.price)) / 1000) * 1000
      const bucketsMap = {}

      trades.forEach(trade => {
        const diff = Math.floor((maxPrice - trade.price) / this.priceInterval)
        const lower = maxPrice - (diff + 1) * this.priceInterval
        const upper = maxPrice - diff * this.priceInterval
        const key = `${lower}-${upper}`

        if (!bucketsMap[key]) {
          bucketsMap[key] = { range: key, totalQty: 0, totalAmount: 0 }
        }
        bucketsMap[key].totalQty = addAmount(bucketsMap[key].totalQty, trade.qty)
        bucketsMap[key].totalAmount = addAmount(bucketsMap[key].totalAmount, mulAmount(trade.price, trade.qty))
      })

      return Object.values(bucketsMap).map(bucket => ({
        range: bucket.range,
        totalQty: bucket.totalQty,
        avgPrice: bucket.totalQty > 0 ? divAmount(bucket.totalAmount, bucket.totalQty, 2) : 0
      })).sort((a, b) => {
        const aUpper = parseInt(a.range.split('-')[1])
        const bUpper = parseInt(b.range.split('-')[1])
        return aUpper - bUpper
      })
    },
    updateChart() {
      if (!this.chart || !this.rowData.length) return

      // 获取当前价格
      price(this.symbol).then(res => {
        this.currentPrice = res
      })

      const buckets = this.groupTradesByPrice(this.rowData)

      console.log(buckets)

      this.chart.setOption({
        title: {
          text: '仓位分布（价格区间 - 持仓量）',
          left: 'center',
          top: 10,
          textStyle: { fontSize: 16 }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter: function(params) {
            const d = params[0].data
            return `
              价格区间: ${d.range}<br/>
              持仓量: ${d.totalQty}<br/>
              平均价格: ${d.avgPrice}
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
          name: '持仓量',
          type: 'value'
        },
        yAxis: {
          name: '价格区间',
          type: 'category',
          data: buckets.map(item => item.range)
        },
        series: [
          {
            type: 'bar',
            data: buckets.map(item => {
              const [lower, upper] = item.range.split('-').map(Number)
              const isCurrent = this.currentPrice ? this.currentPrice >= lower && this.currentPrice <= upper : false
              return {
                value: item.totalQty,
                range: item.range,
                totalQty: item.totalQty,
                avgPrice: item.avgPrice,
                itemStyle: { color: isCurrent ? '#FF3D00' : '#409EFF' } // 当前区间红色
              }
            }),
            label: {
              show: true,
              position: 'right',
              formatter: d => `${d.data.avgPrice}`
            },
            itemStyle: {
              color: '#409EFF'
            }
          }
        ]
      })
    }
  }
}
</script>
