<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">教练姓名</label>
        <el-input v-model="query.coachName" clearable placeholder="教练姓名" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">电话号码</label>
        <el-input v-model="query.coachPhoneNum" clearable placeholder="电话号码" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="教练姓名" prop="coachName">
            <el-input v-model="form.coachName" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="电话号码">
            <el-input v-model="form.coachPhoneNum" style="width: 370px;" />
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
        <el-table-column prop="coachName" label="教练姓名" />
        <el-table-column prop="coachPhoneNum" label="电话号码" />
        <el-table-column v-if="checkPer(['admin','jljsCoachInfo:edit','jljsCoachInfo:del'])" label="操作" width="150px" align="center">
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
import crudJljsCoachInfo from '@/api/jljs/jljsCoachInfo'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, createBy: null, createTime: null, updateBy: null, updateTime: null, delFlag: null, coachName: null, coachPhoneNum: null }
export default {
  name: 'JljsCoachInfo',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '教练管理', url: 'api/jljsCoachInfo', idField: 'id', sort: 'id,desc', crudMethod: { ...crudJljsCoachInfo }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'jljsCoachInfo:add'],
        edit: ['admin', 'jljsCoachInfo:edit'],
        del: ['admin', 'jljsCoachInfo:del']
      },
      rules: {
        coachName: [
          { required: true, message: '教练姓名不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'coachName', display_name: '教练姓名' },
        { key: 'coachPhoneNum', display_name: '电话号码' }
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
