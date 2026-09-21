import request from '@/utils/request'

export function add(data) {
  return request({
    url: 'api/binanceTradeInfo',
    method: 'post',
    data
  })
}

export function del(ids) {
  return request({
    url: 'api/binanceTradeInfo/',
    method: 'delete',
    data: ids
  })
}

export function edit(data) {
  return request({
    url: 'api/binanceTradeInfo',
    method: 'put',
    data
  })
}

export function syncSpotTradeInfo() {
  return request({
    url: 'api/binanceTradeInfo/syncSpotTradeInfo',
    method: 'put'
  })
}

export function syncSelected(params) {
  return request({
    url: 'api/binanceTradeInfo/syncSelected',
    method: 'put',
    params
  })
}

export function syncFuturesHedge() {
  return request({
    url: 'api/binanceTradeInfo/syncFuturesHedge',
    method: 'put'
  })
}

export function stats(params) {
  return request({
    url: 'api/binanceTradeInfo/stats',
    method: 'get',
    params
  })
}

export function createSpotOrder(data) {
  return request({
    url: 'api/binanceTradeInfo/spot/order',
    method: 'post',
    data
  })
}

export function listSpotOpenOrders(params) {
  return request({
    url: 'api/binanceTradeInfo/spot/open-orders',
    method: 'get',
    params
  })
}

export default { add, edit, del, syncSpotTradeInfo, syncSelected, stats, syncFuturesHedge, createSpotOrder, listSpotOpenOrders }
