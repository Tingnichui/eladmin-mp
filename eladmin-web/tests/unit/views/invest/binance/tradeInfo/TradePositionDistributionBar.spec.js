/* eslint-env jest */
import Chart from '@/views/invest/binance/tradeInfo/TradePositionDistributionBar.vue'

describe('trade position distribution core position aggregation', () => {
  it('defaults to price bucket aggregation', () => {
    expect(Chart.data().viewMode).toBe('buckets')
    expect(Chart.data().priceInterval).toBe(2500)
    expect(Chart.data().coreFilter).toBe('all')
  })

  it('switches between fixed price interval levels', () => {
    const vm = { priceInterval: 2500, refreshChartLayout: jest.fn() }

    Chart.methods.shiftPriceInterval.call(vm, -1)
    expect(vm.priceInterval).toBe(1000)
    Chart.methods.shiftPriceInterval.call(vm, 1)
    expect(vm.priceInterval).toBe(2500)
    Chart.methods.shiftPriceInterval.call(vm, 1)
    expect(vm.priceInterval).toBe(5000)
    Chart.methods.shiftPriceInterval.call(vm, 1)

    expect(vm.priceInterval).toBe(5000)
    expect(vm.refreshChartLayout).toHaveBeenCalledTimes(3)
    expect(Chart.computed.canIncreaseInterval.call(vm)).toBe(false)
    expect(Chart.computed.canDecreaseInterval.call(vm)).toBe(true)
  })

  it('uses a content-driven chart height on mobile price buckets', () => {
    const vm = {
      isMobileViewport: true,
      height: '100%',
      viewMode: 'buckets',
      filteredTrades: [
        { openPrice: 78000, qty: 0.001, openAmount: 78, profit: 1, coreQty: 0, availableQty: 0.001 },
        { openPrice: 81000, qty: 0.001, openAmount: 81, profit: 1, coreQty: 0, availableQty: 0.001 }
      ],
      priceInterval: 2500,
      currentPrice: 80000,
      bucketKey: Chart.methods.bucketKey,
      groupTradesByPrice: Chart.methods.groupTradesByPrice
    }

    expect(Chart.computed.chartHeight.call(vm)).toBe('360px')
    vm.viewMode = 'trades'
    expect(Chart.computed.chartHeight.call(vm)).toBe('420px')
    vm.isMobileViewport = false
    expect(Chart.computed.chartHeight.call(vm)).toBe('100%')
  })

  it('aggregates core and available quantities inside each price bucket', () => {
    const vm = {
      priceInterval: 2500,
      currentPrice: 80326.59,
      bucketKey: Chart.methods.bucketKey
    }
    const trades = [
      {
        openPrice: 78200,
        qty: 0.0014,
        openAmount: 109.48,
        profit: 2,
        coreQty: 0.0014,
        availableQty: 0
      },
      {
        openPrice: 79100,
        qty: 0.002,
        openAmount: 158.2,
        profit: 1,
        coreQty: 0,
        availableQty: 0.002
      }
    ]

    const buckets = Chart.methods.groupTradesByPrice.call(vm, trades)

    expect(buckets).toHaveLength(1)
    expect(buckets[0]).toEqual(expect.objectContaining({
      range: '77500-80000',
      count: 2,
      coreCount: 1,
      coreQty: 0.0014,
      availableQty: 0.002
    }))
    expect(buckets[0].trades).toHaveLength(2)
  })

  it('filters trades by profit and core position independently', () => {
    const normalizedTrades = [
      { tradeId: '1', profitable: true, coreQty: 0.001 },
      { tradeId: '2', profitable: false, coreQty: 0 },
      { tradeId: '3', profitable: false, coreQty: 0.002 }
    ]
    const vm = { normalizedTrades, profitFilter: 'all', coreFilter: 'core' }

    expect(Chart.computed.filteredTrades.call(vm).map(trade => trade.tradeId)).toEqual(['1', '3'])

    vm.profitFilter = 'loss'
    expect(Chart.computed.filteredTrades.call(vm).map(trade => trade.tradeId)).toEqual(['3'])

    vm.coreFilter = 'nonCore'
    expect(Chart.computed.filteredTrades.call(vm).map(trade => trade.tradeId)).toEqual(['2'])
    expect(Chart.computed.coreTradeCounts.call({ normalizedTrades })).toEqual({ core: 2, nonCore: 1 })
  })

  it('compacts both ends of a mobile price range', () => {
    expect(Chart.methods.compactBucketRange('120000-122500')).toBe('120–122.5k')
    expect(Chart.methods.compactBucketRange('500-1000')).toBe('500-1k')
  })

  it('removes trailing zeroes from chart quantities', () => {
    expect(Chart.methods.quantityNumber(0.00400000)).toBe('0.004')
    expect(Chart.methods.quantityNumber('0.01600000')).toBe('0.016')
    expect(Chart.methods.quantityNumber(0)).toBe('0')
  })

  it('hides the chart tooltip before opening a price bucket', () => {
    const bucket = { trades: [{ tradeId: '1' }] }
    const vm = {
      chart: { dispatchAction: jest.fn() },
      $emit: jest.fn(),
      hideTooltip: Chart.methods.hideTooltip
    }

    Chart.methods.handleChartClick.call(vm, { data: bucket })

    expect(vm.chart.dispatchAction).toHaveBeenCalledWith({ type: 'hideTip' })
    expect(vm.$emit).toHaveBeenCalledWith('select-bucket', bucket)
  })
})
