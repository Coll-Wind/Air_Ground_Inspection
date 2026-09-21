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
        <el-form-item label="纬度">
          <el-input v-model.number="form.latitude" type="number" style="width:120px" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input v-model.number="form.longitude" type="number" style="width:120px" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">下发任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <template #header><span>任务列表</span></template>
      <el-table :data="tasks" stripe>
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { taskApi, deviceApi } from '../api'
import { ElMessage } from 'element-plus'

const devices = ref<any[]>([])
const tasks = ref<any[]>([])
const form = ref({ deviceCode: '', taskType: 'PATROL', area: '', latitude: 39.9042, longitude: 116.4074, description: '' })

const typeLabel = (t: string) => ({ PATROL: '巡逻', INSPECT: '巡检', ALERT_CHECK: '告警复核' }[t] || t)
const statusLabel = (s: string) => ({ PENDING: '待执行', RUNNING: '执行中', COMPLETED: '已完成', FAILED: '失败' }[s] || s)
const statusType = (s: string) => ({ PENDING: 'info', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger' }[s] || '')

const loadDevices = async () => { const r = await deviceApi.list(); devices.value = r.data || [] }
const loadTasks = async () => { const r = await taskApi.list(); tasks.value = r.data || [] }

const submit = async () => {
  if (!form.value.deviceCode) { ElMessage.warning('请选择设备'); return }
  await taskApi.create(form.value)
  ElMessage.success('任务已下发')
  form.value = { deviceCode: '', taskType: 'PATROL', area: '', latitude: 39.9042, longitude: 116.4074, description: '' }
  loadTasks()
}

const complete = async (code: string) => { await taskApi.complete(code); loadTasks() }

onMounted(() => { loadDevices(); loadTasks(); setInterval(loadTasks, 5000) })
</script>
