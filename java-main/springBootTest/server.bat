@echo off
setlocal enabledelayedexpansion

:: 切到脚本所在目录，项目相关路径均相对项目根
cd /d "%~dp0"

:: ===== 可配置项 =====
set "PORT=8081"
set "JAR=target\springBootTest-0.0.1-SNAPSHOT.jar"
set "LOG=server_run.log"
set "MYSQL_HOST=127.0.0.1"
set "MYSQL_PORT=3306"
set "MYSQL_USER=root"
set "MYSQL_PASS=123456"
:: ===================

echo ============================================
echo  Spring Boot 后端启动脚本 (springBootTest)
echo ============================================
echo.

:: 0) MySQL 检查与自动启动（应对开机 / 系统重置后 MySQL 未运行）
set "MYSQLADMIN="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe" set "MYSQLADMIN=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe"
if not defined MYSQLADMIN if exist "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysqladmin.exe" set "MYSQLADMIN=C:\Program Files\MySQL\MySQL Server 5.7\bin\mysqladmin.exe"
if not defined MYSQLADMIN where mysqladmin >nul 2>&1 && for /f "delims=" %%M in ('where mysqladmin') do set "MYSQLADMIN=%%M"

set "MYSQL_UP=0"
if defined MYSQLADMIN (
  "%MYSQLADMIN%" -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% ping >nul 2>&1 && set "MYSQL_UP=1"
)
if "%MYSQL_UP%"=="1" (
  echo [OK] MySQL 已在运行。
) else (
  echo [信息] MySQL 未运行，尝试启动服务（若提示权限不足，请右键本脚本"以管理员身份运行"）...
  net start MySQL80 >nul 2>&1
  net start MySQL >nul 2>&1
  timeout /t 3 >nul
  if defined MYSQLADMIN "%MYSQLADMIN%" -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASS% ping >nul 2>&1 && set "MYSQL_UP=1"
  if not "!MYSQL_UP!"=="1" (
    echo [警告] MySQL 仍无法连接。后端将因数据库连接失败而退出。
    echo         请先启动 MySQL，或右键本脚本"以管理员身份运行"后重试。
    pause
    exit /b 1
  )
)
echo.

:: 1) 若端口已被占用，先停掉旧实例（避免"端口被占用"导致 java 闪退）
set "OLD_PID="
for /f "tokens=5" %%P in ('netstat -aon ^| find ":8081 " ^| find "LISTENING"') do set "OLD_PID=%%P"
if defined OLD_PID (
  echo [信息] 端口 %PORT% 已被进程 %OLD_PID% 占用，先停止旧实例...
  taskkill /F /PID %OLD_PID% >nul 2>&1
  timeout /t 2 >nul
)
echo.

:: 2) 缺 jar 自动构建（需联网，走 aliyun 镜像）
if not exist "%JAR%" (
  echo [提示] 未找到 %JAR%，尝试自动构建（需联网）...
  call "%~dp0build.bat"
  if not exist "%JAR%" (
    echo [错误] 构建失败，未生成 %JAR%。请检查 build.bat 的输出。
    pause
    exit /b 1
  )
)
echo.

:: 3) 定位 java 可执行文件（避免双击时 PATH 中找不到 java 而静默失败）
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
echo.

:: 4) 前台启动（窗口常驻、日志可见，关闭此窗口即停止服务；java 异常退出时 pause 显示错误）
echo 使用 Java: %JAVA_EXE%
echo 正在前台启动后端（端口 %PORT%）...
echo 关闭此窗口即停止服务；日志同时写入 %LOG%
echo.
"%JAVA_EXE%" -jar "%JAR%" --server.port=%PORT% --logging.file.name=%LOG%
echo.
echo [后端已退出] 退出码 %errorlevel%
echo 若为数据库连接/端口类错误，请检查 MySQL 与端口占用。
pause
