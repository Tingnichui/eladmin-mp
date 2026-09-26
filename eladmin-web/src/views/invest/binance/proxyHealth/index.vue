<template>
  <div class="app-container proxy-health-page">
    <section class="hero-panel">
      <div>
        <div class="eyebrow">BINANCE NETWORK</div>
        <h1>代理状态</h1>
        <p>通过每个代理节点实际访问币安公共接口，检测现货与合约请求链路。</p>
      </div>
      <div class="hero-actions">
        <div class="auto-refresh">
          <span>自动刷新</span>
          <el-switch v-model="autoRefresh" active-color="#13ce66" />
          <small>30 秒</small>
        </div>
        <el-button type="primary" icon="el-icon-refresh" :loading="loading" @click="loadHealth">立即检测</el-button>
      </div>
    </section>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
      class="page-alert"
    />

    <section v-loading="loading && !hasResult" class="overview-panel">
      <div class="overall-status" :class="overallTone">
        <span class="status-orb"><i /></span>
        <div>
          <small>整体状态</small>
          <strong>{{ overallText }}</strong>
          <span>{{ checkedAtText }}</span>
        </div>
      </div>
      <div class="metric-grid">
        <div class="metric-card">
          <span>代理节点</span>
          <strong>{{ health.total || 0 }}</strong>
        </div>
        <div class="metric-card success">
          <span>正常</span>
          <strong>{{ health.healthy || 0 }}</strong>
        </div>
        <div class="metric-card warning">
          <span>部分异常</span>
          <strong>{{ health.degraded || 0 }}</strong>
        </div>
        <div class="metric-card danger">
          <span>异常</span>
          <strong>{{ health.unhealthy || 0 }}</strong>
        </div>
        <div class="metric-card cooling">
          <span>业务冷却中</span>
          <strong>{{ health.coolingDown || 0 }}</strong>
        </div>
      </div>
    </section>

    <el-empty v-if="hasResult && !health.nodes.length" description="尚未配置币安代理节点" />

    <section v-else class="node-grid">
      <article v-for="node in health.nodes" :key="node.address" class="node-card">
        <header>
          <div class="node-identity">
            <span class="node-icon"><i class="el-icon-connection" /></span>
            <div>
              <h2>{{ node.name }}</h2>
              <span>{{ node.address }}</span>
            </div>
          </div>
          <div class="node-state" :class="statusTone(node.status)">
            <i />{{ statusText(node.status) }}
          </div>
        </header>

        <div v-if="node.coolingDown" class="cooldown-banner">
          <i class="el-icon-time" />
          业务请求冷却至 {{ formatTime(node.cooldownUntil) }}
        </div>

        <div class="probe-list">
          <div v-for="probe in node.probes" :key="probe.name" class="probe-row">
            <div class="probe-name">
              <i :class="probe.healthy ? 'el-icon-success' : 'el-icon-error'" />
              <span>{{ probe.name }}</span>
            </div>
            <div class="probe-detail">
              <span v-if="probe.statusCode" class="http-code">HTTP {{ probe.statusCode }}</span>
              <span :class="latencyTone(probe.latencyMs)">{{ probe.latencyMs }} ms</span>
            </div>
            <el-tooltip v-if="probe.error" :content="probe.error" placement="top" effect="dark">
              <div class="probe-error">{{ probe.error }}</div>
            </el-tooltip>
          </div>
        </div>
      </article>
    </section>

    <footer class="status-note">
      <i class="el-icon-info" />
      状态检测不会改变业务请求的代理优先级或冷却时间；“业务冷却中”表示该节点最近发生过真实请求失败。
    </footer>
  </div>
</template>

<script>
import { checkProxyHealth } from '@/api/binanceProxyHealth'
import { parseTime } from '@/utils/index'

