import request from '@/utils/request'
import axios from 'axios'
import { getToken } from '@/utils/auth'

export function add(data) {
  return request({
    url: 'api/investTradeAnalysis',
    method: 'post',
    data
  })
}

export function del(ids) {
  return request({
    url: 'api/investTradeAnalysis/',
    method: 'delete',
    data: ids
  })
}

export function edit(data) {
  return request({
    url: 'api/investTradeAnalysis',
    method: 'put',
    data
  })
}

export function ocrRecognize(file) {
  const data = new FormData()
  data.append('file', file)
  return axios.post('/api/investTradeAnalysis/ocr', data, {
    headers: { 'Authorization': getToken() }
  }).then(res => res.data)
}

export default { add, edit, del, ocrRecognize }
