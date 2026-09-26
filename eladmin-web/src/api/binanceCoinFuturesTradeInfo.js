import request from '@/utils/request'

export function stats(params) {
  return request({
    url: 'api/binanceCoinFuturesTradeInfo/stats',
    method: 'get',
    params
  })
}

export function positionStats(params) {
  return request({
    url: 'api/binanceCoinFuturesTradeInfo/stats/position',
    method: 'get',
    params,
    timeout: 15000
  })
}

export function accountAssets(params) {
  return request({
    url: 'api/binanceCoinFuturesTradeInfo/stats/account',
    method: 'get',
    params,
    timeout: 15000
  })
}

export function closedSummary(params) {
  return request({
    url: 'api/binanceCoinFuturesTradeInfo/stats/closed-summary',
    method: 'get',
    params,
    timeout: 15000
  })
}

export function syncSelected(params) {
  return request({
    url: 'api/binanceCoinFuturesTradeInfo/syncSelected',
    method: 'put',
    params
  })
}

export function placeOrder(data) {
  return request({
    url: 'api/binanceCoinFuturesTradeInfo/order',
    method: 'post',
    data
  })
}

export function openOrders(params) {
  return request({
    url: 'api/binanceCoinFuturesTradeInfo/open-orders',
    method: 'get',
    params
  })
}

export function queryOrder(params) {
  return request({
    url: 'api/binanceCoinFuturesTradeInfo/order',
    method: 'get',
    params
  })
}

export function cancelOrder(params) {
  return request({
    url: 'api/binanceCoinFuturesTradeInfo/order',
    method: 'delete',
    params
  })
}

export default { stats, positionStats, accountAssets, closedSummary, syncSelected, placeOrder, openOrders, queryOrder, cancelOrder }
