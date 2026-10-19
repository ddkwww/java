@echo off
setlocal enabledelayedexpansion

:: 切到脚本所在目录
cd /d "%~dp0"

:: ===== 内嵌 Maven（已随项目存放，抗系统重置）=====
set "MAVEN=%~dp0tools\maven\bin\mvn.cmd"
if not exist "%MAVEN%" (
  echo [错误] 未找到内嵌 Maven: %MAVEN%
  echo 请安装 Maven 并加入 PATH，或把 Maven 解压到 tools\maven 目录。
  pause
  exit /b 1
)

:: ===== Java 定位（优先 PATH，其次常见目录，再次 JAVA_HOME）=====
if not defined JAVA_HOME (
  where java >nul 2>&1 && for /f "delims=" %%J in ('where java') do set "JAVA_HOME=%%~dpJ.."
  if not defined JAVA_HOME if exist "C:\Program Files\Zulu\zulu-21\bin\java.exe" set "JAVA_HOME=C:\Program Files\Zulu\zulu-21"
  if not defined JAVA_HOME if exist "C:\Program Files\Java" for /d %%D in ("C:\Program Files\Java\*") do if exist "%%D\bin\java.exe" set "JAVA_HOME=%%D"
)

echo 使用 Maven: %MAVEN%
if defined JAVA_HOME echo 使用 JAVA_HOME: %JAVA_HOME%
echo 正在构建（首次需联网从 aliyun 镜像拉取依赖，请保持联网）...
echo.

"%MAVEN%" -DskipTests package
if "%errorlevel%"=="0" (
  echo.
  echo [完成] 已生成 target\springBootTest-0.0.1-SNAPSHOT.jar
) else (
  echo.
  echo [失败] 构建出错，请查看上方 Maven 输出。
  pause
  exit /b 1
)
