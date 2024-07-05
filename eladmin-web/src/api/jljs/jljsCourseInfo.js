import request from '@/utils/request'

export function add(data) {
  return request({
    url: 'api/jljsCourseInfo',
    method: 'post',
    data
  })
}

export function del(ids) {
  return request({
    url: 'api/jljsCourseInfo/',
    method: 'delete',
    data: ids
  })
}

export function edit(data) {
  return request({
    url: 'api/jljsCourseInfo',
    method: 'put',
    data
  })
}

export function listAllCourse(params) {
  return request({
    url: 'api/jljsCourseInfo',
    method: 'get',
    params: {
      size: -1,
      ...params
    }
  })
}

export default { add, edit, del }
