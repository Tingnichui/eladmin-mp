<template>
  <div class="app-container">
    <!--工具栏-->
    <div class="head-container">
      <div v-if="crud.props.searchToggle">
        <!-- 搜索 -->
        <label class="el-form-item-label">交易方向</label>
        <el-select
          v-model="query.direction"
          clearable
          size="small"
          placeholder="交易方向"
          class="filter-item"
          style="width: 120px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_trade_analysis_direction"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">入场类型</label>
        <el-select
          v-model="query.entryType"
          clearable
          filterable
          size="small"
          placeholder="入场类型"
          class="filter-item"
          style="width: 210px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_trade_analysis_entry_type"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <label class="el-form-item-label">交易质量</label>
        <el-select
          v-model="query.qualityLevel"
          clearable
          size="small"
          placeholder="交易质量"
          class="filter-item"
          style="width: 120px"
          @change="crud.toQuery"
        >
          <el-option
            v-for="item in dict.invest_trade_quality_level"
            :key="item.value"
            :label="item.label"
            :value="parseInt(item.value)"
          />
        </el-select>
        <label class="el-form-item-label">开仓时间</label>
        <date-range-picker
          v-model="query.openTime"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          class="date-item"
          @change="crud.toQuery"
        />
        <label class="el-form-item-label">平仓时间</label>
        <date-range-picker
          v-model="query.closeTime"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          class="date-item"
          @change="crud.toQuery"
        />
        <rrOperation :crud="crud" />
      </div>
      <!--如果想在工具栏加入更多按钮，可以使用插槽方式， slot = 'left' or 'right'-->
      <crudOperation :permission="permission" />
      <!--表单组件-->
      <el-dialog class="trade-analysis-dialog" :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" :title="crud.status.title" width="960px" @paste.native="handleDialogKlinePaste">
        <el-form ref="form" :model="form" :rules="rules" size="small" label-width="88px">
          <div class="form-section">
            <div class="section-title">基础信息</div>
            <el-row :gutter="16">
              <el-col :span="6">
                <el-form-item label="交易方向" prop="direction">
                  <el-select v-model="form.direction" filterable placeholder="请选择" class="form-control">
                    <el-option
                      v-for="item in dict.invest_trade_analysis_direction"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="交易金额">
                  <el-input v-model="form.amount" class="form-control" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="开仓评分">
                  <el-input-number v-model="form.score" :min="0" :max="10" controls-position="right" class="form-control" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="交易质量">
                  <el-select v-model="form.qualityLevel" filterable placeholder="请选择" class="form-control">
                    <el-option
                      v-for="item in dict.invest_trade_quality_level"
                      :key="item.value"
                      :label="item.label"
                      :value="parseInt(item.value)"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="form-section">
            <div class="section-title">开仓记录</div>
            <el-form-item label="开仓K线" class="kline-form-item">
              <div class="kline-grid">
                <div
                  v-for="period in klinePeriods"
                  :key="'open-' + period"
                  class="kline-slot"
                  :class="{ 'has-image': getKlineUrl('openKlineImages', period), active: isActiveKlineTarget('openKlineImages', period) }"
                  tabindex="0"
                  @click="activateKlineSlot('openKlineImages', period)"
                  @dblclick="selectKlineFile('openKlineImages', period)"
                  @focus="setActiveKlineTarget('openKlineImages', period)"
                  @mouseenter="setActiveKlineTarget('openKlineImages', period)"
                  @paste="handleKlinePaste($event, 'openKlineImages', period)"
                >
                  <input
                    :ref="klineInputRef('openKlineImages', period)"
                    type="file"
                    accept="image/*"
                    class="kline-file"
                    @change="handleKlineFileChange($event, 'openKlineImages', period)"
                  >
                  <div class="kline-period">{{ period }}</div>
                  <img v-if="getKlineUrl('openKlineImages', period)" :src="getKlineUrl('openKlineImages', period)" class="kline-thumb">
                  <div v-else class="kline-empty">
                    <i class="el-icon-upload" />
                    <span>粘贴/上传</span>
                  </div>
                  <div class="kline-actions" @click.stop>
                    <el-tooltip content="上传" placement="top">
                      <el-button type="text" icon="el-icon-upload2" @click="selectKlineFile('openKlineImages', period)" />
                    </el-tooltip>
                    <el-tooltip content="链接" placement="top">
                      <el-button type="text" icon="el-icon-link" @click="promptKlineUrl('openKlineImages', period)" />
                    </el-tooltip>
                    <el-tooltip v-if="getKlineUrl('openKlineImages', period)" content="清除" placement="top">
                      <el-button type="text" icon="el-icon-delete" @click="clearKline('openKlineImages', period)" />
                    </el-tooltip>
                  </div>
                </div>
              </div>
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="开仓价格">
                  <el-input v-model="form.openPrice" class="form-control" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="开仓时间">
                  <el-date-picker v-model="form.openTime" type="datetime" class="form-control" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="入场类型">
                  <el-select v-model="form.entryType" filterable placeholder="请选择" class="form-control">
                    <el-option
                      v-for="item in dict.invest_trade_analysis_entry_type"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="开仓原因">
              <el-input v-model="form.openReason" :rows="4" type="textarea" />
            </el-form-item>
          </div>

          <div class="form-section">
            <div class="section-title">平仓记录</div>
            <el-form-item label="平仓K线" class="kline-form-item">
              <div class="kline-grid">
                <div
                  v-for="period in klinePeriods"
                  :key="'close-' + period"
                  class="kline-slot"
                  :class="{ 'has-image': getKlineUrl('closeKlineImages', period), active: isActiveKlineTarget('closeKlineImages', period) }"
                  tabindex="0"
                  @click="activateKlineSlot('closeKlineImages', period)"
                  @dblclick="selectKlineFile('closeKlineImages', period)"
                  @focus="setActiveKlineTarget('closeKlineImages', period)"
                  @mouseenter="setActiveKlineTarget('closeKlineImages', period)"
                  @paste="handleKlinePaste($event, 'closeKlineImages', period)"
                >
                  <input
                    :ref="klineInputRef('closeKlineImages', period)"
                    type="file"
                    accept="image/*"
                    class="kline-file"
                    @change="handleKlineFileChange($event, 'closeKlineImages', period)"
                  >
                  <div class="kline-period">{{ period }}</div>
                  <img v-if="getKlineUrl('closeKlineImages', period)" :src="getKlineUrl('closeKlineImages', period)" class="kline-thumb">
                  <div v-else class="kline-empty">
                    <i class="el-icon-upload" />
                    <span>粘贴/上传</span>
                  </div>
                  <div class="kline-actions" @click.stop>
                    <el-tooltip content="上传" placement="top">
                      <el-button type="text" icon="el-icon-upload2" @click="selectKlineFile('closeKlineImages', period)" />
                    </el-tooltip>
                    <el-tooltip content="链接" placement="top">
                      <el-button type="text" icon="el-icon-link" @click="promptKlineUrl('closeKlineImages', period)" />
                    </el-tooltip>
                    <el-tooltip v-if="getKlineUrl('closeKlineImages', period)" content="清除" placement="top">
                      <el-button type="text" icon="el-icon-delete" @click="clearKline('closeKlineImages', period)" />
                    </el-tooltip>
                  </div>
                </div>
              </div>
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="平仓价格">
                  <el-input v-model="form.closePrice" class="form-control" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="平仓时间">
                  <el-date-picker v-model="form.closeTime" type="datetime" class="form-control" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="盈亏金额">
                  <el-input v-model="form.netProfit" class="form-control" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <div class="form-section">
            <div class="section-title">复盘</div>
            <el-form-item label="平仓复盘">
              <el-input v-model="form.reviewConclusion" :rows="4" type="textarea" />
            </el-form-item>
            <el-form-item label="交易备注">
              <el-input v-model="form.remark" :rows="3" type="textarea" />
            </el-form-item>
          </div>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button type="text" @click="crud.cancelCU">取消</el-button>
          <el-button :loading="crud.status.cu === 2" type="primary" @click="crud.submitCU">保存</el-button>
        </div>
      </el-dialog>
      <el-dialog append-to-body :visible.sync="imagePreview.visible" :close-on-click-modal="true" width="760px" class="kline-preview-dialog" @click.native="imagePreview.visible = false">
        <img v-if="imagePreview.url" :src="imagePreview.url" class="kline-preview-image">
      </el-dialog>
      <!--表格渲染-->
      <el-table ref="table" v-loading="crud.loading" :data="crud.data" size="small" style="width: 100%;" @selection-change="crud.selectionChangeHandler">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="direction" label="交易方向">
          <template slot-scope="scope">
            {{ dict.label.invest_trade_analysis_direction[scope.row.direction] || scope.row.direction }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="交易金额" />
        <el-table-column prop="entryType" label="入场类型">
          <template slot-scope="scope">
            {{ dict.label.invest_trade_analysis_entry_type[scope.row.entryType] || scope.row.entryType }}
          </template>
        </el-table-column>
        <el-table-column prop="openTime" label="开仓时间" />
        <el-table-column prop="closeTime" label="平仓时间" />
        <el-table-column prop="openPrice" label="开仓价格" />
        <el-table-column prop="closePrice" label="平仓价格" />
        <el-table-column prop="netProfit" label="盈亏金额" />
        <el-table-column prop="score" label="开仓评分" />
        <el-table-column prop="qualityLevel" label="交易质量">
          <template slot-scope="scope">
            {{ dict.label.invest_trade_quality_level[scope.row.qualityLevel] || scope.row.qualityLevel }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column v-if="checkPer(['admin','investTradeAnalysis:edit','investTradeAnalysis:del'])" label="操作" width="150px" align="center">
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
import crudInvestTradeAnalysis from '@/api/investTradeAnalysis'
import CRUD, { presenter, header, form, crud } from '@crud/crud'
import rrOperation from '@crud/RR.operation'
import crudOperation from '@crud/CRUD.operation'
import udOperation from '@crud/UD.operation'
import pagination from '@crud/Pagination'
import DateRangePicker from '@/components/DateRangePicker'
import { upload } from '@/utils/upload'
import { mapGetters } from 'vuex'

const defaultForm = { id: null, direction: null, amount: null, entryType: null, openTime: null, closeTime: null, openPrice: null, closePrice: null, netProfit: null, openReason: null, openKlineImages: null, closeKlineImages: null, score: null, reviewConclusion: null, qualityLevel: null, remark: null, createBy: null, updateBy: null, createTime: null, updateTime: null }
export default {
  name: 'InvestTradeAnalysis',
  components: { pagination, crudOperation, rrOperation, udOperation, DateRangePicker },
  mixins: [presenter(), header(), form(defaultForm), crud()],
  dicts: ['invest_trade_analysis_direction', 'invest_trade_analysis_entry_type', 'invest_trade_quality_level'],
  data() {
    return {
      klinePeriods: ['1D', '4H', '2H', '1H', '15M', '5M'],
      openKlineImagesMap: {},
      closeKlineImagesMap: {},
      activeKlineTarget: null,
      imagePreview: {
        visible: false,
        url: ''
      },
      permission: {
        add: ['admin', 'investTradeAnalysis:add'],
        edit: ['admin', 'investTradeAnalysis:edit'],
        del: ['admin', 'investTradeAnalysis:del']
      },
      rules: {
        direction: [
          { required: true, message: '方向不能为空', trigger: 'blur' }
        ]
      },
      queryTypeOptions: [
        { key: 'direction', display_name: '交易方向' },
        { key: 'entryType', display_name: '入场类型' },
        { key: 'qualityLevel', display_name: '交易质量' }
      ]
    }
  },
  computed: {
    ...mapGetters([
      'imagesUploadApi',
      'baseApi'
    ])
  },
  cruds() {
    return CRUD({ title: '交易分析', url: 'api/investTradeAnalysis', idField: 'id', sort: 'id,desc', crudMethod: { ...crudInvestTradeAnalysis }})
  },
  methods: {
    // 钩子：在获取表格数据之前执行，false 则代表不获取数据
    [CRUD.HOOK.beforeRefresh]() {
      return true
    },
    [CRUD.HOOK.afterToCU]() {
      this.syncKlineMapsFromForm()
    },
    [CRUD.HOOK.beforeSubmit]() {
      this.syncKlineMapsToForm()
      return true
    },
    [CRUD.HOOK.afterSubmit]() {
      this.resetKlineMaps()
    },
    [CRUD.HOOK.afterAddCancel]() {
      this.resetKlineMaps()
    },
    [CRUD.HOOK.afterEditCancel]() {
      this.resetKlineMaps()
    },
    parseKlineImages(value) {
      if (!value) {
        return {}
      }
      if (typeof value === 'object') {
        return { ...value }
      }
      try {
        const data = JSON.parse(value)
        return data && typeof data === 'object' ? data : {}
      } catch (e) {
        return { '5M': value }
      }
    },
    cleanKlineMap(map) {
      return this.klinePeriods.reduce((result, period) => {
        if (map[period]) {
          result[period] = map[period]
        }
        return result
      }, {})
    },
    stringifyKlineMap(map) {
      const data = this.cleanKlineMap(map)
      return Object.keys(data).length ? JSON.stringify(data) : null
    },
    syncKlineMapsFromForm() {
      this.openKlineImagesMap = this.parseKlineImages(this.form.openKlineImages)
      this.closeKlineImagesMap = this.parseKlineImages(this.form.closeKlineImages)
    },
    syncKlineMapsToForm() {
      this.form.openKlineImages = this.stringifyKlineMap(this.openKlineImagesMap)
      this.form.closeKlineImages = this.stringifyKlineMap(this.closeKlineImagesMap)
    },
    resetKlineMaps() {
      this.openKlineImagesMap = {}
      this.closeKlineImagesMap = {}
      this.activeKlineTarget = null
      this.imagePreview = {
        visible: false,
        url: ''
      }
    },
    getKlineMap(field) {
      return field === 'openKlineImages' ? this.openKlineImagesMap : this.closeKlineImagesMap
    },
    getKlineUrl(field, period) {
      return this.getKlineMap(field)[period]
    },
    klineInputRef(field, period) {
      return `${field}-${period}-file`
    },
    setKlineUrl(field, period, url) {
      const map = this.getKlineMap(field)
      this.$set(map, period, url)
    },
    clearKline(field, period) {
      const map = this.getKlineMap(field)
      this.$delete(map, period)
    },
    setActiveKlineTarget(field, period) {
      this.activeKlineTarget = { field, period }
    },
    isActiveKlineTarget(field, period) {
      return !!this.activeKlineTarget && this.activeKlineTarget.field === field && this.activeKlineTarget.period === period
    },
    selectKlineFile(field, period) {
      const ref = this.$refs[this.klineInputRef(field, period)]
      const input = Array.isArray(ref) ? ref[0] : ref
      if (input) {
        input.click()
      }
    },
    activateKlineSlot(field, period) {
      this.setActiveKlineTarget(field, period)
      const url = this.getKlineUrl(field, period)
      if (url) {
        this.imagePreview = {
          visible: true,
          url
        }
      }
    },
    promptKlineUrl(field, period) {
      this.$prompt(`${period} K线图片链接`, '设置图片链接', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: this.getKlineUrl(field, period) || ''
      }).then(({ value }) => {
        if (value) {
          this.setKlineUrl(field, period, value)
        }
      }).catch(() => {})
    },
    handleKlineFileChange(event, field, period) {
      const file = event.target.files && event.target.files[0]
      event.target.value = ''
      if (file) {
        this.uploadKlineFile(file, field, period)
      }
    },
    handleDialogKlinePaste(event) {
      if (!this.activeKlineTarget) {
        return
      }
      this.handleKlinePaste(event, this.activeKlineTarget.field, this.activeKlineTarget.period)
    },
    handleKlinePaste(event, field, period) {
      const clipboardData = event.clipboardData
      if (!clipboardData) {
        return
      }
      const files = Array.from(clipboardData.files || [])
      const file = files.find(item => item.type && item.type.indexOf('image/') === 0)
      if (file) {
        event.preventDefault()
        event.stopPropagation()
        this.uploadKlineFile(file, field, period)
        return
      }
      const items = clipboardData.items || []
      for (let i = 0; i < items.length; i++) {
        const item = items[i]
        if (item.kind === 'file' && item.type.indexOf('image/') === 0) {
          event.preventDefault()
          event.stopPropagation()
          this.uploadKlineFile(item.getAsFile(), field, period)
          return
        }
      }
    },
    uploadKlineFile(file, field, period) {
      if (!file) {
        return
      }
      upload(this.imagesUploadApi, file).then(res => {
        const data = res.data || {}
        const url = data.url || (data.type && data.realName ? `${this.baseApi}/file/${data.type}/${data.realName}` : data.path)
        if (!url) {
          this.$notify({
            title: '图片上传失败',
            type: CRUD.NOTIFICATION_TYPE.ERROR,
            duration: 2500
          })
          return
        }
        this.setKlineUrl(field, period, url)
        this.$notify({
          title: '图片上传成功',
          type: CRUD.NOTIFICATION_TYPE.SUCCESS,
          duration: 2000
        })
      }).catch(() => {
        this.$notify({
          title: '图片上传失败',
          type: CRUD.NOTIFICATION_TYPE.ERROR,
          duration: 2500
        })
      })
    }
  }
}
</script>

<style scoped>
::v-deep .trade-analysis-dialog .el-dialog__body {
  max-height: 70vh;
  padding: 12px 20px 8px;
  overflow-y: auto;
}

.form-section {
  padding-top: 14px;
  margin-top: 12px;
  border-top: 1px solid #ebeef5;
}

.form-section:first-child {
  padding-top: 0;
  margin-top: 0;
  border-top: 0;
}

.section-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.form-control {
  width: 100%;
}

.kline-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.kline-slot {
  position: relative;
  height: 108px;
  overflow: hidden;
  cursor: pointer;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  outline: none;
}

.kline-slot:hover,
.kline-slot:focus {
  background: #f5faff;
  border-color: #409eff;
}

.kline-period {
  position: absolute;
  top: 6px;
  left: 8px;
  z-index: 2;
  padding: 1px 5px;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  background: rgba(255, 255, 255, .86);
  border-radius: 3px;
}

.kline-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 12px;
  color: #909399;
}

.kline-empty i {
  margin-bottom: 6px;
  font-size: 22px;
}

.kline-thumb {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.kline-actions {
  position: absolute;
  right: 4px;
  bottom: 2px;
  display: flex;
  gap: 2px;
  padding: 0 3px;
  background: rgba(255, 255, 255, .9);
  border-radius: 3px;
  opacity: 0;
  transition: opacity .15s;
}

.kline-slot:hover .kline-actions,
.kline-slot:focus .kline-actions {
  opacity: 1;
}

.kline-actions .el-button {
  padding: 4px;
  font-size: 14px;
}

.kline-file {
  display: none;
}

.kline-preview-image {
  display: block;
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

@media (max-width: 900px) {
  .kline-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
