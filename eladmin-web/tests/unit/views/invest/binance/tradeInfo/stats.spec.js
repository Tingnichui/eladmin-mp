/* eslint-env jest */
import crudBinanceTradeInfo from '@/api/binanceTradeInfo'
import Stats from '@/views/invest/binance/tradeInfo/stats.vue'

jest.mock('@/api/binanceTradeInfo', () => ({
  __esModule: true,
  default: {
    stats: jest.fn(),
    syncSpotTradeInfo: jest.fn()
  }
}))
jest.mock('@/api/binanceAccountInfo', () => ({
  listAllAccount: jest.fn()
}))
jest.mock('@/views/invest/binance/tradeInfo/TradePositionDistributionBar.vue', () => ({
  name: 'TradePositionDistributionBar'
}))
jest.mock('@crud/crud', () => ({
  NOTIFICATION_TYPE: { SUCCESS: 'success' }
}), { virtual: true })

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0))

describe('trade stats request lifecycle', () => {
  afterEach(() => {
    jest.clearAllMocks()
    jest.useRealTimers()
  })

  it('debounces filter changes and keeps only the latest request intent', () => {
    jest.useFakeTimers()
    const vm = {
      statsDebounceTimer: null,
      statsRequestId: 0,
      clearStatsDebounce: Stats.methods.clearStatsDebounce,
      executeStats: jest.fn()
    }

    Stats.methods.scheduleStats.call(vm)
    Stats.methods.scheduleStats.call(vm)
    jest.advanceTimersByTime(299)
    expect(vm.executeStats).not.toHaveBeenCalled()

    jest.advanceTimersByTime(1)
    expect(vm.executeStats).toHaveBeenCalledTimes(1)
    expect(vm.executeStats).toHaveBeenCalledWith(2)
  })

  it('ignores an older response and does not mutate the query date', async() => {
    let resolveFirst
    const firstRequest = new Promise(resolve => {
      resolveFirst = resolve
    })
    crudBinanceTradeInfo.stats
      .mockReturnValueOnce(firstRequest)
      .mockResolvedValueOnce({ marker: 'latest' })
    const vm = {
      query: {
        uid: 1,
        symbol: 'BTCUSDT',
        endTime: '2026-09-18 00:00:00'
      },
      statsRequestId: 1,
      statsLoading: false,
      statsInfo: {}
    }

    Stats.methods.executeStats.call(vm, 1)
    vm.statsRequestId = 2
    Stats.methods.executeStats.call(vm, 2)
    await flushPromises()

    expect(vm.statsInfo).toEqual({ marker: 'latest' })
    expect(vm.query.endTime).toBe('2026-09-18 00:00:00')
    expect(crudBinanceTradeInfo.stats.mock.calls[0][0]).not.toBe(vm.query)

    resolveFirst({ marker: 'stale' })
    await flushPromises()
    expect(vm.statsInfo).toEqual({ marker: 'latest' })
    expect(vm.statsLoading).toBe(false)
  })
})
