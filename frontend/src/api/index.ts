import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

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

// 告警 API
export const alertApi = {
  list: (params?: any) => http.get('/alerts', { params }),
  search: (params?: any) => http.get('/alerts/search', { params }),
  geoSearch: (params: any) => http.get('/alerts/geo-search', { params }),
  aggregate: () => http.get('/alerts/aggregate'),
  stats: () => http.get('/alerts/stats'),
  updateStatus: (id: string, status: string) => http.put(`/alerts/${id}/status`, null, { params: { status } })
}

// 巡检记录 API
export const recordApi = {
  list: (params?: any) => http.get('/records', { params }),
  image: (path: string) => `/api/records/image?path=${encodeURIComponent(path)}`
}

export default http
