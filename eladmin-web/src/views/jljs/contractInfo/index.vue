<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">会员</label>
        <el-input v-model="query.memberId" clearable placeholder="会员" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">开单教练</label>
        <el-input v-model="query.belongCoachId" clearable placeholder="开单教练" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">合同状态</label>
        <el-select
          v-model="query.contractStatus"
          clearable
          size="small"
          placeholder="合同状态"
          class="filter-item"
          style="width: 90px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.jljs_contract_status"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">备注</label>
        <el-input v-model="query.contractRemark" clearable placeholder="备注" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="会员" prop="memberId">
            未设置字典，请手动设置 Select
          </el-form-item>
          <el-form-item label="开单教练" prop="belongCoachId">
            未设置字典，请手动设置 Select
          </el-form-item>
          <el-form-item label="合同金额" prop="contractAmount">
            <el-input v-model="form.contractAmount" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.contractRemark" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="实际收取金额" prop="actualChargeAmount">
            <el-input v-model="form.actualChargeAmount" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="课程" prop="courseInfoId">
            <el-input v-model="form.courseInfoId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="课程类型" prop="courseType">
            <el-radio v-for="item in dict.jljs_course_type" :key="item.id" v-model="form.courseType" :label="item.value">{{ item.label }}</el-radio>
          </el-form-item>
          <el-form-item label="使用期限" prop="courseUsePeriodDays">
            <el-input v-model="form.courseUsePeriodDays" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="可使用数量" prop="courseAvailableQuantity">
            <el-input v-model="form.courseAvailableQuantity" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="剩余数量">
            <el-input v-model="form.courseRemainQuantity" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="已使用数量">
            <el-input v-model="form.courseUseQuantity" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="总暂停天数">
            <el-input v-model="form.courseTotalStopDays" style="width: 370px;" />
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
        <el-table-column prop="memberId" label="会员" />
        <el-table-column prop="belongCoachId" label="开单教练" />
        <el-table-column prop="contractAmount" label="合同金额" />
        <el-table-column prop="contractStatus" label="合同状态">
          <template slot-scope="scope">
            {{ dict.label.jljs_contract_status[scope.row.contractStatus] }}
          </template>
        </el-table-column>
        <el-table-column prop="useBeginDate" label="开始日期" />
        <el-table-column prop="useEndDate" label="结束日期" />
        <el-table-column prop="buyTime" label="购买日期" />
        <el-table-column prop="contractRemark" label="备注" />
        <el-table-column prop="actualChargeAmount" label="实际收取金额" />
        <el-table-column prop="courseInfoId" label="课程" />
        <el-table-column prop="courseType" label="课程类型">
          <template slot-scope="scope">
            {{ dict.label.jljs_course_type[scope.row.courseType] }}
          </template>
        </el-table-column>
        <el-table-column prop="courseUsePeriodDays" label="使用期限" />
        <el-table-column prop="courseAvailableQuantity" label="可使用数量" />
        <el-table-column prop="courseRemainQuantity" label="剩余数量" />
        <el-table-column prop="courseUseQuantity" label="已使用数量" />
        <el-table-column prop="courseTotalStopDays" label="总暂停天数" />
        <el-table-column v-if="checkPer(['admin','jljsContractInfo:edit','jljsContractInfo:del'])" label="操作" width="150px" align="center">
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
import crudJljsContractInfo from '@/api/jljsContractInfo'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, createBy: null, createTime: null, updateBy: null, updateTime: null, delFlag: null, memberId: null, belongCoachId: null, contractAmount: null, contractStatus: null, useBeginDate: null, useEndDate: null, buyTime: null, contractRemark: null, actualChargeAmount: null, courseInfoId: null, courseType: null, courseUsePeriodDays: null, courseAvailableQuantity: null, courseRemainQuantity: null, courseUseQuantity: null, courseTotalStopDays: null }
export default {
  name: 'JljsContractInfo',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['jljs_contract_status', 'jljs_course_type'],
  cruds() {
    return CRUD({ title: '合同管理', url: 'api/jljsContractInfo', idField: 'id', sort: 'id,desc', crudMethod: { ...crudJljsContractInfo }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'jljsContractInfo:add'],
        edit: ['admin', 'jljsContractInfo:edit'],
        del: ['admin', 'jljsContractInfo:del']
      },
      rules: {
        memberId: [
          { required: true, message: '会员不能为空', trigger: 'blur' }
        ],
        belongCoachId: [
          { required: true, message: '开单教练不能为空', trigger: 'blur' }
        ],
        contractAmount: [
          { required: true, message: '合同金额不能为空', trigger: 'blur' }
        ],
        actualChargeAmount: [
          { required: true, message: '实际收取金额不能为空', trigger: 'blur' }
        ],
        courseInfoId: [
          { required: true, message: '课程不能为空', trigger: 'blur' }
        ],
        courseType: [
          { required: true, message: '课程类型不能为空', trigger: 'blur' }
        ],
        courseUsePeriodDays: [
          { required: true, message: '使用期限不能为空', trigger: 'blur' }
        ],
        courseAvailableQuantity: [
          { required: true, message: '可使用数量不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'memberId', display_name: '会员' },
        { key: 'belongCoachId', display_name: '开单教练' },
        { key: 'contractStatus', display_name: '合同状态' },
        { key: 'contractRemark', display_name: '备注' }
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
