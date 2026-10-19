# springBootTest 启动手册（给 AI 助手自己看）

> 用途：本机每次重置后从零启动后端。下次直接照本手册执行，不要重新摸索。
> 项目根目录：`C:\Users\Administrator\Desktop\java-main\springBootTest`
> 最后一次验证：2026-10-08（后端联调全通过 + 修复注册 NPE + 补充 test-api.bat 冒烟脚本）

---

## 0. 一句话快启（jar 已存在时）

1. **双击 `server.bat` 即可**（脚本已内置：自动起 MySQL + 缺 jar 自动构建 + 前台启动，无需任何手动前置）。
2. 若弹出“权限不足/MySQL 无法连接”，**右键 `server.bat` → 以管理员身份运行**再试一次。
3. 探活：`curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/` → 期望 `200`。

## 1. 环境事实（均已验证，别再探测）

| 项 | 值 |
|---|---|
| Java | Zulu JDK 21，`java`/`javac` 已在 PATH；备用 `C:\Program Files\Zulu\zulu-21` |
| Maven | **已内嵌** `tools\maven\bin\mvn.cmd`（项目自带，抗系统重置）。系统未装 `mvn` |
| `mvnw` | **不可用**：`.mvn/wrapper/` 缺 `maven-wrapper.jar`，首次运行会联网下载 Maven 本体（沙箱禁网会失败）|
| MySQL | 8.0.39；`mysql.exe` = `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe` |
| 库/账号 | 库 `fanren_login`，`root` / `123456`，端口 `3306`，`localhost` |
| 应用端口 | `8081`（Tomcat）|
| 可执行 jar | `target\springBootTest-0.0.1-SNAPSHOT.jar`（约 54MB，已打进 mysql 驱动）|
| 构建命令 | `tools\maven\bin\mvn.cmd -DskipTests package`（**在线**走 aliyun 镜像）|

## 2. 完整启动步骤（幂等，可每次重跑）

### 2.1 确保 MySQL 已启动（重置后最常漏的一步）
```bat
:: 查看服务名（通常含 MySQL，如 MySQL80）
powershell -NoProfile -Command "Get-Service | Where-Object{$_.DisplayName -match 'MySQL'} | Select Name,Status"
:: 若未 Running，启动（服务名按上面实际结果替换）
net start MySQL80
```
连不通时的兜底探活：
```bat
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 -h127.0.0.1 -P3306 -e "SELECT 1"
```

### 2.2 构建（仅当 `target\*.jar` 不存在或代码有改动时）
双击 `build.bat`，或：
```bat
cd /d "C:\Users\Administrator\Desktop\java-main\springBootTest"
tools\maven\bin\mvn.cmd -DskipTests package
```
- 首次/重置后 `.m2` 缓存不全 → **必须联网**，走 `aliyun` 镜像自动拉取依赖（约 8~60s）。
- 不要加 `-o`（离线）：`.m2` 缺 surefire、spring-boot 插件依赖等，离线必失败。
- **改代码后重建，必须先停掉正在运行的后端**：repackage 阶段要把旧 `*.jar` 改名成 `*.jar.original`，若旧 jar 被运行中的 java 进程占用，`rename` 失败 → `BUILD FAILURE`。停服：`stop.bat` 或 `taskkill /F /PID <pid>`（pid 见 `netstat -aon | findstr :8081`）。

### 2.3 启动后端
- 方式 A（推荐，缺 jar 会自动构建）：双击 `server.bat`
- 方式 B（直接跑）：
```bat
cd /d "C:\Users\Administrator\Desktop\java-main\springBootTest"
java -jar target\springBootTest-0.0.1-SNAPSHOT.jar --server.port=8081 --logging.file.name=server_run.log
```
日志：控制台 + `server_run.log`。关闭 `server.bat` 开的窗口即停服务。

