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
import { addAmount, divAmount, formatPercent, mulAmount, subAmount } from '@/utils/numberUtil'

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
      priceInterval: 500,
      currentPrice: '',
      totalWaitAvgSellPrice: '',
      totalWaitSellQty: ''
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

    // 每 5 更新一次
    setInterval(() => {
      this.updateChart()
    }, 5000)
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

      this.totalWaitSellQty = trades.reduce((sum, t) => addAmount(sum, t.qty), 0)

      this.totalWaitAvgSellPrice = trades.reduce((sum, t) => addAmount(sum, mulAmount(t.price, t.qty)), 0)

      this.totalWaitAvgSellPrice = divAmount(this.totalWaitAvgSellPrice, this.totalWaitSellQty, 2)

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

      return Object.values(bucketsMap).map(bucket => {
        const avgPrice = bucket.totalQty > 0 ? divAmount(bucket.totalAmount, bucket.totalQty, 2) : 0
        let profit = 0
        let profitRate = 0
        if (this.currentPrice) {
          profit = bucket.totalQty > 0 ? mulAmount(subAmount(this.currentPrice, avgPrice), bucket.totalQty) : 0
          profitRate = avgPrice > 0 ? divAmount(subAmount(this.currentPrice, avgPrice), avgPrice) : 0
        }

        return {
          range: bucket.range,
          totalQty: bucket.totalQty,
          avgPrice,
          profit: profit.toFixed(2),
          profitRate: formatPercent(profitRate)
        }
      }).sort((a, b) => {
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
      // 先建立一个 map，range -> avgPrice
      const rangeAvgPriceMap = {}
      buckets.forEach(item => {
        rangeAvgPriceMap[item.range] = item.avgPrice
      })

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
              平均价格: ${d.avgPrice}<br/>
              收益: ${d.profit}<br/>
              收益率: ${d.profitRate}
            `
          }
        },
        toolbox: {
          feature: {
            myRefresh: {
              show: true,
              title: '刷新图表',
              icon: 'path://M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448H832c0 176.7-143.3 320-320 320S192 688.7 192 512 335.3 192 512 192v80l128-128-128-128v80z', // 可自定义刷新图标
              onclick: () => {
                this.updateChart()
              }
            },
            saveAsImage: { show: true, title: '保存图片' }
          },
          right: 10,
          top: 10
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
          data: buckets.map(item => item.range),
          axisLabel: {
            formatter: (value) => {
              const upper = value.split('-').map(Number)[1]
              const avgPrice = rangeAvgPriceMap[value]
              // 判断当前价格是否高于该区间
              if (this.currentPrice && upper <= this.currentPrice && avgPrice <= mulAmount(this.currentPrice, 0.995)) {
                return `{green|${value}}`
              }
              return `{normal|${value}}`
            },
            rich: {
              green: {
                color: '#00C853', // 红色
                fontWeight: 'bold'
              },
              normal: {
                color: '#666' // 默认灰色
              }
            }
          }
        },
        series: [
          {
            type: 'bar',
            data: buckets.map(item => {
              const [lower, upper] = item.range.split('-').map(Number)
              const isAvgBuyPrice = this.totalWaitAvgSellPrice ? this.totalWaitAvgSellPrice >= lower && this.totalWaitAvgSellPrice <= upper : false

              let color = '#409EFF' // 默认蓝色
              if (isAvgBuyPrice) {
                // 平均卖价橙色
                color = '#FFA500'
              }

              return {
                ...item,
                value: item.totalQty,
                itemStyle: { color }
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
        ],
        graphic: [
          {
            type: 'group', // 用 group 包裹多个 text，实现分行排版
            right: 20,
            top: 50,
            children: [
              {
                type: 'text',
                style: {
                  text: `${this.currentPrice || 0}`,
                  fill: '#FF3D00',
                  font: 'bold 14px Arial',
                  align: 'right'
                }
              },
              {
                type: 'text',
                top: 20, // 相对于 group 向下偏移
                style: {
                  text: `${this.totalWaitSellQty}`,
                  fill: '#409EFF',
                  font: 'bold 14px Arial',
                  align: 'right'
                }
              },
              {
                type: 'text',
                top: 40, // 相对于 group 向下偏移
                style: {
                  text: `${this.totalWaitAvgSellPrice}`,
                  fill: '#409EFF',
                  font: 'bold 14px Arial',
                  align: 'right'
                }
              }
            ]
          }
        ]
      })
    }
  }
}
</script>