export default {
  name: 'BinanceProxyHealth',
  data() {
    return {
      loading: false,
      autoRefresh: true,
      refreshTimer: null,
      errorMessage: '',
      health: {
        checkedAt: null,
        total: 0,
        healthy: 0,
        degraded: 0,
        unhealthy: 0,
        coolingDown: 0,
        nodes: []
      }
    }
  },
  computed: {
    hasResult() {
      return this.health.checkedAt != null
    },
    overallText() {
      if (!this.hasResult) return '等待检测'
      if (!this.health.total) return '未配置'
      if (this.health.unhealthy > 0) return '存在异常节点'
      if (this.health.degraded > 0) return '部分链路异常'
      return '全部正常'
    },
    overallTone() {
      if (!this.hasResult || !this.health.total) return 'muted'
      if (this.health.unhealthy > 0) return 'danger'
      if (this.health.degraded > 0) return 'warning'
      return 'success'
    },
    checkedAtText() {
      return this.hasResult ? `检测时间 ${this.formatTime(this.health.checkedAt)}` : '尚未进行检测'
    }
  },
  watch: {
    autoRefresh(enabled) {
      if (enabled) this.startAutoRefresh()
      else this.stopAutoRefresh()
    }
  },
  created() {
    this.loadHealth()
    this.startAutoRefresh()
  },
  beforeDestroy() {
    this.stopAutoRefresh()
  },
  methods: {
    async loadHealth() {
      if (this.loading) return
      this.loading = true
      this.errorMessage = ''
      try {
        const data = await checkProxyHealth()
        this.health = { ...this.health, ...data, nodes: data.nodes || [] }
      } catch (error) {
        this.errorMessage = (error && error.message) || '代理状态检测失败，请稍后重试'
      } finally {
        this.loading = false
      }
    },
    startAutoRefresh() {
      this.stopAutoRefresh()
      this.refreshTimer = window.setInterval(() => this.loadHealth(), 30000)
    },
    stopAutoRefresh() {
      if (this.refreshTimer) {
        window.clearInterval(this.refreshTimer)
        this.refreshTimer = null
      }
    },
    formatTime(value) {
      return value ? parseTime(new Date(value), '{y}-{m}-{d} {h}:{i}:{s}') : '--'
    },
    statusText(status) {
      return { UP: '正常', DEGRADED: '部分异常', DOWN: '异常' }[status] || '未知'
    },
    statusTone(status) {
      return { UP: 'success', DEGRADED: 'warning', DOWN: 'danger' }[status] || 'muted'
    },
    latencyTone(latency) {
      if (latency >= 3000) return 'latency danger-text'
      if (latency >= 1000) return 'latency warning-text'
      return 'latency'
    }
  }
}
</script>

