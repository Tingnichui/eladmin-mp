import request from '@/utils/request'

export function add(data) {
  return request({
    url: 'api/binanceAccountInfo',
    method: 'post',
    data
  })
}

export function del(ids) {
  return request({
    url: 'api/binanceAccountInfo/',
    method: 'delete',
    data: ids
  })
}

export function edit(data) {
  return request({
    url: 'api/binanceAccountInfo',
    method: 'put',
    data
  })
}

export function changeAutoTradeFlag(id) {
  return request({
    url: 'api/binanceAccountInfo/changeAutoTradeFlag',
    method: 'put',
    data: id
  })
}

export function listAllAccount(params) {
  return request({
    url: 'api/binanceAccountInfo',
    method: 'get',
    params: {
      size: -1,
      ...params
    }
  })
}

export default { add, edit, del }
