<template>
  <div class="app-container">
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <label class="el-form-item-label">真实姓名</label>
        <el-input v-model="query.realName" clearable placeholder="真实姓名" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">手机号</label>
        <el-input v-model="query.mobile" clearable placeholder="手机号" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker v-model="query.createTime" class="date-item" />
        <rrOperation :crud="crud" />
      </div>
      <crudOperation :permission="permission" />
      <el-table ref="table" v-loading="crud.loading" :data="crud.data" size="small">
        <el-table-column prop="sysUserId" label="系统用户ID" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column label="身份证" min-width="230">
          <template slot-scope="scope">
            <span>{{ identityValues[scope.row.id] || scope.row.maskedIdNo || '暂无' }}</span>
            <el-button v-if="identityValues[scope.row.id]" type="text" size="mini" @click="hideIdentity(scope.row.id)">隐藏</el-button>
            <el-button v-else-if="scope.row.maskedIdNo && checkPer(['admin', 'iouUser:viewIdentity'])" type="text" size="mini" :loading="!!identityLoading[scope.row.id]" @click="revealIdentity(scope.row)">查看</el-button>
          </template>
        </el-table-column>
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
import { revealIdentity } from '@/api/iouUser'

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
    return {
      permission: { add: [], edit: [], del: [] },
      identityValues: {},
      identityLoading: {},
      identityGeneration: 0
    }
  },
  deactivated() {
    this.clearIdentities()
  },
  beforeDestroy() {
    this.clearIdentities()
  },
  beforeRouteLeave(to, from, next) {
    this.clearIdentities()
    next()
  },
  methods: {
    [CRUD.HOOK.beforeRefresh]() {
      this.clearIdentities()
    },
    clearIdentities() {
      this.identityGeneration++
      this.identityValues = {}
      this.identityLoading = {}
    },
    hideIdentity(id) {
      this.$delete(this.identityValues, id)
    },
    async revealIdentity(row) {
      const id = row.id
      if (this.identityLoading[id]) return
      const generation = this.identityGeneration
      this.$set(this.identityLoading, id, true)
      try {
        const result = await revealIdentity(id)
        // 查询、翻页或离开后到达的旧响应不能重新显示明文。
        if (generation === this.identityGeneration) {
          this.$set(this.identityValues, id, result.idNo)
        }
      } catch (error) {
        // 请求拦截器统一提示失败，列表继续保留脱敏号码。
      } finally {
        if (generation === this.identityGeneration) {
          this.$delete(this.identityLoading, id)
        }
      }
    }
  }
}
</script>
