<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">姓名</label>
        <el-input v-model="query.memberName" clearable placeholder="姓名" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">性别</label>
        <el-select
          v-model="query.memberGender"
          clearable
          size="small"
          placeholder="性别"
          class="filter-item"
          style="width: 90px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.jljs_member_gender"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">手机号</label>
        <el-input v-model="query.memberPhoneNum" clearable placeholder="手机号" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">生日</label>
        <el-input v-model="query.birthDay" clearable placeholder="生日" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.memberAge"
          start-placeholder="memberAgeStart"
          end-placeholder="memberAgeStart"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="姓名" prop="memberName">
            <el-input v-model="form.memberName" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio v-for="item in dict.jljs_member_gender" :key="item.id" v-model="form.memberGender" :label="item.value">{{ item.label }}</el-radio>
          </el-form-item>
          <el-form-item label="年龄">
            <el-input v-model="form.memberAge" type="number" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.memberPhoneNum" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="生日" prop="birthDay">
            <el-input v-model="form.birthDay" style="width: 370px;" />
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
        <el-table-column prop="memberName" label="姓名" />
        <el-table-column prop="memberGender" label="性别">
          <template slot-scope="scope">
            {{ dict.label.jljs_member_gender[scope.row.memberGender] }}
          </template>
        </el-table-column>
        <el-table-column prop="memberAge" label="年龄" />
        <el-table-column prop="memberPhoneNum" label="手机号" />
        <el-table-column prop="birthDay" label="生日" />
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column v-if="checkPer(['admin','jljsMemberInfo:edit','jljsMemberInfo:del'])" label="操作" width="150px" align="center">
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
import crudJljsMemberInfo from '@/api/jljs/jljsMemberInfo'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, createBy: null, createTime: null, updateBy: null, updateTime: null, delFlag: null, memberName: null, memberGender: null, memberAge: null, memberPhoneNum: null, birthDay: null }
export default {
  name: 'JljsMemberInfo',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['jljs_member_gender'],
  cruds() {
    return CRUD({ title: '会员管理', url: 'api/jljsMemberInfo', idField: 'id', sort: 'id,desc', crudMethod: { ...crudJljsMemberInfo }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'jljsMemberInfo:add'],
        edit: ['admin', 'jljsMemberInfo:edit'],
        del: ['admin', 'jljsMemberInfo:del']
      },
      rules: {
        memberName: [
          { required: true, message: '姓名不能为空', trigger: 'blur' }
        ],
        birthDay: [
          { validator: (rule, value, callback) => {
            // 如果 birthday 为空，直接返回
            if (!value) {
              callback()
            }
            // 定义一个正则表达式来匹配 mm/dd 格式
            const regex = /^(0[1-9]|1[0-2])\/(0[1-9]|[12][0-9]|3[01])$/
            // 验证 birthday 是否符合 mm/dd 格式
            if (regex.test(value)) {
              callback()
            } else {
              callback(new Error('生日格式不正确，请使用 mm/dd 格式，例如：05/20'))
            }
          }, trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'memberName', display_name: '姓名' },
        { key: 'memberGender', display_name: '性别' },
        { key: 'memberPhoneNum', display_name: '手机号' },
        { key: 'birthDay', display_name: '生日' }
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
