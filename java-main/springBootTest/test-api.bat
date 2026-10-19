@echo off
setlocal
:: springBootTest API smoke test (pure curl, no deps)
:: NOTE: a local HTTP proxy exists, curl MUST use --noproxy * to reach localhost
set "PORT=8081"
set "BASE=http://127.0.0.1:%PORT%"
set "CURL=curl.exe --noproxy * --max-time 10 -s"

echo ============================================
echo  springBootTest API Smoke Test  (port %PORT%)
echo ============================================
echo.

set "FAIL=0"

for /f %%c in ('%CURL% -o nul -w "%%{http_code}" %BASE%/') do set "code=%%c"
call :check "GET / (health)" "%code%" "200"

for /f %%c in ('%CURL% -o nul -w "%%{http_code}" %BASE%/api/captcha') do set "code=%%c"
call :check "GET /api/captcha" "%code%" "200"

for /f %%c in ('%CURL% -o nul -w "%%{http_code}" -X POST -H "Content-Type: application/json" -d "{"username":"smoke","password":"wrong"}" %BASE%/api/login') do set "code=%%c"
call :check "POST /api/login (wrong pwd)" "%code%" "401"

for /f %%c in ('%CURL% -o nul -w "%%{http_code}" -X POST -H "Content-Type: application/json" -d "{"username":"a","password":"short"}" %BASE%/api/register') do set "code=%%c"
call :check "POST /api/register (no captcha)" "%code%" "400"

for /f %%c in ('%CURL% -o nul -w "%%{http_code}" %BASE%/api/me') do set "code=%%c"
call :check "GET /api/me (no token)" "%code%" "401"

echo.
if "%FAIL%"=="0" (
  echo [ALL PASS] all endpoints responded as expected
) else (
  echo [%FAIL% FAILED] some checks failed, see server_run.log
)
echo.
pause
goto :eof

:check
set "name=%~1"
set "got=%~2"
set "want=%~3"
if "%got%"=="%want%" (
  echo [PASS] %name%  -^> %got% (want %want%)
) else (
  echo [FAIL] %name%  -^> %got% (want %want%)
  set /a FAIL+=1
)
goto :eof
