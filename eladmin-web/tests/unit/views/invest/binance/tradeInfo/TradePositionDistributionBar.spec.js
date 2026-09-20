/* eslint-env jest */
import Chart from '@/views/invest/binance/tradeInfo/TradePositionDistributionBar.vue'

describe('trade position distribution core position aggregation', () => {
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
})
