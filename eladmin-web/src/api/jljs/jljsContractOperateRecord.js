import request from '@/utils/request'

export function add(data) {
  return request({
    url: 'api/jljsContractOperateRecord',
    method: 'post',
    data
  })
}

export function del(ids) {
  return request({
    url: 'api/jljsContractOperateRecord/',
    method: 'delete',
    data: ids
  })
}

export function edit(data) {
  return request({
    url: 'api/jljsContractOperateRecord',
    method: 'put',
    data
  })
}

export function revoke(id) {
  return request({
    url: 'api/jljsContractOperateRecord/revoke',
    method: 'put',
    data: id
  })
}

export default { add, edit, del, revoke }
