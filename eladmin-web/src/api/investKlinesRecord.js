import request from '@/utils/request'

export function add(data) {
  return request({
    url: 'api/investKlinesRecord',
    method: 'post',
    data
  })
}

export function del(ids) {
  return request({
    url: 'api/investKlinesRecord/',
    method: 'delete',
    data: ids
  })
}

export function edit(data) {
  return request({
    url: 'api/investKlinesRecord',
    method: 'put',
    data
  })
}

export function price(symbol) {
  return request({
    url: 'api/investKlinesRecord/price',
    method: 'get',
    params: { symbol }
  })
}

export default { add, edit, del }
