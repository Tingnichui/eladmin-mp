<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">实名姓名</label>
        <el-input v-model="query.idCardName" clearable placeholder="实名姓名" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">用户编号</label>
        <el-input v-model="query.uid" clearable placeholder="用户编号" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">手机号码</label>
        <el-input v-model="query.phoneNumber" clearable placeholder="手机号码" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">邮箱</label>
        <el-input v-model="query.email" clearable placeholder="邮箱" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.totalInvestment"
          start-placeholder="totalInvestmentStart"
          end-placeholder="totalInvestmentStart"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="实名姓名">
            <el-input v-model="form.idCardName" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="用户编号" prop="uid">
            <el-input v-model="form.uid" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="手机号码" prop="phoneNumber">
            <el-input v-model="form.phoneNumber" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="总投资额" prop="totalInvestment">
            <el-input v-model="form.totalInvestment" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="apiKey">
            <el-input v-model="form.apiKey" :rows="3" type="textarea" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="apiSecret">
            <el-input v-model="form.apiSecret" :rows="3" type="textarea" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" :rows="3" type="textarea" style="width: 370px;" />
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
        <el-table-column prop="idCardName" label="实名姓名" />
        <el-table-column prop="uid" label="用户编号" />
        <el-table-column prop="phoneNumber" label="手机号码" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="totalInvestment" label="总投资额" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column v-if="checkPer(['admin','binanceAccountInfo:edit','binanceAccountInfo:del'])" label="操作" width="150px" align="center">
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
import crudBinanceAccountInfo from '@/api/binanceAccountInfo'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, idCardName: null, uid: null, phoneNumber: null, email: null, totalInvestment: null, apiKey: null, apiSecret: null, remark: null }
export default {
  name: 'BinanceAccountInfo',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '币安账户', url: 'api/binanceAccountInfo', idField: 'id', sort: 'id,desc', crudMethod: { ...crudBinanceAccountInfo }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'binanceAccountInfo:add'],
        edit: ['admin', 'binanceAccountInfo:edit'],
        del: ['admin', 'binanceAccountInfo:del']
      },
      rules: {
        uid: [
          { required: true, message: '用户编号不能为空', trigger: 'blur' }
        ],
        phoneNumber: [
          { required: true, message: '手机号码不能为空', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '邮箱不能为空', trigger: 'blur' }
        ],
        totalInvestment: [
          { required: true, message: '总投资额不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'idCardName', display_name: '实名姓名' },
        { key: 'uid', display_name: '用户编号' },
        { key: 'phoneNumber', display_name: '手机号码' },
        { key: 'email', display_name: '邮箱' }
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
