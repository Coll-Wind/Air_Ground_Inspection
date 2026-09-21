@echo off
chcp 65001 >nul
echo ============================================
echo  演示数据重置:告警清空并注入种子(每类 1 条)
echo ============================================

echo.
echo [1/2] MongoDB 重置...
docker exec -i inspection-mongodb mongosh --quiet --file /dev/stdin < "%~dp0mongo-seed.js"
if errorlevel 1 (
    echo MongoDB 重置失败,请确认容器 inspection-mongodb 正在运行
    pause & exit /b 1
)

echo.
echo [2/2] Elasticsearch 重置...
python "%~dp0clear-es.py"
if errorlevel 1 (
    echo Elasticsearch 重置失败,请确认容器 9200 端口可访问
    pause & exit /b 1
)

echo.
echo 完成!当前告警:每种类型 1 条(2 条待处理 / 3 条已处理)
echo 后续演示中,仿真器每 3 分钟自然产生 1 条新告警(WebSocket 实时推送)
pause
