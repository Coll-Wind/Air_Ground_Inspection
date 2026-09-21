<template>
  <div class="devmap-wrap">
    <div ref="mapRef" class="map"></div>
    <!-- 右上图例 -->
    <div class="panel legend">
      <div class="legend-item"><span class="dot online"></span>在线</div>
      <div class="legend-item"><span class="dot fault"></span>故障</div>
      <div class="legend-item"><span class="dot offline"></span>离线</div>
      <div class="legend-item"><span class="alert-mark"></span>待处理告警</div>
      <div class="legend-item"><span class="track-line"></span>巡检轨迹</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { deviceApi, alertApi, recordApi } from '../api'

const mapRef = ref()

let map: L.Map
// 复用 marker/polyline,避免轮询重建导致闪烁
const deviceMarkers = new Map<string, L.Marker>()
const trackLines = new Map<string, L.Polyline>()
const alertMarkers = new Map<string, L.Marker>()
let timers: number[] = []
let fitDone = false

const TRACK_LEN = 30 // 每台设备展示的轨迹点数

// 设备配色(轨迹线):按 deviceCode 稳定取色
const palette = ['#2563eb', '#16a34a', '#ea580c', '#9333ea', '#0d9488', '#db2777']
function hash(s: string) {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0
  return h
}
const deviceColor = (code: string) => palette[Math.abs(hash(code)) % palette.length]

