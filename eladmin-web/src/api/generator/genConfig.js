import request from '@/utils/request'

export function get(tableName, dataSource = 'master') {
  return request({
    url: 'api/genConfig/' + tableName,
    method: 'get',
    params: { dataSource }
  })
}

export function update(data) {
  return request({
    url: 'api/genConfig',
    data,
    method: 'put'
  })
}
