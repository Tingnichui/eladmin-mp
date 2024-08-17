<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">用户昵称</label>
        <el-input v-model="query.nickname" clearable placeholder="用户昵称" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">笔记类型(normal | video)</label>
        <el-input v-model="query.type" clearable placeholder="笔记类型(normal | video)" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">笔记标题</label>
        <el-input v-model="query.title" clearable placeholder="笔记标题" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <label class="el-form-item-label">笔记描述</label>
        <el-input v-model="query.desc" clearable placeholder="笔记描述" style="width: 185px;" class="filter-item" @keyup.enter.native="crud.toQuery" />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="500px">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
          <el-form-item label="用户ID" prop="userId">
            <el-input v-model="form.userId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="用户昵称">
            <el-input v-model="form.nickname" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="用户头像地址">
            <el-input v-model="form.avatar" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="评论时的IP地址">
            <el-input v-model="form.ipLocation" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="记录添加时间戳" prop="addTs">
            <el-input v-model="form.addTs" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="记录最后修改时间戳" prop="lastModifyTs">
            <el-input v-model="form.lastModifyTs" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记ID" prop="noteId">
            <el-input v-model="form.noteId" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记类型(normal | video)">
            <el-input v-model="form.type" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记标题">
            <el-input v-model="form.title" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记描述">
            <el-input v-model="form.desc" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="视频地址">
            <el-input v-model="form.videoUrl" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记发布时间戳" prop="time">
            <el-input v-model="form.time" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记最后更新时间戳" prop="lastUpdateTime">
            <el-input v-model="form.lastUpdateTime" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记点赞数">
            <el-input v-model="form.likedCount" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记收藏数">
            <el-input v-model="form.collectedCount" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记评论数">
            <el-input v-model="form.commentCount" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记分享数">
            <el-input v-model="form.shareCount" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记封面图片列表">
            <el-input v-model="form.imageList" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="标签列表">
            <el-input v-model="form.tagList" style="width: 370px;" />
          </el-form-item>
          <el-form-item label="笔记详情页的URL">
            <el-input v-model="form.noteUrl" style="width: 370px;" />
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
        <el-table-column prop="userId" label="用户ID" />
        <el-table-column prop="nickname" label="用户昵称" />
        <el-table-column prop="avatar" label="用户头像地址" />
        <el-table-column prop="ipLocation" label="评论时的IP地址" />
        <el-table-column prop="addTs" label="记录添加时间戳" />
        <el-table-column prop="lastModifyTs" label="记录最后修改时间戳" />
        <el-table-column prop="noteId" label="笔记ID" />
        <el-table-column prop="type" label="笔记类型(normal | video)" />
        <el-table-column prop="title" label="笔记标题" />
        <el-table-column prop="desc" label="笔记描述" />
        <el-table-column prop="videoUrl" label="视频地址" />
        <el-table-column prop="time" label="笔记发布时间戳" />
        <el-table-column prop="lastUpdateTime" label="笔记最后更新时间戳" />
        <el-table-column prop="likedCount" label="笔记点赞数" />
        <el-table-column prop="collectedCount" label="笔记收藏数" />
        <el-table-column prop="commentCount" label="笔记评论数" />
        <el-table-column prop="shareCount" label="笔记分享数" />
        <el-table-column prop="imageList" label="笔记封面图片列表" />
        <el-table-column prop="tagList" label="标签列表" />
        <el-table-column prop="noteUrl" label="笔记详情页的URL" />
        <el-table-column v-if="checkPer(['admin','xhsNote:edit','xhsNote:del'])" label="操作" width="150px" align="center">
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
import crudXhsNote from '@/api/xhsNote'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'

const defaultForm = { id: null, userId: null, nickname: null, avatar: null, ipLocation: null, addTs: null, lastModifyTs: null, noteId: null, type: null, title: null, desc: null, videoUrl: null, time: null, lastUpdateTime: null, likedCount: null, collectedCount: null, commentCount: null, shareCount: null, imageList: null, tagList: null, noteUrl: null }
export default {
  name: 'XhsNote',
  components: { pagination, crudOperation, rrOperation, udOperation },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  cruds() {
    return CRUD({ title: '小红书笔记', url: 'api/xhsNote', idField: 'id', sort: 'id,desc', crudMethod: { ...crudXhsNote }})
  },
  data() {
    return {
      permission: {
        add: ['admin', 'xhsNote:add'],
        edit: ['admin', 'xhsNote:edit'],
        del: ['admin', 'xhsNote:del']
      },
      rules: {
        userId: [
          { required: true, message: '用户ID不能为空', trigger: 'blur' }
        ],
        addTs: [
          { required: true, message: '记录添加时间戳不能为空', trigger: 'blur' }
        ],
        lastModifyTs: [
          { required: true, message: '记录最后修改时间戳不能为空', trigger: 'blur' }
        ],
        noteId: [
          { required: true, message: '笔记ID不能为空', trigger: 'blur' }
        ],
        time: [
          { required: true, message: '笔记发布时间戳不能为空', trigger: 'blur' }
        ],
        lastUpdateTime: [
          { required: true, message: '笔记最后更新时间戳不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'nickname', display_name: '用户昵称' },
        { key: 'type', display_name: '笔记类型(normal | video)' },
        { key: 'title', display_name: '笔记标题' },
        { key: 'desc', display_name: '笔记描述' }
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
