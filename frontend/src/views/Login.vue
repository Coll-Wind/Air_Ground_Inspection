<template>
  <div class="login-bg">
    <el-card class="login-card">
      <div class="title">
        <el-icon :size="28" color="#2563eb"><Monitor /></el-icon>
        <span>空地协同巡检平台</span>
      </div>
      <div class="subtitle">无人机-机器狗园区安防巡检集成平台</div>
      <el-form :model="form" @keyup.enter="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" clearable>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>
      <div class="tip">默认账号:admin / admin123</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'

const router = useRouter()
const form = ref({ username: '', password: '' })
const loading = ref(false)

const submit = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await authApi.login(form.value)
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('username', res.data.username)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 60%, #3b82f6 100%);
}
.login-card {
  width: 380px;
  border-radius: 12px;
  padding: 8px 12px 4px;
}
.title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}
.subtitle {
  text-align: center;
  color: #64748b;
  font-size: 13px;
  margin: 8px 0 22px;
}
.tip {
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
  margin-top: 14px;
}
</style>
