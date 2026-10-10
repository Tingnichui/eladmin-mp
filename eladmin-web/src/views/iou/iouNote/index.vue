<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">借条编号</label>
        <el-input v-model="query.noteNo" clearable placeholder="借条编号" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">发起人ID</label>
        <el-input v-model="query.creatorUserId" clearable placeholder="发起人ID" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">借款人</label>
        <el-input v-model="query.borrowerName" clearable placeholder="借款人" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">出借人</label>
        <el-input v-model="query.lenderName" clearable placeholder="出借人" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">借款类型</label>
        <el-input v-model="query.loanType" clearable placeholder="借款类型" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">借条状态</label>
        <el-input v-model="query.status" clearable placeholder="借条状态" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.dueDate"
          start-placeholder="dueDateStart"
          end-placeholder="dueDateEnd"
          class="date-item"
        />
        <date-range-picker
          v-model="query.createTime"
          start-placeholder="createTimeStart"
          end-placeholder="createTimeEnd"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="借条编号" prop="noteNo">
            <el-input v-model="form.noteNo" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="发起人ID" prop="creatorUserId">
            <el-input v-model="form.creatorUserId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="借款人" prop="borrowerName">
            <el-input v-model="form.borrowerName" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="借款人证件">
            <el-input v-model="form.borrowerIdNo" type="password" autocomplete="off" placeholder="新增必填，编辑留空保留原证件" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="出借人证件">
            <el-input v-model="form.lenderIdNo" type="password" autocomplete="off" placeholder="新增必填，编辑留空保留原证件" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="出借人" prop="lenderName">
            <el-input v-model="form.lenderName" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="借款类型" prop="loanType">
            <el-select v-model="form.loanType" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.iou_note_loan_type"
                :key="item.id"
                :label="item.label"
                :value="Number(item.value)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="本金" prop="principalAmount">
            <el-input-number v-model="form.principalAmount" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="年利率（0.15表示15%）" prop="annualRate">
            <el-input-number v-model="form.annualRate" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="借款日期" prop="loanDate">
            <el-date-picker v-model="form.loanDate" type="date" value-format="yyyy-MM-dd HH:mm:ss" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="最终还款日期" prop="dueDate">
            <el-date-picker v-model="form.dueDate" type="date" value-format="yyyy-MM-dd HH:mm:ss" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="总利息" prop="interestAmount">
            <el-input-number v-model="form.interestAmount" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="应还本金加利息" prop="totalAmount">
            <el-input-number v-model="form.totalAmount" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="借款用途">
            <el-input v-model="form.purpose" :rows="3" type="textarea" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" :rows="3" type="textarea" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="服务费" prop="serviceFee">
            <el-input-number v-model="form.serviceFee" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="协议文件存储标识">
            <el-input v-model="form.agreementFile" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="借条状态" prop="status">
            <el-select v-model="form.status" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.iou_note_status"
                :key="item.id"
                :label="item.label"
                :value="Number(item.value)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="取消原因">
            <el-input v-model="form.cancelReason" :rows="3" type="textarea" style="width: 370px;" />
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
        <el-table-column prop="noteNo" label="借条编号" />
        <el-table-column prop="creatorUserId" label="发起人ID" />
        <el-table-column prop="borrowerName" label="借款人" />
        <el-table-column prop="lenderName" label="出借人" />
        <el-table-column prop="loanType" label="借款类型">
          <template slot-scope="scope">
            {{ dict.label.iou_note_loan_type[scope.row.loanType] }}
          </template>
        </el-table-column>
        <el-table-column prop="principalAmount" label="本金" />
        <el-table-column prop="annualRate" label="年利率（0.15表示15%）" />
        <el-table-column prop="loanDate" label="借款日期" />
        <el-table-column prop="dueDate" label="最终还款日期" />
        <el-table-column prop="interestAmount" label="总利息" />
        <el-table-column prop="totalAmount" label="应还本金加利息" />
        <el-table-column prop="serviceFee" label="服务费" />
        <el-table-column prop="borrowerSignedAt" label="借款人签署时间" />
        <el-table-column prop="lenderSignedAt" label="出借人签署时间" />
        <el-table-column prop="status" label="借条状态">
          <template slot-scope="scope">
            {{ dict.label.iou_note_status[scope.row.status] }}
          </template>
        </el-table-column>
        <el-table-column prop="effectiveAt" label="生效时间" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column v-if="checkPer(['admin','iouNote:edit','iouNote:del'])" label="操作" width="150px" align="center">
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
import crudIouNote from '@/api/iouNote'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

const defaultForm = { borrowerIdNo: null, lenderIdNo: null, id: null, noteNo: null, creatorUserId: null, borrowerName: null, borrowerIdNoHash: null, borrowerIdNoEncrypted: null, lenderName: null, lenderIdNoHash: null, lenderIdNoEncrypted: null, loanType: 1, principalAmount: null, annualRate: 0, loanDate: null, dueDate: null, interestAmount: 0, totalAmount: null, purpose: null, remark: null, serviceFee: 0, borrowerSignedAt: null, lenderSignedAt: null, agreementFile: null, status: 0, effectiveAt: null, cancelReason: null, createTime: null, updateTime: null }
export default {
  name: 'IouNote',
  components: { DateRangePicker, pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['iou_note_loan_type', 'iou_note_status'],
  cruds() {
    return CRUD({ title: '借条', url: 'api/iouNote', idField: 'id', sort: 'id,desc', crudMethod: { ...crudIouNote }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'iouNote:add'],
        edit: ['admin', 'iouNote:edit'],
        del: ['admin', 'iouNote:del']
      },
      rules: {
        noteNo: [
          { required: true, message: '借条编号不能为空', trigger: 'blur' }
        ],
        creatorUserId: [
          { required: true, message: '发起人ID不能为空', trigger: 'blur' }
        ],
        borrowerName: [
          { required: true, message: '借款人不能为空', trigger: 'blur' }
        ],
        lenderName: [
          { required: true, message: '出借人不能为空', trigger: 'blur' }
        ],
        loanType: [
          { required: true, message: '借款类型不能为空', trigger: 'blur' }
        ],
        principalAmount: [
          { required: true, message: '本金不能为空', trigger: 'blur' }
        ],
        annualRate: [
          { required: true, message: '年利率（0.15表示15%）不能为空', trigger: 'blur' }
        ],
        loanDate: [
          { required: true, message: '借款日期不能为空', trigger: 'blur' }
        ],
        dueDate: [
          { required: true, message: '最终还款日期不能为空', trigger: 'blur' }
        ],
        interestAmount: [
          { required: true, message: '总利息不能为空', trigger: 'blur' }
        ],
        totalAmount: [
          { required: true, message: '应还本金加利息不能为空', trigger: 'blur' }
        ],
        serviceFee: [
          { required: true, message: '服务费不能为空', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '借条状态不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'noteNo', display_name: '借条编号' },
        { key: 'creatorUserId', display_name: '发起人ID' },
        { key: 'borrowerName', display_name: '借款人' },
        { key: 'lenderName', display_name: '出借人' },
        { key: 'loanType', display_name: '借款类型' },
        { key: 'status', display_name: '借条状态' }
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
