<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">自媒体平台</label>
        <el-select
          v-model="query.platform"
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
        <label class="el-form-item-label">爬取类型</label>
        <el-select
          v-model="query.crawlerType"
          clearable
          size="small"
          placeholder="操作类型"
          class="filter-item"
          style="width: 90px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.media_crawler_crawler_type"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">关键词</label>
        <el-input v-model="query.keywords" clearable placeholder="关键词" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">爬虫状态</label>
        <el-select
          v-model="query.crawlerStatus"
          clearable
          size="small"
          placeholder="操作类型"
          class="filter-item"
          style="width: 90px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.media_crawler_crawler_status"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="自媒体平台" prop="platform">
            <el-select v-model="form.platform" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.media_crawler_platform_name"
                :key="item.id"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="爬取类型" prop="crawlerType">
            <el-select v-model="form.crawlerType" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.media_crawler_crawler_type"
                :key="item.id"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="form.keywords" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="开始页数" prop="startPage">
            <el-input v-model="form.startPage" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="爬虫状态" prop="crawlerStatus">
            <el-select v-model="form.crawlerStatus" filterable placeholder="请选择">
              <el-option
                v-for="item in dict.media_crawler_crawler_status"
                :key="item.id"
                :label="item.label"
                :value="item.value"
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
        <el-table-column prop="platform" label="自媒体平台">
          <template slot-scope="scope">
            {{ dict.label.media_crawler_platform_name[scope.row.platform] }}
          </template>
        </el-table-column>
        <el-table-column prop="crawlerType" label="爬取类型">
          <template slot-scope="scope">
            {{ dict.label.media_crawler_crawler_type[scope.row.crawlerType] }}
          </template>
        </el-table-column>
        <el-table-column prop="keywords" label="关键词" />
        <el-table-column prop="startPage" label="开始页数" />
        <el-table-column prop="endPage" label="结束页数" />
        <el-table-column prop="crawlerStatus" label="爬虫状态">
          <template slot-scope="scope">
            {{ dict.label.media_crawler_crawler_status[scope.row.crawlerStatus] }}
          </template>
        </el-table-column>
        <!--        <el-table-column prop="errorMsg" label="异常信息" />-->
        <el-table-column prop="logPath" label="日志路径" />
        <el-table-column prop="startTime" label="开始时间" />
        <el-table-column prop="endTime" label="结束时间" />
        <el-table-column v-if="checkPer(['admin','crawlerRecord:edit','crawlerRecord:del'])" label="操作" width="150px" align="center">
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
import crudCrawlerRecord from '@/api/crawlerRecord'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, platform: null, crawlerType: null, keywords: null, startPage: null, endPage: null, crawlerStatus: null, errorMsg: null, logPath: null, startTime: null, endTime: null, createBy: null, updateBy: null, createTime: null, updateTime: null }
export default {
  name: 'CrawlerRecord',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['media_crawler_platform_name', 'media_crawler_crawler_type', 'media_crawler_crawler_status'],
  cruds() {
    return CRUD({ title: '爬虫记录', url: 'api/crawlerRecord', idField: 'id', sort: 'id,desc', crudMethod: { ...crudCrawlerRecord }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'crawlerRecord:add'],
        edit: ['admin', 'crawlerRecord:edit'],
        del: ['admin', 'crawlerRecord:del']
      },
      rules: {
        platform: [
          { required: true, message: '自媒体平台不能为空', trigger: 'blur' }
        ],
        crawlerType: [
          { required: true, message: '爬取类型不能为空', trigger: 'blur' }
        ],
        startPage: [
          { required: true, message: '开始页数不能为空', trigger: 'blur' }
        ],
        crawlerStatus: [
          { required: true, message: '爬虫状态不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'platform', display_name: '自媒体平台' },
        { key: 'crawlerType', display_name: '爬取类型' },
        { key: 'keywords', display_name: '关键词' },
        { key: 'crawlerStatus', display_name: '爬虫状态' }
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
