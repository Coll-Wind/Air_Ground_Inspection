# -*- coding: utf-8 -*-
"""演示数据种子:清空 ES alert-index 文档(保留 mapping),写入与 Mongo 一致的 5 条告警"""
import json
import urllib.request

ES = 'http://localhost:9200'
DOCS = [
    {
        'id': 'seed-alert-overheat', 'alertCode': 'ALT-DEMO-0001',
        'deviceCode': 'UAV-001', 'deviceType': 'DRONE',
        'alertType': 'OVERHEAT', 'level': 'MEDIUM',
        'description': 'UAV-001 巡检中检测到电机温度 78℃,超过安全阈值',
        'status': 'PENDING',
        'alertTime': __import__('datetime').datetime.now().isoformat(),
        'location': {'lat': 39.9086, 'lon': 116.4102},
    },
    {
        'id': 'seed-alert-intrusion', 'alertCode': 'ALT-DEMO-0002',
        'deviceCode': 'DOG-001', 'deviceType': 'ROBOT_DOG',
        'alertType': 'INTRUSION', 'level': 'HIGH',
        'description': 'DOG-001 在 A 区围墙发现可疑人员翻越,已联动抓拍取证',
        'status': 'PENDING',
        'alertTime': __import__('datetime').datetime.now().isoformat(),
        'location': {'lat': 39.9021, 'lon': 116.4035},
    },
    {
        'id': 'seed-alert-smoke', 'alertCode': 'ALT-DEMO-0003',
        'deviceCode': 'UAV-002', 'deviceType': 'DRONE',
        'alertType': 'SMOKE', 'level': 'HIGH',
        'description': 'UAV-002 在 B 区仓库顶部检测到浓烟,疑似火情',
        'status': 'PROCESSED', 'handler': 'admin',
        'remark': '经核实为附近农户焚烧秸秆,已协调处理,安排持续观察',
        'handleTime': (__import__('datetime').datetime.now() - __import__('datetime').timedelta(hours=1)).isoformat(),
        'alertTime': (__import__('datetime').datetime.now() - __import__('datetime').timedelta(hours=1)).isoformat(),
        'location': {'lat': 39.9062, 'lon': 116.4058},
    },
    {
        'id': 'seed-alert-lowbat', 'alertCode': 'ALT-DEMO-0004',
        'deviceCode': 'DOG-002', 'deviceType': 'ROBOT_DOG',
        'alertType': 'LOW_BATTERY', 'level': 'MEDIUM',
        'description': 'DOG-002 电量不足(22%),已自动返航充电',
        'status': 'PROCESSED', 'handler': 'admin',
        'remark': '设备已返回充电桩,充电中,预计 40 分钟后恢复巡逻',
        'handleTime': (__import__('datetime').datetime.now() - __import__('datetime').timedelta(hours=2)).isoformat(),
        'alertTime': (__import__('datetime').datetime.now() - __import__('datetime').timedelta(hours=2)).isoformat(),
        'location': {'lat': 39.9005, 'lon': 116.4089},
    },
    {
        'id': 'seed-alert-fault', 'alertCode': 'ALT-DEMO-0005',
        'deviceCode': 'UAV-003', 'deviceType': 'DRONE',
        'alertType': 'DEVICE_FAULT', 'level': 'MEDIUM',
        'description': 'UAV-003 云台电机运行异常,需要检修',
        'status': 'PROCESSED', 'handler': 'admin',
        'remark': '现场重启云台自检通过,恢复正常巡逻',
        'handleTime': (__import__('datetime').datetime.now() - __import__('datetime').timedelta(hours=1.5)).isoformat(),
        'alertTime': (__import__('datetime').datetime.now() - __import__('datetime').timedelta(hours=1.5)).isoformat(),
        'location': {'lat': 39.9048, 'lon': 116.4121},
    },
]


def post(path, payload, ndjson=False):
    data = payload if isinstance(payload, bytes) else json.dumps(payload, ensure_ascii=False).encode('utf-8')
    headers = {'Content-Type': 'application/x-ndjson' if ndjson else 'application/json'}
    req = urllib.request.Request(ES + path, data=data, headers=headers, method='POST')
    return json.load(urllib.request.urlopen(req))


# 1) 清空文档,保留索引 mapping(geo_point 等)
r = post('/alert-index/_delete_by_query?conflicts=proceed', {'query': {'match_all': {}}})
print('ES 清空文档数:', r.get('deleted'))

# 2) bulk 写入种子
lines = []
for d in DOCS:
    lines.append(json.dumps({'index': {'_index': 'alert-index', '_id': d.pop('id')}}))
    lines.append(json.dumps(d, ensure_ascii=False))
r = post('/_bulk', ('\n'.join(lines) + '\n').encode('utf-8'), ndjson=True)
print('ES bulk 写入 errors =', r.get('errors'))

# 3) 立即刷新可查(ES 默认 1 秒自动刷新,失败无影响)
try:
    req = urllib.request.Request(ES + '/alert-index/_refresh', data=b'', headers={'Content-Type': 'application/json'}, method='POST')
    urllib.request.urlopen(req)
except Exception as e:
    print('refresh 跳过:', e)
print('ES 告警种子写入完成')
