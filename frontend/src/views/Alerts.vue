<template>
  <div>
    <el-card shadow="hover" style="margin-bottom:16px">
      <el-alert type="warning" :closable="false" show-icon
        title="实时告警:通过 WebSocket 接收设备告警推送,新告警将在顶部高亮显示" />
    </el-card>
    <el-card shadow="hover">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>告警列表</span>
          <el-select v-model="filterType" placeholder="告警类型" clearable size="small" style="width:160px" @change="load">
            <el-option label="过热" value="OVERHEAT" />
            <el-option label="入侵" value="INTRUSION" />
            <el-option label="烟雾" value="SMOKE" />
            <el-option label="低电量" value="LOW_BATTERY" />
            <el-option label="设备故障" value="DEVICE_FAULT" />
          </el-select>
        </div>
      </template>
      <el-table :data="alerts" stripe>
        <el-table-column prop="alertCode" label="告警编号" width="180" />
        <el-table-column prop="deviceCode" label="设备" width="110" />
        <el-table-column prop="alertType" label="类型" width="120">
          <template #default="{ row }">{{ typeLabel(row.alertType) }}</template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="90">
          <template #default="{ row }">
            <el-tag :type="row.level === 'HIGH' ? 'danger' : row.level === 'MEDIUM' ? 'warning' : 'info'" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PENDING' ? 'warning' : 'success'" size="small">
              {{ row.status === 'PENDING' ? '待处理' : '已处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertTime" label="告警时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { alertApi } from '../api'
import { ElMessage } from 'element-plus'

const alerts = ref<any[]>([])
const filterType = ref('')
let ws: WebSocket

const typeLabel = (t: string) => ({
  OVERHEAT: '过热', INTRUSION: '入侵', SMOKE: '烟雾',
  LOW_BATTERY: '低电量', DEVICE_FAULT: '设备故障'
}[t] || t)

const load = async () => {
  const params: any = {}
  if (filterType.value) params.type = filterType.value
  const res = await alertApi.list(params)
  alerts.value = res.data || []
}

const handle = async (row: any) => {
  await alertApi.updateStatus(row.id, 'PROCESSED')
  ElMessage.success('告警已处理')
  load()
}

onMounted(() => {
  load()
  setInterval(load, 8000)
  // WebSocket 实时告警
  ws = new WebSocket(`ws://${location.host}/ws/alerts`)
  ws.onmessage = (e) => {
    const alert = JSON.parse(e.data)
    alerts.value.unshift(alert)
    ElMessage.warning(`新告警: ${typeLabel(alert.alertType)} - ${alert.deviceCode}`)
  }
})
onUnmounted(() => ws?.close())
</script>