### 2.4 探活与冒烟
```bat
curl -s -o /dev/null -w "HTTP:%{http_code}\n" http://localhost:8081/
:: 未带 token 访问受保护接口应 401
curl -s -i http://localhost:8081/api/me | head -1
```
**一键冒烟脚本**：双击 `test-api.bat`，自动跑 5 项校验并给出 PASS/FAIL 汇总（首页 200 / captcha 200 / 错密登录 401 / 无验证码注册 400 / 无 token 访问 me 401）。脚本纯 ASCII（规避 §9 乱码坑），已 `set "CURL=curl.exe --noproxy *"`，绕过本机代理直打 localhost。

## 3. 接口契约（联调用，已验证）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/captcha` | 返回 `captchaId` + base64 图片（验证码文字只在图里，不回传）|
| POST | `/api/register` | body `{username,password,captchaId,captcha}`；成功返回 `fx_` 开头 token |
| POST | `/api/login` | body `{username,password}`；成功返回 token |
| GET | `/api/me` | Header `Authorization: Bearer <token>`；返回 `{username, realm, level}` |
| POST | `/api/logout` | 同上 Header；登出后该 token 立即失效（再访问 → 401）|

密码以 **SHA-256（64 位哈希）** 落库，非明文。JPA 启动时自动建表 `t_user`。

端到端冒烟（注册需先取图识图）：取 `/api/captcha` → 识图得 4 位码 → 带 `captchaId+captcha` 注册 → 登录 → 带 token 取 `/api/me` → 登出 → 再用原 token 访问应 401。

## 4. 已踩过的坑（不要再花时间排查）

1. **离线构建 `-o` 必失败**：`.m2` 缓存不全（surefire 3.5.3、spring-boot 插件依赖缺失）。一律在线构建。
2. **`mvnw` 不可用**：缺 `maven-wrapper.jar`，首跑下载 Maven 本体被沙箱拦。改用内嵌 `tools\maven\bin\mvn.cmd`。
3. **`pom.xml` 已固定 `mysql-connector-j` 版本为 `8.0.33`**（第 46~47 行）：BOM 默认 9.1.0 当时未缓存、离线拉不到。8.0.33 兼容 MySQL 8.0.39，功能无影响。**联网且 `.m2` 已有 9.x 后，可删掉 `<version>8.0.33</version>` 这行恢复默认。**
4. **PowerShell 调用 `mvn.cmd` 时，`-Dxxx.yyy=zzz` 会被误当生命周期阶段**：用 `--%` 停止解析原样传参；写 `.bat` 则无需 `--%`。
5. **Git Bash 下跑 `mvn`（sh 脚本）找不到 `boot/classworlds`**：因 MAVEN_HOME 被设成虚拟路径、Windows `java.exe` 读不懂。务必用 `mvn.cmd`（Windows 批处理）。
6. **改代码后 `package` 失败 `Unable to rename ...jar to ...jar.original`**：正在运行的后端锁住了旧 jar（见 §2.2）。先停服再构建。
7. **`POST /api/register` 缺 `captchaId` 曾返回 500**：`ConcurrentHashMap.get(null)` 抛 NPE（key 不允许 null）。已修复为「先判空」→ 返回 400「验证码已失效，请刷新」。回归：`GET /api/captcha` 200、错密登录 401 均正常。

## 5. 项目当前「缺什么」/ 待办

- [x] 内嵌 Maven（`tools/maven`）——已补，抗重置。
- [x] 一键构建脚本（`build.bat`）——已补。
- [x] `server.bat` 缺 jar 自动构建——已补。
- [x] 本启动手册（`STARTUP_GUIDE.md`）——已补。
- [x] MySQL 自动启动：`server.bat` 已内置检测+`net start MySQL80`（见 §2.1 逻辑已合入脚本），重置后双击即可，无需手动起库。
- [ ] `.m2` 依赖缓存每次重置后需重新联网拉取（aliyun 可达，慢但稳）。
- [ ] `pom.xml` 的 `mysql-connector-j` 版本固定为 8.0.33 是离线 workaround，联网后可恢复 BOM 默认。
- [x] 前端 `fanren-login-vue` 启动流程：双击其根目录 `start.bat`（自动 npm install + 启 Vite 5173 + 开浏览器）；已验证与后端 8081 跨域联通（见 §7）。
- [x] 一键冒烟脚本 `test-api.bat`：纯 curl，5 项接口校验 + PASS/FAIL 汇总；已补（2026-10-08）。

