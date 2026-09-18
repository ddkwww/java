@echo off
setlocal

:: 切换到脚本所在目录（即项目根目录）
cd /d "%~dp0"

:: 选择 node/npm：优先使用 WorkBuddy 管理的 node，找不到则回退到系统 PATH 中的 npm
set "NPM=%USERPROFILE%\.workbuddy\binaries\node\versions\22.22.2-2\npm.cmd"
if not exist "%NPM%" set "NPM=npm.cmd"
if not exist "%NPM%" set "NPM=npm"

:: 依赖缺失则自动安装（国内走 npmmirror 镜像，速度更快）
if not exist "node_modules" (
  echo 首次运行，正在安装依赖（npmmirror 镜像）...
  "%NPM%" install --registry=https://registry.npmmirror.com
)

:: 后台启动开发服务器，并自动打开浏览器
echo 正在启动「凡人修仙传」登录页...
echo 访问地址： http://localhost:5173
start "" cmd /c "%NPM%" run dev
timeout /t 3 /nobreak >nul
start "" http://localhost:5173/

endlocal
