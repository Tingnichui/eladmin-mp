<template>
  <div class="app-container">
    <div class="trade-form">
      <el-select v-model="form.productId" filterable placeholder="请选择" class="select-short">
        <el-option
          v-for="item in productList"
          :key="item.id"
          :label="item.investProductName"
          :value="item.id"
        />
      </el-select>
      ，以 <el-input v-model="form.leverage" type="number" min="0" class="input-short" /> 倍杠杆
      <el-select v-model="form.tradeType" filterable placeholder="请选择" class="select-short">
        <el-option
          v-for="item in dict.invest_trade_type"
          :key="item.id"
          :label="item.label"
          :value="parseInt(item.value)"
        />
      </el-select>
      <el-input v-model="form.tradeNum" type="number" min="0" class="input-short" /> *
      <div v-for="item in productList" :key="item.id">
        <span v-if="item.id === form.productId"> {{ 1/item.minSize }} </span>
      </div>
      <div v-for="item in productList" :key="item.id">
        <span v-if="item.id === form.productId"> {{ item.measurementUnit }} </span>
      </div>
      ，
      开仓价格设置为 <el-input v-model="form.openPrice" type="number" step="0.00000001" class="input-short" />，
      开仓成本为 <el-input v-model="form.cost" type="number" step="0.00000001" class="input-short" />，
      开仓时间为 <el-date-picker v-model="form.openTime" type="datetime" placeholder="选择日期时间" />
      止损价格为 <el-input v-model="form.stopLoss" type="number" step="0.00000001" class="input-short" />，
      止损成本为 <el-input v-model="form.stopLossCost" type="number" step="0.00000001" class="input-short" />，
      止盈价格为 <el-input v-model="form.takeProfit" type="number" step="0.00000001" class="input-short" />，
      当前交易状态
      <el-select v-model="form.operateStatus" filterable placeholder="请选择" class="select-short">
        <el-option
          v-for="item in dict.invest_trade_operate_status"
          :key="item.id"
          :label="item.label"
          :value="parseInt(item.value)"
        />
      </el-select>
      ，平仓价格为 <el-input v-model="form.closePrice" type="number" step="0.00000001" class="input-short" />
      ，平仓时间为 <el-date-picker v-model="form.closeTime" type="datetime" placeholder="选择日期时间" />
      ，收益为 <el-input v-model="form.profit" type="number" step="0.00000001" class="input-short" />
      。
    </div>
    <div style="margin-top: 20px;">
      <mavon-editor
        ref="md"
        v-model="form.analysis"
        :style="'height:' + editorHeight"
        :subfield="subfield"
        default-open="preview"
        @imgAdd="imgAdd"
      >
        <template slot="right-toolbar-after">
          <button
            type="button"
            class="op-icon far fa-mavon-eye"
            aria-hidden="true"
            title="预览"
            @click="changePreviewStatus"
          />
        </template>
      </mavon-editor>
    </div>
    <!-- 评价弹窗 -->
    <el-dialog :visible.sync="reviewLoading" title="评价" width="500px" center>
      <el-form ref="form" :model="form" :rules="rules" size="small" label-width="80px">
        <el-form-item label="状态" prop="reviewStatus">
          <el-select v-model="form.reviewStatus" filterable placeholder="请选择">
            <el-option
              v-for="item in dict.invest_review_status"
              :key="item.id"
              :label="item.label"
              :value="parseInt(item.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="复盘" prop="review">
          <el-input
            v-model="form.review"
            type="textarea"
            :rows="5"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="评分" prop="score">
          <el-rate
            v-model="form.score"
            :icon-classes="iconClasses"
            void-icon-class="icon-rate-face-off"
            :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
          />
        </el-form-item>
      </el-form>
    </el-dialog>
    <!-- 底部操作按钮 -->
    <div class="button-container">
      <el-button v-permission="['admin','investTradeRecord:edit']" type="primary" @click="save">保存</el-button>
      <el-button v-permission="['admin','investTradeRecord:edit']" type="primary" @click="reviewLoading = true">评价</el-button>
      <el-button @click="cancel">取消</el-button>
    </div>
  </div>
</template>

