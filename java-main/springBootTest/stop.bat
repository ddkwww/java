@echo off
setlocal

:: 切到脚本所在目录
cd /d "%~dp0"

:: 关闭监听 8081 端口的后端进程
set "PORT=8081"

netstat -aon | findstr ":%PORT% " >nul
if errorlevel 1 (
    echo 未找到监听端口 %PORT% 的后端进程（可能本来就没运行）。
    echo.
    pause
    exit /b 0
)

echo 正在停止监听端口 %PORT% 的后端进程...
for /f "tokens=5" %%P in ('netstat -aon ^| findstr ":%PORT% "') do (
    echo   进程 PID=%%P
    taskkill /F /PID %%P >nul 2>&1
    if errorlevel 1 (
        echo   结束失败，请右键以管理员身份运行本脚本。
    ) else (
        echo   已结束。
    )
)

echo.
echo 后端已停止（端口 %PORT%）。
pause
