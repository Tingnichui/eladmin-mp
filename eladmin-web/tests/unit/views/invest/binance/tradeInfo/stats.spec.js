/* eslint-env jest */
import crudBinanceTradeInfo from '@/api/binanceTradeInfo'
import { listAllAccount } from '@/api/binanceAccountInfo'
import Stats from '@/views/invest/binance/tradeInfo/stats.vue'

jest.mock('@/api/binanceTradeInfo', () => ({
  __esModule: true,
  default: {
    stats: jest.fn(),
    syncSpotTradeInfo: jest.fn(),
    syncSelected: jest.fn()
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
    window.localStorage.clear()
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
      statsInfo: {},
      buildStatsParams: Stats.methods.buildStatsParams
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

  it('uses the end of the selected day without mutating the query', () => {
    const vm = {
      query: {
        uid: 1,
        symbol: 'BTCUSDT',
        endTime: '2026-09-18'
      }
    }

    const params = Stats.methods.buildStatsParams.call(vm)

    expect(params.endTime).toBe('2026-09-18 23:59:59')
    expect(vm.query.endTime).toBe('2026-09-18')
  })

  it('omits the cutoff when no date is selected', () => {
    const vm = {
      query: {
        uid: 1,
        symbol: 'BTCUSDT',
        endTime: null
      }
    }

    const params = Stats.methods.buildStatsParams.call(vm)

    expect(params).not.toHaveProperty('endTime')
  })

  it('maps the spot return field to roi', () => {
    const descriptions = Stats.computed.statsDescriptions.call({ statsInfo: {}})
    const spotStats = descriptions.find(item => item.title === '现货统计')

    expect(spotStats.descriptionsItems).toContainEqual({
      label: '收益率',
      key: 'roi',
      type: 'percent'
    })
    expect(spotStats.descriptionsItems).toContainEqual({
      label: '未匹配卖出数量',
      key: 'unmatchedSellQty'
    })
  })

  it('keeps zero values visible in statistics', () => {
    expect(Stats.methods.formatValue({}, 'amount')).toBe('--')
    expect(Stats.methods.formatValue({ amount: 0 }, 'amount')).toBe('0.0000')
    expect(Stats.methods.formatValue({ roi: 0 }, 'roi', 'percent')).toBe('0.00%')
  })

  it('initializes the reactive open price filter', () => {
    const state = Stats.data()

    expect(state.filterForm).toEqual({
      side: null,
      openPrice: null,
      priceRange: 500
    })
    expect(state.filterForm).not.toHaveProperty('minPrice')
  })

  it('filters open trades by direction and open price range', () => {
    const filteredTrades = Stats.computed.filteredTrades.call({
      tradeList: [
        { id: 1, side: true, openPrice: 110 },
        { id: 2, side: true, openPrice: 90 },
        { id: 3, side: false, openPrice: 90 }
      ],
      filterForm: {
        side: true,
        openPrice: '100',
        priceRange: 20
      }
    })

    expect(filteredTrades.map(item => item.id)).toEqual([1])
  })

  it('syncs only the selected account and symbol before refreshing stats', async() => {
    crudBinanceTradeInfo.syncSelected.mockResolvedValue({
      spotCount: 2,
      usdFuturesCount: 1,
      coinFuturesCount: 0
    })
    const vm = {
      query: { uid: 7, symbol: 'BTCUSDT' },
      syncLoading: false,
      doStats: jest.fn(),
      $notify: jest.fn()
    }

    Stats.methods.syncSpotTradeInfo.call(vm)
    expect(vm.syncLoading).toBe(true)
    await flushPromises()

    expect(crudBinanceTradeInfo.syncSelected).toHaveBeenCalledWith({
      uid: 7,
      symbol: 'BTCUSDT'
    })
    expect(vm.doStats).toHaveBeenCalledTimes(1)
    expect(vm.$notify).toHaveBeenCalledWith(expect.objectContaining({
      title: '同步成功：现货 2 条，U 本位 1 条，币本位 0 条'
    }))
    expect(vm.syncLoading).toBe(false)
  })

  it('restores the last selected account before loading stats', async() => {
    window.localStorage.setItem('binanceTradeInfoStats.uid', '8')
    listAllAccount.mockResolvedValue({
      content: [
        { uid: 7, idCardName: '账户一' },
        { uid: 8, idCardName: '账户二' }
      ]
    })
    const vm = {
      accountList: [],
      query: { uid: null, symbol: 'BTCUSDT' },
      doStats: jest.fn()
    }

    await Stats.methods.refreshAccountList.call(vm)

    expect(vm.query.uid).toBe(8)
    expect(vm.doStats).toHaveBeenCalledTimes(1)
  })

  it('selects the first available account when the saved account is unavailable', async() => {
    window.localStorage.setItem('binanceTradeInfoStats.uid', '99')
    listAllAccount.mockResolvedValue({
      content: [{ uid: 7, idCardName: '账户一' }]
    })
    const vm = {
      accountList: [],
      query: { uid: null, symbol: 'BTCUSDT' },
      doStats: jest.fn()
    }

    await Stats.methods.refreshAccountList.call(vm)

    expect(vm.query.uid).toBe(7)
    expect(window.localStorage.getItem('binanceTradeInfoStats.uid')).toBe('7')
    expect(vm.doStats).toHaveBeenCalledTimes(1)
  })
})
