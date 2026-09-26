/* eslint-env jest */
import { formatQuantity } from '@/utils/numberUtil'

describe('number quantity formatting', () => {
  it('keeps up to eight decimal places without trailing zeroes', () => {
    expect(formatQuantity('0.00400000')).toBe('0.004')
    expect(formatQuantity('0.12345678')).toBe('0.12345678')
    expect(formatQuantity('0.00000000')).toBe('0')
  })

  it('keeps missing and invalid quantities distinguishable', () => {
    expect(formatQuantity(null)).toBe('--')
    expect(formatQuantity('')).toBe('--')
    expect(formatQuantity('invalid')).toBe('--')
  })
})
