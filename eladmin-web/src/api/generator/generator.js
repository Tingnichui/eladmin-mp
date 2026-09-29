import request from '@/utils/request'

export function generator(tableName, type, dataSource = 'master') {
  return request({
    url: 'api/generator/' + tableName + '/' + type,
    method: 'post',
    params: { dataSource },
    responseType: type === 2 ? 'blob' : ''
  })
}

export function save(data) {
  return request({
    url: 'api/generator',
    data,
    method: 'put'
  })
}

export function sync(dataSource, tables) {
  return request({
    url: 'api/generator/sync',
    method: 'post',
    data: { dataSource, tables }
  })
}

