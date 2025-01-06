export function convertAmountToCent(value) {
  return value !== null && value !== undefined ? Number((value * 100000000).toFixed(0)) : value
}
export function convertAmountToYuan(value) {
  return value !== null && value !== undefined ? Number((value / 100000000).toFixed(8)) : value
}
