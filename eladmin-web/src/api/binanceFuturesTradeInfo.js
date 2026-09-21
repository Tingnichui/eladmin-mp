import request from '@/utils/request'

export function stats(params) {
  return request({
    url: 'api/binanceFuturesTradeInfo/stats',
    method: 'get',
    params
  })
}

export function syncSelected(params) {
  return request({
    url: 'api/binanceFuturesTradeInfo/syncSelected',
    method: 'put',
    params
  })
}

export default { stats, syncSelected }
