<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">币安账户用户编号</label>
        <el-input v-model="query.uid" clearable placeholder="币安账户用户编号" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">现货交易对</label>
        <el-input v-model="query.symbol" clearable placeholder="现货交易对" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">买入成交 ID</label>
        <el-input v-model="query.buyTradeId" clearable placeholder="买入成交 ID" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">卖出成交 ID</label>
        <el-input v-model="query.sellTradeId" clearable placeholder="卖出成交 ID" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.buyTime"
          start-placeholder="buyTimeStart"
          end-placeholder="buyTimeEnd"
          class="date-item"
        />
        <date-range-picker
          v-model="query.sellTime"
          start-placeholder="sellTimeStart"
          end-placeholder="sellTimeEnd"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="币安账户用户编号" prop="uid">
            <el-input-number v-model="form.uid" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="现货交易对" prop="symbol">
            <el-input v-model="form.symbol" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="买入成交 ID" prop="buyTradeId">
            <el-input v-model="form.buyTradeId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="卖出成交 ID" prop="sellTradeId">
            <el-input v-model="form.sellTradeId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="撮合数量" prop="matchedQty">
            <el-input-number v-model="form.matchedQty" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="买入价格快照" prop="buyPrice">
            <el-input-number v-model="form.buyPrice" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="卖出价格快照" prop="sellPrice">
            <el-input-number v-model="form.sellPrice" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="买入成交时间" prop="buyTime">
            <el-date-picker v-model="form.buyTime" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="卖出成交时间" prop="sellTime">
            <el-date-picker v-model="form.sellTime" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="撮合买入金额" prop="buyAmount">
            <el-input-number v-model="form.buyAmount" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="撮合卖出金额" prop="sellAmount">
            <el-input-number v-model="form.sellAmount" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="手续费率" prop="feeRate">
            <el-input-number v-model="form.feeRate" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="已实现盈亏" prop="pnl">
            <el-input-number v-model="form.pnl" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="手续费" prop="fee">
            <el-input-number v-model="form.fee" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="扣除手续费后的净盈亏" prop="netPnl">
            <el-input-number v-model="form.netPnl" :controls="false" style="width: 370px;" />
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
        <el-table-column prop="uid" label="币安账户用户编号" />
        <el-table-column prop="symbol" label="现货交易对" />
        <el-table-column prop="buyTradeId" label="买入成交 ID" />
        <el-table-column prop="sellTradeId" label="卖出成交 ID" />
        <el-table-column prop="matchedQty" label="撮合数量" />
        <el-table-column prop="buyPrice" label="买入价格快照" />
        <el-table-column prop="sellPrice" label="卖出价格快照" />
        <el-table-column prop="buyTime" label="买入成交时间" />
        <el-table-column prop="sellTime" label="卖出成交时间" />
        <el-table-column prop="buyAmount" label="撮合买入金额" />
        <el-table-column prop="sellAmount" label="撮合卖出金额" />
        <el-table-column prop="feeRate" label="手续费率" />
        <el-table-column prop="pnl" label="已实现盈亏" />
        <el-table-column prop="fee" label="手续费" />
        <el-table-column prop="netPnl" label="扣除手续费后的净盈亏" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column v-if="checkPer(['admin','binanceSpotTradeMatch:edit','binanceSpotTradeMatch:del'])" label="操作" width="150px" align="center">
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
import crudBinanceSpotTradeMatch from '@/api/binanceSpotTradeMatch'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

const defaultForm = { id: null, uid: null, symbol: null, buyTradeId: null, sellTradeId: null, matchedQty: null, buyPrice: null, sellPrice: null, buyTime: null, sellTime: null, buyAmount: null, sellAmount: null, feeRate: null, pnl: null, fee: null, netPnl: null, createTime: null }
export default {
  name: 'BinanceSpotTradeMatch',
  components: { DateRangePicker, pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '现货撮合明细', url: 'api/binanceSpotTradeMatch', idField: 'id', sort: 'id,desc', crudMethod: { ...crudBinanceSpotTradeMatch }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'binanceSpotTradeMatch:add'],
        edit: ['admin', 'binanceSpotTradeMatch:edit'],
        del: ['admin', 'binanceSpotTradeMatch:del']
      },
      rules: {
        uid: [
          { required: true, message: '币安账户用户编号不能为空', trigger: 'blur' }
        ],
        symbol: [
          { required: true, message: '现货交易对不能为空', trigger: 'blur' }
        ],
        buyTradeId: [
          { required: true, message: '买入成交 ID不能为空', trigger: 'blur' }
        ],
        sellTradeId: [
          { required: true, message: '卖出成交 ID不能为空', trigger: 'blur' }
        ],
        matchedQty: [
          { required: true, message: '撮合数量不能为空', trigger: 'blur' }
        ],
        buyPrice: [
          { required: true, message: '买入价格快照不能为空', trigger: 'blur' }
        ],
        sellPrice: [
          { required: true, message: '卖出价格快照不能为空', trigger: 'blur' }
        ],
        buyTime: [
          { required: true, message: '买入成交时间不能为空', trigger: 'blur' }
        ],
        sellTime: [
          { required: true, message: '卖出成交时间不能为空', trigger: 'blur' }
        ],
        buyAmount: [
          { required: true, message: '撮合买入金额不能为空', trigger: 'blur' }
        ],
        sellAmount: [
          { required: true, message: '撮合卖出金额不能为空', trigger: 'blur' }
        ],
        feeRate: [
          { required: true, message: '手续费率不能为空', trigger: 'blur' }
        ],
        pnl: [
          { required: true, message: '已实现盈亏不能为空', trigger: 'blur' }
        ],
        fee: [
          { required: true, message: '手续费不能为空', trigger: 'blur' }
        ],
        netPnl: [
          { required: true, message: '扣除手续费后的净盈亏不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'uid', display_name: '币安账户用户编号' },
        { key: 'symbol', display_name: '现货交易对' },
        { key: 'buyTradeId', display_name: '买入成交 ID' },
        { key: 'sellTradeId', display_name: '卖出成交 ID' }
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
