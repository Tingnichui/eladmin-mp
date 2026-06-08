<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">交易方向</label>
        <el-select
          v-model="query.direction"
          clearable
          size="small"
          placeholder="交易方向"
          class="filter-item"
          style="width: 120px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_trade_analysis_direction"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">入场类型</label>
        <el-select
          v-model="query.entryType"
          clearable
          filterable
          size="small"
          placeholder="入场类型"
          class="filter-item"
          style="width: 210px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_trade_analysis_entry_type"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">交易质量</label>
        <el-select
          v-model="query.qualityLevel"
          clearable
          size="small"
          placeholder="交易质量"
          class="filter-item"
          style="width: 120px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_trade_quality_level"
            :key="item.value"
            :label="item.label"
            :value="parseInt(item.value)"
          />
        </el-select>
        <label class="el-form-item-label">开仓时间</label>
        <date-range-picker
          v-model="query.openTime"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          class="date-item"
          @change="crud.toQuery"
        />
        <label class="el-form-item-label">平仓时间</label>
        <date-range-picker
          v-model="query.closeTime"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          class="date-item"
          @change="crud.toQuery"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <invest-trade-analysis-form
        ref="tradeAnalysisForm"
        :crud="crud"
        :form="form"
        :rules="rules"
        :dict="dict"
        :images-upload-api="imagesUploadApi"
        :base-api="baseApi"
      />
      <!--表格渲染-->
      <el-table ref="table" v-loading="crud.loading" :data="crud.data" size="small" style="width: 100%;" @selection-change="crud.selectionChangeHandler">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="direction" label="交易方向">
          <template slot-scope="scope">
            {{ dict.label.invest_trade_analysis_direction[scope.row.direction] || scope.row.direction }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="交易金额" />
        <el-table-column prop="entryType" label="入场类型">
          <template slot-scope="scope">
            {{ dict.label.invest_trade_analysis_entry_type[scope.row.entryType] || scope.row.entryType }}
          </template>
        </el-table-column>
        <el-table-column prop="openTime" label="开仓时间" />
        <el-table-column prop="closeTime" label="平仓时间" />
        <el-table-column prop="openPrice" label="开仓价格" />
        <el-table-column prop="closePrice" label="平仓价格" />
        <el-table-column prop="netProfit" label="盈亏金额" />
        <el-table-column prop="score" label="开仓评分" />
        <el-table-column prop="qualityLevel" label="交易质量">
          <template slot-scope="scope">
            {{ dict.label.invest_trade_quality_level[scope.row.qualityLevel] || scope.row.qualityLevel }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column v-if="checkPer(['admin','investTradeAnalysis:edit','investTradeAnalysis:del'])" label="操作" width="150px" align="center">
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
import crudInvestTradeAnalysis from '@/api/investTradeAnalysis'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'
import InvestTradeAnalysisForm from './components/InvestTradeAnalysisForm'
import { mapGetters } from 'vuex'

const defaultForm = { id: null, direction: null, amount: null, entryType: null, openTime: null, closeTime: null, openPrice: null, closePrice: null, netProfit: null, openReason: null, openKlineImages: null, closeKlineImages: null, score: null, reviewConclusion: null, qualityLevel: null, remark: null, createBy: null, updateBy: null, createTime: null, updateTime: null }
export default {
  name: 'InvestTradeAnalysis',
  components: { pagination, crudOperation, rrOperation, udOperation, DateRangePicker, InvestTradeAnalysisForm },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['invest_trade_analysis_direction', 'invest_trade_analysis_entry_type', 'invest_trade_quality_level'],
  data() {
    return {
      permission: {
        add: ['admin', 'investTradeAnalysis:add'],
        edit: ['admin', 'investTradeAnalysis:edit'],
        del: ['admin', 'investTradeAnalysis:del']
      },
      rules: {
        direction: [
          { required: true, message: '方向不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'direction', display_name: '交易方向' },
        { key: 'entryType', display_name: '入场类型' },
        { key: 'qualityLevel', display_name: '交易质量' }
      ]
    }
  },
  computed: {
    ...mapGetters([
      'imagesUploadApi',
      'baseApi'
    ])
  },
  cruds() {
    return CRUD({ title: '交易分析', url: 'api/investTradeAnalysis', idField: 'id', sort: 'id,desc', crudMethod: { ...crudInvestTradeAnalysis }})
  },
  methods: {
    // 钩子：在获取表格数据之前执行，false 则代表不获取数据
    [CRUD.HOOK.beforeRefresh]() {
      return true
    },
    [CRUD.HOOK.afterToCU]() {
      this.$nextTick(() => {
        if (this.$refs.tradeAnalysisForm) {
          this.$refs.tradeAnalysisForm.afterToCU()
        }
      })
    },
    [CRUD.HOOK.beforeSubmit]() {
      if (this.$refs.tradeAnalysisForm) {
        this.$refs.tradeAnalysisForm.beforeSubmit()
      }
      return true
    },
    [CRUD.HOOK.afterSubmit]() {
      if (this.$refs.tradeAnalysisForm) {
        this.$refs.tradeAnalysisForm.afterSubmit()
      }
    },
    [CRUD.HOOK.afterAddCancel]() {
      if (this.$refs.tradeAnalysisForm) {
        this.$refs.tradeAnalysisForm.afterAddCancel()
      }
    },
    [CRUD.HOOK.afterEditCancel]() {
      if (this.$refs.tradeAnalysisForm) {
        this.$refs.tradeAnalysisForm.afterEditCancel()
      }
    }
  }
}
</script>
