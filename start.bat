@echo off
chcp 65001 >nul
title 空地协同巡检平台 - 一键启动
setlocal enabledelayedexpansion

rem ================= 配置 =================
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
set "DOCKER_BIN=C:\Users\liuyazhe\AppData\Local\Programs\DockerDesktop\resources\bin"
set "PATH=%DOCKER_BIN%;%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"
set "BACKEND_JAR=backend\target\air-ground-inspection-1.0.0.jar"

echo ============================================
echo   无人机-机器狗空地协同巡检平台 一键启动
echo ============================================

rem [1/6] 启动中间件容器
echo [1/6] 启动中间件容器(docker compose)...
docker compose -f docker\docker-compose.yml up -d
if errorlevel 1 (
    echo [错误] Docker 未就绪,请先启动 Docker Desktop 后重试
    pause & exit /b 1
)

rem [2/6] 等待中间件端口就绪(Kafka 9092 / Mongo 27017 / ES 9200 / HDFS 9000)
echo [2/6] 等待中间件端口就绪(最长约 3 分钟)...
set /a tries=0
:wait_middleware
timeout /t 3 /nobreak >nul
set /a tries+=1
powershell -NoProfile -Command "foreach($p in 9092,27017,9200,9000){ if(-not (Test-NetConnection -ComputerName localhost -Port $p -InformationLevel Quiet -WarningAction SilentlyContinue)){ exit 1 } }" >nul 2>nul
if errorlevel 1 (
    if !tries! geq 60 (
        echo [错误] 中间件等待超时,请检查 docker compose ps
        pause & exit /b 1
    )
    goto wait_middleware
)
echo       中间件全部就绪!

rem [3/6] 启动后端(若 8081 未被占用)
echo [3/6] 启动后端服务(端口 8081)...
powershell -NoProfile -Command "if(Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue){ exit 0 } else { exit 1 }" >nul 2>nul
if not errorlevel 1 (
    echo       8081 已有服务在运行,跳过后端启动
    goto frontend
)
if not exist "%BACKEND_JAR%" (
    echo [错误] 未找到 %BACKEND_JAR%,请先在 backend 目录执行: mvn package -DskipTests
    pause & exit /b 1
)
start "backend-8081" cmd /c ""%JAVA_HOME%\bin\java" --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/sun.nio.cs=ALL-UNNAMED --add-opens java.base/sun.security.action=ALL-UNNAMED --add-opens java.base/sun.util.calendar=ALL-UNNAMED -jar "%BACKEND_JAR%" && pause"

rem [4/6] 启动前端(若 5173 未被占用)
:frontend
echo [4/6] 启动前端服务(端口 5173)...
powershell -NoProfile -Command "if(Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue){ exit 0 } else { exit 1 }" >nul 2>nul
if not errorlevel 1 (
    echo       5173 已有服务在运行,跳过前端启动
    goto wait_backend
)
start "frontend-5173" cmd /c "cd /d "%~dp0frontend" && (if not exist node_modules call npm install) && npm run dev && pause"

rem [5/6] 等待后端接口就绪
echo [5/6] 等待后端接口就绪(最长约 90 秒)...
set /a tries=0
:wait_backend
timeout /t 3 /nobreak >nul
set /a tries+=1
powershell -NoProfile -Command "try{ $r=Invoke-WebRequest -UseBasicParsing -Uri http://localhost:8081/api/auth/login -Method Options -TimeoutSec 2; exit 0 }catch{ exit 1 }" >nul 2>nul
if errorlevel 1 (
    if !tries! geq 30 (
        echo [错误] 后端启动超时,请查看 backend 窗口日志
        pause & exit /b 1
    )
    goto wait_backend
)
echo       后端就绪!

rem [6/6] 打开浏览器
echo [6/6] 打开浏览器...
start "" http://localhost:5173

echo ============================================
echo   启动完成! 登录账号: admin / admin123
echo   前端: http://localhost:5173
echo   后端: http://localhost:8081
echo ============================================
pause
