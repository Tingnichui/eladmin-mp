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
        <el-input v-model="query.orderId" clearable placeholder="订单 ID" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">手续费资产</label>
        <el-select
          v-model="query.commissionAsset"
          clearable
          size="small"
          placeholder="手续费资产"
          class="filter-item"
          style="width: 185px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_binance_commission_asset"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">操作方向</label>
        <el-select
          v-model="query.isBuyer"
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
          v-model="query.isMaker"
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
          v-model="query.isBestMatch"
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
        <!--        <date-range-picker-->
        <!--          v-model="query.price"-->
        <!--          start-placeholder="priceStart"-->
        <!--          end-placeholder="priceStart"-->
        <!--          class="date-item"-->
        <!--        />-->
        <!--        <date-range-picker-->
        <!--          v-model="query.qty"-->
        <!--          start-placeholder="qtyStart"-->
        <!--          end-placeholder="qtyStart"-->
        <!--          class="date-item"-->
        <!--        />-->
        <!--        <date-range-picker-->
        <!--          v-model="query.commission"-->
        <!--          start-placeholder="commissionStart"-->
        <!--          end-placeholder="commissionStart"-->
        <!--          class="date-item"-->
        <!--        />-->
        <label class="el-form-item-label">成交时间</label>
        <date-range-picker v-model="query.time" class="date-item" @change="crud.toQuery" />

        <!--        <date-range-picker-->
        <!--          v-model="query.quoteQty"-->
        <!--          start-placeholder="quoteQtyStart"-->
        <!--          end-placeholder="quoteQtyStart"-->
        <!--          class="date-item"-->
        <!--        />-->
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission">
        <el-button
          slot="right"
          class="filter-item"
          size="mini"
          type="success"
          icon="el-icon-tickets"
          @click="showStats = true;doStats()"
        >汇总</el-button>
      </crudOperation>
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
          <el-form-item label="成交价格" prop="price">
            <el-input v-model="form.price" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交数量" prop="qty">
            <el-input v-model="form.qty" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="手续费" prop="commission">
            <el-input v-model="form.commission" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交时间" prop="time">
            <el-date-picker v-model="form.time" type="datetime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="订单 ID" prop="orderId">
            <el-input v-model="form.orderId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交额" prop="quoteQty">
            <el-input v-model="form.quoteQty" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="手续费资产" prop="commissionAsset">
            <el-select v-model="form.commissionAsset" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.invest_binance_commission_asset"
                :key="item.id"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="是否为买方" prop="isBuyer">
            <el-select v-model="form.isBuyer" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.invest_binance_is_buyer"
                :key="item.id"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="是否为挂单方" prop="isMaker">
            <el-select v-model="form.isMaker" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.invest_binance_is_maker"
                :key="item.id"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="是否为最佳匹配" prop="isBestMatch">
            <el-select v-model="form.isBestMatch" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.invest_binance_is_best_match"
                :key="item.id"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
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
        <el-table-column prop="price" label="成交价格" />
        <el-table-column prop="qty" label="成交数量" />
        <el-table-column prop="quoteQty" label="成交额" />
        <el-table-column prop="commission" label="手续费" />
        <el-table-column prop="commissionAsset" label="手续费资产">
          <template slot-scope="scope">
            {{ dict.label.invest_binance_commission_asset[scope.row.commissionAsset] }}
          </template>
        </el-table-column>
        <el-table-column prop="isBuyer" label="操作类型">
          <template slot-scope="scope">
            {{ dict.label.invest_binance_is_buyer[scope.row.isBuyer] }}
          </template>
        </el-table-column>
        <el-table-column prop="isMaker" label="订单类型">
          <template slot-scope="scope">
            {{ dict.label.invest_binance_is_maker[scope.row.isMaker] }}
          </template>
        </el-table-column>
        <el-table-column prop="isBestMatch" label="最佳匹配">
          <template slot-scope="scope">
            {{ dict.label.invest_binance_is_best_match[scope.row.isBestMatch] }}
          </template>
        </el-table-column>
        <el-table-column prop="orderId" label="订单 ID" />
        <el-table-column prop="time" label="成交时间" />
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

      <el-dialog :visible.sync="showStats" append-to-body title="交易汇总" width="60%" class="stats-dialog">
        <!-- 搜索 -->
        <div class="head-container">
          <label class="el-form-item-label">交易对</label>
          <el-select
            v-model="statsQuery.symbol"
            size="small"
            placeholder="投资类型"
            class="filter-item"
            style="width: 185px"
            @change="doStats"
          >
            <el-option
              v-for="item in dict.invest_binance_symbol"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <label class="el-form-item-label">撮合逻辑</label>
          <el-select
            v-model="statsQuery.tradePairingLogic"
            size="small"
            placeholder="投资类型"
            class="filter-item"
            style="width: 185px"
            @change="doStats"
          >
            <el-option
              v-for="item in dict.invest_binance_trade_pairing_logic"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <label class="el-form-item-label">成交时间</label>
          <date-range-picker v-model="statsQuery.time" class="date-item" @change="doStats" />
        </div>

        <el-descriptions :column="3" border class="stats-descriptions">
          <el-descriptions-item label="买入均价">
            {{ formatDecimal(statsInfo.avgBuyPrice) }}
          </el-descriptions-item>
          <el-descriptions-item label="卖出均价">
            {{ formatDecimal(statsInfo.avgSellPrice) }}
          </el-descriptions-item>
          <el-descriptions-item label="收益率">
            {{ formatPercent(statsInfo.profitPct) }}
          </el-descriptions-item>
          <el-descriptions-item label="买入总金额">
            {{ formatDecimal(statsInfo.totalBuyAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="卖出总金额">
            {{ formatDecimal(statsInfo.totalSellAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="利润">
            {{ formatDecimal(statsInfo.profit) }}
          </el-descriptions-item>
          <el-descriptions-item label="未平仓均价">
            {{ formatDecimal(statsInfo.totalWaitAvgSellPrice) }}
          </el-descriptions-item>
          <el-descriptions-item label="未平仓数量">
            {{ formatDecimal(statsInfo.totalWaitSellQty) }}
          </el-descriptions-item>
          <el-descriptions-item label="未平仓总额">
            {{ formatDecimal(statsInfo.totalWaitSellAmount) }}
          </el-descriptions-item>
        </el-descriptions>
        <template #footer>
          <div style="text-align: center;">
            <el-button type="primary" @click="showStats = false">关闭</el-button>
          </div>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import crudBinanceTradeInfo, { stats } from '@/api/binanceTradeInfo'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker/index.vue'

const defaultForm = { id: null, symbol: null, price: null, qty: null, commission: null, time: null, orderId: null, quoteQty: null, commissionAsset: null, isBuyer: null, isMaker: null, isBestMatch: null }
export default {
  name: 'BinanceTradeInfo',
  components: { DateRangePicker, pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['invest_binance_trade_pairing_logic', 'invest_binance_symbol', 'invest_binance_commission_asset', 'invest_binance_is_buyer', 'invest_binance_is_maker', 'invest_binance_is_best_match'],
  cruds() {
    return CRUD({ title: '币安交易', url: 'api/binanceTradeInfo', idField: 'id', sort: 'id,desc', crudMethod: { ...crudBinanceTradeInfo }})
  },
  data() {
    return {
      showStats: false,
      statsInfo: {},
      statsQuery: {
        symbol: 'BTCUSDT',
        tradePairingLogic: 'FIFO'
      },
      permission: {
        add: ['admin', 'binanceTradeInfo:add'],
        edit: ['admin', 'binanceTradeInfo:edit'],
        del: ['admin', 'binanceTradeInfo:del']
      },
      rules: {
        symbol: [
          { required: true, message: '交易对不能为空', trigger: 'blur' }
        ],
        price: [
          { required: true, message: '成交价格不能为空', trigger: 'blur' }
        ],
        qty: [
          { required: true, message: '成交数量不能为空', trigger: 'blur' }
        ],
        commission: [
          { required: true, message: '手续费不能为空', trigger: 'blur' }
        ],
        time: [
          { required: true, message: '成交时间不能为空', trigger: 'blur' }
        ],
        orderId: [
          { required: true, message: '订单 ID不能为空', trigger: 'blur' }
        ],
        quoteQty: [
          { required: true, message: '成交额不能为空', trigger: 'blur' }
        ],
        commissionAsset: [
          { required: true, message: '手续费资产不能为空', trigger: 'blur' }
        ],
        isBuyer: [
          { required: true, message: '是否为买方不能为空', trigger: 'blur' }
        ],
        isMaker: [
          { required: true, message: '是否为挂单方不能为空', trigger: 'blur' }
        ],
        isBestMatch: [
          { required: true, message: '是否为最佳匹配不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'symbol', display_name: '交易对' },
        { key: 'orderId', display_name: '订单 ID' },
        { key: 'commissionAsset', display_name: '手续费资产' },
        { key: 'isBuyer', display_name: '是否为买方' },
        { key: 'isMaker', display_name: '是否为挂单方' },
        { key: 'isBestMatch', display_name: '是否为最佳匹配' }
      ]
    }
  },
  methods: {
    // 钩子：在获取表格数据之前执行，false 则代表不获取数据
    [CRUD.HOOK.beforeRefresh]() {
      return true
    },
    // 显示汇总
    doStats() {
      stats(this.statsQuery).then(res => {
        this.statsInfo = res
      })
    },
    formatDecimal(val) {
      return val != null ? Number(val).toFixed(4) : '--'
    },
    formatPercent(val) {
      return val != null ? (val * 100).toFixed(2) + '%' : '--'
    }
  }
}
</script>

<style scoped>

</style>
