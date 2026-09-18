// 后端 API 基础地址：在 .env 中配置 VITE_API_BASE
// 例如接 Spring Boot： VITE_API_BASE=http://localhost:8080
// 未配置时走本地模拟，保证项目开箱即跑。
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

function delay(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

export async function login(username, password) {
  if (!BASE) {
    await delay(1400)
    return { token: 'fx_' + Math.random().toString(36).slice(2, 12), username }
  }
  return request('/api/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}

export async function register(username, password) {
  if (!BASE) {
    await delay(1400)
    return { ok: true }
  }
  return request('/api/register', { method: 'POST', body: JSON.stringify({ username, password }) })
}

export async function getProfile() {
  if (!BASE) {
    await delay(400)
    return { username: localStorage.getItem('fx_user') || '无名', realm: '炼气' }
  }
  return request('/api/me')
}

export async function logout() {
  if (!BASE) return
  return request('/api/logout', { method: 'POST' })
}
