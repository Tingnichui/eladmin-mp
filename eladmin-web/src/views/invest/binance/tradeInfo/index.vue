<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">交易对</label>
        <el-select
          v-model="query.symbol"
          clearable
          size="small"
          placeholder="投资类型"
          class="filter-item"
          style="width: 185px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_binance_symbol"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">订单ID</label>
        <el-input v-model="query.orderid" clearable placeholder="订单 ID" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">手续费资产</label>
        <el-select
          v-model="query.commissionasset"
          clearable
          size="small"
          placeholder="手续费资产"
          class="filter-item"
          style="width: 185px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_binance_commission"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">操作方向</label>
        <el-select
          v-model="query.isbuyer"
          clearable
          size="small"
          placeholder="操作方向"
          class="filter-item"
          style="width: 185px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_binance_is_buyer"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">订单类型</label>
        <el-select
          v-model="query.ismaker"
          clearable
          size="small"
          placeholder="订单类型"
          class="filter-item"
          style="width: 185px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_binance_is_maker"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">最佳匹配</label>
        <el-select
          v-model="query.isbestmatch"
          clearable
          size="small"
          placeholder="最佳匹配"
          class="filter-item"
          style="width: 185px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_binance_is_best_match"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <date-range-picker
          v-model="query.price"
          start-placeholder="priceStart"
          end-placeholder="priceStart"
          class="date-item"
        />
        <date-range-picker
          v-model="query.qty"
          start-placeholder="qtyStart"
          end-placeholder="qtyStart"
          class="date-item"
        />
        <date-range-picker
          v-model="query.quoteqty"
          start-placeholder="quoteqtyStart"
          end-placeholder="quoteqtyStart"
          class="date-item"
        />
        <date-range-picker
          v-model="query.commission"
          start-placeholder="commissionStart"
          end-placeholder="commissionStart"
          class="date-item"
        />
        <date-range-picker
          v-model="query.time"
          start-placeholder="timeStart"
          end-placeholder="timeStart"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="交易对" prop="symbol">
            <el-select v-model="form.symbol" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.invest_binance_symbol"
                :key="item.id"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="订单 ID" prop="orderid">
            <el-input v-model="form.orderid" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交价格" prop="price">
            <el-input v-model="form.price" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交数量" prop="qty">
            <el-input v-model="form.qty" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交额" prop="quoteqty">
            <el-input v-model="form.quoteqty" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="手续费" prop="commission">
            <el-input v-model="form.commission" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="手续费资产" prop="commissionasset">
            <el-input v-model="form.commissionasset" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交时间" prop="time">
            <el-input v-model="form.time" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="操作方向" prop="isbuyer">
            <el-input v-model="form.isbuyer" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="订单类型" prop="ismaker">
            <el-input v-model="form.ismaker" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="最佳匹配" prop="isbestmatch">
            <el-input v-model="form.isbestmatch" style="width: 370px;" />
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
        <el-table-column prop="symbol" label="交易对">
          <template slot-scope="scope">
            {{ dict.label.invest_binance_symbol[scope.row.symbol] }}
          </template>
        </el-table-column>
        <el-table-column prop="orderid" label="订单 ID" />
        <el-table-column prop="price" label="成交价格" />
        <el-table-column prop="qty" label="成交数量" />
        <el-table-column prop="quoteqty" label="成交额" />
        <el-table-column prop="commission" label="手续费" />
        <el-table-column prop="commissionasset" label="手续费资产">
          <template slot-scope="scope">
            {{ dict.label.invest_binance_commission[scope.row.commissionasset] }}
          </template>
        </el-table-column>
        <el-table-column prop="time" label="成交时间" />
        <el-table-column prop="isbuyer" label="操作方向">
          <template slot-scope="scope">
            {{ dict.label.invest_binance_is_buyer[scope.row.isbuyer] }}
          </template>
        </el-table-column>
        <el-table-column prop="ismaker" label="是否为挂单方">
          <template slot-scope="scope">
            {{ dict.label.invest_binance_is_maker[scope.row.ismaker] }}
          </template>
        </el-table-column>
        <el-table-column prop="isbestmatch" label="是否为最佳匹配">
          <template slot-scope="scope">
            {{ dict.label.invest_binance_is_best_match[scope.row.isbestmatch] }}
          </template>
        </el-table-column>
        <el-table-column v-if="checkPer(['admin','binanceTradeInfo:edit','binanceTradeInfo:del'])" label="操作" width="150px" align="center">
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
import crudBinanceTradeInfo from '@/api/binanceTradeInfo'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, symbol: null, orderid: null, price: null, qty: null, quoteqty: null, commission: null, commissionasset: null, time: null, isbuyer: null, ismaker: null, isbestmatch: null }
export default {
  name: 'BinanceTradeInfo',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['invest_binance_symbol', 'invest_binance_commission', 'invest_binance_is_buyer', 'invest_binance_is_maker', 'invest_binance_is_best_match'],
  cruds() {
    return CRUD({ title: '币安交易', url: 'api/binanceTradeInfo', idField: 'id', sort: 'id,desc', crudMethod: { ...crudBinanceTradeInfo }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'binanceTradeInfo:add'],
        edit: ['admin', 'binanceTradeInfo:edit'],
        del: ['admin', 'binanceTradeInfo:del']
      },
      rules: {
        symbol: [
          { required: true, message: '交易对不能为空', trigger: 'blur' }
        ],
        orderid: [
          { required: true, message: '订单 ID不能为空', trigger: 'blur' }
        ],
        price: [
          { required: true, message: '成交价格不能为空', trigger: 'blur' }
        ],
        qty: [
          { required: true, message: '成交数量不能为空', trigger: 'blur' }
        ],
        quoteqty: [
          { required: true, message: '成交额不能为空', trigger: 'blur' }
        ],
        commission: [
          { required: true, message: '手续费不能为空', trigger: 'blur' }
        ],
        commissionasset: [
          { required: true, message: '手续费资产不能为空', trigger: 'blur' }
        ],
        time: [
          { required: true, message: '成交时间不能为空', trigger: 'blur' }
        ],
        isbuyer: [
          { required: true, message: '操作方向不能为空', trigger: 'blur' }
        ],
        ismaker: [
          { required: true, message: '是否为挂单方不能为空', trigger: 'blur' }
        ],
        isbestmatch: [
          { required: true, message: '是否为最佳匹配不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'symbol', display_name: '交易对' },
        { key: 'orderid', display_name: '订单 ID' },
        { key: 'commissionasset', display_name: '手续费资产' },
        { key: 'isbuyer', display_name: '操作方向' },
        { key: 'ismaker', display_name: '是否为挂单方' },
        { key: 'isbestmatch', display_name: '是否为最佳匹配' }
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
