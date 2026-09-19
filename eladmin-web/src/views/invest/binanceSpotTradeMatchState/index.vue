<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">币安成交 ID</label>
        <el-input v-model="query.tradeId" clearable placeholder="币安成交 ID" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">币安账户用户编号</label>
        <el-input v-model="query.uid" clearable placeholder="币安账户用户编号" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">现货交易对</label>
        <el-input v-model="query.symbol" clearable placeholder="现货交易对" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">是否为买方：1买入，0卖出</label>
        <el-input v-model="query.isBuyer" clearable placeholder="是否为买方：1买入，0卖出" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">撮合状态：PENDING、PARTIAL、COMPLETED、EXCEPTION</label>
        <el-input v-model="query.matchStatus" clearable placeholder="撮合状态：PENDING、PARTIAL、COMPLETED、EXCEPTION" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.tradeTime"
          start-placeholder="tradeTimeStart"
          end-placeholder="tradeTimeEnd"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="币安成交 ID" prop="tradeId">
            <el-input v-model="form.tradeId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="币安账户用户编号" prop="uid">
            <el-input-number v-model="form.uid" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="现货交易对" prop="symbol">
            <el-input v-model="form.symbol" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="是否为买方：1买入，0卖出" prop="isBuyer">
            <el-input-number v-model="form.isBuyer" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交时间" prop="tradeTime">
            <el-date-picker v-model="form.tradeTime" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="原始成交数量" prop="originalQty">
            <el-input-number v-model="form.originalQty" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="已撮合数量" prop="matchedQty">
            <el-input-number v-model="form.matchedQty" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="剩余未撮合数量" prop="remainingQty">
            <el-input-number v-model="form.remainingQty" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="撮合状态：PENDING、PARTIAL、COMPLETED、EXCEPTION" prop="matchStatus">
            <el-input v-model="form.matchStatus" style="width: 370px;" />
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
        <el-table-column prop="tradeId" label="币安成交 ID" />
        <el-table-column prop="uid" label="币安账户用户编号" />
        <el-table-column prop="symbol" label="现货交易对" />
        <el-table-column prop="isBuyer" label="是否为买方：1买入，0卖出" />
        <el-table-column prop="tradeTime" label="成交时间" />
        <el-table-column prop="originalQty" label="原始成交数量" />
        <el-table-column prop="matchedQty" label="已撮合数量" />
        <el-table-column prop="remainingQty" label="剩余未撮合数量" />
        <el-table-column prop="matchStatus" label="撮合状态：PENDING、PARTIAL、COMPLETED、EXCEPTION" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column prop="updateTime" label="更新时间" />
        <el-table-column v-if="checkPer(['admin','binanceSpotTradeMatchState:edit','binanceSpotTradeMatchState:del'])" label="操作" width="150px" align="center">
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
import crudBinanceSpotTradeMatchState from '@/api/binanceSpotTradeMatchState'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

const defaultForm = { id: null, tradeId: null, uid: null, symbol: null, isBuyer: null, tradeTime: null, originalQty: null, matchedQty: null, remainingQty: null, matchStatus: null, createTime: null, updateTime: null }
export default {
  name: 'BinanceSpotTradeMatchState',
  components: { DateRangePicker, pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '现货撮合状态', url: 'api/binanceSpotTradeMatchState', idField: 'id', sort: 'id,desc', crudMethod: { ...crudBinanceSpotTradeMatchState }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'binanceSpotTradeMatchState:add'],
        edit: ['admin', 'binanceSpotTradeMatchState:edit'],
        del: ['admin', 'binanceSpotTradeMatchState:del']
      },
      rules: {
        tradeId: [
          { required: true, message: '币安成交 ID不能为空', trigger: 'blur' }
        ],
        uid: [
          { required: true, message: '币安账户用户编号不能为空', trigger: 'blur' }
        ],
        symbol: [
          { required: true, message: '现货交易对不能为空', trigger: 'blur' }
        ],
        isBuyer: [
          { required: true, message: '是否为买方：1买入，0卖出不能为空', trigger: 'blur' }
        ],
        tradeTime: [
          { required: true, message: '成交时间不能为空', trigger: 'blur' }
        ],
        originalQty: [
          { required: true, message: '原始成交数量不能为空', trigger: 'blur' }
        ],
        matchedQty: [
          { required: true, message: '已撮合数量不能为空', trigger: 'blur' }
        ],
        remainingQty: [
          { required: true, message: '剩余未撮合数量不能为空', trigger: 'blur' }
        ],
        matchStatus: [
          { required: true, message: '撮合状态：PENDING、PARTIAL、COMPLETED、EXCEPTION不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'tradeId', display_name: '币安成交 ID' },
        { key: 'uid', display_name: '币安账户用户编号' },
        { key: 'symbol', display_name: '现货交易对' },
        { key: 'isBuyer', display_name: '是否为买方：1买入，0卖出' },
        { key: 'matchStatus', display_name: '撮合状态：PENDING、PARTIAL、COMPLETED、EXCEPTION' }
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
