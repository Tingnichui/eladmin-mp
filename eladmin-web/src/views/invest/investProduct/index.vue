<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">产品类型</label>
        <el-input v-model="query.investType" clearable placeholder="投资类型；1股票；2期货；3加密货币" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">产品编码</label>
        <el-input v-model="query.investProductCode" clearable placeholder="投资产品编码" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">产品名称</label>
        <el-input v-model="query.investProductName" clearable placeholder="投资产品名称" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">计量单位</label>
        <el-input v-model="query.measurementUnit" clearable placeholder="计量单位" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.minSize"
          start-placeholder="minSizeStart"
          end-placeholder="minSizeStart"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="120px">
          <el-form-item label="产品类型" prop="investType">
            <el-select v-model="form.investType" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.invest_type"
                :key="item.id"
                :label="item.label"
                :value="parseInt(item.value)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="产品编码">
            <el-input v-model="form.investProductCode" style="width: 300px;" />
          </el-form-item>
          <el-form-item label="产品名称" prop="investProductName">
            <el-input v-model="form.investProductName" style="width: 300px;" />
          </el-form-item>
          <el-form-item label="最小购买单位" prop="minSize">
            <el-input v-model="form.minSize" style="width: 300px;" placeholder="例如最小购买0.001，那么这边设置为1000" />
          </el-form-item>
          <el-form-item label="计量单位" prop="measurementUnit">
            <el-input v-model="form.measurementUnit" style="width: 300px;" />
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
        <el-table-column prop="investType" label="产品类型">
          <template slot-scope="scope">
            {{ dict.label.invest_type[scope.row.investType.toString()] }}
          </template>
        </el-table-column>
        <el-table-column prop="investProductCode" label="产品编码" />
        <el-table-column prop="investProductName" label="产品名称" />
        <el-table-column prop="minSize" label="最小购买单位" />
        <el-table-column prop="measurementUnit" label="计量单位" />
        <el-table-column v-if="checkPer(['admin','investProduct:edit','investProduct:del'])" label="操作" width="150px" align="center">
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
import crudInvestProduct from '@/api/invest/investProduct'
import CRUD, { crud, form, header, presenter } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, investType: null, investProductCode: null, investProductName: null, minSize: null, measurementUnit: null, createTime: null, updateBy: null, updateTime: null, createBy: null }
export default {
  name: 'InvestProduct',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['invest_type'],
  cruds() {
    return CRUD({ title: '投资产品', url: 'api/investProduct', idField: 'id', sort: 'id,desc', crudMethod: { ...crudInvestProduct }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'investProduct:add'],
        edit: ['admin', 'investProduct:edit'],
        del: ['admin', 'investProduct:del']
      },
      rules: {
        investType: [
          { required: true, message: '投资类型不能为空', trigger: 'blur' }
        ],
        investProductName: [
          { required: true, message: '投资产品名称不能为空', trigger: 'blur' }
        ],
        minSize: [
          { required: true, message: '最小购买单位不能为空', trigger: 'blur' }
        ],
        measurementUnit: [
          { required: true, message: '计量单位不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'investType', display_name: '产品类型' },
        { key: 'investProductCode', display_name: '产品编码' },
        { key: 'investProductName', display_name: '产品名称' },
        { key: 'measurementUnit', display_name: '计量单位' }
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
