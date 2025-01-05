export function convertAmountToCent(value) {
  return value !== null && value !== undefined ? Number((value * 100).toFixed(0)) : value
}
export function convertAmountToYuan(value) {
  return value !== null && value !== undefined ? Number((value / 100).toFixed(2)) : value
}
