// 后端 API 基础地址：在 .env 中配置 VITE_API_BASE
// 例如接 Spring Boot： VITE_API_BASE=http://localhost:8081
// 所有数据必须来自后端，禁止 mock。
//
// === 接口契约（前端依赖，后端按此实现即可，组件零改动）===
// GET  /api/captcha   无参；成功 200 { captchaId, image }（image 为 base64 PNG）
// POST /api/register  body: { username, password, captchaId, captcha }
//                      成功 200 { token, username }；失败 409 已存在 / 400 验证码错误或密码不符合规范
//                      密码长度（5–15 位）由后端强制校验
// POST /api/login     body: { username, password } → 200 { token, username, permission }
// POST /api/admin/login body: { username, password } → 仅 permission=0 可成功，否则 403
// GET  /api/me        Header Authorization: Bearer <token> → 200 { username, realm, level, permission }
// POST /api/logout    Header Authorization: Bearer <token> → 200 { ok:true }
const BASE = import.meta.env.VITE_API_BASE || ''

// 从本地读取 token，自动附加到请求头（登录后访问受保护接口必需）
function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

async function request(url, options = {}) {
  const res = await fetch(BASE + url, {
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    credentials: 'include',
    ...options
  })
  if (!res.ok) {
    let msg = '请求失败'
    try {
      const data = await res.json()
      msg = data.message || data.error || msg
    } catch (_) { /* ignore */ }
    throw new Error(msg)
  }
  return res.json()
}

// 获取验证码
export async function getCaptcha() {
  return request('/api/captcha')
}

export async function login(username, password) {
  return request('/api/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}

// 后台管理员登录：仅 permission=0 可成功，否则后端返回 403
export async function adminLogin(username, password) {
  return request('/api/admin/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}

// 注册：profile 为人物初始化资料：{ gender, age, spiritRoot, physique, avatar, initialTreasure, initialGongfa }
export async function register(username, password, captchaId, captcha, profile = {}) {
  return request('/api/register', {
    method: 'POST',
    body: JSON.stringify({
      username,
      password,
      captchaId,
      captcha,
      gender: profile.gender,
      age: profile.age,
      spiritRoot: profile.spiritRoot,
      physique: profile.physique,
      avatar: profile.avatar,
      initialTreasure: profile.initialTreasure,
      initialGongfa: profile.initialGongfa
    })
  })
}

export async function getProfile() {
  return request('/api/me')
}

export async function logout() {
  return request('/api/logout', { method: 'POST' })
}
