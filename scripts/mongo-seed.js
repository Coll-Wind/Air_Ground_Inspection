// 演示数据种子:清空告警集合,注入每种类型 1 条样本
// 用法:由 reset-demo-data.bat 调用(docker exec -i ... mongosh --file /dev/stdin)
const dba = db.getSiblingDB('agi');
dba.alert.drop();

dba.alert.insertMany([
  {
    _id: 'seed-alert-overheat',
    alertCode: 'ALT-DEMO-0001',
    deviceCode: 'UAV-001',
    deviceType: 'DRONE',
    alertType: 'OVERHEAT',
    level: 'MEDIUM',
    description: 'UAV-001 巡检中检测到电机温度 78℃,超过安全阈值',
    latitude: 39.9086, longitude: 116.4102,
    status: 'PENDING',
    alertTime: new Date()
  },
  {
    _id: 'seed-alert-intrusion',
    alertCode: 'ALT-DEMO-0002',
    deviceCode: 'DOG-001',
    deviceType: 'ROBOT_DOG',
    alertType: 'INTRUSION',
    level: 'HIGH',
    description: 'DOG-001 在 A 区围墙发现可疑人员翻越,已联动抓拍取证',
    latitude: 39.9021, longitude: 116.4035,
    status: 'PENDING',
    alertTime: new Date()
  },
  {
    _id: 'seed-alert-smoke',
    alertCode: 'ALT-DEMO-0003',
    deviceCode: 'UAV-002',
    deviceType: 'DRONE',
    alertType: 'SMOKE',
    level: 'HIGH',
    description: 'UAV-002 在 B 区仓库顶部检测到浓烟,疑似火情',
    latitude: 39.9062, longitude: 116.4058,
    status: 'PROCESSED',
    handler: 'admin',
    remark: '经核实为附近农户焚烧秸秆,已协调处理,安排持续观察',
    handleTime: new Date(),
    alertTime: new Date(Date.now() - 3600 * 1000)
  },
  {
    _id: 'seed-alert-lowbat',
    alertCode: 'ALT-DEMO-0004',
    deviceCode: 'DOG-002',
    deviceType: 'ROBOT_DOG',
    alertType: 'LOW_BATTERY',
    level: 'MEDIUM',
    description: 'DOG-002 电量不足(22%),已自动返航充电',
    latitude: 39.9005, longitude: 116.4089,
    status: 'PROCESSED',
    handler: 'admin',
    remark: '设备已返回充电桩,充电中,预计 40 分钟后恢复巡逻',
    handleTime: new Date(Date.now() - 7200 * 1000),
    alertTime: new Date(Date.now() - 7200 * 1000)
  },
  {
    _id: 'seed-alert-fault',
    alertCode: 'ALT-DEMO-0005',
    deviceCode: 'UAV-003',
    deviceType: 'DRONE',
    alertType: 'DEVICE_FAULT',
    level: 'MEDIUM',
    description: 'UAV-003 云台电机运行异常,需要检修',
    latitude: 39.9048, longitude: 116.4121,
    status: 'PROCESSED',
    handler: 'admin',
    remark: '现场重启云台自检通过,恢复正常巡逻',
    handleTime: new Date(Date.now() - 5400 * 1000),
    alertTime: new Date(Date.now() - 5400 * 1000)
  }
]);

print('MongoDB 告警种子写入完成,共 ' + dba.alert.countDocuments({}) + ' 条');
