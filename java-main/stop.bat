@echo off
setlocal
cd /d "%~dp0"

set "BACKEND_PORT=8081"
set "FRONTEND_PORT=5173"
set "BACKEND_TITLE=springBootTest-Backend"
set "FRONTEND_TITLE=fanren-login-vue-Frontend"
set "KILLED=0"

echo 正在停止后端（端口 %BACKEND_PORT%）...
for /f "tokens=5" %%P in ('netstat -aon ^| find ":%BACKEND_PORT% " ^| find "LISTENING"') do (
  taskkill /F /PID %%P >nul 2>&1 && set "KILLED=1"
)

echo 正在停止前端（端口 %FRONTEND_PORT%）...
for /f "tokens=5" %%P in ('netstat -aon ^| find ":%FRONTEND_PORT% " ^| find "LISTENING"') do (
  taskkill /F /PID %%P >nul 2>&1 && set "KILLED=1"
)

echo 兜底：按窗口标题结束可能残留的进程...
taskkill /F /FI "WINDOWTITLE eq %BACKEND_TITLE%" >nul 2>&1 && set "KILLED=1"
taskkill /F /FI "WINDOWTITLE eq %FRONTEND_TITLE%" >nul 2>&1 && set "KILLED=1"

if "%KILLED%"=="1" (echo 已停止相关服务（后端 / 前端）) else (echo 未发现运行中的后端 / 前端进程)
pause
goto :eof
