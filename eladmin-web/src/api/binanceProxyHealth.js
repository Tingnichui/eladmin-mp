import request from '@/utils/request'

export function checkProxyHealth() {
  return request({
    url: 'api/binanceProxyHealth',
    method: 'get'
  })
}

export default { checkProxyHealth }
