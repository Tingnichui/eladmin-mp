
// 计算两个时间差  返回天小时分钟
export function timeDiff(begin_time, end_time) {
  // 年月日时分秒转换为时间戳
  const beginTime = (new Date(begin_time).getTime()) / 1000
  const endTime = (new Date(end_time).getTime()) / 1000
  var starttime = ''
  var endtime = ''
  if (beginTime < endTime) {
    starttime = beginTime
    endtime = endTime
  } else {
    starttime = endTime
    endtime = beginTime
  }
  // 计算天数
  var remain = endtime - starttime
  var days = parseInt(remain / 86400)
  // 计算小时数
  remain = remain % 86400
  var hours = parseInt(remain / 3600)
  // 计算分钟数
  remain = remain % 3600
  var mins = parseInt(remain / 60)
  return days + '天' + hours + '小时' + mins + '分'
}
