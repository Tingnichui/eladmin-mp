<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">币安账户用户编号</label>
        <el-input v-model="query.uid" clearable placeholder="币安账户用户编号" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">币安订单号（orderNumber）</label>
        <el-input v-model="query.orderNumber" clearable placeholder="币安订单号（orderNumber）" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">交易方向（tradeType）：BUY、SELL</label>
        <el-input v-model="query.tradeType" clearable placeholder="交易方向（tradeType）：BUY、SELL" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">数字资产（asset），如 USDT</label>
        <el-input v-model="query.asset" clearable placeholder="数字资产（asset），如 USDT" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">法币（fiat），如 CNY</label>
        <el-input v-model="query.fiat" clearable placeholder="法币（fiat），如 CNY" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">订单状态（orderStatus）</label>
        <el-input v-model="query.orderStatus" clearable placeholder="订单状态（orderStatus）" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.orderTime"
          start-placeholder="orderTimeStart"
          end-placeholder="orderTimeEnd"
          class="date-item"
        />
        <date-range-picker
          v-model="query.syncTime"
          start-placeholder="syncTimeStart"
          end-placeholder="syncTimeEnd"
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
          <el-form-item label="币安订单号（orderNumber）" prop="orderNumber">
            <el-input v-model="form.orderNumber" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="广告编号（advNo）">
            <el-input v-model="form.advNo" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="交易方向（tradeType）：BUY、SELL" prop="tradeType">
            <el-input v-model="form.tradeType" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="数字资产（asset），如 USDT" prop="asset">
            <el-input v-model="form.asset" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="法币（fiat），如 CNY" prop="fiat">
            <el-input v-model="form.fiat" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="法币符号（fiatSymbol）">
            <el-input v-model="form.fiatSymbol" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="数字资产数量（amount）">
            <el-input-number v-model="form.amount" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="数字资产数量（takerAmount）">
            <el-input-number v-model="form.takerAmount" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="法币总金额（totalPrice）" prop="totalPrice">
            <el-input-number v-model="form.totalPrice" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交单价（unitPrice）">
            <el-input-number v-model="form.unitPrice" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="订单状态（orderStatus）" prop="orderStatus">
            <el-input v-model="form.orderStatus" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="币安订单创建时间戳（createTime，毫秒）" prop="orderCreateTime">
            <el-input v-model="form.orderCreateTime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="币安订单创建时间（本地转换值）" prop="orderTime">
            <el-date-picker v-model="form.orderTime" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="手续费（commission）">
            <el-input-number v-model="form.commission" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="交易对手昵称（counterPartNickName）">
            <el-input v-model="form.counterPartNickName" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="广告角色（advertisementRole）">
            <el-input v-model="form.advertisementRole" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="币安接口原始 JSON">
            <el-input v-model="form.rawData" :rows="3" type="textarea" style="width: 370px;" />
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
        <el-table-column prop="orderNumber" label="币安订单号（orderNumber）" />
        <el-table-column prop="tradeType" label="交易方向（tradeType）：BUY、SELL" />
        <el-table-column prop="asset" label="数字资产（asset），如 USDT" />
        <el-table-column prop="fiat" label="法币（fiat），如 CNY" />
        <el-table-column prop="amount" label="数字资产数量（amount）" />
        <el-table-column prop="takerAmount" label="数字资产数量（takerAmount）" />
        <el-table-column prop="totalPrice" label="法币总金额（totalPrice）" />
        <el-table-column prop="unitPrice" label="成交单价（unitPrice）" />
        <el-table-column prop="orderStatus" label="订单状态（orderStatus）" />
        <el-table-column prop="orderTime" label="币安订单创建时间（本地转换值）" />
        <el-table-column prop="commission" label="手续费（commission）" />
        <el-table-column prop="syncTime" label="最近同步时间" />
        <el-table-column v-if="checkPer(['admin','binanceC2cOrder:edit','binanceC2cOrder:del'])" label="操作" width="150px" align="center">
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
import crudBinanceC2cOrder from '@/api/binanceC2cOrder'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

const defaultForm = { id: null, uid: null, orderNumber: null, advNo: null, tradeType: null, asset: null, fiat: null, fiatSymbol: null, amount: null, takerAmount: null, totalPrice: null, unitPrice: null, orderStatus: null, orderCreateTime: null, orderTime: null, commission: null, counterPartNickName: null, advertisementRole: null, rawData: null, syncTime: null, createTime: null, updateTime: null }
export default {
  name: 'BinanceC2cOrder',
  components: { DateRangePicker, pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '币安C2C订单', url: 'api/binanceC2cOrder', idField: 'id', sort: 'id,desc', crudMethod: { ...crudBinanceC2cOrder }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'binanceC2cOrder:add'],
        edit: ['admin', 'binanceC2cOrder:edit'],
        del: ['admin', 'binanceC2cOrder:del']
      },
      rules: {
        uid: [
          { required: true, message: '币安账户用户编号不能为空', trigger: 'blur' }
        ],
        orderNumber: [
          { required: true, message: '币安订单号（orderNumber）不能为空', trigger: 'blur' }
        ],
        tradeType: [
          { required: true, message: '交易方向（tradeType）：BUY、SELL不能为空', trigger: 'blur' }
        ],
        asset: [
          { required: true, message: '数字资产（asset），如 USDT不能为空', trigger: 'blur' }
        ],
        fiat: [
          { required: true, message: '法币（fiat），如 CNY不能为空', trigger: 'blur' }
        ],
        totalPrice: [
          { required: true, message: '法币总金额（totalPrice）不能为空', trigger: 'blur' }
        ],
        orderStatus: [
          { required: true, message: '订单状态（orderStatus）不能为空', trigger: 'blur' }
        ],
        orderCreateTime: [
          { required: true, message: '币安订单创建时间戳（createTime，毫秒）不能为空', trigger: 'blur' }
        ],
        orderTime: [
          { required: true, message: '币安订单创建时间（本地转换值）不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'uid', display_name: '币安账户用户编号' },
        { key: 'orderNumber', display_name: '币安订单号（orderNumber）' },
        { key: 'tradeType', display_name: '交易方向（tradeType）：BUY、SELL' },
        { key: 'asset', display_name: '数字资产（asset），如 USDT' },
        { key: 'fiat', display_name: '法币（fiat），如 CNY' },
        { key: 'orderStatus', display_name: '订单状态（orderStatus）' }
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
