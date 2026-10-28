@echo off
setlocal

:: 切换到脚本所在目录（即前端项目根目录）
cd /d "%~dp0"

:: ---------- 定位 node/npm：优先 WorkBuddy 管理的 node，再回退系统 PATH ----------
:: 说明：管理的 node 版本目录会随 WorkBuddy 更新而变化，这里动态探测，
::       避免把版本号硬编码死（如 22.22.2-2 失效后静默回退导致莫名闪退）。
set "NPM="
if exist "%USERPROFILE%\.workbuddy\binaries\node\versions\current\npm.cmd" (
  set "NPM=%USERPROFILE%\.workbuddy\binaries\node\versions\current\npm.cmd"
) else (
  for /f "delims=" %%V in ('dir /b /ad "%USERPROFILE%\.workbuddy\binaries\node\versions" 2^>nul ^| findstr /v /i "current"') do (
    if exist "%USERPROFILE%\.workbuddy\binaries\node\versions\%%V\npm.cmd" (
      set "NPM=%USERPROFILE%\.workbuddy\binaries\node\versions\%%V\npm.cmd"
    )
  )
)
if not defined NPM (
  set "NPM=npm.cmd"
  where npm.cmd >nul 2>nul || set "NPM=npm"
)

:: 校验：找不到任何 node/npm 时，明确报错并暂停，避免窗口一闪而过
set "NPM_RESOLVED="
for /f "delims=" %%P in ('where "%NPM%" 2^>nul') do set "NPM_RESOLVED=%%P"
if not defined NPM_RESOLVED (
  echo [错误] 未找到可用的 node / npm，前端无法启动。
  echo 请确认以下任一条件满足：
  echo   1) WorkBuddy 管理的 node 已安装（默认位于 %%USERPROFILE%%\.workbuddy\binaries\node）；
  echo   2) 系统已安装 Node.js 且其目录已加入 PATH 环境变量。
  pause
  goto :eof
)
echo 使用 node/npm： %NPM_RESOLVED%

:: ---------- 依赖检测：仅 node_modules 目录不够，必须 vite 入口存在 ----------
:: （旧脚本只判断 node_modules 目录，依赖不完整时 vite 缺失会导致启动窗口“闪退”）
if not exist "node_modules\.bin\vite.cmd" (
  echo 检测到前端依赖不完整/残缺（缺少 vite 入口），准备重装...
  :: 关键：先彻底清除残缺的 node_modules，避免 npm 在半成品上增量修复再次卡死
  if exist "node_modules" (
    echo   已有 node_modules 可能残缺，先清理...
    rmdir /s /q node_modules
  )
  :: --prefer-offline 优先用本地缓存（已命中 vite/esbuild），几乎不联网、秒级完成
  :: --no-audit --no-fund 关闭额外网络请求；--loglevel=http 显示进度，避免误判卡死
  echo 正在自动安装依赖（npmmirror 镜像 + 离线优先）...
  "%NPM%" install --prefer-offline --no-audit --no-fund --loglevel=http --registry=https://registry.npmmirror.com
  if not exist "node_modules\.bin\vite.cmd" (
    echo [错误] 依赖安装失败，请检查网络连接或 npm registry 配置。
    echo [提示] 可将项目目录加入 Windows Defender 排除项，加速 node_modules 写入。
    pause
    goto :eof
  )
)

:: ---------- 启动开发服务器：用 cmd /k 保持窗口，启动失败也能看到报错 ----------
echo 正在启动「凡人修仙传」登录页...
echo 访问地址： http://192.168.68.100:5173
start "fanren-login-vue" cmd /k call "%NPM%" run dev

:: 轮询等待服务就绪（最多约 15 秒）后再打开浏览器，避免过早打开空白页
set "READY=0"
for /L %%i in (1,1,15) do (
  powershell -NoProfile -Command "try{(Invoke-WebRequest -Uri 'http://192.168.68.100:5173' -UseBasicParsing -TimeoutSec 1).StatusCode;exit 0}catch{exit 1}" >nul 2>nul
  if not errorlevel 1 (
    set "READY=1"
    goto :open
  )
  timeout /t 1 /nobreak >nul
)
:open
timeout /t 1 /nobreak >nul
start "" http://192.168.68.100:5173/

echo.
echo 前端已在独立窗口启动；若该窗口报错且未自动关闭，请截图反馈。
pause
goto :eof
