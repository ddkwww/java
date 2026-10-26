@echo off
setlocal

:: 切到脚本所在目录（java-main/java-main），后续全部使用相对路径
:: 这样即使移动整个 java-main 文件夹，脚本依然可用
cd /d "%~dp0"

:: ========= 可配置项（相对路径，不写死绝对路径）=========
set "BACKEND_DIR=springBootTest"
set "FRONTEND_DIR=fanren-login-vue"
set "BACKEND_PORT=8081"
set "FRONTEND_PORT=5173"
set "JAR=target\springBootTest-0.0.1-SNAPSHOT.jar"
set "MAVEN=tools\maven\bin\mvn.cmd"
set "MVNW=mvnw.cmd"
set "BACKEND_TITLE=springBootTest-Backend"
set "FRONTEND_TITLE=fanren-login-vue-Frontend"
:: =====================================================

echo ============================================
echo  前后端总控启动脚本
echo  后端目录 : %BACKEND_DIR%  (端口 %BACKEND_PORT%)
echo  前端目录 : %FRONTEND_DIR%  (端口 %FRONTEND_PORT%)
echo ============================================
echo.

:: ---------- 1) 启动后端 ----------
echo [1/2] 准备启动后端（端口 %BACKEND_PORT%）...

:: 定位 java 可执行文件（避免双击时 PATH 中找不到 java 而静默失败）
set "JAVA_EXE="
where java >nul 2>&1 && set "JAVA_EXE=java"
if not defined JAVA_EXE if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE (
  for %%D in (
    "C:\Program Files\Zulu"
    "C:\Program Files\Java"
    "C:\Program Files\Eclipse Adoptium"
    "C:\Program Files\Amazon Corretto"
  ) do (
    if not defined JAVA_EXE (
      for /d %%J in ("%%D\*") do (
        if exist "%%J\bin\java.exe" set "JAVA_EXE=%%J\bin\java.exe"
      )
    )
  )
)
if not defined JAVA_EXE (
  echo [错误] 未找到 java。请安装 JDK，或将 JDK 的 bin 目录加入 PATH，或设置 JAVA_HOME 环境变量。
  pause
  exit /b 1
)
echo   使用 Java: %JAVA_EXE%

:: 进入后端目录，jar 缺失则自动构建
pushd "%BACKEND_DIR%"
if not exist "%JAR%" (
  echo   jar 不存在，开始自动构建（可能需要联网下载依赖）...
  if exist "%MAVEN%" (
    call "%MAVEN%" -DskipTests package
  ) else if exist "%MVNW%" (
    call "%MVNW%" -DskipTests package
  ) else (
    echo [错误] 未找到 Maven（tools\maven 或 mvnw），无法自动构建，请先手动 mvn package。
    popd
    pause
    exit /b 1
  )
  if not exist "%JAR%" (
    echo [错误] 构建后仍未找到 %JAR%，请检查上面的构建日志。
    popd
    pause
    exit /b 1
  )
)
:: 启动后端（独立窗口运行，关闭该窗口即停止）
start "%BACKEND_TITLE%" "%JAVA_EXE%" -jar "%JAR%" --server.port=%BACKEND_PORT% --logging.file.name=server_run.log
popd
echo   后端启动命令已下发，日志写入 %BACKEND_DIR%\server_run.log

:: ---------- 2) 启动前端 ----------
echo [2/2] 准备启动前端（端口 %FRONTEND_PORT%）...

:: 定位 npm（优先 WorkBuddy 管理的 node，其次系统 PATH 中的 npm）
set "NPM="
if exist "%USERPROFILE%\.workbuddy\binaries\node\versions\22.22.2-2\npm.cmd" set "NPM=%USERPROFILE%\.workbuddy\binaries\node\versions\22.22.2-2\npm.cmd"
if not defined NPM where npm.cmd >nul 2>&1 && set "NPM=npm.cmd"
if not defined NPM where npm >nul 2>&1 && set "NPM=npm"
if not defined NPM (
  echo [错误] 未找到 npm。请安装 Node.js 并将其加入 PATH。
  pause
  exit /b 1
)
echo   使用 npm: %NPM%

:: 进入前端目录，依赖缺失则安装
:: 注意：仅判断 node_modules 目录不够，必须确认 vite 入口存在，
::       否则依赖装一半时 npm run dev 会瞬间退出导致“闪退”
pushd "%FRONTEND_DIR%"
if not exist "node_modules\.bin\vite.cmd" (
  echo   前端依赖缺失（缺少 vite），正在安装（npmmirror 镜像）...
  "%NPM%" install --registry=https://registry.npmmirror.com
  if not exist "node_modules\.bin\vite.cmd" (
    echo [错误] 依赖安装失败，请检查网络连接或 npm registry 配置。
    popd
    pause
    exit /b 1
  )
)
:: 用 cmd /k 保持窗口：即使启动失败也能看到报错，避免一闪而过
start "%FRONTEND_TITLE%" cmd /k call "%NPM%" run dev
popd
echo   前端启动命令已下发

echo.
echo ============ 启动完成 ============
echo 后端访问： http://192.168.68.100:%BACKEND_PORT%
echo 前端访问： http://192.168.68.100:%FRONTEND_PORT%
echo 停止服务：双击 stop.bat，或关闭上面两个黑色命令行窗口。
echo ====================================
pause
goto :eof
