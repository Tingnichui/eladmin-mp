import request from '@/utils/request'

export function add(data) {
  return request({
    url: 'api/binanceSpotCorePosition',
    method: 'post',
    data: {
      uid: data.uid,
      symbol: data.symbol,
      tradeId: data.tradeId,
      coreQty: data.coreQty,
      remark: data.remark
    }
  })
}

export function del(ids) {
  return request({
    url: 'api/binanceSpotCorePosition/',
    method: 'delete',
    data: ids
  })
}

export function edit(data) {
  return request({
    url: `api/binanceSpotCorePosition/${data.id}`,
    method: 'put',
    data: {
      coreQty: data.coreQty,
      remark: data.remark
    }
  })
}

export function release(id) {
  return request({
    url: `api/binanceSpotCorePosition/${id}/release`,
    method: 'put'
  })
}

export function releaseAll(ids) {
  return request({
    url: 'api/binanceSpotCorePosition/batch-release',
    method: 'put',
    data: ids
  })
}

export function lockAll(data) {
  return request({
    url: 'api/binanceSpotCorePosition/batch-lock',
    method: 'post',
    data: {
      uid: data.uid,
      symbol: data.symbol,
      tradeIds: data.tradeIds
    }
  })
}

export function getCandidates(params) {
  return request({
    url: 'api/binanceSpotCorePosition/candidates',
    method: 'get',
    params
  })
}

export default { add, edit, del, release, releaseAll, lockAll, getCandidates }
