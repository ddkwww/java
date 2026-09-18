# 凡人修仙传 · 登录页（Vue 3 + Vite）

将原本的单文件 `凡人修仙传登录.html` 改造为标准 Vue 3 单文件组件（SFC）项目。

## 运行

```bash
npm install
npm run dev      # 开发服务器，默认 http://localhost:5173
npm run build    # 产出 dist/ 用于部署
npm run preview  # 预览构建产物
```

> 国内环境若 `npm install` 慢，可使用镜像：
> `npm install --registry=https://registry.npmmirror.com`

## 一键启动（Windows 双击运行）

项目根目录已提供 `start.bat`：双击即可自动安装依赖（若 `node_modules` 不存在，走 npmmirror 镜像）并启动开发服务器，同时打开浏览器 `http://localhost:5173`。

脚本优先使用 WorkBuddy 管理的 node（`%USERPROFILE%\.workbuddy\binaries\node\...`），找不到时回退到系统 PATH 中的 `npm`。

## 组件结构

| 文件 | 说明 |
|---|---|
| `src/main.js` | 入口：注册 `createPinia()` 与 `router` |
| `src/App.vue` | 根组件：粒子 + 雾气全局背景 + `<router-view>` |
| `src/router/index.js` | 路由表（`/login`、`/`）与未登录拦截守卫 |
| `src/api/auth.js` | 后端接口层：`login` / `logout` / `register` / `getProfile`（fetch 封装，自动带 `Authorization: Bearer`；未配置地址时回退本地模拟） |
| `src/stores/auth.js` | Pinia 鉴权 store：`token` / `user` / async `login` / `logout` |
| `src/views/LoginView.vue` | 登录页：组合 `LoginVisual` + `LoginForm` |
| `src/views/RegisterView.vue` | 注册页：同风格入门表单，提交调 `auth.register` |
| `src/views/HomeView.vue` | 登录后首页（已入仙途 + 退归山林） |
| `src/components/QiParticles.vue` | 灵气粒子 canvas 动画（自动 resize、卸载时清理） |
| `src/components/LoginVisual.vue` | 左侧意境：品牌 / 标题 / 境界流转高亮 |
| `src/components/LoginForm.vue` | 右侧登录表单：账号密码、显隐切换、提交 loading、调 store 并跳转 |
| `src/components/Toast.vue` | 全局风格提示条（固定定位） |
| `src/styles/global.css` | `:root` 主题变量与基础重置 |

## 路由与鉴权

- 访问 `/` 需登录：`router.beforeEach` 检测 `localStorage.fx_token`，无则跳 `/login`。
- 注册页 `/register` 无需登录；登录页与注册页互相有跳转入口。
- 登录成功：`LoginForm` 调 `auth.login()` 写入 token，1.4s 模拟"渡劫"后 `router.push('/')`。
- "铭记道号"：token 存 `fx_token`、账号存 `fx_user`，下次进入登录页自动回填。
- 退出：`HomeView` 调 `auth.logout()` 清除后回 `/login`。

## 与原 HTML 的差异 / 改造点

- 三段原生 `<script>` 逻辑拆为多个 `<script setup>` 组件，状态用 `ref` 管理。
- 粒子动画、境界轮播、resize 监听均在 `onUnmounted` 中清理，避免内存泄漏。
- 表单交互（`v-model` + `@submit.prevent`）替代原生 DOM 事件绑定；登录流程接入 Pinia + 路由。
- CSS 按组件 `scoped` 拆分，全局变量保留在 `global.css`。

## 补充背景图（可选）

原页面引用了 `hanli.png`（韩立背景图），但源文件并未附带该图，原页面实际只显示渐变底色。
若你有这张图，放入 `public/hanli.png` 即可自动生效（Vite 会将其映射到 `/hanli.png`）。
没有图片也不影响运行，左侧会呈现深色渐变意境。

## 后端对接

登录逻辑已抽到 `src/api/auth.js`，通过 `import.meta.env.VITE_API_BASE` 读取后端地址：

- **未配置（默认）**：走本地模拟登录，开箱即跑，便于先调 UI。
- **接 Spring Boot**：在项目根新建 `.env`（可复制 `.env.example`），写入
  `VITE_API_BASE=http://localhost:8080`；`auth.login` 已改为 `async` 并统一处理错误，
  失败会以 toast 提示。路由守卫统一读取 `localStorage.fx_token`，把模拟 token 换成 JWT 即可。

### 附：可运行 mock 鉴权后端（server/）

本仓库内置一个 Node + Express 的最小鉴权后端，接口契约与前端完全一致，**无需 Spring Boot 即可跑通全栈闭环**：

```bash
cd server
npm install --registry=https://registry.npmmirror.com
node index.js        # 默认监听 http://localhost:8080
```

前端 `.env` 已设为 `VITE_API_BASE=http://localhost:8080`，启动 dev 后即可走通：
注册 → 登录（签发 token）→ 首页带 `Authorization: Bearer` 拉取资料 → 退出。

#### 接口契约（前端依赖）

| 方法 | 路径 | 入参 | 成功 | 失败 |
|---|---|---|---|---|
| POST | `/api/register` | `{ username, password }` | 200 `{ token, username }` | 409 已存在 |
| POST | `/api/login` | `{ username, password }` | 200 `{ token, username }` | 401 凭证错误 |
| GET | `/api/me` | Header `Authorization: Bearer <token>` | 200 `{ username, realm, level }` | 401 无/失效 token |
| POST | `/api/logout` | Header `Authorization: Bearer <token>` | 200 `{ ok:true }` | 401 无 token |

> 用 Spring Boot 替换时，只要暴露上面 4 个接口、返回相同结构即可，前端组件零改动。
> 后端为内存存储（重启清空），仅用于联调演示。

## 全栈一键运行

1. 先启动后端：`cd server && node index.js`（保持运行）
2. 再双击项目根 `start.bat`（自动装前端依赖 + 启 dev 服务器 + 开浏览器）
3. 浏览器打开 `http://localhost:5173` → 注册 → 登录 → 查看境界
