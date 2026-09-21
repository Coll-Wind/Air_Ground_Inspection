<template>
  <div>
    <el-card shadow="hover">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>巡检记录</span>
          <el-select v-model="filterDeviceType" placeholder="设备类型" clearable size="small" style="width:140px" @change="load">
            <el-option label="无人机" value="DRONE" />
            <el-option label="机器狗" value="ROBOT_DOG" />
          </el-select>
        </div>
      </template>
      <el-table :data="records" stripe>
        <el-table-column prop="recordCode" label="记录编号" width="180" />
        <el-table-column prop="deviceCode" label="设备" width="110" />
        <el-table-column prop="deviceType" label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.deviceType === 'DRONE' ? 'primary' : 'success'" size="small">
              {{ row.deviceType === 'DRONE' ? '无人机' : '机器狗' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="位置" width="200">
          <template #default="{ row }">{{ row.latitude?.toFixed(4) }}, {{ row.longitude?.toFixed(4) }}</template>
        </el-table-column>
        <el-table-column prop="payload" label="设备数据" show-overflow-tooltip />
        <el-table-column label="巡检图片" width="100">
          <template #default="{ row }">
            <el-button v-if="row.imagePath" size="small" link type="primary" @click="preview(row.imagePath)">查看</el-button>
            <span v-else style="color:#999">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="reportTime" label="上报时间" width="180" />
      </el-table>
    </el-card>

    <el-dialog v-model="imgVisible" title="巡检图片" width="500px">
      <img :src="imgUrl" style="width:100%" alt="巡检图片" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { recordApi } from '../api'

const records = ref<any[]>([])
const filterDeviceType = ref('')
const imgVisible = ref(false)
const imgUrl = ref('')

const load = async () => {
  const params: any = {}
  if (filterDeviceType.value) params.deviceType = filterDeviceType.value
  const res = await recordApi.list(params)
  records.value = (res.data || []).slice(0, 100)
}

const preview = (path: string) => {
  imgUrl.value = recordApi.image(path)
  imgVisible.value = true
}

onMounted(() => { load(); setInterval(load, 10000) })
</script>
