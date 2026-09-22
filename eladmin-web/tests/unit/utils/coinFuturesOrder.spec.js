import { calculateCoinQuantity, calculateInitialMarginCoin } from '@/utils/coinFuturesOrder'

describe('coin futures order display calculations', () => {
  test('calculates BTC represented by contract quantity', () => {
    expect(calculateCoinQuantity(1, 100, 85476)).toBeCloseTo(0.001169919, 8)
    expect(calculateCoinQuantity(2, 100, 85493.2)).toBeCloseTo(0.002339369, 8)
  })

  test('calculates estimated initial margin using leverage', () => {
    expect(calculateInitialMarginCoin(10, 100, 100000, 10)).toBeCloseTo(0.001, 9)
  })

  test('returns null when required market data is unavailable', () => {
    expect(calculateCoinQuantity(1, null, 85476)).toBeNull()
    expect(calculateInitialMarginCoin(1, 100, 85476, 0)).toBeNull()
  })
})
