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
          <el-select v-model="filterType" placeholder="告警类型" clearable size="small" style="width:160px" @change="filterChange">
            <el-option label="过热" value="OVERHEAT" />
            <el-option label="入侵" value="INTRUSION" />
            <el-option label="烟雾" value="SMOKE" />
            <el-option label="低电量" value="LOW_BATTERY" />
            <el-option label="设备故障" value="DEVICE_FAULT" />
          </el-select>
        </div>
      </template>
      <el-table :data="alerts" stripe v-loading="loading">
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
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="openHandle(row)">处理</el-button>
            <el-button v-else size="small" type="info" plain @click="openDetail(row)">处理详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
        style="margin-top:12px;justify-content:flex-end"
      />
    </el-card>

    <!-- 处理告警对话框 -->
    <el-dialog v-model="handleVisible" title="处理告警" width="520px">
      <el-descriptions :column="2" border size="small" style="margin-bottom:16px">
        <el-descriptions-item label="告警编号">{{ current?.alertCode }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ current?.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeLabel(current?.alertType) }}</el-descriptions-item>
        <el-descriptions-item label="级别">
          <el-tag :type="current?.level === 'HIGH' ? 'danger' : current?.level === 'MEDIUM' ? 'warning' : 'info'" size="small">{{ current?.level }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ current?.description }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="处理人" required>
          <el-input v-model="handleForm.handler" placeholder="处理人姓名" style="width:220px" />
        </el-form-item>
        <el-form-item label="处理措施" required>
          <el-input v-model="handleForm.remark" type="textarea" :rows="4"
            placeholder="请填写处理措施,如:已派人现场核实并驱离,持续监控中" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 处理详情对话框 -->
    <el-dialog v-model="detailVisible" title="处理详情" width="480px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="告警编号">{{ current?.alertCode }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ current?.description }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ current?.handler || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理措施">{{ current?.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ current?.handleTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { alertApi } from '../api'
import { ElMessage } from 'element-plus'

const alerts = ref<any[]>([])
const filterType = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
let ws: WebSocket
let wsRetry: number | undefined

const typeLabel = (t: string) => ({
  OVERHEAT: '过热', INTRUSION: '入侵', SMOKE: '烟雾',
  LOW_BATTERY: '低电量', DEVICE_FAULT: '设备故障'
}[t] || t)

const load = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize }
    if (filterType.value) params.type = filterType.value
    const res = await alertApi.list(params)
    alerts.value = res.data.list || []
    total.value = res.data.total || 0
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

// 切换筛选条件回到第 1 页
const filterChange = () => { page.value = 1; load() }

// ===== 处理告警 =====
const handleVisible = ref(false)
const detailVisible = ref(false)
const submitting = ref(false)
const current = ref<any>(null)
const handleForm = ref({ handler: '', remark: '' })

const openHandle = (row: any) => {
  current.value = row
  handleForm.value = { handler: localStorage.getItem('username') || '', remark: '' }
  handleVisible.value = true
}

const confirmHandle = async () => {
  if (!handleForm.value.handler.trim()) { ElMessage.warning('请填写处理人'); return }
  if (!handleForm.value.remark.trim()) { ElMessage.warning('请填写处理措施'); return }
  submitting.value = true
  try {
    await alertApi.updateStatus(current.value.id, 'PROCESSED', handleForm.value.handler.trim(), handleForm.value.remark.trim())
    ElMessage.success(`告警 ${current.value.alertCode} 已处理`)
    handleVisible.value = false
    load()
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    submitting.value = false
  }
}

const openDetail = (row: any) => {
  current.value = row
  detailVisible.value = true
}

// WebSocket 实时告警(断线自动重连)
const connectWs = () => {
  ws = new WebSocket(`ws://${location.host}/ws/alerts`)
  ws.onmessage = (e) => {
    const alert = JSON.parse(e.data)
    alerts.value.unshift(alert)
    total.value++
    ElMessage.warning(`新告警: ${typeLabel(alert.alertType)} - ${alert.deviceCode}`)
  }
  ws.onclose = () => { wsRetry = window.setTimeout(connectWs, 3000) }
}

onMounted(() => {
  load()
  connectWs()
})
onUnmounted(() => {
  window.clearTimeout(wsRetry)
  ws?.close()
})
</script>