<script>
import { convertAmountToYuan, convertAmountToCent } from '@/utils/numberUtil'
import crudInvestTradeRecord from '@/api/invest/investTradeRecord'
import { listAllProduct } from '@/api/invest/investProduct'
import { upload } from '@/utils/upload'
import { mapGetters } from 'vuex'
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'
import '@/assets/styles/fonts/style.css'
import CRUD from '@crud/crud'

// 需要转换单位的字段
const amountFields = ['openPrice', 'cost', 'stopLoss', 'stopLossCost', 'takeProfit', 'closePrice', 'profit']
export default {
  name: 'Markdown',
  components: {
    mavonEditor
  },
  dicts: ['invest_trade_type', 'invest_trade_operate_status', 'invest_review_status'],
  data() {
    return {
      editorHeight: '500px',
      productList: [],
      form: {},
      subfield: true,
      defaultOpen: 'edit',
      iconClasses: ['icon-rate-face-1', 'icon-rate-face-2', 'icon-rate-face-3'],
      reviewLoading: false
    }
  },
  computed: {
    ...mapGetters([
      'imagesUploadApi',
      'baseApi'
    ])
  },
  mounted() {
    // 初始化时设置编辑器高度
    this.updateEditorHeight()
    // 监听窗口大小变化
    window.onresize = this.updateEditorHeight
    // 从路径中获取参数
    const id = this.$route.params.id
    if (id && id !== ':id') {
      this.form.id = id
    }
    if (this.form.id) {
      crudInvestTradeRecord.getById(this.form.id).then(data => {
        this.form = data
        // 奖金额转换为元
        amountFields.forEach(field => {
          this.form[field] = convertAmountToYuan(this.form[field])
        })
      })
    }
    // 初始化
    this.refreshProductList()
  },
  methods: {
    imgAdd(pos, $file) {
      upload(this.imagesUploadApi, $file).then(res => {
        const data = res.data
        const url = this.baseApi + '/file/' + data.type + '/' + data.realName
        this.$refs.md.$img2Url(pos, url)
      })
    },
    updateEditorHeight() {
      const tradeFormHeight = this.$el.querySelector('.trade-form').offsetHeight
      const buttonHeight = this.$el.querySelector('.button-container').offsetHeight
      this.editorHeight = document.documentElement.clientHeight - tradeFormHeight - buttonHeight - 200 + 'px' // 减去 trade-form 的高度和20px的间距
    },
    refreshProductList() {
      listAllProduct().then(data => {
        this.productList = data.content
      })
    },
    save() {
      const saveData = { ...this.form }
      // 将金额转化为分单位
      amountFields.forEach(field => {
        saveData[field] = convertAmountToCent(saveData[field])
      })
      if (this.form.id) {
        crudInvestTradeRecord.edit(saveData).then(() => {
          this.$notify({
            title: '保存成功',
            type: CRUD.NOTIFICATION_TYPE.SUCCESS,
            duration: 2500
          })
        })
      } else {
        crudInvestTradeRecord.add(saveData).then((data) => {
          // 删除当前路由页面
          this.$store.state.tagsView.visitedViews.splice(this.$store.state.tagsView.visitedViews.findIndex(item => item.path === this.$route.path), 1)
          // 跳转页面
          this.$router.push('/invest/investTradeRecord/edit/' + data)
          this.$notify({
            title: '保存成功',
            type: CRUD.NOTIFICATION_TYPE.SUCCESS,
            duration: 2500
          })
        })
      }
    },
    cancel() {
      // 删除当前路由页面
      this.$store.state.tagsView.visitedViews.splice(this.$store.state.tagsView.visitedViews.findIndex(item => item.path === this.$route.path), 1)
      // 跳转页面
      this.$router.push('/invest/investTradeRecord')
    },
    changePreviewStatus() {
      if (this.subfield) {
        this.defaultOpen = 'edit'
      } else {
        this.defaultOpen = 'preview'
      }
      this.subfield = !this.subfield
    }
  }
}
</script>

<style scoped>
.trade-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  line-height: 1.8;
}

.input-short {
  width: 110px;
  margin: 0 5px;
}

.select-short {
  width: 130px;
  margin: 0 5px;
}

.button-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

</style>

