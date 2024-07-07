<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">合同编号</label>
        <el-input v-model="query.contractInfoId" clearable placeholder="合同编号" style="width: 200px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">教练</label>
        <el-select
          v-model="query.coachId"
          clearable
          filterable
          size="small"
          placeholder="教练"
          class="filter-item"
          style="width: 185px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in coachList"
            :key="item.id"
            :label="item.coachName"
            :value="item.id"
          />
        </el-select>
        <label class="el-form-item-label">会员</label>
        <el-select
          v-model="query.memberId"
          clearable
          filterable
          size="small"
          placeholder="会员"
          class="filter-item"
          style="width: 185px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in memberList"
            :key="item.id"
            :label="item.memberName"
            :value="item.id"
          />
        </el-select>
        <label class="el-form-item-label">上课时间</label>
        <date-range-picker v-model="query.classBeginTime" class="date-item" @change="crud.toQuery" />
        <label class="el-form-item-label">课程备注</label>
        <el-input v-model="query.classRemark" clearable placeholder="课程备注" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="会员" prop="memberId">
            <el-select
              v-model="form.memberId"
              clearable
              filterable
              size="small"
              placeholder="会员"
              class="filter-item"
              style="width: 185px"
            >
              <el-option
                v-for="item in memberList"
                :key="item.id"
                :label="item.memberName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="教练" prop="coachId">
            <el-select
              v-model="form.coachId"
              clearable
              filterable
              size="small"
              placeholder="教练"
              class="filter-item"
              style="width: 185px"
            >
              <el-option
                v-for="item in coachList"
                :key="item.id"
                :label="item.coachName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="课程开始时间" prop="classBeginTime">
            <el-date-picker v-model="form.classBeginTime" type="datetime" style="width: 370px;" @change="changeClassBeginTime" />
          </el-form-item>
          <el-form-item label="课程结束时间" prop="classEndTime">
            <el-date-picker v-model="form.classEndTime" type="datetime" style="width: 370px;" @change="changeClassEndTime" />
          </el-form-item>
          <el-form-item label="课程备注">
            <el-input v-model="form.classRemark" style="width: 370px;" />
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
        <el-table-column prop="memberId" label="会员">
          <template slot-scope="scope">
            <div v-for="item in memberList" :key="item.id">
              <span v-if="item.id === scope.row.memberId">{{ item.memberName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="coachId" label="教练">
          <template slot-scope="scope">
            <div v-for="item in coachList" :key="item.id">
              <span v-if="item.id === scope.row.coachId"> {{ item.coachName }} </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="classBeginTime" label="开始时间" />
        <el-table-column prop="classEndTime" label="结束时间" />
        <el-table-column prop="classRemark" label="备注" />
        <el-table-column prop="contractInfoId" label="关联合同" />
        <el-table-column v-if="checkPer(['admin','jljsClassRecord:edit','jljsClassRecord:del'])" label="操作" width="150px" align="center">
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
import crudJljsClassRecord from '@/api/jljs/jljsClassRecord'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import { listAllMember } from '@/api/jljs/jljsMemberInfo'
import { listAllCoach } from '@/api/jljs/jljsCoachInfo'
import DateRangePicker from '@/components/DateRangePicker/index.vue'

const defaultForm = { id: null, createBy: null, createTime: null, updateBy: null, updateTime: null, delFlag: null, coachId: null, memberId: null, contractInfoId: null, classBeginTime: null, classEndTime: null, classRemark: null }
export default {
  name: 'JljsClassRecord',
  components: { DateRangePicker, pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '上课记录', url: 'api/jljsClassRecord', idField: 'id', sort: 'id,desc', crudMethod: { ...crudJljsClassRecord }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'jljsClassRecord:add'],
        edit: ['admin', 'jljsClassRecord:edit'],
        del: ['admin', 'jljsClassRecord:del']
      },
      rules: {
        coachId: [
          { required: true, message: '教练id不能为空', trigger: 'blur' }
        ],
        memberId: [
          { required: true, message: '会员id不能为空', trigger: 'blur' }
        ],
        classBeginTime: [
          { required: true, message: '课程开始时间不能为空', trigger: 'blur' }
        ],
        classEndTime: [
          { required: true, message: '课程结束时间不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'coachId', display_name: '教练id' },
        { key: 'memberId', display_name: '会员id' },
        { key: 'contractInfoId', display_name: '关联合同id' },
        { key: 'classRemark', display_name: '课程备注' }
      ],
      memberList: [],
      coachList: []
    }
  },
  mounted() {
    this.refreshMemberList()
    this.refreshCoachList()
  },
  methods: {
    // 钩子：在获取表格数据之前执行，false 则代表不获取数据
    [CRUD.HOOK.beforeRefresh]() {
      return true
    },
    changeClassBeginTime(value) {
      if (value) {
        if (!this.form.classEndTime) {
          this.form.classEndTime = new Date(value.getTime() + 60 * 60 * 1000)
        }
      }
    },
    changeClassEndTime(value) {
      if (value) {
        if (!this.form.classBeginTime) {
          this.form.classBeginTime = new Date(value.getTime() - 60 * 60 * 1000)
        }
      }
    },
    refreshMemberList() {
      listAllMember().then(data => {
        this.memberList = data.content
      })
    },
    refreshCoachList() {
      listAllCoach().then(data => {
        this.coachList = data.content
      })
    }
  }
}
</script>

<style scoped>

</style>
