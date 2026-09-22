export function calculateCoinQuantity(contractQuantity, contractSize, price) {
  const quantity = Number(contractQuantity)
  const faceValue = Number(contractSize)
  const referencePrice = Number(price)
  if (![quantity, faceValue, referencePrice].every(value => Number.isFinite(value) && value > 0)) return null
  return quantity * faceValue / referencePrice
}

export function calculateInitialMarginCoin(contractQuantity, contractSize, price, leverage) {
  const coinQuantity = calculateCoinQuantity(contractQuantity, contractSize, price)
  const leverageValue = Number(leverage)
  if (coinQuantity == null || !Number.isFinite(leverageValue) || leverageValue <= 0) return null
  return coinQuantity / leverageValue
}
