<template>
  <div>
    <el-dialog class="trade-analysis-dialog" :close-on-click-modal="false" :before-close="crud.cancelCU" :visible.sync="crud.status.cu > 0" width="960px" @paste.native="handleDialogPaste">
      <div slot="title" class="trade-dialog-title">
        <span>{{ crud.status.title }}</span>
        <div class="trade-dialog-actions">
          <el-button size="mini" icon="el-icon-camera" :loading="orderOcr.recognizing" @click="recognizeClipboardOrder">识别剪贴板订单</el-button>
          <el-button size="mini" icon="el-icon-document-copy" @click="copyFormJson">复制JSON</el-button>
        </div>
      </div>
      <el-form ref="form" :model="form" :rules="rules" size="small" label-width="88px">
        <div class="form-section order-ocr-section">
          <div class="section-title">订单识别</div>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="订单截图" class="order-ocr-upload-item">
                <div
                  class="order-ocr-upload"
                  tabindex="0"
                  @click="selectOrderOcrFile"
                  @focus="setOrderPasteTarget"
                  @mouseenter="setOrderPasteTarget"
                  @mouseleave="clearPasteTarget('order')"
                >
                  <input ref="orderOcrFile" type="file" accept="image/*" class="order-ocr-file" @change="handleOrderOcrFileChange">
                  <img v-if="orderOcr.previewUrl" :src="orderOcr.previewUrl" class="order-ocr-thumb">
                  <div v-else class="order-ocr-empty">
                    <i class="el-icon-upload" />
                    <span>点击上传 / 粘贴截图</span>
                  </div>
                </div>
                <div class="order-ocr-actions">
                  <el-button size="mini" type="primary" :loading="orderOcr.recognizing" :disabled="!orderOcr.file || orderOcr.recognizing" @click="recognizeOrder">识别订单</el-button>
                  <el-button size="mini" :disabled="!orderOcr.file && !hasOrderOcrResult" @click="clearOrderOcr">重新上传</el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="16">
              <div class="order-ocr-result">
                <div class="order-ocr-result-title">
                  <span>识别结果</span>
                  <el-button size="mini" type="primary" :disabled="!hasOrderOcrResult" @click="applyOrderOcrToForm">应用到表单</el-button>
                </div>
                <el-row :gutter="12">
                  <el-col :span="8">
                    <el-form-item label="交易方向" label-width="72px">
                      <el-select v-model="orderOcr.result.direction" clearable size="mini" placeholder="未识别" class="form-control">
                        <el-option
                          v-for="item in dict.invest_trade_analysis_direction"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="交易金额" label-width="72px">
                      <el-input v-model="orderOcr.result.amount" size="mini" inputmode="decimal" @input="setNumericField(orderOcr.result, 'amount', $event)" @blur="normalizeNumericField(orderOcr.result, 'amount')" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="盈亏金额" label-width="72px">
                      <el-input v-model="orderOcr.result.netProfit" size="mini" inputmode="decimal" @input="setNumericField(orderOcr.result, 'netProfit', $event, true)" @blur="normalizeNumericField(orderOcr.result, 'netProfit', true)" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="开仓价格" label-width="72px">
                      <el-input v-model="orderOcr.result.openPrice" size="mini" inputmode="decimal" @input="setNumericField(orderOcr.result, 'openPrice', $event)" @blur="normalizeNumericField(orderOcr.result, 'openPrice')" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="开仓时间" label-width="72px">
                      <el-input v-model="orderOcr.result.openTime" size="mini" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="平仓价格" label-width="72px">
                      <el-input v-model="orderOcr.result.closePrice" size="mini" inputmode="decimal" @input="setNumericField(orderOcr.result, 'closePrice', $event)" @blur="normalizeNumericField(orderOcr.result, 'closePrice')" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="平仓时间" label-width="72px">
                      <el-input v-model="orderOcr.result.closeTime" size="mini" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>
            </el-col>
          </el-row>
        </div>
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
                <el-input v-model="form.amount" class="form-control" inputmode="decimal" @input="setNumericField(form, 'amount', $event)" @blur="normalizeNumericField(form, 'amount')" />
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
                @focus="setKlinePasteTarget('openKlineImages', period, true)"
                @mouseenter="setKlinePasteTarget('openKlineImages', period)"
                @mouseleave="clearKlinePasteTarget('openKlineImages', period)"
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
                <el-input v-model="form.openPrice" class="form-control" inputmode="decimal" @input="setNumericField(form, 'openPrice', $event)" @blur="normalizeNumericField(form, 'openPrice')" />
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
                @focus="setKlinePasteTarget('closeKlineImages', period, true)"
                @mouseenter="setKlinePasteTarget('closeKlineImages', period)"
                @mouseleave="clearKlinePasteTarget('closeKlineImages', period)"
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
                <el-input v-model="form.closePrice" class="form-control" inputmode="decimal" @input="setNumericField(form, 'closePrice', $event)" @blur="normalizeNumericField(form, 'closePrice')" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="平仓时间">
                <el-date-picker v-model="form.closeTime" type="datetime" class="form-control" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="盈亏金额">
                <el-input v-model="form.netProfit" class="form-control" inputmode="decimal" @input="setNumericField(form, 'netProfit', $event, true)" @blur="normalizeNumericField(form, 'netProfit', true)" />
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
    <el-dialog append-to-body :visible.sync="imagePreview.visible" :close-on-click-modal="true" width="92vw" class="kline-preview-dialog">
      <div slot="title" class="kline-preview-title">
        <span>{{ getKlinePreviewTitle() }}</span>
        <small>{{ imagePreview.period }}</small>
      </div>
      <div class="kline-preview-body">
        <el-button class="kline-preview-arrow left" icon="el-icon-arrow-left" circle :disabled="!getAdjacentKlinePeriod(-1)" @click="switchKlinePreviewByOffset(-1)" />
        <div class="kline-preview-stage">
          <img v-if="imagePreview.url" :src="imagePreview.url" class="kline-preview-image">
          <div v-else class="kline-preview-empty">暂无图片</div>
        </div>
        <el-button class="kline-preview-arrow right" icon="el-icon-arrow-right" circle :disabled="!getAdjacentKlinePeriod(1)" @click="switchKlinePreviewByOffset(1)" />
      </div>
      <div class="kline-preview-periods">
        <el-button
          v-for="period in klinePeriods"
          :key="'preview-' + period"
          size="mini"
          :type="imagePreview.period === period ? 'primary' : 'default'"
          :disabled="!getKlineUrl(imagePreview.field, period)"
          @click="switchKlinePreview(period)"
        >
          {{ period }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import crudInvestTradeAnalysis from '@/api/investTradeAnalysis'
import CRUD from '@crud/crud'
import { upload } from '@/utils/upload'

export default {
  name: 'InvestTradeAnalysisForm',
  props: {
    crud: {
      type: Object,
      required: true
    },
    form: {
      type: Object,
      required: true
    },
    rules: {
      type: Object,
      required: true
    },
    dict: {
      type: Object,
      required: true
    },
    imagesUploadApi: {
      type: String,
      required: true
    },
    baseApi: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      klinePeriods: ['1D', '4H', '2H', '1H', '15M', '5M'],
      openKlineImagesMap: {},
      closeKlineImagesMap: {},
      activeKlineTarget: null,
      pasteTarget: null,
      imagePreview: {
        visible: false,
        field: null,
        period: null,
        url: ''
      },
      orderOcr: {
        file: null,
        previewUrl: '',
        recognizing: false,
        result: {
          direction: null,
          amount: '',
          openPrice: '',
          openTime: '',
          closePrice: '',
          closeTime: '',
          netProfit: ''
        }
      }
    }
  },
  computed: {
    hasOrderOcrResult() {
      const result = this.orderOcr.result
      return Object.keys(result).some(key => !!result[key])
    }
  },
  mounted() {
    document.addEventListener('paste', this.handleGlobalPaste, true)
  },
  beforeDestroy() {
    document.removeEventListener('paste', this.handleGlobalPaste, true)
  },
  methods: {
    // 钩子：在获取表格数据之前执行，false 则代表不获取数据
    cleanDecimalValue(value, allowSign) {
      if (value === null || value === undefined) {
        return ''
      }
      let text = String(value)
      let sign = ''
      if (allowSign && /^[+-]/.test(text)) {
        sign = text.charAt(0)
      }
      text = text.replace(allowSign ? /[^\d.]/g : /[^\d.]/g, '')
      const dotIndex = text.indexOf('.')
      if (dotIndex !== -1) {
        text = text.slice(0, dotIndex + 1) + text.slice(dotIndex + 1).replace(/\./g, '')
      }
      return sign + text
    },
    setNumericField(target, field, value, allowSign) {
      this.$set(target, field, this.cleanDecimalValue(value, allowSign))
    },
    normalizeNumericField(target, field, allowSign) {
      let value = this.cleanDecimalValue(target[field], allowSign)
      if (!value || /^[+-]?$/.test(value) || /^[+-]?\.$/.test(value)) {
        this.$set(target, field, '')
        return
      }
      if (/^[+-]?\d+\.$/.test(value)) {
        value = value.slice(0, -1)
      }
      if (/^[+-]?\.\d+$/.test(value)) {
        value = value.replace('.', '0.')
      }
      this.$set(target, field, value)
    },
    normalizeTradeNumericFields() {
      this.normalizeNumericField(this.form, 'amount')
      this.normalizeNumericField(this.form, 'openPrice')
      this.normalizeNumericField(this.form, 'closePrice')
      this.normalizeNumericField(this.form, 'netProfit', true)
    },
    getDictLabel(dictName, value) {
      if (value === null || value === undefined || value === '') {
        return null
      }
      const items = this.dict[dictName] || []
      const item = items.find(item => String(item.value) === String(value))
      return item ? item.label : value
    },
    formatJsonDateTime(value) {
      if (!value) {
        return null
      }
      if (typeof value === 'string') {
        return value
      }
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return value
      }
      const pad = number => String(number).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
    emptyToNull(value) {
      return value === '' || value === undefined ? null : value
    },
    buildFormJsonPayload() {
      this.normalizeTradeNumericFields()
      const openKlineImages = this.cleanKlineMap(this.openKlineImagesMap)
      const closeKlineImages = this.cleanKlineMap(this.closeKlineImagesMap)
      return {
        id: this.form.id,
        direction: this.emptyToNull(this.form.direction),
        directionLabel: this.getDictLabel('invest_trade_analysis_direction', this.form.direction),
        amount: this.emptyToNull(this.form.amount),
        score: this.emptyToNull(this.form.score),
        qualityLevel: this.emptyToNull(this.form.qualityLevel),
        qualityLevelLabel: this.getDictLabel('invest_trade_quality_level', this.form.qualityLevel),
        openKlineImages,
        openPrice: this.emptyToNull(this.form.openPrice),
        openTime: this.formatJsonDateTime(this.form.openTime),
        openReason: this.emptyToNull(this.form.openReason),
        entryType: this.emptyToNull(this.form.entryType),
        entryTypeLabel: this.getDictLabel('invest_trade_analysis_entry_type', this.form.entryType),
        closeKlineImages,
        closePrice: this.emptyToNull(this.form.closePrice),
        closeTime: this.formatJsonDateTime(this.form.closeTime),
        netProfit: this.emptyToNull(this.form.netProfit),
        reviewConclusion: this.emptyToNull(this.form.reviewConclusion),
        remark: this.emptyToNull(this.form.remark)
      }
    },
    copyText(text) {
      return this.copyTextByTextarea(text).catch(error => {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          return navigator.clipboard.writeText(text)
        }
        return Promise.reject(error)
      })
    },
    copyTextByTextarea(text) {
      return new Promise((resolve, reject) => {
        const textarea = document.createElement('textarea')
        textarea.value = text
        textarea.setAttribute('readonly', 'readonly')
        textarea.style.position = 'fixed'
        textarea.style.left = '-9999px'
        document.body.appendChild(textarea)
        textarea.select()
        try {
          document.execCommand('copy') ? resolve() : reject(new Error('copy failed'))
        } catch (error) {
          reject(error)
        } finally {
          document.body.removeChild(textarea)
        }
      })
    },
    copyFormJson() {
      const text = JSON.stringify(this.buildFormJsonPayload(), null, 2)
      this.copyText(text).then(() => {
        this.$notify({
          title: 'JSON已复制',
          type: CRUD.NOTIFICATION_TYPE.SUCCESS,
          duration: 2000
        })
      }).catch(() => {
        this.$notify({
          title: '复制失败，请手动复制',
          type: CRUD.NOTIFICATION_TYPE.ERROR,
          duration: 2500
        })
      })
    },
    getEmptyOrderOcrResult() {
      return {
        direction: null,
        amount: '',
        openPrice: '',
        openTime: '',
        closePrice: '',
        closeTime: '',
        netProfit: ''
      }
    },
    resetOrderOcr() {
      if (this.orderOcr.previewUrl) {
        URL.revokeObjectURL(this.orderOcr.previewUrl)
      }
      this.orderOcr = {
        file: null,
        previewUrl: '',
        recognizing: false,
        result: this.getEmptyOrderOcrResult()
      }
      const input = this.$refs.orderOcrFile
      if (input) {
        input.value = ''
      }
    },
    selectOrderOcrFile() {
      const input = this.$refs.orderOcrFile
      if (input) {
        input.click()
      }
    },
    handleOrderOcrFileChange(event) {
      const file = event.target.files && event.target.files[0]
      event.target.value = ''
      if (file) {
        this.setOrderOcrFile(file, true)
      }
    },
    handleOrderOcrPaste(event) {
      const file = this.getImageFileFromClipboard(event)
      if (file) {
        event.preventDefault()
        event.stopPropagation()
        this.setOrderOcrFile(file, true)
        return true
      }
      return false
    },
    getImageFileFromClipboard(event) {
      const clipboardData = event.clipboardData
      if (!clipboardData) {
        return null
      }
      const files = Array.from(clipboardData.files || [])
      let file = files.find(item => item.type && item.type.indexOf('image/') === 0)
      if (!file) {
        const items = clipboardData.items || []
        for (let i = 0; i < items.length; i++) {
          const item = items[i]
          if (item.kind === 'file' && item.type.indexOf('image/') === 0) {
            file = item.getAsFile()
            break
          }
        }
      }
      return file || null
    },
    getImageExtension(mimeType) {
      const type = (mimeType || '').split('/')[1] || 'png'
      return type === 'jpeg' ? 'jpg' : type
    },
    async getImageFileFromClipboardRead() {
      if (!navigator.clipboard || !navigator.clipboard.read) {
        return null
      }
      const items = await navigator.clipboard.read()
      for (const item of items) {
        const type = item.types.find(type => type.indexOf('image/') === 0)
        if (type) {
          const blob = await item.getType(type)
          return new File([blob], `clipboard-order-${Date.now()}.${this.getImageExtension(type)}`, { type })
        }
      }
      return null
    },
    recognizeClipboardOrder() {
      if (this.orderOcr.recognizing) {
        return
      }
      this.getImageFileFromClipboardRead().then(file => {
        if (!file) {
          this.$notify({
            title: '剪贴板中没有图片',
            message: '请先截图并复制订单信息，再点击识别剪贴板订单。',
            type: CRUD.NOTIFICATION_TYPE.WARNING,
            duration: 3000
          })
          return
        }
        this.setOrderOcrFile(file, false)
        this.$nextTick(() => {
          this.recognizeOrder(true)
        })
      }).catch(() => {
        this.$notify({
          title: '无法读取剪贴板图片',
          message: '当前浏览器未允许读取剪贴板，请使用订单截图区域的粘贴上传。',
          type: CRUD.NOTIFICATION_TYPE.WARNING,
          duration: 3500
        })
      })
    },
    setOrderOcrFile(file, autoRecognize) {
      if (this.orderOcr.previewUrl) {
        URL.revokeObjectURL(this.orderOcr.previewUrl)
      }
      this.orderOcr.file = file
      this.orderOcr.previewUrl = URL.createObjectURL(file)
      this.orderOcr.result = this.getEmptyOrderOcrResult()
      if (autoRecognize) {
        this.$nextTick(() => {
          this.recognizeOrder()
        })
      }
    },
    clearOrderOcr() {
      this.resetOrderOcr()
    },
    recognizeOrder(autoApply) {
      if (!this.orderOcr.file || this.orderOcr.recognizing) {
        return
      }
      this.orderOcr.recognizing = true
      crudInvestTradeAnalysis.ocrRecognize(this.orderOcr.file).then(data => {
        this.orderOcr.result = this.normalizeOrderOcrResult(data)
        if (autoApply && this.hasOrderOcrResult) {
          this.applyOrderOcrToForm(false)
        }
        this.$notify({
          title: this.hasOrderOcrResult ? (autoApply ? '订单识别并填充完成' : '订单识别完成') : '未识别到可用字段',
          type: this.hasOrderOcrResult ? CRUD.NOTIFICATION_TYPE.SUCCESS : CRUD.NOTIFICATION_TYPE.WARNING,
          duration: 2000
        })
      }).finally(() => {
        this.orderOcr.recognizing = false
      })
    },
    normalizeOrderOcrResult(data) {
      const result = this.getEmptyOrderOcrResult()
      if (!data || typeof data !== 'object') {
        return result
      }
      Object.keys(result).forEach(key => {
        result[key] = data[key] || ''
      })
      return result
    },
    applyOrderOcrToForm(showNotify = true) {
      const result = this.orderOcr.result
      const mapping = {
        direction: 'direction',
        amount: 'amount',
        openPrice: 'openPrice',
        openTime: 'openTime',
        closePrice: 'closePrice',
        closeTime: 'closeTime',
        netProfit: 'netProfit'
      }
      Object.keys(mapping).forEach(key => {
        if (result[key] !== null && result[key] !== undefined && result[key] !== '') {
          this.$set(this.form, mapping[key], result[key])
        }
      })
      if (showNotify) {
        this.$notify({
          title: '已应用到表单',
          type: CRUD.NOTIFICATION_TYPE.SUCCESS,
          duration: 2000
        })
      }
    },
    validate(callback) {
      if (this.$refs.form) {
        this.$refs.form.validate(callback)
      } else if (callback) {
        callback(false)
      }
    },
    clearValidate() {
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }
    },
    afterToCU() {
      this.syncKlineMapsFromForm()
      this.resetOrderOcr()
    },
    beforeSubmit() {
      this.normalizeTradeNumericFields()
      this.syncKlineMapsToForm()
      return true
    },
    afterSubmit() {
      this.resetKlineMaps()
      this.resetOrderOcr()
    },
    afterAddCancel() {
      this.resetKlineMaps()
      this.resetOrderOcr()
    },
    afterEditCancel() {
      this.resetKlineMaps()
      this.resetOrderOcr()
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
      this.pasteTarget = null
      this.imagePreview = {
        visible: false,
        field: null,
        period: null,
        url: ''
      }
    },
    getKlineMap(field) {
      if (field === 'openKlineImages') {
        return this.openKlineImagesMap
      }
      if (field === 'closeKlineImages') {
        return this.closeKlineImagesMap
      }
      return {}
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
    setOrderPasteTarget() {
      this.activeKlineTarget = null
      this.pasteTarget = { type: 'order' }
    },
    clearPasteTarget(type) {
      if (this.pasteTarget && this.pasteTarget.type === type) {
        this.pasteTarget = null
      }
    },
    setKlinePasteTarget(field, period, rememberActive) {
      if (rememberActive) {
        this.activeKlineTarget = { field, period }
      }
      this.pasteTarget = { type: 'kline', field, period }
    },
    clearKlinePasteTarget(field, period) {
      if (this.pasteTarget && this.pasteTarget.type === 'kline' && this.pasteTarget.field === field && this.pasteTarget.period === period) {
        this.pasteTarget = null
      }
    },
    setActiveKlineTarget(field, period) {
      this.setKlinePasteTarget(field, period, true)
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
      this.setKlinePasteTarget(field, period, true)
      const url = this.getKlineUrl(field, period)
      if (url) {
        this.openKlinePreview(field, period)
      }
    },
    openKlinePreview(field, period) {
      this.imagePreview = {
        visible: true,
        field,
        period,
        url: this.getKlineUrl(field, period)
      }
    },
    getKlinePreviewTitle() {
      if (!this.imagePreview.field) {
        return 'K线预览'
      }
      return this.imagePreview.field === 'openKlineImages' ? '开仓K线预览' : '平仓K线预览'
    },
    getAdjacentKlinePeriod(offset) {
      if (!this.imagePreview.field || !this.imagePreview.period) {
        return null
      }
      const currentIndex = this.klinePeriods.indexOf(this.imagePreview.period)
      if (currentIndex === -1) {
        return null
      }
      for (let index = currentIndex + offset; index >= 0 && index < this.klinePeriods.length; index += offset) {
        const period = this.klinePeriods[index]
        if (this.getKlineUrl(this.imagePreview.field, period)) {
          return period
        }
      }
      return null
    },
    switchKlinePreview(period) {
      const url = this.getKlineUrl(this.imagePreview.field, period)
      if (!url) {
        return
      }
      this.imagePreview.period = period
      this.imagePreview.url = url
    },
    switchKlinePreviewByOffset(offset) {
      const period = this.getAdjacentKlinePeriod(offset)
      if (period) {
        this.switchKlinePreview(period)
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
    isTradeDialogOpen() {
      return !!(this.crud && this.crud.status && this.crud.status.cu > 0)
    },
    handleGlobalPaste(event) {
      if (!this.isTradeDialogOpen() || event._tradeAnalysisPasteHandled) {
        return
      }
      const target = event.target
      const isInDialog = target && target.closest && target.closest('.trade-analysis-dialog')
      if (!this.pasteTarget && !isInDialog) {
        return
      }
      this.handleDialogPaste(event)
    },
    handleDialogPaste(event) {
      if (event._tradeAnalysisPasteHandled) {
        return
      }
      const target = event.target
      const isOrderOcrTarget = target && target.closest && target.closest('.order-ocr-section')
      let handled = false
      if (isOrderOcrTarget || (this.pasteTarget && this.pasteTarget.type === 'order')) {
        handled = this.handleOrderOcrPaste(event)
        if (handled) {
          event._tradeAnalysisPasteHandled = true
          return
        }
      }
      if (this.pasteTarget && this.pasteTarget.type === 'kline') {
        handled = this.handleKlinePaste(event, this.pasteTarget.field, this.pasteTarget.period)
        if (handled) {
          event._tradeAnalysisPasteHandled = true
        }
        return
      }
      if (this.activeKlineTarget) {
        handled = this.handleKlinePaste(event, this.activeKlineTarget.field, this.activeKlineTarget.period)
        if (handled) {
          event._tradeAnalysisPasteHandled = true
        }
        return
      }
      handled = this.handleOrderOcrPaste(event)
      if (handled) {
        event._tradeAnalysisPasteHandled = true
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
        return false
      }
      const files = Array.from(clipboardData.files || [])
      const file = files.find(item => item.type && item.type.indexOf('image/') === 0)
      if (file) {
        event.preventDefault()
        event.stopPropagation()
        this.uploadKlineFile(file, field, period)
        return true
      }
      const items = clipboardData.items || []
      for (let i = 0; i < items.length; i++) {
        const item = items[i]
        if (item.kind === 'file' && item.type.indexOf('image/') === 0) {
          event.preventDefault()
          event.stopPropagation()
          this.uploadKlineFile(item.getAsFile(), field, period)
          return true
        }
      }
      return false
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

.trade-dialog-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 34px;
}

.trade-dialog-title span {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.trade-dialog-actions {
  display: flex;
  gap: 8px;
  align-items: center;
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

.order-ocr-section {
  padding-bottom: 2px;
}

.order-ocr-upload-item {
  margin-bottom: 0;
}

.order-ocr-upload {
  position: relative;
  height: 126px;
  overflow: hidden;
  cursor: pointer;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  outline: none;
}

.order-ocr-upload:hover,
.order-ocr-upload:focus {
  background: #f5faff;
  border-color: #409eff;
}

.order-ocr-file {
  display: none;
}

.order-ocr-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 12px;
  color: #909399;
}

.order-ocr-empty i {
  margin-bottom: 8px;
  font-size: 24px;
}

.order-ocr-thumb {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order-ocr-actions {
  margin-top: 8px;
}

.order-ocr-result {
  min-height: 166px;
}

.order-ocr-result-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 28px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

::v-deep .order-ocr-result .el-form-item {
  margin-bottom: 10px;
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

::v-deep .kline-preview-dialog .el-dialog {
  max-width: 1280px;
}

::v-deep .kline-preview-dialog .el-dialog__body {
  padding: 8px 18px 18px;
}

.kline-preview-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.kline-preview-title span {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.kline-preview-title small {
  padding: 2px 8px;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  border-radius: 3px;
}

.kline-preview-body {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 44px;
  gap: 12px;
  align-items: center;
}

.kline-preview-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  height: min(76vh, 760px);
  min-height: 460px;
  overflow: hidden;
  background: #111827;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.kline-preview-image {
  display: block;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.kline-preview-empty {
  color: #c0c4cc;
  font-size: 14px;
}

.kline-preview-arrow {
  justify-self: center;
}

.kline-preview-periods {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding-top: 14px;
}

@media (max-width: 900px) {
  .kline-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .kline-preview-body {
    grid-template-columns: 36px minmax(0, 1fr) 36px;
    gap: 8px;
  }

  .kline-preview-stage {
    min-height: 320px;
  }

  .kline-preview-periods {
    flex-wrap: wrap;
  }
}
</style>
