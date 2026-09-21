<template>
  <div>
    <el-card shadow="hover">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>设备台账</span>
          <div>
            <el-select v-model="filterType" placeholder="设备类型" clearable size="small" style="width:140px;margin-right:8px" @change="load">
              <el-option label="无人机" value="DRONE" />
              <el-option label="机器狗" value="ROBOT_DOG" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态" clearable size="small" style="width:120px" @change="load">
              <el-option label="在线" value="ONLINE" />
              <el-option label="离线" value="OFFLINE" />
              <el-option label="故障" value="FAULT" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="devices" stripe>
        <el-table-column prop="deviceCode" label="设备编号" width="120" />
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="deviceType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.deviceType === 'DRONE' ? 'primary' : 'success'" size="small">
              {{ row.deviceType === 'DRONE' ? '无人机' : '机器狗' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="型号" width="140" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : row.status === 'FAULT' ? 'danger' : 'info'" size="small">
              {{ row.status === 'ONLINE' ? '在线' : row.status === 'OFFLINE' ? '离线' : '故障' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="电量" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.battery || 0" :status="row.battery > 30 ? '' : 'exception'" :stroke-width="14" />
          </template>
        </el-table-column>
        <el-table-column label="位置" width="200">
          <template #default="{ row }">{{ row.latitude?.toFixed(4) }}, {{ row.longitude?.toFixed(4) }}</template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { deviceApi } from '../api'

const devices = ref<any[]>([])
const filterType = ref('')
const filterStatus = ref('')

const load = async () => {
  const params: any = {}
  if (filterType.value) params.type = filterType.value
  if (filterStatus.value) params.status = filterStatus.value
  const res = await deviceApi.list(params)
  devices.value = res.data || []
}

onMounted(() => {
  load()
  setInterval(load, 5000)
})
</script>
