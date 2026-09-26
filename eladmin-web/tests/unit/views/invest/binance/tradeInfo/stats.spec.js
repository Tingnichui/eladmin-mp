/* eslint-env jest */
import crudBinanceTradeInfo from '@/api/binanceTradeInfo'
import { listAllAccount } from '@/api/binanceAccountInfo'
import { lockAll as lockAllCorePositionsApi, release as releaseCorePositionApi, releaseAll as releaseAllCorePositionsApi } from '@/api/binanceSpotCorePosition'
import Stats from '@/views/invest/binance/tradeInfo/stats.vue'

jest.mock('@/api/binanceTradeInfo', () => ({
  __esModule: true,
  default: {
    stats: jest.fn(),
    syncSpotTradeInfo: jest.fn(),
    syncSelected: jest.fn(),
    cancelSpotOrder: jest.fn()
  }
}))
jest.mock('@/api/binanceAccountInfo', () => ({
  listAllAccount: jest.fn()
}))
jest.mock('@/api/binanceC2cOrder', () => ({
  getAccountAssets: jest.fn(),
  sync: jest.fn()
}))
jest.mock('@/views/invest/binance/tradeInfo/TradePositionDistributionBar.vue', () => ({
  name: 'TradePositionDistributionBar'
}))
jest.mock('@/api/binanceSpotCorePosition', () => ({
  add: jest.fn(),
  edit: jest.fn(),
  lockAll: jest.fn(),
  release: jest.fn(),
  releaseAll: jest.fn()
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
      lastUpdatedAt: ''
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

  it('reads only the spot statistics payload', () => {
    const spot = { roi: 0.12, unmatchedSellQty: 0 }
    const result = Stats.computed.spotStats.call({
      statsInfo: {
        spotFuturesStatsInfo: spot,
        usdFuturesStatsInfo: { roi: 0.3 }
      }
    })

    expect(result).toBe(spot)
  })

  it('keeps zero values visible in statistics', () => {
    const vm = { decimalValue: Stats.methods.decimalValue }
    expect(Stats.methods.decimalValue(null, 2)).toBe('--')
    expect(Stats.methods.decimalValue(0, 4)).toBe('0.0000')
    expect(Stats.methods.signedPercentValue(0)).toBe('0.00%')
    expect(Stats.methods.currencyValue.call(vm, 0, '$')).toBe('$0.00')
  })

  it('removes trailing zeroes from asset quantities', () => {
    const vm = { query: { symbol: 'BTCUSDT' }}
    expect(Stats.methods.quantityValue.call(vm, 0.00200000, false)).toBe('0.002')
    expect(Stats.methods.quantityValue.call(vm, 0, false)).toBe('0')
    expect(Stats.methods.quantityValue.call(vm, null, false)).toBe('--')
  })

  it('shows only a non-zero net profit change', () => {
    const vm = {
      spotStats: {},
      netPnlDelta: 0,
      moneyValue: value => `$${value || 0}`,
      signedMoneyValue: value => `${Number(value) > 0 ? '+' : ''}$${value || 0}`,
      signedPercentValue: value => `${value || 0}%`,
      quantityValue: value => String(value || 0),
      valueTone: Stats.methods.valueTone
    }

    let netPnlItem = Stats.computed.tradeSummaryItems.call(vm).find(item => item.label === '净盈亏')
    expect(netPnlItem.delta).toBe('')

    vm.netPnlDelta = 12.34
    netPnlItem = Stats.computed.tradeSummaryItems.call(vm).find(item => item.label === '净盈亏')
    expect(netPnlItem).toEqual(expect.objectContaining({ delta: '↑$12.34', deltaTone: 'positive' }))

    vm.netPnlDelta = -5.67
    netPnlItem = Stats.computed.tradeSummaryItems.call(vm).find(item => item.label === '净盈亏')
    expect(netPnlItem).toEqual(expect.objectContaining({ delta: '↓$5.67', deltaTone: 'negative' }))
  })

  it('hides futures warnings from the spot page', () => {
    const text = Stats.computed.realtimeWarningText.call({
      statsInfo: {
        warnings: ['现货数据延迟', 'U本位合约不可用', '币本位接口超时'],
        realtimeStatus: {}
      }
    })

    expect(text).toBe('现货数据延迟')
  })

  it('does not keep the removed standalone trade details state', () => {
    const state = Stats.data()

    expect(state).not.toHaveProperty('showOpenTrades')
    expect(state).not.toHaveProperty('tradeList')
    expect(state).not.toHaveProperty('filterForm')
    expect(state).not.toHaveProperty('tableColumns')
    expect(state).not.toHaveProperty('showCorePositions')
    expect(state).not.toHaveProperty('corePositionLoading')
    expect(state).not.toHaveProperty('corePositionCandidates')
  })

  it('polls open orders only while a related panel is visible', () => {
    jest.useFakeTimers()
    const vm = {
      showCoreActions: true,
      showSpotOpenOrdersDialog: false,
      spotOpenOrdersPollingTimer: null,
      hasVisibleSpotOpenOrdersPanel: Stats.methods.hasVisibleSpotOpenOrdersPanel,
      refreshSpotOpenOrdersIfVisible: jest.fn(),
      stopSpotOpenOrdersPolling: Stats.methods.stopSpotOpenOrdersPolling
    }

    Stats.methods.updateSpotOpenOrdersPolling.call(vm)
    jest.advanceTimersByTime(15000)
    expect(vm.refreshSpotOpenOrdersIfVisible).toHaveBeenCalledTimes(1)

    vm.showCoreActions = false
    Stats.methods.updateSpotOpenOrdersPolling.call(vm)
    jest.advanceTimersByTime(15000)
    expect(vm.refreshSpotOpenOrdersIfVisible).toHaveBeenCalledTimes(1)
    expect(vm.spotOpenOrdersPollingTimer).toBeNull()
  })

  it('normalizes a statistics trade for core position actions', () => {
    const vm = { query: { uid: 7, symbol: 'BTCUSDT' }}

    const row = Stats.methods.toCoreActionRow.call(vm, {
      tradeId: '1001',
      openTime: '2026-02-08 16:28:00',
      openPrice: 78200,
      qty: 0.0014,
      coreQty: 0.0004,
      openAmount: 109.48,
      breakEvenPrice: 78278.2,
      netPnl: 3.12,
      roi: 0.0285
    })

    expect(row).toEqual(expect.objectContaining({
      uid: 7,
      symbol: 'BTCUSDT',
      tradeId: '1001',
      price: 78200,
      remainingQty: 0.0014,
      coreQty: 0.0004,
      availableQty: 0.001,
      openAmount: 109.48,
      breakEvenPrice: 78278.2,
      netPnl: 3.12,
      roi: 0.0285
    }))
  })

  it('opens a price bucket as individual actionable trades', () => {
    const vm = {
      query: { uid: 7, symbol: 'BTCUSDT' },
      selectedCoreRange: null,
      coreActionRows: [],
      showCoreActions: false,
      toCoreActionRow: Stats.methods.toCoreActionRow,
      loadSpotOpenOrders: jest.fn()
    }
    const bucket = {
      range: '77500-80000',
      trades: [
        { raw: { tradeId: '1', openPrice: 78000, qty: 0.001 }},
        { raw: { tradeId: '2', openPrice: 79000, qty: 0.002 }}
      ]
    }

    Stats.methods.openBucketCoreActions.call(vm, bucket)

    expect(vm.selectedCoreRange).toEqual(expect.objectContaining({ range: '77500-80000' }))
    expect(vm.coreActionRows.map(row => row.tradeId)).toEqual(['1', '2'])
    expect(vm.showCoreActions).toBe(true)
  })

  it('aggregates fills from the same order without changing actionable trades', () => {
    const vm = {
      coreAvailableQty: Stats.methods.coreAvailableQty,
      coreRawAvailableQty: Stats.methods.coreRawAvailableQty,
      hasCorePosition: Stats.methods.hasCorePosition
    }
    const rows = [
      {
        tradeId: '11',
        orderId: '88',
        tradeTime: '2026-09-20 10:00:00',
        price: 100,
        remainingQty: 1,
        availableQty: 0.6,
        openAmount: 100,
        netPnl: 10,
        breakEvenPrice: 101,
        corePositionId: '101',
        coreQty: 0.4
      },
      {
        tradeId: '12',
        orderId: '88',
        tradeTime: '2026-09-20 10:00:01',
        price: 110,
        remainingQty: 2,
        availableQty: 2,
        openAmount: 220,
        netPnl: -5,
        breakEvenPrice: 111,
        corePositionId: null,
        coreQty: 0
      }
    ]

    const group = Stats.methods.createCoreOrderGroup.call(vm, 'order:88', rows)

    expect(group).toEqual(expect.objectContaining({
      orderId: '88',
      tradeCount: 2,
      remainingQty: 3,
      availableQty: 2.6,
      openAmount: 320,
      price: 106.66666667,
      netPnl: 5,
      roi: 0.015625,
      coreQty: 0.4,
      coreStatus: '部分设置',
      trades: rows
    }))
  })

  it('uses the trade id as a safe grouping key when an order id is missing', () => {
    expect(Stats.methods.coreActionOrderKey({ orderId: '88', tradeId: '11' })).toBe('order:88')
    expect(Stats.methods.coreActionOrderKey({ orderId: null, tradeId: '11' })).toBe('trade:11')
  })

  it('keeps the drawer open and defers statistics refresh until it closes', async() => {
    const row = {
      corePositionId: '9',
      coreQty: 0.0014,
      remainingQty: 0.0014,
      availableQty: 0
    }
    const vm = {
      showCoreActions: true,
      coreActionsDirty: false,
      coreActionRows: [row],
      selectedCoreRange: {
        range: '67500-70000',
        coreCount: 1,
        coreQty: 0.0014,
        availableQty: 0
      },
      coreAvailableQty: Stats.methods.coreRawAvailableQty,
      refreshSelectedCoreRangeSummary: Stats.methods.refreshSelectedCoreRangeSummary,
      doStats: jest.fn()
    }

    await Stats.methods.handleCorePositionMutation.call(vm, row, { releasedAt: '2026-09-20' }, 'release')

    expect(vm.showCoreActions).toBe(true)
    expect(vm.coreActionsDirty).toBe(true)
    expect(row).toEqual(expect.objectContaining({
      corePositionId: null,
      coreQty: 0,
      availableQty: 0.0014
    }))
    expect(vm.selectedCoreRange).toEqual(expect.objectContaining({
      coreCount: 0,
      coreQty: 0,
      availableQty: 0.0014
    }))
    expect(vm.doStats).not.toHaveBeenCalled()

    Stats.methods.handleCoreActionDrawerClosed.call(vm)

    expect(vm.coreActionsDirty).toBe(false)
    expect(vm.doStats).toHaveBeenCalledTimes(1)
  })

  it('releases a core position from the drawer popover without opening a global dialog', async() => {
    const row = { corePositionId: '9' }
    const resource = { releasedAt: '2026-09-20 20:00:00' }
    releaseCorePositionApi.mockResolvedValue(resource)
    const vm = {
      $message: { success: jest.fn() },
      handleCorePositionMutation: jest.fn().mockResolvedValue()
    }

    await Stats.methods.confirmDrawerReleaseCorePosition.call(vm, row)

    expect(releaseCorePositionApi).toHaveBeenCalledWith('9')
    expect(vm.$message.success).toHaveBeenCalledWith('底仓已解除')
    expect(vm.handleCorePositionMutation).toHaveBeenCalledWith(row, resource, 'release')
  })

  it('releases every core position in the selected price bucket at once', async() => {
    const coreRows = [
      { corePositionId: '9' },
      { corePositionId: '10' }
    ]
    releaseAllCorePositionsApi.mockResolvedValue([])
    const vm = {
      coreActionCoreRows: coreRows,
      batchReleaseLoading: false,
      releaseCoreRows: Stats.methods.releaseCoreRows,
      handleCorePositionMutation: jest.fn().mockResolvedValue(),
      $message: { success: jest.fn() }
    }

    await Stats.methods.releaseAllCorePositions.call(vm)

    expect(releaseAllCorePositionsApi).toHaveBeenCalledWith(['9', '10'])
    expect(vm.handleCorePositionMutation).toHaveBeenCalledTimes(2)
    expect(vm.handleCorePositionMutation).toHaveBeenNthCalledWith(1, coreRows[0], null, 'release')
    expect(vm.handleCorePositionMutation).toHaveBeenNthCalledWith(2, coreRows[1], null, 'release')
    expect(vm.$message.success).toHaveBeenCalledWith('已解除 2 笔底仓')
    expect(vm.batchReleaseLoading).toBe(false)
  })

  it('locks every unlocked trade in the selected price bucket at once', async() => {
    const rows = [
      { tradeId: '11', remainingQty: 0.0014 },
      { tradeId: '12', remainingQty: 0.002 }
    ]
    const resources = [
      { id: '101', tradeId: '11', coreQty: 0.0014 },
      { id: '102', tradeId: '12', coreQty: 0.002 }
    ]
    lockAllCorePositionsApi.mockResolvedValue(resources)
    const vm = {
      query: { uid: 7, symbol: 'BTCUSDT' },
      coreActionUnlockedRows: rows,
      batchLockLoading: false,
      lockCoreRows: Stats.methods.lockCoreRows,
      handleCorePositionMutation: jest.fn().mockResolvedValue(),
      $message: { success: jest.fn() }
    }

    await Stats.methods.lockAllCorePositions.call(vm)

    expect(lockAllCorePositionsApi).toHaveBeenCalledWith({
      uid: 7,
      symbol: 'BTCUSDT',
      tradeIds: ['11', '12']
    })
    expect(vm.handleCorePositionMutation).toHaveBeenCalledTimes(2)
    expect(vm.handleCorePositionMutation).toHaveBeenNthCalledWith(1, rows[0], resources[0], 'lock')
    expect(vm.handleCorePositionMutation).toHaveBeenNthCalledWith(2, rows[1], resources[1], 'lock')
    expect(vm.$message.success).toHaveBeenCalledWith('已设置 2 笔底仓')
    expect(vm.batchLockLoading).toBe(false)
  })

  it('syncs only the selected account and symbol before refreshing stats', async() => {
    crudBinanceTradeInfo.syncSelected.mockResolvedValue({
      spotCount: 2
    })
    const vm = {
      query: { uid: 7, symbol: 'BTCUSDT' },
      syncLoading: false,
      doStats: jest.fn(),
      refreshSpotOpenOrdersIfVisible: jest.fn(),
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
      title: '现货同步成功：新增 2 条成交'
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

  it('separates important mobile summary metrics from the detail grid', () => {
    const tradeSummaryItems = [
      { label: '买入总额' },
      { label: '收益率' },
      { label: '净盈亏' },
      { label: '持仓总额' },
      { label: '手续费' }
    ]

    expect(Stats.computed.mobileTradeSummaryHighlights.call({ tradeSummaryItems }).map(item => item.label))
      .toEqual(['净盈亏', '收益率', '持仓总额'])
    expect(Stats.computed.mobileTradeSummaryDetails.call({ tradeSummaryItems }).map(item => item.label))
      .toEqual(['买入总额', '手续费'])
  })

  it('calculates the remaining quantity displayed on mobile open-order cards', () => {
    expect(Stats.methods.spotOpenOrderRemainingQty({ origQty: '0.01000000', executedQty: '0.00400000' }))
      .toBe(0.006)
    expect(Stats.methods.spotOpenOrderRemainingQty({ origQty: '0.00400000', executedQty: '0.00600000' }))
      .toBe(0)
  })

  it('shows the trigger and commission prices for a position sell order', () => {
    const vm = {
      decimalValue: Stats.methods.decimalValue,
      spotOpenOrderTypeLabel: Stats.methods.spotOpenOrderTypeLabel,
      spotOpenOrderPriceLabel: Stats.methods.spotOpenOrderPriceLabel
    }

    expect(Stats.methods.spotPendingOrderPriceLabel.call(vm, {
      type: 'TAKE_PROFIT_LIMIT',
      stopPrice: '92000',
      pegPriceType: 'MARKET_PEG',
      peggedPrice: '91998.5'
    })).toBe('限价止盈 92,000.00 → 对手价1（91,998.50000000）')
    expect(Stats.methods.spotPendingOrderPriceLabel.call(vm, {
      type: 'STOP_LOSS_LIMIT',
      stopPrice: '78000',
      price: '77950'
    })).toBe('限价止损 78,000.00 → 77,950.00000000')
  })

  it('cancels an order directly from the current open-orders panel', async() => {
    crudBinanceTradeInfo.cancelSpotOrder.mockResolvedValue({})
    const vm = {
      query: { uid: 7, symbol: 'BTCUSDT' },
      spotOrderCancellingIds: [],
      sameId: Stats.methods.sameId,
      isSpotOrderCancelling: Stats.methods.isSpotOrderCancelling,
      loadSpotOpenOrders: jest.fn().mockResolvedValue(),
      $message: { success: jest.fn() }
    }

    await Stats.methods.cancelSpotOpenOrder.call(vm, { orderId: '66813689167' })

    expect(crudBinanceTradeInfo.cancelSpotOrder).toHaveBeenCalledWith({
      uid: 7,
      symbol: 'BTCUSDT',
      orderId: '66813689167'
    })
    expect(vm.$message.success).toHaveBeenCalledWith('挂单 66813689167 已撤销')
    expect(vm.loadSpotOpenOrders).toHaveBeenCalledTimes(1)
    expect(vm.spotOrderCancellingIds).toEqual([])
  })
})
