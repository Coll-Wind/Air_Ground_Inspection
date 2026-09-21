<template>
  <!-- 公开页(登录)不套主布局 -->
  <router-view v-if="isPublic" />
  <el-container v-else class="layout">
    <el-header class="header">
      <div class="logo">
        <el-icon :size="24"><Monitor /></el-icon>
        <span>无人机-机器狗空地协同巡检集成平台</span>
      </div>
      <div class="header-right">
        <span class="user-name">
          <el-icon><User /></el-icon>
          {{ username }}
        </span>
        <el-button size="small" text style="color:#fff" @click="logout">退出登录</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu :default-active="activeMenu" router class="menu">
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>运行概览</span>
          </el-menu-item>
          <el-menu-item index="/devices">
            <el-icon><Cpu /></el-icon>
            <span>设备台账</span>
          </el-menu-item>
          <el-menu-item index="/tasks">
            <el-icon><List /></el-icon>
            <span>巡检任务</span>
          </el-menu-item>
          <el-menu-item index="/alerts">
            <el-icon><Warning /></el-icon>
            <span>告警管理</span>
          </el-menu-item>
          <el-menu-item index="/search">
            <el-icon><Search /></el-icon>
            <span>检索分析</span>
          </el-menu-item>
          <el-menu-item index="/records">
            <el-icon><Document /></el-icon>
            <span>巡检记录</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)
const isPublic = computed(() => !!route.meta.public)
const username = computed(() => localStorage.getItem('username') || 'admin')

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style>
html, body, #app { margin: 0; padding: 0; height: 100%; }
.layout { height: 100vh; }
.header {
  background: linear-gradient(90deg, #1e3a8a, #2563eb);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.logo { display: flex; align-items: center; gap: 10px; font-size: 18px; font-weight: 600; }
.aside { background: #1e293b; }
.menu { border: none; background: #1e293b; }
.menu .el-menu-item { color: #cbd5e1; }
.menu .el-menu-item.is-active { background: #2563eb; color: #fff; }
.main { background: #f1f5f9; padding: 20px; }
</style>