/** 设备 marker 的 HTML 图标:圆形底 + 类型 emoji,颜色随状态 */
function deviceIcon(d: any) {
  const color = d.status === 'ONLINE' ? '#16a34a' : d.status === 'FAULT' ? '#dc2626' : '#6b7280'
  const emoji = d.deviceType === 'DRONE' ? '✈' : '🐕'
  return L.divIcon({
    className: '',
    html: `<div class="dev-marker" style="background:${color}"><span>${emoji}</span></div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 15],
    popupAnchor: [0, -16]
  })
}

function devicePopup(d: any) {
  const type = d.deviceType === 'DRONE' ? '无人机' : '机器狗'
  const st = d.status === 'ONLINE' ? '在线' : d.status === 'FAULT' ? '故障' : '离线'
  return `
    <div class="popup">
      <b>${d.name}</b> <span class="muted">(${d.deviceCode})</span><br/>
      类型:${type}<br/>
      状态:<b style="color:${d.status === 'ONLINE' ? '#16a34a' : d.status === 'FAULT' ? '#dc2626' : '#6b7280'}">${st}</b><br/>
      电量:${d.battery ?? '-'}%<br/>
      位置:${d.latitude?.toFixed(5)}, ${d.longitude?.toFixed(5)}<br/>
      最后心跳:${d.lastHeartbeat ?? '-'}
    </div>`
}

/** 拉取设备并刷新 marker */
async function loadDevices() {
  try {
    const res = await deviceApi.list()
    const list = res.data || []
    const bounds: L.LatLngExpression[] = []
    for (const d of list) {
      if (d.latitude == null || d.longitude == null) continue
      const latlng: L.LatLngExpression = [d.latitude, d.longitude]
      bounds.push(latlng)
      let m = deviceMarkers.get(d.deviceCode)
      if (m) {
        m.setLatLng(latlng).setIcon(deviceIcon(d)).setPopupContent(devicePopup(d))
      } else {
        m = L.marker(latlng, { icon: deviceIcon(d) }).bindPopup(devicePopup(d))
        m.addTo(map)
        deviceMarkers.set(d.deviceCode, m)
      }
    }
    // 首次加载自适应视野
    if (bounds.length && !fitDone) {
      map.fitBounds(L.latLngBounds(bounds).pad(0.4))
      fitDone = true
    }
  } catch {}
}

/** 拉取每台设备最近巡检记录,绘制轨迹线 */
async function loadTracks() {
  for (const code of deviceMarkers.keys()) {
    try {
      const res = await recordApi.list({ deviceCode: code })
      const records = (res.data || [])
        .filter((r: any) => r.latitude != null && r.longitude != null)
        .sort((a: any, b: any) => (a.reportTime < b.reportTime ? -1 : 1))
        .slice(-TRACK_LEN)
      const latlngs = records.map((r: any) => [r.latitude, r.longitude])
      const color = deviceColor(code)
      let line = trackLines.get(code)
      if (line) {
        line.setLatLngs(latlngs)
      } else {
        line = L.polyline(latlngs, { color, weight: 3, opacity: 0.65 })
        line.addTo(map)
        trackLines.set(code, line)
      }
    } catch {}
  }
}

/** 拉取待处理告警,红色标记 */
async function loadAlerts() {
  try {
    const res = await alertApi.list()
    const pending = (res.data || []).filter((a: any) => a.status === 'PENDING' && a.latitude != null)
    const codes = new Set(pending.map((a: any) => a.alertCode))
    // 移除已不在列表中的旧告警标记
    for (const [code, m] of alertMarkers) {
      if (!codes.has(code)) {
        m.remove()
        alertMarkers.delete(code)
      }
    }
    for (const a of pending) {
      if (alertMarkers.has(a.alertCode)) continue
      const m = L.marker([a.latitude, a.longitude], {
        icon: L.divIcon({
          className: '',
          html: `<div class="alert-marker"><span class="pulse"></span>⚠</div>`,
          iconSize: [24, 24],
          iconAnchor: [12, 12],
          popupAnchor: [0, -12]
        })
      }).bindPopup(`
        <div class="popup">
          <b style="color:#dc2626">${typeLabel(a.alertType)}</b>
          <span class="muted">(${a.level})</span><br/>
          ${a.description}<br/>
          <span class="muted">${a.deviceCode} · ${a.alertTime}</span>
        </div>`)
      m.addTo(map)
      alertMarkers.set(a.alertCode, m)
    }
  } catch {}
}

const typeLabel = (t: string) =>
  ({ OVERHEAT: '过热', INTRUSION: '入侵', SMOKE: '烟雾', LOW_BATTERY: '低电量', DEVICE_FAULT: '设备故障' }[t] || t)

onMounted(() => {
  map = L.map(mapRef.value, { zoomControl: true, attributionControl: false })
  // OSM 标准瓦片;若加载慢可换高德瓦片(无需 key):
  // L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', { subdomains: '1234' })
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    subdomains: 'abc'
  }).addTo(map)
  map.setView([39.9, 116.4], 14)

  loadDevices().then(() => { loadTracks(); loadAlerts() })
  timers.push(window.setInterval(loadDevices, 5000))
  timers.push(window.setInterval(loadTracks, 15000))
  timers.push(window.setInterval(loadAlerts, 8000))
})

onUnmounted(() => {
  timers.forEach(clearInterval)
  map?.remove()
})
</script>

<style scoped>
.devmap-wrap { position: relative; }
.map { height: 420px; border-radius: 8px; z-index: 0; }

/* 右上图例 */
.panel {
  position: absolute;
  z-index: 500;
  top: 10px; right: 10px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 10px 12px;
  font-size: 13px;
}
.legend-item { display: flex; align-items: center; gap: 8px; margin: 4px 0; }
.legend-item .track-line { display: inline-block; width: 20px; height: 3px; background: #2563eb; border-radius: 2px; }
.legend-item .alert-mark {
  display: inline-block; width: 10px; height: 10px; border-radius: 50%;
  background: #dc2626; box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.3);
}

/* 状态圆点 */
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; }
.dot.online { background: #16a34a; }
.dot.fault { background: #dc2626; }
.dot.offline { background: #6b7280; }

/* 地图上的设备 marker */
:global(.dev-marker) {
  width: 30px; height: 30px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 15px;
  border: 2px solid #fff;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.4);
}
/* 告警 marker + 呼吸动画 */
:global(.alert-marker) {
  width: 24px; height: 24px; border-radius: 50%;
  background: #dc2626; color: #fff; font-size: 13px;
  display: flex; align-items: center; justify-content: center;
  position: relative; border: 2px solid #fff;
}
:global(.alert-marker .pulse) {
  position: absolute; inset: -6px; border-radius: 50%;
  background: rgba(220, 38, 38, 0.35);
  animation: pulse 1.6s ease-out infinite;
}
:global(@keyframes pulse) {
  0% { transform: scale(0.6); opacity: 1; }
  100% { transform: scale(1.8); opacity: 0; }
}

/* 弹窗内容 */
:global(.popup) { font-size: 13px; line-height: 1.7; }
:global(.popup .muted) { color: #64748b; font-size: 12px; }
</style>
