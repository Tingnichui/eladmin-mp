<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">投资产品</label>
        <el-select
          v-model="query.productId"
          clearable
          size="small"
          placeholder="投资产品"
          class="filter-item"
          style="width: 150px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in productList"
            :key="item.id"
            :label="item.investProductName"
            :value="item.id"
          />
        </el-select>
        <label class="el-form-item-label">交易类型</label>
        <el-select
          v-model="query.tradeType"
          clearable
          size="small"
          placeholder="交易类型"
          class="filter-item"
          style="width: 120px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_trade_type"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">交易状态</label>
        <el-select
          v-model="query.operateStatus"
          clearable
          size="small"
          placeholder="交易状态"
          class="filter-item"
          style="width: 120px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_trade_operate_status"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">开仓时间</label>
        <date-range-picker v-model="query.openTime" class="date-item" @change="crud.toQuery" />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation>
        <el-button
          slot="left"
          v-permission="['admin','investTradeRecord:add']"
          class="filter-item"
          type="primary"
          icon="el-icon-plus"
          size="mini"
          @click="doAdd()"
        >
          新增
        </el-button>
      </crudOperation>
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="投资产品" prop="productId">
            <el-select v-model="form.productId" filterable placeholder="请选择">
              <el-option
                v-for="item in productList"
                :key="item.id"
                :label="item.investProductName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="交易类型" prop="tradeType">
            <el-select v-model="form.tradeType" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.invest_trade_type"
                :key="item.id"
                :label="item.label"
                :value="parseInt(item.value)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="交易数量" prop="tradeNum">
            <el-input-number v-model="form.tradeNum" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="开仓价格" prop="openPrice">
            <el-input-number v-model="form.openPrice" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="杠杆" prop="leverage">
            <el-input-number v-model="form.leverage" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="开仓金额" prop="cost">
            <el-input-number v-model="form.cost" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="止损价格" prop="stopLoss">
            <el-input-number v-model="form.stopLoss" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="止盈价格">
            <el-input-number v-model="form.takeProfit" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="平仓价格">
            <el-input-number v-model="form.closePrice" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="交易状态" prop="operateStatus">
            <el-select v-model="form.operateStatus" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.invest_trade_operate_status"
                :key="item.id"
                :label="item.label"
                :value="parseInt(item.value)"
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
      <el-table ref="table" v-loading="crud.loading" :data="crud.data" size="small" style="width: 100%;" @selection-change="crud.selectionChangeHandler" @sort-change="sortChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="investProductName" label="投资产品" width="100" />
        <el-table-column prop="tradeType" label="交易类型">
          <template slot-scope="scope">
            {{ dict.label.invest_trade_type[scope.row.tradeType] }}
          </template>
        </el-table-column>
        <el-table-column prop="leverage" label="杠杆" />
        <el-table-column prop="tradeNum" label="交易数量" />
        <el-table-column prop="openPrice" label="开仓价格" :formatter="convertAmount" />
        <el-table-column prop="cost" label="开仓成本" :formatter="convertAmount" />
        <el-table-column prop="stopLoss" label="止损价格" :formatter="convertAmount" />
        <el-table-column prop="takeProfit" label="止盈价格" :formatter="convertAmount" />
        <el-table-column prop="closePrice" label="平仓价格" :formatter="convertAmount" />
        <el-table-column prop="profit" label="收益" sortable="custom" :formatter="convertAmount" />
        <el-table-column prop="operateStatus" label="交易状态">
          <template slot-scope="scope">
            {{ dict.label.invest_trade_operate_status[scope.row.operateStatus] }}
          </template>
        </el-table-column>
        <el-table-column prop="openTime" label="开仓时间" />
        <el-table-column prop="closeTime" label="平仓时间" />
        <el-table-column prop="score" label="评分">
          <template slot-scope="scope">
            <el-rate
              v-model="scope.row.score"
              disabled
              :icon-classes="iconClasses"
              void-icon-class="icon-rate-face-off"
              :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
            />
          </template>
        </el-table-column>
        <el-table-column v-if="checkPer(['admin','investTradeRecord:edit','investTradeRecord:del'])" label="操作" width="150px" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" style="margin-right: 2px" type="text">
              <router-link :to="'/invest/investTradeRecord/edit/' + scope.row.id">
                详情
              </router-link>
            </el-button>
            <el-popover
              :ref="scope.row.id"
              v-permission="['admin','investTradeRecord:del']"
              placement="top"
              width="200"
            >
              <p>确定删除该条记录吗？</p>
              <div style="text-align: right; margin: 0">
                <el-button size="mini" type="text" @click="$refs[scope.row.id].doClose()">取消</el-button>
                <el-button :loading="delLoading" type="primary" size="mini" @click="delMethod(scope.row.id)">确定</el-button>
              </div>
              <el-button slot="reference" type="text" size="mini" style="color: #FF4949">删除</el-button>
            </el-popover>
          </template>
        </el-table-column>
      </el-table>
      <!--分页组件-->
      <pagination />
    </div>
  </div>