## 6. 重置后的最小动作清单（照抄）

```
1. 双击 server.bat      # 脚本自动：起 MySQL → 缺 jar 则构建 → 前台启动（端口被占用也会先杀旧实例）
   （若提示 MySQL 权限不足，右键“以管理员身份运行”）
2. curl http://localhost:8081/   # 应 200
```
> 说明：`server.bat` 已把 §2.1（起 MySQL）、§2.2（构建）、§2.3（启动）三件事合一，且会先杀掉占用 8081 的旧进程，杜绝“端口被占用闪退”。`build.bat`/`stop.bat` 仍保留作单独使用。

## 7. 前端 fanren-login-vue 启动（前后端联调）

前端路径：`C:\Users\Administrator\Desktop\java-main\fanren-login-vue`（Vue 3 + Vite 5，端口 5173）。

### 7.1 启动
- 双击 `start.bat`：自动 `npm install`（无 node_modules 时走 npmmirror 镜像）→ `npm run dev` → 自动开浏览器 `http://localhost:5173`。
- 脚本优先用 WorkBuddy 管理 node：`%USERPROFILE%\.workbuddy\binaries\node\versions\22.22.2-2\npm.cmd`，找不到回退系统 `npm`。
- 若 5173 被占，Vite 会顺延到 5174/5175…（属正常，浏览器用实际端口即可）。

### 7.2 后端对接（已验证联通）
- 前端 `.env`：`VITE_API_BASE=http://localhost:8081`，**直连** Spring Boot 后端（无 vite proxy）。
- 后端 CORS 动态回显请求源且 `Access-Control-Allow-Credentials: true` → 任何前端端口都能跨域带凭据调用 `/api/*`（已用 Origin:5173 / 9999 探活确认）。
- 验证结果：5173 页面正常返回「凡人修仙传 · 踏入仙途」，`/src/main.js` 200，带 Origin 调 `/api/captcha` 返回 200 + 正确 CORS 头 → 前后端全栈打通。

### 7.3 探活与联调
```bat
curl -s -o /dev/null -w "前端 HTTP:%{http_code}\n" http://localhost:5173/      :: 应 200
:: 浏览器打开后：注册（需先识图填验证码）→ 登录 → 首页 /api/me 拉资料 → 退出
```

## 8. 重置后完整最小清单（前后端都起）

```
1. 后端：双击 springBootTest\server.bat   # 自动起 MySQL+构建+启动 8081
2. 前端：双击 fanren-login-vue\start.bat   # 自动装依赖+启 Vite 5173+开浏览器
3. 浏览器开 http://localhost:5173 → 注册/登录 → 应进入首页
```
> 注：后端若提示 MySQL 权限不足，右键 server.bat「以管理员身份运行」。前端 start.bat 已处理 node 选择，通常无需管理员。

## 9. 重要：.bat 必须用 GBK(ANSI) 保存（否则双击乱码/命令被拆坏）

**现象**：若 .bat 被保存成 UTF-8（哪怕加了 `chcp 65001`），中文 Windows 双击时会用系统默认代码页 936(GBK) 读取，导致中文行变乱码（如 `鐩戝惉...`），并连带把 `netstat` 等命令拆坏成 `tat`、`端口` 等“不是内部或外部命令”。

**规则**：
- `server.bat` / `stop.bat` / `build.bat` 必须存为 **GBK / ANSI（代码页 936），无 BOM**。
- 切勿在脚本里写 `chcp 65001`（UTF-8 无 BOM 批处理中此命令经常失效，反而添乱）。
- 若用编辑器重改这些 .bat，保存时选「编码：ANSI」或「GB2312/GBK」，不要选 UTF-8。
- 重置后若发现乱码，按上面三条重新保存即可，逻辑无需改。