<style lang="scss" scoped>
.proxy-health-page {
  min-height: calc(100vh - 84px);
  color: #172033;
  background:
    radial-gradient(circle at 92% 2%, rgba(33, 150, 243, .09), transparent 30%),
    linear-gradient(180deg, #f5f8fc 0%, #eef3f8 100%);
}

.hero-panel,
.overview-panel,
.node-card {
  border: 1px solid rgba(136, 153, 181, .2);
  box-shadow: 0 12px 32px rgba(34, 55, 90, .07);
}

.hero-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 30px;
  color: #fff;
  border: 0;
  border-radius: 14px;
  background: linear-gradient(118deg, #172a46 0%, #1f4b68 58%, #147d78 100%);

  .eyebrow { margin-bottom: 7px; color: #83d9d4; font-size: 11px; font-weight: 700; letter-spacing: 2.4px; }
  h1 { margin: 0 0 8px; font-size: 30px; font-weight: 650; letter-spacing: 1px; }
  p { margin: 0; color: rgba(255, 255, 255, .72); font-size: 14px; }
}

.hero-actions,
.auto-refresh { display: flex; align-items: center; }
.hero-actions { gap: 22px; }
.auto-refresh { gap: 8px; color: rgba(255, 255, 255, .86); font-size: 13px; }
.auto-refresh small { color: rgba(255, 255, 255, .55); }
.page-alert { margin-top: 16px; }

.overview-panel {
  display: grid;
  grid-template-columns: 280px 1fr;
  margin-top: 18px;
  padding: 22px;
  border-radius: 12px;
  background: rgba(255, 255, 255, .9);
}

.overall-status {
  display: flex;
  align-items: center;
  gap: 16px;
  border-right: 1px solid #e7edf4;

  .status-orb {
    display: grid;
    width: 48px;
    height: 48px;
    place-items: center;
    border-radius: 50%;
    background: rgba(110, 123, 145, .1);
  }
  .status-orb i { width: 13px; height: 13px; border-radius: 50%; background: #8a96a8; box-shadow: 0 0 0 7px rgba(110, 123, 145, .1); }
  div { display: flex; flex-direction: column; gap: 3px; }
  small { color: #8490a3; font-size: 12px; }
  strong { font-size: 18px; }
  div > span { color: #9aa5b5; font-size: 11px; }
  &.success .status-orb i { background: #1eb980; box-shadow: 0 0 0 7px rgba(30, 185, 128, .12); }
  &.warning .status-orb i { background: #e6a23c; box-shadow: 0 0 0 7px rgba(230, 162, 60, .13); }
  &.danger .status-orb i { background: #ef5b5b; box-shadow: 0 0 0 7px rgba(239, 91, 91, .12); }
}

.metric-grid { display: grid; grid-template-columns: repeat(5, minmax(100px, 1fr)); gap: 12px; padding-left: 22px; }
.metric-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 70px;
  padding: 12px 16px;
  border-radius: 9px;
  background: #f6f8fb;

  span { color: #7d899b; font-size: 12px; }
  strong { margin-top: 5px; color: #2d3b50; font-size: 24px; }
  &.success strong { color: #15966a; }
  &.warning strong { color: #d88b22; }
  &.danger strong { color: #e14e4e; }
  &.cooling strong { color: #68758a; }
}

.node-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(420px, 1fr)); gap: 18px; margin-top: 18px; }
.node-card {
  overflow: hidden;
  border-radius: 12px;
  background: #fff;

  header { display: flex; align-items: center; justify-content: space-between; padding: 20px 22px; border-bottom: 1px solid #edf1f6; }
}
.node-identity { display: flex; align-items: center; gap: 13px; }
.node-icon { display: grid; width: 40px; height: 40px; place-items: center; color: #167f79; font-size: 20px; border-radius: 9px; background: #e8f6f4; }
.node-identity h2 { margin: 0 0 4px; font-size: 17px; }
.node-identity div > span { color: #8a96a7; font-family: Consolas, monospace; font-size: 12px; }
.node-state {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 7px 11px;
  color: #778397;
  font-size: 12px;
  font-weight: 600;
  border-radius: 16px;
  background: #f1f4f8;

  i { width: 7px; height: 7px; border-radius: 50%; background: currentColor; }
  &.success { color: #15966a; background: #eaf8f2; }
  &.warning { color: #d88b22; background: #fff6e8; }
  &.danger { color: #e14e4e; background: #fff0f0; }
}
.cooldown-banner { padding: 9px 22px; color: #996515; font-size: 12px; background: #fff8e8; }
.cooldown-banner i { margin-right: 6px; }
.probe-list { padding: 7px 22px 12px; }
.probe-row { display: grid; grid-template-columns: 120px 1fr; align-items: center; padding: 14px 0; border-bottom: 1px dashed #e8edf3; }
.probe-row:last-child { border-bottom: 0; }
.probe-name { display: flex; align-items: center; gap: 9px; font-size: 13px; font-weight: 600; }
.probe-name .el-icon-success { color: #1eb980; }
.probe-name .el-icon-error { color: #ef5b5b; }
.probe-detail { display: flex; justify-content: flex-end; gap: 14px; color: #778397; font-family: Consolas, monospace; font-size: 12px; }
.http-code { color: #536176; }
.latency { min-width: 70px; text-align: right; }
.warning-text { color: #d88b22; }
.danger-text { color: #e14e4e; }
.probe-error { grid-column: 1 / -1; overflow: hidden; margin: 8px 0 0 26px; color: #e14e4e; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.status-note { margin-top: 18px; color: #8792a3; font-size: 12px; text-align: center; }
.status-note i { margin-right: 5px; }

@media (max-width: 900px) {
  .hero-panel { align-items: flex-start; flex-direction: column; gap: 22px; }
  .overview-panel { grid-template-columns: 1fr; }
  .overall-status { padding-bottom: 18px; border-right: 0; border-bottom: 1px solid #e7edf4; }
  .metric-grid { grid-template-columns: repeat(2, 1fr); padding: 18px 0 0; }
  .node-grid { grid-template-columns: 1fr; }
}

@media (max-width: 560px) {
  .hero-actions { align-items: flex-start; flex-direction: column; }
  .node-grid { grid-template-columns: minmax(0, 1fr); }
  .node-card header { align-items: flex-start; gap: 12px; }
  .probe-row { grid-template-columns: 1fr; gap: 8px; }
  .probe-detail { justify-content: flex-start; margin-left: 26px; }
}
</style>
