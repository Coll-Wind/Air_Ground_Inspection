import { createRouter, createWebHistory } from 'vue-router'

const routes = [
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

export default router
