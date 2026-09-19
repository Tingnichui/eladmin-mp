<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="币安账户用户编号" prop="uid">
            <el-input-number v-model="form.uid" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="现货交易对，如 BTCUSDT" prop="symbol">
            <el-input v-model="form.symbol" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="原始现货买入成交 ID" prop="tradeId">
            <el-input v-model="form.tradeId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="锁定为底仓的数量" prop="coreQty">
            <el-input-number v-model="form.coreQty" :controls="false" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" style="width: 370px;" />
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
        <el-table-column prop="symbol" label="现货交易对，如 BTCUSDT" />
        <el-table-column prop="tradeId" label="原始现货买入成交 ID" />
        <el-table-column prop="coreQty" label="锁定为底仓的数量" />
        <el-table-column prop="lockedAt" label="设为底仓时间" />
        <el-table-column prop="releasedAt" label="解除底仓时间，空表示仍在锁定" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column prop="createBy" label="创建者" />
        <el-table-column prop="updateBy" label="更新者" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column prop="updateTime" label="更新时间" />
        <el-table-column v-if="checkPer(['admin','binanceSpotCorePosition:edit'])" label="操作" width="180px" align="center">
          <template slot-scope="scope">
            <udOperation
              :data="scope.row"
              :permission="permission"
            />
            <el-button
              v-if="!scope.row.releasedAt"
              type="text"
              @click="releasePosition(scope.row)"
            >解除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!--分页组件-->
      <pagination />
    </div>
  </div>
</template>

<script>
import crudBinanceSpotCorePosition, { release } from '@/api/binanceSpotCorePosition'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, uid: null, symbol: null, tradeId: null, coreQty: null, lockedAt: null, releasedAt: null, remark: null, createBy: null, updateBy: null, createTime: null, updateTime: null }
export default {
  name: 'BinanceSpotCorePosition',
  components: { pagination, crudOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '现货底仓', url: 'api/binanceSpotCorePosition', idField: 'id', sort: 'id,desc', crudMethod: { ...crudBinanceSpotCorePosition }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'binanceSpotCorePosition:add'],
        edit: ['admin', 'binanceSpotCorePosition:edit'],
        del: []
      },
      rules: {
        uid: [
          { required: true, message: '币安账户用户编号不能为空', trigger: 'blur' }
        ],
        symbol: [
          { required: true, message: '现货交易对，如 BTCUSDT不能为空', trigger: 'blur' }
        ],
        tradeId: [
          { required: true, message: '原始现货买入成交 ID不能为空', trigger: 'blur' }
        ],
        coreQty: [
          { required: true, message: '锁定为底仓的数量不能为空', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    // 钩子：在获取表格数据之前执行，false 则代表不获取数据
    [CRUD.HOOK.beforeRefresh]() {
      return true
    },
    releasePosition(row) {
      this.$confirm('解除后该数量将重新参与后续 FIFO 撮合，是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => release(row.id)).then(() => {
        this.crud.notify('解除成功', CRUD.NOTIFICATION_TYPE.SUCCESS)
        this.crud.refresh()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>

</style>
