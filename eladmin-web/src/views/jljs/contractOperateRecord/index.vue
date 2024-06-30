<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">合同操作类型</label>
        <el-input v-model="query.contractOperateType" clearable placeholder="合同操作类型" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">操作状态；1成功2撤销</label>
        <el-input v-model="query.operateStatus" clearable placeholder="操作状态；1成功2撤销" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.operateBeginDate"
          start-placeholder="operateBeginDateStart"
          end-placeholder="operateBeginDateStart"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="合同id">
            <el-input v-model="form.contractInfoId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="合同操作类型">
            <el-select v-model="form.contractOperateType" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.jljs_contract_operate_type"
                :key="item.id"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="间隔天数">
            <el-input v-model="form.intervalDays" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="form.operateBeginDate" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker v-model="form.operateEndDate" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="操作原因">
            <el-input v-model="form.operateReason" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="操作金额">
            <el-input v-model="form.operateAmount" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="操作状态；1成功2撤销" prop="operateStatus">
            <el-input v-model="form.operateStatus" style="width: 370px;" />
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
        <el-table-column prop="contractInfoId" label="合同id" />
        <el-table-column prop="contractOperateType" label="合同操作类型">
          <template slot-scope="scope">
            {{ dict.label.jljs_contract_operate_type[scope.row.contractOperateType] }}
          </template>
        </el-table-column>
        <el-table-column prop="intervalDays" label="间隔天数" />
        <el-table-column prop="operateBeginDate" label="开始时间" />
        <el-table-column prop="operateEndDate" label="结束时间" />
        <el-table-column prop="operateReason" label="操作原因" />
        <el-table-column prop="operateAmount" label="操作金额" />
        <el-table-column prop="operateStatus" label="操作状态；1成功2撤销">
          <template slot-scope="scope">
            {{ dict.label.jljs_operate_status[scope.row.operateStatus] }}
          </template>
        </el-table-column>
        <el-table-column v-if="checkPer(['admin','jljsContractOperateRecord:edit','jljsContractOperateRecord:del'])" label="操作" width="150px" align="center">
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
import crudJljsContractOperateRecord from '@/api/jljsContractOperateRecord'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, createBy: null, createTime: null, updateBy: null, updateTime: null, delFlag: null, contractInfoId: null, contractOperateType: null, intervalDays: null, operateBeginDate: null, operateEndDate: null, operateReason: null, operateAmount: null, operateStatus: null }
export default {
  name: 'JljsContractOperateRecord',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['jljs_contract_operate_type', 'jljs_operate_status'],
  cruds() {
    return CRUD({ title: '合同操作记录', url: 'api/jljsContractOperateRecord', idField: 'id', sort: 'id,desc', crudMethod: { ...crudJljsContractOperateRecord }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'jljsContractOperateRecord:add'],
        edit: ['admin', 'jljsContractOperateRecord:edit'],
        del: ['admin', 'jljsContractOperateRecord:del']
      },
      rules: {
        operateStatus: [
          { required: true, message: '操作状态；1成功2撤销不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'contractOperateType', display_name: '合同操作类型' },
        { key: 'operateStatus', display_name: '操作状态；1成功2撤销' }
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
