import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截:自动携带 JWT
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截:统一错误提示,401 跳登录
http.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg = err.response?.data?.error || err.message || '请求失败'
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      if (location.pathname !== '/login') {
        ElMessage.error('登录已过期,请重新登录')
        location.href = '/login'
      }
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

// 认证 API
export const authApi = {
  login: (data: { username: string; password: string }) => http.post('/auth/login', data)
}

// 设备 API
export const deviceApi = {
  list: (params?: any) => http.get('/devices', { params }),
  stats: () => http.get('/devices/stats'),
  get: (code: string) => http.get(`/devices/${code}`),
  delete: (code: string) => http.delete(`/devices/${code}`)
}

// 任务 API
export const taskApi = {
  list: (params?: any) => http.get('/tasks', { params }),
  create: (data: any) => http.post('/tasks', data),
  complete: (code: string) => http.put(`/tasks/${code}/complete`)
}

// 告警 API(列表类接口统一返回 { list, total })
export const alertApi = {
  list: (params?: any) => http.get('/alerts', { params }),
  search: (params?: any) => http.get('/alerts/search', { params }),
  geoSearch: (params: any) => http.get('/alerts/geo-search', { params }),
  aggregate: () => http.get('/alerts/aggregate'),
  stats: () => http.get('/alerts/stats'),
  updateStatus: (id: string, status: string) => http.put(`/alerts/${id}/status`, null, { params: { status } })
}

// 巡检记录 API(列表接口返回 { list, total })
export const recordApi = {
  list: (params?: any) => http.get('/records', { params }),
  image: (path: string) => `/api/records/image?path=${encodeURIComponent(path)}`
}

export default http
