<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
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
        <label class="el-form-item-label">开单教练</label>
        <el-select
          v-model="query.belongCoachId"
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
        <label class="el-form-item-label">合同状态</label>
        <el-select
          v-model="query.contractStatus"
          clearable
          size="small"
          placeholder="合同状态"
          class="filter-item"
          style="width: 90px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.jljs_contract_status"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">课程</label>
        <el-select
          v-model="query.courseInfoId"
          clearable
          size="small"
          placeholder="课程"
          class="filter-item"
          style="width: 90px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in courseList"
            :key="item.id"
            :label="item.courseName"
            :value="item.id"
          />
        </el-select>
        <label class="el-form-item-label">备注</label>
        <el-input v-model="query.contractRemark" clearable placeholder="备注" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">合同编号</label>
        <el-input v-model="query.id" clearable placeholder="合同编号" style="width: 200px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
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
          <el-form-item label="开单教练" prop="belongCoachId">
            <el-select
              v-model="form.belongCoachId"
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
          <el-form-item label="课程" prop="courseInfoId">
            <el-select
              v-model="form.courseInfoId"
              clearable
              filterable
              size="small"
              placeholder="课程"
              class="filter-item"
              style="width: 185px"
              @change="changeCourse"
            >
              <el-option
                v-for="item in courseList"
                :key="item.id"
                :label="item.courseName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="合同金额" prop="contractAmount">
            <el-input v-model="form.contractAmount" type="number" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="实收金额" prop="actualChargeAmount">
            <el-input v-model="form.actualChargeAmount" type="number" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="使用期限" prop="courseUsePeriodDays">
            <el-input v-model="form.courseUsePeriodDays" type="number" placeholder="有效天数，例如：30天" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="课程类型" prop="courseType">
            <el-radio v-for="item in dict.jljs_course_type" :key="item.id" v-model="form.courseType" :label="item.value">{{ item.label }}</el-radio>
          </el-form-item>
          <el-form-item label="可使用数" prop="courseAvailableQuantity">
            <el-input v-model="form.courseAvailableQuantity" type="number" placeholder="根据课程类型，例如：15次，30天" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.contractRemark" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="">
            <span>{{ `合同金额：${form.contractAmount || '0'}元，实收：${form.actualChargeAmount || '0'} 元，使用期限 ${form.courseUsePeriodDays || '0'} 天。
                        自开卡 ${form.courseUsePeriodDays || '0'} 天内，
                        可使用 ${form.courseAvailableQuantity || '0'} ${form.courseType && dict.label.jljs_course_type[form.courseType] ? dict.label.jljs_course_type[form.courseType].charAt(1): ''}` }}</span>
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
        <el-table-column prop="memberName" label="会员" />
        <el-table-column prop="belongCoachName" label="开单教练" />
        <el-table-column prop="courseName" label="课程" />
        <el-table-column prop="contractStatus" label="合同状态">
          <template slot-scope="scope">
            {{ dict.label.jljs_contract_status[scope.row.contractStatus] }}
          </template>
        </el-table-column>
        <el-table-column
          label="已收/总计"
        >
          <template #default="scope">
            <span
              :style="scope.row.actualChargeAmount/scope.row.contractAmount === 0 ? 'color:#f56c6c' :
                (scope.row.actualChargeAmount/scope.row.contractAmount === 1 ? 'color:#67c23a' : 'color:#FF9800')"
            >
              {{ scope.row.actualChargeAmount }} / {{ scope.row.contractAmount }}
            </span>
          </template>
        </el-table-column>
        <!-- <el-table-column
            label="起始日期"
        >
          <template #default="scope">
            <div v-if="scope.row.useBeginDate && scope.row.useEndDate">
              {{
                `${scope.row.useBeginDate} ~ ${scope.row.useEndDate}`
              }}
            </div>
            <div v-else>
              -
            </div>
          </template>
        </el-table-column> -->
        <!-- <el-table-column prop="actualChargeAmount" label="实际收取金额" /> -->
        <!-- <el-table-column prop="contractAmount" label="合同金额" /> -->
        <el-table-column prop="useBeginDate" label="开始日期" />
        <el-table-column prop="useEndDate" label="结束日期" />
        <el-table-column
          label="剩余数量"
          prop="courseRemainQuantity"
        >
          <template #default="scope">
            <div v-if="scope.row.courseRemainQuantity || scope.row.courseRemainQuantity === 0">
              {{ `${scope.row.courseRemainQuantity}${scope.row.courseType === '1' ? '次' : '天'}` }}
            </div>
            <div v-else>
              -
            </div>
          </template>
        </el-table-column>
        <!-- <el-table-column prop="buyTime" label="购买日期" /> -->
        <el-table-column prop="courseTotalStopDays" label="总暂停天数" />
        <el-table-column
          label="实际上课"
          prop="courseUseQuantity"
          :width="150"
        >
          <template #default="scope">
            <div v-if="scope.row.courseUseQuantity || scope.row.courseUseQuantity === 0">
              <el-tooltip
                effect="dark"
                :content="`${scope.row.courseUseQuantity} / ${scope.row.courseAvailableQuantity} ${scope.row.courseType === '1' ? '次' : '天'}`"
                placement="top"
              >
                <progress
                  :value="scope.row.courseUseQuantity"
                  :max="scope.row.courseAvailableQuantity"
                />
              </el-tooltip>
            </div>
            <div v-else>
              -
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="contractRemark" label="备注" />
        <el-table-column prop="id" label="编号" />
        <!-- <el-table-column prop="courseType" label="课程类型">
          <template slot-scope="scope">
            {{ dict.label.jljs_course_type[scope.row.courseType] }}
          </template>
        </el-table-column> -->
        <!-- <el-table-column prop="courseUsePeriodDays" label="使用期限" /> -->
        <!-- <el-table-column prop="courseAvailableQuantity" label="可使用数量" /> -->
        <!-- <el-table-column prop="courseRemainQuantity" label="剩余数量" /> -->
        <!-- <el-table-column prop="courseUseQuantity" label="已使用数量" /> -->
        <el-table-column v-if="checkPer(['admin','jljsContractInfo:edit','jljsContractInfo:del'])" label="操作" width="150px" align="center">
          <template slot-scope="scope">
            <el-button v-permission="['admin','jljsContractInfo:edit']" size="mini" style="margin-right: 3px;" type="text" @click="crud.toEdit(scope.row)">编辑</el-button>
            <el-dropdown v-permission="['admin','jljsContractOperateRecord:add']" style="margin-left: -2px">
              <span style="cursor: pointer;color: #1890ff;font-size: 12px;">
                操作
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item v-if="scope.row.contractStatus === '1'">
                  <router-link :to="`/jljs/contractOperateRecord/${scope.row.id}/1`">
                    开卡
                  </router-link>
                </el-dropdown-item>
                <el-dropdown-item v-if="scope.row.contractStatus === '2' || scope.row.contractStatus === '4'">
                  <router-link :to="`/jljs/contractOperateRecord/${scope.row.id}/2`">
                    暂停
                  </router-link>
                </el-dropdown-item>
                <!--                <el-dropdown-item v-if="scope.row.contractStatus !== '5'">-->
                <!--                  <router-link :to="`/jljs/contractOperateRecord/${scope.row.id}/3`">-->
                <!--                    退课-->
                <!--                  </router-link>-->
                <!--                </el-dropdown-item>-->
                <el-dropdown-item>
                  <router-link :to="`/jljs/contractOperateRecord/${scope.row.id}/:operateType`">
                    记录
                  </router-link>
                </el-dropdown-item>
                <!-- <el-dropdown-item>补缴</el-dropdown-item> -->
              </el-dropdown-menu>
            </el-dropdown>
            <el-popover
              :ref="scope.row.id"
              v-permission="['admin','jljsContractInfo:del']"
              placement="top"
              width="200"
            >
              <p>确定删除该合同吗？</p>
              <div style="text-align: right; margin: 0">
                <el-button size="mini" type="text" @click="$refs[scope.row.id].doClose()">取消</el-button>
                <el-button :loading="delLoading" type="primary" size="mini" @click="delMethod(scope.row.id)">确定</el-button>
              </div>
              <el-button slot="reference" type="text" size="mini">删除</el-button>
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
import crudJljsContractInfo from '@/api/jljs/jljsContractInfo'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import pagination from '@crud/Pagination'
import { listAllMember } from '@/api/jljs/jljsMemberInfo'
import { listAllCoach } from '@/api/jljs/jljsCoachInfo'
import { listAllCourse } from '@/api/jljs/jljsCourseInfo'

