<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">交易对</label>
        <el-input v-model="query.symbol" clearable placeholder="交易对" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">周期，单位分钟</label>
        <el-input v-model="query.period" clearable placeholder="周期，单位分钟" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.openTime"
          start-placeholder="openTimeStart"
          end-placeholder="openTimeStart"
          class="date-item"
        />
        <date-range-picker
          v-model="query.closeTime"
          start-placeholder="closeTimeStart"
          end-placeholder="closeTimeStart"
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
            未设置字典，请手动设置 Select
          </el-form-item>
          <el-form-item label="周期，单位分钟" prop="period">
            <el-input v-model="form.period" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="开盘时间" prop="openTime">
            <el-input v-model="form.openTime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="收盘时间" prop="closeTime">
            <el-input v-model="form.closeTime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="开盘价" prop="openPrice">
            <el-input v-model="form.openPrice" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="收盘价" prop="closePrice">
            <el-input v-model="form.closePrice" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="最高价" prop="highPrice">
            <el-input v-model="form.highPrice" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="最低价" prop="lowPrice">
            <el-input v-model="form.lowPrice" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交量" prop="volume">
            <el-input v-model="form.volume" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交额">
            <el-input v-model="form.turnover" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="成交笔数">
            <el-input v-model="form.tradeCount" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="主动买入成交量">
            <el-input v-model="form.buyVolume" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="主动买入成交额">
            <el-input v-model="form.buyTurnover" style="width: 370px;" />
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
        <el-table-column prop="symbol" label="交易对" />
        <el-table-column prop="period" label="周期，单位分钟" />
        <el-table-column prop="openTime" label="开盘时间" />
        <el-table-column prop="closeTime" label="收盘时间" />
        <el-table-column prop="openPrice" label="开盘价" />
        <el-table-column prop="closePrice" label="收盘价" />
        <el-table-column prop="highPrice" label="最高价" />
        <el-table-column prop="lowPrice" label="最低价" />
        <el-table-column prop="volume" label="成交量" />
        <el-table-column prop="turnover" label="成交额" />
        <el-table-column prop="tradeCount" label="成交笔数" />
        <el-table-column prop="buyVolume" label="主动买入成交量" />
        <el-table-column prop="buyTurnover" label="主动买入成交额" />
        <el-table-column v-if="checkPer(['admin','investKlinesRecord:edit','investKlinesRecord:del'])" label="操作" width="150px" align="center">
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
import crudInvestKlinesRecord from '@/api/investKlinesRecord'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, symbol: null, period: null, openTime: null, closeTime: null, openPrice: null, closePrice: null, highPrice: null, lowPrice: null, volume: null, turnover: null, tradeCount: null, buyVolume: null, buyTurnover: null }
export default {
  name: 'InvestKlinesRecord',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '投资K线', url: 'api/investKlinesRecord', idField: 'id', sort: 'id,desc', crudMethod: { ...crudInvestKlinesRecord }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'investKlinesRecord:add'],
        edit: ['admin', 'investKlinesRecord:edit'],
        del: ['admin', 'investKlinesRecord:del']
      },
      rules: {
        symbol: [
          { required: true, message: '交易对不能为空', trigger: 'blur' }
        ],
        period: [
          { required: true, message: '周期，单位分钟不能为空', trigger: 'blur' }
        ],
        openTime: [
          { required: true, message: '开盘时间不能为空', trigger: 'blur' }
        ],
        closeTime: [
          { required: true, message: '收盘时间不能为空', trigger: 'blur' }
        ],
        openPrice: [
          { required: true, message: '开盘价不能为空', trigger: 'blur' }
        ],
        closePrice: [
          { required: true, message: '收盘价不能为空', trigger: 'blur' }
        ],
        highPrice: [
          { required: true, message: '最高价不能为空', trigger: 'blur' }
        ],
        lowPrice: [
          { required: true, message: '最低价不能为空', trigger: 'blur' }
        ],
        volume: [
          { required: true, message: '成交量不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'symbol', display_name: '交易对' },
        { key: 'period', display_name: '周期，单位分钟' }
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
