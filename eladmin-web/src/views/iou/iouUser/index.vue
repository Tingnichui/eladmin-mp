<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">手机号</label>
        <el-input v-model="query.mobile" clearable placeholder="手机号" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">真实姓名</label>
        <el-input v-model="query.realName" clearable placeholder="真实姓名" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">认证状态</label>
        <el-input v-model="query.verifyStatus" clearable placeholder="认证状态" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.createTime"
          start-placeholder="createTimeStart"
          end-placeholder="createTimeEnd"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="手机号" prop="mobile">
            <el-input v-model="form.mobile" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input v-model="form.password" type="password" autocomplete="new-password" placeholder="新增必填，编辑留空保留原密码" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="form.idNo" type="password" autocomplete="off" placeholder="编辑留空保留原证件" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="form.realName" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="认证状态">
            <el-select v-model="form.verifyStatus" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.iou_user_verify_status"
                :key="item.id"
                :label="item.label"
                :value="Number(item.value)"
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
        <el-table-column prop="mobile" label="手机号" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="verifyStatus" label="认证状态">
          <template slot-scope="scope">
            {{ dict.label.iou_user_verify_status[scope.row.verifyStatus] }}
          </template>
        </el-table-column>
        <el-table-column prop="verifiedAt" label="认证通过时间" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column prop="updateTime" label="更新时间" />
        <el-table-column v-if="checkPer(['admin','iouUser:edit','iouUser:del'])" label="操作" width="150px" align="center">
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
import crudIouUser from '@/api/iouUser'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'

const defaultForm = { password: null, idNo: null, id: null, mobile: null, passwordHash: null, realName: null, idNoEncrypted: null, idNoHash: null, verifyStatus: 0, verifiedAt: null, createTime: null, updateTime: null }
export default {
  name: 'IouUser',
  components: { DateRangePicker, pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['iou_user_verify_status'],
  cruds() {
    return CRUD({ title: '借条用户', url: 'api/iouUser', idField: 'id', sort: 'id,desc', crudMethod: { ...crudIouUser }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'iouUser:add'],
        edit: ['admin', 'iouUser:edit'],
        del: ['admin', 'iouUser:del']
      },
      rules: {
        mobile: [
          { required: true, message: '手机号不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'mobile', display_name: '手机号' },
        { key: 'realName', display_name: '真实姓名' },
        { key: 'verifyStatus', display_name: '认证状态' }
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
