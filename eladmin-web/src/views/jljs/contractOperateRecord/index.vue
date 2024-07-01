<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <!-- <label class="el-form-item-label">合同id</label> -->
        <!-- <el-input v-model="query.contractInfoId" clearable placeholder="合同id" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" /> -->
        <label class="el-form-item-label">操作类型</label>
        <el-select
          v-model="query.contractOperateType"
          clearable
          size="small"
          placeholder="操作类型"
          class="filter-item"
          style="width: 90px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.jljs_contract_operate_type"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">状态</label>
        <el-select
          v-model="query.operateStatus"
          clearable
          size="small"
          placeholder="状态"
          class="filter-item"
          style="width: 90px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.jljs_operate_status"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="合同id" prop="contractInfoId">
            <el-input v-model="form.contractInfoId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="操作类型" prop="contractOperateType">
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
          <el-form-item label="状态">
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
        <!-- <el-table-column prop="contractInfoId" label="合同id" /> -->
        <el-table-column prop="memberName" label="会员" />
        <el-table-column prop="courseName" label="课程" />
        <el-table-column prop="contractOperateType" label="操作类型">
          <template slot-scope="scope">
            {{ dict.label.jljs_contract_operate_type[scope.row.contractOperateType] }}
          </template>
        </el-table-column>
        <el-table-column prop="operateBeginDate" label="开始时间" />
        <el-table-column prop="operateEndDate" label="结束时间" />
        <el-table-column prop="intervalDays" label="间隔天数" />
        <el-table-column prop="operateAmount" label="操作金额" />
        <el-table-column prop="operateReason" label="操作原因" />
        <el-table-column prop="operateStatus" label="状态">
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
        contractInfoId: [
          { required: true, message: '合同id不能为空', trigger: 'blur' }
        ],
        contractOperateType: [
          { required: true, message: '合同操作类型不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'contractInfoId', display_name: '合同id' },
        { key: 'contractOperateType', display_name: '合同操作类型' },
        { key: 'operateStatus', display_name: '状态' }
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
