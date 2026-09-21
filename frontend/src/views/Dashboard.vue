<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6"><el-card shadow="hover" class="stat-card online"><el-statistic title="设备总数" :value="deviceStats.total" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" class="stat-card online"><el-statistic title="在线设备" :value="deviceStats.online" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" class="stat-card warn"><el-statistic title="待处理告警" :value="alertStats.pending" /></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover" class="stat-card info"><el-statistic title="告警总数" :value="alertStats.total" /></el-card></el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>设备实时分布</span>
          <span style="font-size:12px;color:#909399">点击标记查看设备/告警详情,每 5 秒自动刷新</span>
        </div>
      </template>
      <DeviceMap />
    </el-card>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>告警类型分布</span></template>
          <div ref="pieChartRef" style="height:320px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>设备状态分布</span></template>
          <div ref="barChartRef" style="height:320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top:16px">
      <template #header><span>最近告警(实时)</span></template>
      <el-table :data="recentAlerts" stripe size="small" max-height="300">
        <el-table-column prop="alertCode" label="告警编号" width="180" />
        <el-table-column prop="deviceCode" label="设备" width="110" />
        <el-table-column prop="alertType" label="类型" width="130" />
        <el-table-column prop="level" label="级别" width="90">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="alertTime" label="时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { deviceApi, alertApi } from '../api'
import DeviceMap from '../components/DeviceMap.vue'

const deviceStats = ref({ total: 0, online: 0, offline: 0, fault: 0 })
const alertStats = ref({ total: 0, pending: 0, processed: 0 })
const recentAlerts = ref<any[]>([])
const pieChartRef = ref()
const barChartRef = ref()
let pieChart: any, barChart: any
let ws: WebSocket

const levelType = (l: string) => l === 'HIGH' ? 'danger' : l === 'MEDIUM' ? 'warning' : 'info'

const loadStats = async () => {
  const [d, a] = await Promise.all([deviceApi.stats(), alertApi.stats()])
  deviceStats.value = d.data
  alertStats.value = a.data
  // 设备状态柱状图
  barChart.setOption({
    tooltip: {},
    xAxis: { type: 'category', data: ['在线', '离线', '故障'] },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: [deviceStats.value.online, deviceStats.value.offline, deviceStats.value.fault],
      itemStyle: { color: (p:any)=>['#67c23a','#909399','#f56c6c'][p.dataIndex] } }]
  })
}

const loadAggregate = async () => {
  try {
    const res = await alertApi.aggregate()
    const data = Object.entries(res.data).map(([name, value]) => ({ name, value }))
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{ type: 'pie', radius: ['40%', '70%'], data, label: { show: true, formatter: '{b}: {c}' } }]
    })
  } catch {}
}

const loadRecentAlerts = async () => {
  try {
    const res = await alertApi.list({ page: 1, size: 10 })
    recentAlerts.value = res.data.list || []
  } catch {}
}

let refreshTimer: number | undefined
let wsRetry: number | undefined
let closed = false

// WebSocket 实时告警推送(断线自动重连)
const connectWs = () => {
  ws = new WebSocket(`ws://${location.host}/ws/alerts?token=${localStorage.getItem('token')}`)
  ws.onmessage = (e) => {
    const alert = JSON.parse(e.data)
    recentAlerts.value.unshift(alert)
    if (recentAlerts.value.length > 10) recentAlerts.value.pop()
  }
  ws.onclose = () => {
    if (!closed) wsRetry = window.setTimeout(connectWs, 3000)
  }
}

onMounted(() => {
  pieChart = echarts.init(pieChartRef.value)
  barChart = echarts.init(barChartRef.value)
  loadStats()
  loadAggregate()
  loadRecentAlerts()
  // 定时刷新
  refreshTimer = window.setInterval(() => { loadStats(); loadAggregate(); loadRecentAlerts() }, 10000)
  connectWs()
})

onUnmounted(() => {
  closed = true
  window.clearInterval(refreshTimer)
  window.clearTimeout(wsRetry)
  ws?.close()
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-card.online :deep(.el-statistic__content) { color: #67c23a; }
.stat-card.warn :deep(.el-statistic__content) { color: #e6a23c; }
.stat-card.info :deep(.el-statistic__content) { color: #409eff; }
</style>
