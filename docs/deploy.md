# 空地协同巡检集成平台 - 部署说明

## 一、技术栈

| 层次 | 技术 | 版本 |
|---|---|---|
| 前端 | Vue 3 + Vite + Element Plus + ECharts | Vue 3.4 |
| 后端 | Java 17 + Spring Boot 3.2 + Maven | Java 17 |
| 消息中间件 | Apache Kafka (KRaft 模式) | 3.7.0 |
| 分布式数据库 | MongoDB | 7.0 |
| 分布式文件系统 | Hadoop HDFS (伪分布式) | 3.2.1 |
| 全文检索引擎 | Elasticsearch + Kibana | 8.13.4 |
| 网关/负载均衡 | Nginx | 1.25 |
| 容器化 | Docker Desktop + WSL2 | Docker 29.8 |

## 二、环境准备

### 2.1 安装 WSL2 + Ubuntu
```powershell
# 管理员 PowerShell
wsl --install
# 重启后设置 Ubuntu 用户名密码
```

### 2.2 安装 Docker Desktop
- 下载: https://www.docker.com/products/docker-desktop/
- 安装时勾选 "Use WSL 2 instead of Hyper-V"
- 配置镜像加速: Settings → Docker Engine → 添加:
```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://dockerproxy.com"
  ]
}
```

### 2.3 安装 JDK 17 + Maven
- JDK 17: https://www.microsoft.com/openjdk
- Maven 3.9+: https://maven.apache.org/

### 2.4 安装 Node.js
- Node 18+: https://nodejs.org/

## 三、启动中间件

```bash
cd docker
docker compose up -d
```

验证各组件(均 healthy):
- HDFS NameNode: http://localhost:9870
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601
- MongoDB: localhost:27017
- Kafka: localhost:9092
- Nginx: http://localhost

## 四、启动后端

```bash
cd backend
# 编译打包
mvn package -DskipTests
# 启动(需带 --add-opens 参数解决 Hadoop + Java 17 兼容)
java --add-opens=java.base/java.lang=ALL-UNNAMED \
     --add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
     --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
     --add-opens=java.base/java.io=ALL-UNNAMED \
     --add-opens=java.base/java.net=ALL-UNNAMED \
     --add-opens=java.base/java.nio=ALL-UNNAMED \
     --add-opens=java.base/java.util=ALL-UNNAMED \
     --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
     --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
     --add-opens=java.base/sun.nio.cs=ALL-UNNAMED \
     -jar target/air-ground-inspection-1.0.0.jar
```

后端启动后: http://localhost:8080

## 五、启动前端

```bash
cd frontend
npm install
npm run dev
```

前端访问: http://localhost:5173

## 六、访问入口

| 入口 | 地址 | 说明 |
|---|---|---|
| 前端(直连) | http://localhost:5173 | Vite 开发服务器 |
| Nginx 网关 | http://localhost | 统一入口,反向代理后端 |
| 后端 API | http://localhost:8080/api | Spring Boot |
| Kibana | http://localhost:5601 | ES 可视化 |
| HDFS Web | http://localhost:9870 | HDFS 管理界面 |

## 七、系统架构

```
前端(Vue3) → Nginx(80) → Spring Boot(8080)
                              ├─ Kafka(9092)   消息生产/消费
                              ├─ MongoDB(27017) 业务数据
                              ├─ HDFS(9000)     巡检图片
                              └─ ES(9200)       告警检索
```

## 八、核心数据流

1. 设备仿真器(@Scheduled)生成无人机/机器狗数据 → Kafka
2. Kafka 消费者消费消息 → 写入 MongoDB(设备/任务/告警/记录)
3. 告警同时索引到 Elasticsearch(含 geo_point 地理检索)
4. 巡检图片上传到 HDFS
5. 告警通过 WebSocket 实时推送到前端

## 九、核心 API

| 接口 | 方法 | 说明 |
|---|---|---|
| /api/devices | GET | 设备列表 |
| /api/devices/stats | GET | 设备统计 |
| /api/tasks | POST | 下发巡检任务 |
| /api/tasks | GET | 任务列表 |
| /api/alerts | GET | 告警列表 |
| /api/alerts/search | GET | ES 多条件检索 |
| /api/alerts/geo-search | GET | ES 地理范围检索 |
| /api/alerts/aggregate | GET | ES 告警聚合统计 |
| /api/records | GET | 巡检记录 |
| /api/records/image | GET | 下载 HDFS 巡检图片 |

## 十、测试用例对照

全部 10 个测试用例(TC-001 ~ TC-010)已验证通过,详见测试报告。
