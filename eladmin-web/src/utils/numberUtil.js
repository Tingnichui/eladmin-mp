import Decimal from 'decimal.js'

export function convertAmountToCent(value) {
  return value !== null && value !== undefined ? Number((value * 100000000).toFixed(0)) : value
}
export function convertAmountToYuan(value) {
  return value !== null && value !== undefined ? Number((value / 100000000).toFixed(8)) : value
}

export function formatPercent(val) {
  return val != null ? (val * 100).toFixed(2) + '%' : '--'
}

export function addAmount(a, b, fixed = null) {
  const result = new Decimal(a || 0).plus(new Decimal(b || 0))
  return fixed != null ? Number(result.toFixed(fixed)) : Number(result)
}

export function subAmount(a, b, fixed = null) {
  const result = new Decimal(a || 0).sub(new Decimal(b || 0))
  return fixed != null ? Number(result.toFixed(fixed)) : Number(result)
}

export function divAmount(a, b, fixed = null) {
  const num1 = new Decimal(a || 0)
  const num2 = new Decimal(b || 0)
  if (num2.isZero()) return 0
  const result = num1.div(num2)
  return fixed != null ? Number(result.toFixed(fixed)) : Number(result)
}

export function mulAmount(a, b, fixed = null) {
  const result = new Decimal(a || 0).mul(new Decimal(b || 0))
  return fixed != null ? Number(result.toFixed(fixed)) : Number(result)
}

export function formatDecimal(val) {
  return val != null ? Number(val).toFixed(4) : '--'
}

export function formatByType(value, type) {
  if (!value) {
    return ''
  }
  if (type === 'percent') return this.formatPercent(value)
  if (type === 'length') return value.length
  return this.formatDecimal(value)
}
