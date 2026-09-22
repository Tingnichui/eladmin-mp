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

export default { stats, syncSelected }
