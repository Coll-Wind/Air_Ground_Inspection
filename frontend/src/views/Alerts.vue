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
          <el-button size="small" circle @click="doSearch">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
      </template>
      <el-form :inline="true" size="small" class="filter-bar" @submit.prevent>
        <el-form-item label="告警编号">
          <el-input v-model="filters.alertCode" placeholder="如 17899" clearable style="width:140px" @keyup.enter="doSearch" @clear="doSearch" />
        </el-form-item>
        <el-form-item label="设备">
          <el-input v-model="filters.deviceCode" placeholder="如 UAV-001" clearable style="width:130px" @keyup.enter="doSearch" @clear="doSearch" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.type" placeholder="全部" clearable style="width:110px" @change="doSearch">
            <el-option label="过热" value="OVERHEAT" />
            <el-option label="入侵" value="INTRUSION" />
            <el-option label="烟雾" value="SMOKE" />
            <el-option label="低电量" value="LOW_BATTERY" />
            <el-option label="设备故障" value="DEVICE_FAULT" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="filters.level" placeholder="全部" clearable style="width:100px" @change="doSearch">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width:110px" @change="doSearch">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="PROCESSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="filters.keyword" placeholder="关键词" clearable style="width:130px" @keyup.enter="doSearch" @clear="doSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doSearch">检索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { alertApi } from '../api'
import { ElMessage } from 'element-plus'

const alerts = ref<any[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const filters = reactive({ alertCode: '', deviceCode: '', type: '', level: '', status: '', keyword: '' })
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
    if (filters.alertCode) params.alertCode = filters.alertCode
    if (filters.deviceCode) params.deviceCode = filters.deviceCode
    if (filters.type) params.type = filters.type
    if (filters.level) params.level = filters.level
    if (filters.status) params.status = filters.status
    if (filters.keyword) params.keyword = filters.keyword
    const res = await alertApi.list(params)
    alerts.value = res.data.list || []
    total.value = res.data.total || 0
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

// 发起检索:回到第 1 页
const doSearch = () => { page.value = 1; load() }

// 清空全部筛选条件
const resetFilters = () => {
  Object.assign(filters, { alertCode: '', deviceCode: '', type: '', level: '', status: '', keyword: '' })
  doSearch()
}

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
// 判断实时新告警是否匹配当前筛选条件(仅让符合条件的新告警进入列表,保持列表与筛选一致)
const matchFilter = (a: any) => {
  if (filters.type && a.alertType !== filters.type) return false
  if (filters.level && a.level !== filters.level) return false
  if (filters.status && a.status !== filters.status) return false
  if (filters.deviceCode && !a.deviceCode?.includes(filters.deviceCode)) return false
  if (filters.alertCode && !a.alertCode?.includes(filters.alertCode)) return false
  if (filters.keyword && !a.description?.includes(filters.keyword)) return false
  return true
}

const connectWs = () => {
  ws = new WebSocket(`ws://${location.host}/ws/alerts?token=${localStorage.getItem('token')}`)
  ws.onmessage = (e) => {
    const alert = JSON.parse(e.data)
    ElMessage.warning(`新告警: ${typeLabel(alert.alertType)} - ${alert.deviceCode}`)
    if (matchFilter(alert)) {
      alerts.value.unshift(alert)
      total.value++
    }
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

<style scoped>
.filter-bar {
  margin-bottom: 6px;
  padding: 10px 12px 0;
  background: #f8fafc;
  border-radius: 6px;
}
</style>
