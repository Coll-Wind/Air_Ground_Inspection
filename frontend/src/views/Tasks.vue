<template>
  <div>
    <el-card shadow="hover" style="margin-bottom:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>下发巡检任务</span>
        </div>
      </template>
      <el-form :model="form" inline label-width="80px">
        <el-form-item label="设备">
          <el-select v-model="form.deviceCode" placeholder="选择设备" style="width:160px">
            <el-option v-for="d in devices" :key="d.deviceCode" :label="`${d.name} (${d.deviceCode})`" :value="d.deviceCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="form.taskType" style="width:140px">
            <el-option label="巡逻" value="PATROL" />
            <el-option label="巡检" value="INSPECT" />
            <el-option label="告警复核" value="ALERT_CHECK" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域">
          <el-input v-model="form.area" placeholder="如:A区围墙" style="width:160px" />
        </el-form-item>
        <el-form-item label="目标坐标">
          <el-input v-model.number="form.latitude" type="number" placeholder="纬度" style="width:110px" />
          <el-input v-model.number="form.longitude" type="number" placeholder="经度" style="width:110px;margin-left:4px" />
          <el-button style="margin-left:8px" @click="openPicker">地图选点</el-button>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">下发任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 地图选点弹窗 -->
    <el-dialog v-model="pickVisible" title="地图选点 - 点击地图选择任务目标位置" width="720px">
      <div ref="pickMapRef" class="pick-map"></div>
      <div class="pick-bar">
        <span>当前选中:纬度 <b>{{ picked.lat.toFixed(6) }}</b>,经度 <b>{{ picked.lon.toFixed(6) }}</b></span>
        <div>
          <el-button @click="pickVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmPick">确定选点</el-button>
        </div>
      </div>
    </el-dialog>

    <el-card shadow="hover">
      <template #header><span>任务列表</span></template>
      <el-table :data="tasks" stripe v-loading="loading">
        <el-table-column prop="taskCode" label="任务编号" width="180" />
        <el-table-column prop="name" label="任务名称" width="140" />
        <el-table-column prop="deviceCode" label="目标设备" width="110" />
        <el-table-column prop="taskType" label="类型" width="100">
          <template #default="{ row }">{{ typeLabel(row.taskType) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="area" label="区域" width="140" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === 'RUNNING'" size="small" type="success" @click="complete(row.taskCode)">完成</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadTasks"
        style="margin-top:12px;justify-content:flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { taskApi, deviceApi } from '../api'
import { ElMessage } from 'element-plus'

const devices = ref<any[]>([])
const tasks = ref<any[]>([])
const form = ref({ deviceCode: '', taskType: 'PATROL', area: '', latitude: 39.9042, longitude: 116.4074, description: '' })
const page = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
let refreshTimer: number | undefined

const typeLabel = (t: string) => ({ PATROL: '巡逻', INSPECT: '巡检', ALERT_CHECK: '告警复核' }[t] || t)
const statusLabel = (s: string) => ({ PENDING: '待执行', RUNNING: '执行中', COMPLETED: '已完成', FAILED: '失败' }[s] || s)
const statusType = (s: string) => ({ PENDING: 'info', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger' }[s] || '')

const loadDevices = async () => { const r = await deviceApi.list(); devices.value = r.data || [] }

const loadTasks = async () => {
  loading.value = true
  try {
    const r = await taskApi.list({ page: page.value, size: pageSize })
    tasks.value = r.data.list || []
    total.value = r.data.total || 0
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!form.value.deviceCode) { ElMessage.warning('请选择设备'); return }
  await taskApi.create(form.value)
  ElMessage.success('任务已下发')
  form.value = { deviceCode: '', taskType: 'PATROL', area: '', latitude: 39.9042, longitude: 116.4074, description: '' }
  page.value = 1
  loadTasks()
}

const complete = async (code: string) => {
  try {
    await taskApi.complete(code)
    ElMessage.success('任务已完成')
    loadTasks()
  } catch {
    // 状态机校验失败(如任务非执行中)由拦截器提示
  }
}

// ---------- 地图选点 ----------
const pickVisible = ref(false)
const pickMapRef = ref()
let pickMap: L.Map | null = null
let pickMarker: L.Marker | null = null
const picked = ref({ lat: 39.9042, lon: 116.4074 })

const placePickMarker = () => {
  if (!pickMap) return
  const ll: L.LatLngExpression = [picked.value.lat, picked.value.lon]
  if (pickMarker) pickMarker.setLatLng(ll)
  else pickMarker = L.marker(ll).addTo(pickMap)
}

const openPicker = () => {
  picked.value = { lat: form.value.latitude, lon: form.value.longitude }
  pickVisible.value = true
  nextTick(() => {
    if (!pickMap) {
      pickMap = L.map(pickMapRef.value, { attributionControl: false })
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { subdomains: 'abc', maxZoom: 19 }).addTo(pickMap)
      pickMap.on('click', (e: L.LeafletMouseEvent) => {
        picked.value = { lat: +e.latlng.lat.toFixed(6), lon: +e.latlng.lng.toFixed(6) }
        placePickMarker()
      })
    }
    // 弹窗内地图尺寸变化后需要重算
    setTimeout(() => pickMap?.invalidateSize(), 150)
    pickMap.setView([picked.value.lat, picked.value.lon], 15)
    placePickMarker()
  })
}

const confirmPick = () => {
  form.value.latitude = picked.value.lat
  form.value.longitude = picked.value.lon
  pickVisible.value = false
  ElMessage.success('已选择目标坐标')
}

onMounted(() => {
  loadDevices()
  loadTasks()
  refreshTimer = window.setInterval(loadTasks, 5000)
})
onUnmounted(() => {
  window.clearInterval(refreshTimer)
  pickMap?.remove()
  pickMap = null
})
</script>

<style scoped>
.pick-map { height: 420px; border-radius: 6px; z-index: 0; }
.pick-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 12px; font-size: 14px; color: #334155;
}
</style>
