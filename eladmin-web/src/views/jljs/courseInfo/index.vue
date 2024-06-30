<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">名称</label>
        <el-input v-model="query.courseName" clearable placeholder="名称" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">描述</label>
        <el-input v-model="query.courseDescribe" clearable placeholder="描述" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">类型</label>
        <el-input v-model="query.courseType" clearable placeholder="类型" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <date-range-picker
          v-model="query.courseUsePeriodDays"
          start-placeholder="courseUsePeriodDaysStart"
          end-placeholder="courseUsePeriodDaysStart"
          class="date-item"
        />
        <date-range-picker
          v-model="query.courseAvailableQuantity"
          start-placeholder="courseAvailableQuantityStart"
          end-placeholder="courseAvailableQuantityStart"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="名称" prop="courseName">
            <el-input v-model="form.courseName" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="价格" prop="coursePrice">
            <el-input v-model="form.coursePrice" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.courseDescribe" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="类型" prop="courseType">
            未设置字典，请手动设置 Radio
          </el-form-item>
          <el-form-item label="使用期限" prop="courseUsePeriodDays">
            <el-input v-model="form.courseUsePeriodDays" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="可使用数量" prop="courseAvailableQuantity">
            <el-input v-model="form.courseAvailableQuantity" style="width: 370px;" />
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
        <el-table-column prop="courseName" label="名称" />
        <el-table-column prop="coursePrice" label="价格" />
        <el-table-column prop="courseDescribe" label="描述" />
        <el-table-column prop="courseType" label="类型" />
        <el-table-column prop="courseUsePeriodDays" label="使用期限" />
        <el-table-column prop="courseAvailableQuantity" label="可使用数量" />
        <el-table-column v-if="checkPer(['admin','jljsCourseInfo:edit','jljsCourseInfo:del'])" label="操作" width="150px" align="center">
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
import crudJljsCourseInfo from '@/api/jljsCourseInfo'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, createBy: null, createTime: null, updateBy: null, updateTime: null, delFlag: null, courseName: null, coursePrice: null, courseDescribe: null, courseType: null, courseUsePeriodDays: null, courseAvailableQuantity: null }
export default {
  name: 'JljsCourseInfo',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '课程管理', url: 'api/jljsCourseInfo', idField: 'id', sort: 'id,desc', crudMethod: { ...crudJljsCourseInfo }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'jljsCourseInfo:add'],
        edit: ['admin', 'jljsCourseInfo:edit'],
        del: ['admin', 'jljsCourseInfo:del']
      },
      rules: {
        courseName: [
          { required: true, message: '名称不能为空', trigger: 'blur' }
        ],
        coursePrice: [
          { required: true, message: '价格不能为空', trigger: 'blur' }
        ],
        courseType: [
          { required: true, message: '类型不能为空', trigger: 'blur' }
        ],
        courseUsePeriodDays: [
          { required: true, message: '使用期限不能为空', trigger: 'blur' }
        ],
        courseAvailableQuantity: [
          { required: true, message: '可使用数量不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'courseName', display_name: '名称' },
        { key: 'courseDescribe', display_name: '描述' },
        { key: 'courseType', display_name: '类型' }
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
