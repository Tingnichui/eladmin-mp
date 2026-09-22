import request from '@/utils/request'

export function stats(params) {
  return request({
    url: 'api/binanceCoinFuturesTradeInfo/stats',
    method: 'get',
    params
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

export default { stats, syncSelected, placeOrder, openOrders, queryOrder, cancelOrder }
