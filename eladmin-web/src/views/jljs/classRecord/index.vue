<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">教练id</label>
        <el-input v-model="query.coachId" clearable placeholder="教练id" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="教练id">
            未设置字典，请手动设置 Select
          </el-form-item>
          <el-form-item label="会员id" prop="memberId">
            未设置字典，请手动设置 Select
          </el-form-item>
          <el-form-item label="关联合同id">
            未设置字典，请手动设置 Select
          </el-form-item>
          <el-form-item label="课程开始时间">
            <el-date-picker v-model="form.classBeginTime" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="课程结束时间">
            <el-date-picker v-model="form.classEndTime" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="课程备注">
            <el-input v-model="form.classRemark" style="width: 370px;" />
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button type="text" @click="crud.cancelCU">取消</el-button>
          <el-button :loading="crud.status.cu === 2" type="primary" @click="crud.submitCU">确认</el-button>
        </div>
      </el-dialog>
      <!--表格渲染-->
      <el-table ref="table" v-loading="crud.loading" :data="crud.data" size="small" style="width: 100%;" @selection-change="crud.selectionChangeHandler">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="coachId" label="教练id" />
        <el-table-column prop="memberId" label="会员id" />
        <el-table-column prop="contractInfoId" label="关联合同id" />
        <el-table-column prop="classBeginTime" label="课程开始时间" />
        <el-table-column prop="classEndTime" label="课程结束时间" />
        <el-table-column prop="classRemark" label="课程备注" />
        <el-table-column v-if="checkPer(['admin','jljsClassRecord:edit','jljsClassRecord:del'])" label="操作" width="150px" align="center">
          <template slot-scope="scope">
            <udOperation
              :data="scope.row"
              :permission="permission"
            />
          </template>
        </el-table-column>
      </el-table>
      <!--分页组件-->
      <pagination />
    </div>
  </div>
</template>

<script>
import crudJljsClassRecord from '@/api/jljsClassRecord'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, createBy: null, createTime: null, updateBy: null, updateTime: null, delFlag: null, coachId: null, memberId: null, contractInfoId: null, classBeginTime: null, classEndTime: null, classRemark: null }
export default {
  name: 'JljsClassRecord',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '上课记录', url: 'api/jljsClassRecord', idField: 'id', sort: 'id,desc', crudMethod: { ...crudJljsClassRecord }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'jljsClassRecord:add'],
        edit: ['admin', 'jljsClassRecord:edit'],
        del: ['admin', 'jljsClassRecord:del']
      },
      rules: {
        memberId: [
          { required: true, message: '会员id不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'coachId', display_name: '教练id' }
      ]
    }
  },
  methods: {
    // 钩子：在获取表格数据之前执行，false 则代表不获取数据
    [CRUD.HOOK.beforeRefresh]() {
      return true
    }
  }
}
</script>

<style scoped>

</style>
