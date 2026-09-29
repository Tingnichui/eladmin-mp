<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">策略ID</label>
        <el-input v-model="query.strategyId" clearable placeholder="策略ID" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">交易对</label>
        <el-input v-model="query.symbol" clearable placeholder="交易对" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">周期</label>
        <el-input v-model="query.intervalCode" clearable placeholder="周期" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">操作</label>
        <el-input v-model="query.action" clearable placeholder="操作" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.decisionTime"
          start-placeholder="decisionTimeStart"
          end-placeholder="decisionTimeEnd"
          class="date-item"
        />
        <date-range-picker
          v-model="query.createdAt"
          start-placeholder="createdAtStart"
          end-placeholder="createdAtEnd"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="事件唯一键" prop="eventKey">
            <el-input v-model="form.eventKey" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="告警ID" prop="alertId">
            <el-input v-model="form.alertId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="结构版本" prop="schemaVersion">
            <el-input-number v-model="form.schemaVersion" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="策略ID" prop="strategyId">
            <el-input v-model="form.strategyId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="交易对" prop="symbol">
            <el-input v-model="form.symbol" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="周期" prop="intervalCode">
            <el-input v-model="form.intervalCode" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="策略版本" prop="profileRevision">
            <el-input v-model="form.profileRevision" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="参数指纹" prop="parameterFingerprint">
            <el-input v-model="form.parameterFingerprint" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="数据源指纹" prop="sourceFingerprint">
            <el-input v-model="form.sourceFingerprint" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="K线开盘时间" prop="barOpenTime">
            <el-date-picker v-model="form.barOpenTime" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="决策时间" prop="decisionTime">
            <el-date-picker v-model="form.decisionTime" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="操作" prop="action">
            <el-input v-model="form.action" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="操作前仓位" prop="positionBefore">
            <el-input-number v-model="form.positionBefore" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="操作后仓位" prop="positionAfter">
            <el-input-number v-model="form.positionAfter" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="操作价格" prop="actionPrice">
            <el-input v-model="form.actionPrice" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="收盘价格" prop="closePrice">
            <el-input v-model="form.closePrice" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="止损价格">
            <el-input v-model="form.stopLossPrice" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="触发原因" prop="reason">
            <el-input v-model="form.reason" :rows="3" type="textarea" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="消息" prop="message">
            <el-input v-model="form.message" :rows="3" type="textarea" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="事件内容" prop="payload">
            <el-input v-model="form.payload" :rows="3" type="textarea" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="创建时间" prop="createdAt">
            <el-date-picker v-model="form.createdAt" type="datetime" style="width: 370px;" />
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
        <el-table-column prop="alertId" label="告警ID" />
        <el-table-column prop="schemaVersion" label="结构版本" />
        <el-table-column prop="strategyId" label="策略ID" />
        <el-table-column prop="symbol" label="交易对" />
        <el-table-column prop="intervalCode" label="周期" />
        <el-table-column prop="profileRevision" label="策略版本" />
        <el-table-column prop="barOpenTime" label="K线开盘时间" />
        <el-table-column prop="decisionTime" label="决策时间" />
        <el-table-column prop="action" label="操作" />
        <el-table-column prop="positionBefore" label="操作前仓位" />
        <el-table-column prop="positionAfter" label="操作后仓位" />
        <el-table-column prop="actionPrice" label="操作价格" />
        <el-table-column prop="closePrice" label="收盘价格" />
        <el-table-column prop="stopLossPrice" label="止损价格" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column v-if="checkPer(['admin','researchStrategyAlertEvents:edit','researchStrategyAlertEvents:del'])" label="操作" width="150px" align="center">
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
import crudResearchStrategyAlertEvents from '@/api/researchStrategyAlertEvents'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

const validateJson = (rule, value, callback) => {
  if (!value) return callback()
  try {
    JSON.parse(value)
    callback()
  } catch (error) {
    callback(new Error('事件内容必须是合法 JSON'))
  }
}

const defaultForm = { id: null, eventKey: null, alertId: null, schemaVersion: null, strategyId: null, symbol: null, intervalCode: null, profileRevision: null, parameterFingerprint: null, sourceFingerprint: null, barOpenTime: null, decisionTime: null, action: null, positionBefore: null, positionAfter: null, actionPrice: null, closePrice: null, stopLossPrice: null, reason: null, title: null, message: null, payload: null, createdAt: null }
export default {
  name: 'ResearchStrategyAlertEvents',
  components: { DateRangePicker, pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '策略告警事件', url: 'api/researchStrategyAlertEvents', idField: 'id', sort: 'id,desc', crudMethod: { ...crudResearchStrategyAlertEvents }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'researchStrategyAlertEvents:add'],
        edit: ['admin', 'researchStrategyAlertEvents:edit'],
        del: ['admin', 'researchStrategyAlertEvents:del']
      },
      rules: {
        eventKey: [
          { required: true, message: '事件唯一键不能为空', trigger: 'blur' }
        ],
        alertId: [
          { required: true, message: '告警ID不能为空', trigger: 'blur' }
        ],
        schemaVersion: [
          { required: true, message: '结构版本不能为空', trigger: 'blur' }
        ],
        strategyId: [
          { required: true, message: '策略ID不能为空', trigger: 'blur' }
        ],
        symbol: [
          { required: true, message: '交易对不能为空', trigger: 'blur' }
        ],
        intervalCode: [
          { required: true, message: '周期不能为空', trigger: 'blur' }
        ],
        profileRevision: [
          { required: true, message: '策略版本不能为空', trigger: 'blur' }
        ],
        parameterFingerprint: [
          { required: true, message: '参数指纹不能为空', trigger: 'blur' }
        ],
        sourceFingerprint: [
          { required: true, message: '数据源指纹不能为空', trigger: 'blur' }
        ],
        barOpenTime: [
          { required: true, message: 'K线开盘时间不能为空', trigger: 'blur' }
        ],
        decisionTime: [
          { required: true, message: '决策时间不能为空', trigger: 'blur' }
        ],
        action: [
          { required: true, message: '操作不能为空', trigger: 'blur' }
        ],
        positionBefore: [
          { required: true, message: '操作前仓位不能为空', trigger: 'blur' }
        ],
        positionAfter: [
          { required: true, message: '操作后仓位不能为空', trigger: 'blur' }
        ],
        actionPrice: [
          { required: true, message: '操作价格不能为空', trigger: 'blur' }
        ],
        closePrice: [
          { required: true, message: '收盘价格不能为空', trigger: 'blur' }
        ],
        reason: [
          { required: true, message: '触发原因不能为空', trigger: 'blur' }
        ],
        title: [
          { required: true, message: '标题不能为空', trigger: 'blur' }
        ],
        message: [
          { required: true, message: '消息不能为空', trigger: 'blur' }
        ],
        payload: [
          { required: true, message: '事件内容不能为空', trigger: 'blur' },
          { validator: validateJson, trigger: 'blur' }
        ],
        createdAt: [
          { required: true, message: '创建时间不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'strategyId', display_name: '策略ID' },
        { key: 'symbol', display_name: '交易对' },
        { key: 'intervalCode', display_name: '周期' },
        { key: 'action', display_name: '操作' }
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
