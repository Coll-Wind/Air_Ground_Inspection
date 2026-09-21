<template>
  <div>
    <el-card shadow="hover" style="margin-bottom:16px">
      <template #header><span>Elasticsearch 多条件检索</span></template>
      <el-form :inline="true" :model="form">
        <el-form-item label="设备编号"><el-input v-model="form.deviceCode" placeholder="如 UAV-001" style="width:140px" /></el-form-item>
        <el-form-item label="告警类型">
          <el-select v-model="form.alertType" clearable style="width:130px">
            <el-option label="过热" value="OVERHEAT" />
            <el-option label="入侵" value="INTRUSION" />
            <el-option label="烟雾" value="SMOKE" />
            <el-option label="低电量" value="LOW_BATTERY" />
            <el-option label="设备故障" value="DEVICE_FAULT" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.level" clearable style="width:100px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" clearable style="width:110px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="PROCESSED" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">检索</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" style="margin-bottom:16px">
      <template #header><span>地理范围检索(基于 ES geo_point)</span></template>
      <el-form :inline="true" :model="geoForm">
        <el-form-item label="纬度"><el-input v-model.number="geoForm.lat" type="number" style="width:130px" /></el-form-item>
        <el-form-item label="经度"><el-input v-model.number="geoForm.lon" type="number" style="width:130px" /></el-form-item>
        <el-form-item label="半径(km)"><el-input v-model.number="geoForm.distance" type="number" style="width:100px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="geoSearch">地理检索</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <template #header><span>检索结果 ({{ results.length }} 条)</span></template>
      <el-table :data="results" stripe>
        <el-table-column prop="alertCode" label="告警编号" width="180" />
        <el-table-column prop="deviceCode" label="设备" width="110" />
        <el-table-column prop="alertType" label="类型" width="120">
          <template #default="{ row }">{{ typeLabel(row.alertType) }}</template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="90" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="位置" width="200">
          <template #default="{ row }">{{ row.location?.lat?.toFixed(4) }}, {{ row.location?.lon?.toFixed(4) }}</template>
        </el-table-column>
        <el-table-column prop="alertTime" label="时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { alertApi } from '../api'

const form = ref({ deviceCode: '', alertType: '', level: '', status: '' })
const geoForm = ref({ lat: 39.9042, lon: 116.4074, distance: 5 })
const results = ref<any[]>([])

const typeLabel = (t: string) => ({
  OVERHEAT: '过热', INTRUSION: '入侵', SMOKE: '烟雾',
  LOW_BATTERY: '低电量', DEVICE_FAULT: '设备故障'
}[t] || t)

const search = async () => {
  const res = await alertApi.search(form.value)
  results.value = res.data || []
}

const geoSearch = async () => {
  const res = await alertApi.geoSearch(geoForm.value)
  results.value = res.data || []
}

// 进入页面时自动加载全部告警数据
onMounted(() => {
  search()
})
</script>
