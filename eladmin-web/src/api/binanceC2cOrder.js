import request from '@/utils/request'

export function sync(uid) {
  return request({
    url: 'api/binanceC2cOrder/sync',
    method: 'put',
    params: { uid }
  })
}

export function add(data) {
  return request({
    url: 'api/binanceC2cOrder',
    method: 'post',
    data
  })
}

export function del(ids) {
  return request({
    url: 'api/binanceC2cOrder/',
    method: 'delete',
    data: ids
  })
}

export function edit(data) {
  return request({
    url: 'api/binanceC2cOrder',
    method: 'put',
    data
  })
}

export default { add, edit, del, sync }