const defaultForm = { id: null, createBy: null, createTime: null, updateBy: null, updateTime: null, delFlag: null, memberId: null, belongCoachId: null, contractAmount: null, contractStatus: null, useBeginDate: null, useEndDate: null, buyTime: null, contractRemark: null, actualChargeAmount: null, courseInfoId: null, courseType: null, courseUsePeriodDays: null, courseAvailableQuantity: null, courseRemainQuantity: null, courseUseQuantity: null, courseTotalStopDays: null }
export default {
  name: 'JljsContractInfo',
  components: { pagination, crudOperation, rrOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['jljs_contract_status', 'jljs_course_type'],
  cruds() {
    return CRUD({ title: '合同管理', url: 'api/jljsContractInfo', idField: 'id', sort: 'id,desc', crudMethod: { ...crudJljsContractInfo }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'jljsContractInfo:add'],
        edit: ['admin', 'jljsContractInfo:edit'],
        del: ['admin', 'jljsContractInfo:del']
      },
      rules: {
        memberId: [
          { required: true, message: '会员不能为空', trigger: 'blur' }
        ],
        belongCoachId: [
          { required: true, message: '开单教练不能为空', trigger: 'blur' }
        ],
        contractAmount: [
          { required: true, message: '合同金额不能为空', trigger: 'blur' }
        ],
        actualChargeAmount: [
          { required: true, message: '实际收取金额不能为空', trigger: 'blur' }
        ],
        courseInfoId: [
          { required: true, message: '课程不能为空', trigger: 'blur' }
        ],
        courseType: [
          { required: true, message: '课程类型不能为空', trigger: 'blur' }
        ],
        courseUsePeriodDays: [
          { required: true, message: '使用期限不能为空', trigger: 'blur' }
        ],
        courseAvailableQuantity: [
          { required: true, message: '可使用数量不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'memberId', display_name: '会员' },
        { key: 'belongCoachId', display_name: '开单教练' },
        { key: 'contractStatus', display_name: '合同状态' },
        { key: 'contractRemark', display_name: '备注' }
      ],
      delLoading: false,
      memberList: [],
      coachList: [],
      courseList: []
    }
  },
  mounted() {
    this.refreshMemberList()
    this.refreshCoachList()
    this.refreshCourseList()
  },
  methods: {
    // 钩子：在获取表格数据之前执行，false 则代表不获取数据
    [CRUD.HOOK.beforeRefresh]() {
      return true
    },
    changeCourse(value) {
      const courseInfo = this.courseList.find(v => v.id === value)
      this.form.contractAmount = courseInfo.coursePrice
      this.form.courseUsePeriodDays = courseInfo.courseUsePeriodDays
      this.form.courseAvailableQuantity = courseInfo.courseAvailableQuantity
      this.form.courseType = courseInfo.courseType
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
    },
    refreshCourseList() {
      listAllCourse().then(data => {
        this.courseList = data.content
      })
    },
    delMethod(id) {
      this.delLoading = true
      crudJljsContractInfo.del([id]).then(() => {
        this.delLoading = false
        this.$refs[id].doClose()
        this.crud.dleChangePage(1)
        this.crud.delSuccessNotify()
        this.crud.toQuery()
      }).catch(() => {
        this.delLoading = false
        this.$refs[id].doClose()
      })
    }
  }
}
</script>

<style scoped>

</style>
