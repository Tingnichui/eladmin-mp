<template>
  <div class="app-container">
    <el-alert title="账号由 H5 注册创建，密码和启停在系统用户页面管理；身份与认证状态不能在此手工修改。" type="info" :closable="false" />
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <el-input v-model="query.realName" clearable placeholder="真实姓名" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <el-input v-model="query.mobile" clearable placeholder="手机号" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker v-model="query.createTime" class="date-item" />
        <rrOperation :crud="crud" />
      </div>
      <crudOperation :permission="permission" />
      <el-table ref="table" v-loading="crud.loading" :data="crud.data" size="small">
        <el-table-column prop="sysUserId" label="系统用户ID" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="mobile" label="手机号" />
        <el-table-column label="二要素认证">
          <template slot-scope="scope">{{ scope.row.idVerifyStatus === 2 ? '已通过' : '未通过' }}</template>
        </el-table-column>
        <el-table-column prop="idVerifiedAt" label="二要素认证时间" />
        <el-table-column label="三要素认证">
          <template slot-scope="scope">{{ dict.label.iou_user_verify_status[scope.row.verifyStatus] }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
      </el-table>
      <pagination />
    </div>
  </div>
</template>

<script>
import CRUD, { presenter, header, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

export default {
  name: 'IouUser',
  components: { DateRangePicker, pagination, crudOperation, rrOperation },
  mixins: [presenter(), header(), crud()],
  dicts: ['iou_user_verify_status'],
  cruds() {
    return CRUD({
      title: '借条用户',
      url: 'api/iouUser',
      idField: 'id',
      sort: 'id,desc',
      optShow: { add: false, edit: false, del: false, download: true }
    })
  },
  data() {
    return { permission: { add: [], edit: [], del: [] }}
  }
}
</script>
