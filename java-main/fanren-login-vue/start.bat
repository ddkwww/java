@echo off
setlocal

:: 切换到脚本所在目录（即前端项目根目录）
cd /d "%~dp0"

:: 选择 npm：优先使用 WorkBuddy 管理的 node，找不到则回退到系统 PATH 中的 npm
set "NPM=%USERPROFILE%\.workbuddy\binaries\node\versions\22.22.2-2\npm.cmd"
if not exist "%NPM%" set "NPM=npm.cmd"
if not exist "%NPM%" set "NPM=npm"

:: 依赖检测：仅 node_modules 目录存在还不够，必须 vite 入口存在，否则自动安装
:: （旧脚本只判断 node_modules 目录，依赖不完整时 vite 缺失会导致启动窗口“闪退”）
if not exist "node_modules\.bin\vite.cmd" (
  echo 检测到前端依赖不完整（缺少 vite），正在自动安装（npmmirror 镜像）...
  "%NPM%" install --registry=https://registry.npmmirror.com
)

:: 启动开发服务器：用 cmd /k 保持窗口，启动失败也不会闪退，可直接看到报错
echo 正在启动「凡人修仙传」登录页...
echo 访问地址： http://localhost:5173
start "fanren-login-vue" cmd /k call "%NPM%" run dev

:: 稍等几秒让服务起来，再打开浏览器
timeout /t 4 /nobreak >nul
start "" http://localhost:5173/

echo.
echo 前端已在独立窗口启动；若该窗口报错且未自动关闭，请截图反馈。
pause
goto :eof
