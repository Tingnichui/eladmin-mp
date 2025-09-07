import request from '@/utils/request'

export function add(data) {
  return request({
    url: 'api/binanceTradeInfoExt',
    method: 'post',
    data
  })
}

export function del(ids) {
  return request({
    url: 'api/binanceTradeInfoExt/',
    method: 'delete',
    data: ids
  })
}

export function edit(data) {
  return request({
    url: 'api/binanceTradeInfoExt',
    method: 'put',
    data
  })
}

export function changeHedgedFlag(orderId) {
  return request({
    url: 'api/binanceTradeInfoExt/changeHedgedFlag',
    method: 'put',
    data: orderId
  })
}

export default { add, edit, del, changeHedgedFlag }
