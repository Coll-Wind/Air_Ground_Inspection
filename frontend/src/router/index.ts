import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
  { path: '/devices', name: 'Devices', component: () => import('../views/Devices.vue') },
  { path: '/tasks', name: 'Tasks', component: () => import('../views/Tasks.vue') },
  { path: '/alerts', name: 'Alerts', component: () => import('../views/Alerts.vue') },
  { path: '/search', name: 'Search', component: () => import('../views/Search.vue') },
  { path: '/records', name: 'Records', component: () => import('../views/Records.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫:未登录跳转登录页
router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!localStorage.getItem('token')) return '/login'
  return true
})

export default router