</template>

<script>
import '@/assets/styles/fonts/style.css'
import { convertAmountToYuan } from '@/utils/numberUtil'
import crudInvestTradeRecord from '@/api/invest/investTradeRecord'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import pagination from '@crud/Pagination'
import { listAllProduct } from '@/api/invest/investProduct'
import DateRangePicker from '@/components/DateRangePicker/index.vue'

const defaultForm = { id: null, productId: null, tradeType: null, tradeNum: null, openPrice: null, leverage: null, cost: null, stopLoss: null, takeProfit: null, closePrice: null, operateStatus: null, createTime: null, updateBy: null, updateTime: null, createBy: null }
export default {
  name: 'InvestTradeRecord',
  components: { DateRangePicker, pagination, crudOperation, rrOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['invest_trade_type', 'invest_trade_operate_status'],
  cruds() {
    return CRUD({ title: '投资记录', url: 'api/investTradeRecord', idField: 'id', sort: 'id,desc', crudMethod: { ...crudInvestTradeRecord }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'investTradeRecord:add'],
        edit: ['admin', 'investTradeRecord:edit'],
        del: ['admin', 'investTradeRecord:del']
      },
      rules: {
        productId: [
          { required: true, message: '投资产品不能为空', trigger: 'blur' }
        ],
        tradeType: [
          { required: true, message: '交易类型不能为空', trigger: 'blur' }
        ],
        tradeNum: [
          { required: true, message: '交易数量不能为空', trigger: 'blur' }
        ],
        openPrice: [
          { required: true, message: '开仓价格不能为空', trigger: 'blur' }
        ],
        leverage: [
          { required: true, message: '杠杆不能为空', trigger: 'blur' }
        ],
        cost: [
          { required: true, message: '开仓金额不能为空', trigger: 'blur' }
        ],
        stopLoss: [
          { required: true, message: '止损价格不能为空', trigger: 'blur' }
        ],
        operateStatus: [
          { required: true, message: '交易状态不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'tradeType', display_name: '交易类型' },
        { key: 'operateStatus', display_name: '交易状态' }
      ],
      delLoading: false,
      productList: [],
      iconClasses: ['icon-rate-face-1', 'icon-rate-face-2', 'icon-rate-face-3']
    }
  },
  mounted() {
    this.refreshProductList()
  },
  created() {
    this.crud.optShow = {
      add: false,
      edit: false,
      del: false,
      download: true
    }
  },
  methods: {
    convertAmount(row, column, cellValue, index) {
      return convertAmountToYuan(cellValue)
    },
    // 钩子：在获取表格数据之前执行，false 则代表不获取数据
    [CRUD.HOOK.beforeRefresh]() {
      return true
    },
    refreshProductList() {
      listAllProduct().then(data => {
        this.productList = data.content
      })
    },
    delMethod(id) {
      this.delLoading = true
      crudInvestTradeRecord.del([id]).then(() => {
        this.delLoading = false
        this.$refs[id].doClose()
        this.crud.dleChangePage(1)
        this.crud.delSuccessNotify()
        this.crud.toQuery()
      }).catch(() => {
        this.delLoading = false
        this.$refs[id].doClose()
      })
    },
    doAdd() {
      this.$router.push('/invest/investTradeRecord/edit/:id')
    },
    sortChange(sortInfo) {
      console.log(sortInfo)
      if (sortInfo && sortInfo.prop && sortInfo.order) {
        this.query.sortField = sortInfo.prop
        this.query.sortOrder = sortInfo.order === 'ascending' ? 'asc' : 'desc'
      } else {
        this.query.sortField = null
        this.query.sortOrder = null
      }
      this.crud.toQuery()
    }
  }
}
</script>

<style scoped>

</style>
