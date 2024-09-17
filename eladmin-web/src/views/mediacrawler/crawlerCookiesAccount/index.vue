<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">账号名称</label>
        <el-input v-model="query.accountName" clearable placeholder="账号名称" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">平台名称</label>
        <el-select
          v-model="query.platformName"
          clearable
          size="small"
          placeholder="操作类型"
          class="filter-item"
          style="width: 90px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.media_crawler_platform_name"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <!--        <label class="el-form-item-label">cookies</label>-->
        <!--        <el-input v-model="query.cookies" clearable placeholder="cookies" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />-->
        <label class="el-form-item-label">状态</label>
        <el-select
          v-model="query.status"
          clearable
          size="small"
          placeholder="操作类型"
          class="filter-item"
          style="width: 90px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.media_crawler_account_status"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <date-range-picker
          v-model="query.invalidTimestamp"
          start-placeholder="invalidTimestampStart"
          end-placeholder="invalidTimestampStart"
          class="date-item"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="平台名称" prop="platformName">
            <el-select v-model="form.platformName" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.media_crawler_platform_name"
                :key="item.id"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="账号名称" prop="accountName">
            <el-input v-model="form.accountName" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="cookies" prop="cookies">
            <el-input v-model="form.cookies" :rows="3" type="textarea" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="状态" prop="courseType">
            <el-radio v-for="item in dict.media_crawler_account_status" :key="item.id" v-model="form.status" :label="Number(item.value)">{{ item.label }}</el-radio>
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
        <el-table-column prop="accountName" label="账号名称" />
        <el-table-column prop="platformName" label="平台名称">
          <template slot-scope="scope">
            {{ dict.label.media_crawler_platform_name[scope.row.platformName] }}
          </template>
        </el-table-column>
        <!--        <el-table-column prop="cookies" label="cookies" />-->
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column prop="updateTime" label="更新时间" />
        <el-table-column prop="invalidTimestamp" label="失效时间">
          <template slot-scope="scope">
            {{ scope.row.invalidTimestamp !== 0 ? new Date(scope.row.invalidTimestamp).toLocaleString() : 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template slot-scope="scope">
            {{ dict.label.media_crawler_account_status[scope.row.status] }}
          </template>
        </el-table-column>
        <el-table-column v-if="checkPer(['admin','crawlerCookiesAccount:edit','crawlerCookiesAccount:del'])" label="操作" width="150px" align="center">
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
import crudCrawlerCookiesAccount from '@/api/crawlerCookiesAccount'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, accountName: null, platformName: null, cookies: null, createTime: null, updateTime: null, invalidTimestamp: null, status: null }
export default {
  name: 'CrawlerCookiesAccount',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['media_crawler_platform_name', 'media_crawler_account_status'],
  cruds() {
    return CRUD({ title: '自媒体账号', url: 'api/crawlerCookiesAccount', idField: 'id', sort: 'id,desc', crudMethod: { ...crudCrawlerCookiesAccount }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'crawlerCookiesAccount:add'],
        edit: ['admin', 'crawlerCookiesAccount:edit'],
        del: ['admin', 'crawlerCookiesAccount:del']
      },
      rules: {
        accountName: [
          { required: true, message: '账号名称不能为空', trigger: 'blur' }
        ],
        platformName: [
          { required: true, message: '平台名称不能为空', trigger: 'blur' }
        ],
        cookies: [
          { required: true, message: 'cookies不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'accountName', display_name: '账号名称' },
        { key: 'platformName', display_name: '平台名称' },
        { key: 'cookies', display_name: 'cookies' },
        { key: 'status', display_name: '状态' }
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
