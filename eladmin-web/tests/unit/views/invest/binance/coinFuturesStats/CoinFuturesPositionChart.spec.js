/* eslint-env jest */
import Chart from '@/views/invest/binance/coinFuturesStats/CoinFuturesPositionChart.vue'

describe('coin futures position chart', () => {
  it('defaults to price bucket aggregation for coin contracts', () => {
    expect(Chart.data().viewMode).toBe('buckets')
    expect(Chart.data().intervalIndex).toBe(2)
    expect(Chart.data().intervals).toEqual([500, 1000, 2500, 5000])
  })

  it('normalizes contract quantity separately from base asset quantity', () => {
    const rows = [{
      contractQty: '12',
      baseQty: '0.012',
      openPrice: '100000',
      unrealizedPnl: '0.001',
      openTime: '2026-09-22T00:00:00Z'
    }]

    const result = Chart.computed.normalizedTrades.call({ rowData: rows })

    expect(result[0].contractQty).toBe(12)
    expect(result[0].baseQty).toBe(0.012)
    expect(result[0].openPrice).toBe(100000)
  })

  it('filters inverse-contract positions by calculated profit', () => {
    const normalizedTrades = [
      { tradeId: 1, unrealizedPnl: 0.001 },
      { tradeId: 2, unrealizedPnl: -0.002 }
    ]
    const vm = { normalizedTrades, profitFilter: 'profit' }

    expect(Chart.computed.filteredTrades.call(vm).map(item => item.tradeId)).toEqual([1])
    vm.profitFilter = 'loss'
    expect(Chart.computed.filteredTrades.call(vm).map(item => item.tradeId)).toEqual([2])
  })
})
