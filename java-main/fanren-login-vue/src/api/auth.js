// 后端 API 基础地址：在 .env 中配置 VITE_API_BASE
// 例如接 Spring Boot： VITE_API_BASE=http://localhost:8080
// 未配置时走本地模拟，保证项目开箱即跑。
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

function delay(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

// 本地模拟用的正确验证码（仅 mock 模式）
let mockCaptcha = ''

// 生成 4 位验证码（去除易混字符），绘制为带噪声的 base64 图片
function genMockCaptcha() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 4; i++) code += chars[Math.floor(Math.random() * chars.length)]
  mockCaptcha = code

  const canvas = document.createElement('canvas')
  canvas.width = 110
  canvas.height = 40
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = 'rgba(255,255,255,0.05)'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  // 干扰线
  for (let i = 0; i < 5; i++) {
    ctx.strokeStyle = `hsla(${Math.random() * 360}, 60%, 65%, 0.25)`
    ctx.beginPath()
    ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height)
    ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height)
    ctx.stroke()
  }
  // 字符
  for (let i = 0; i < code.length; i++) {
    ctx.save()
    ctx.translate(16 + i * 22, 26)
    ctx.rotate((Math.random() - 0.5) * 0.5)
    ctx.font = 'bold 24px serif'
    ctx.fillStyle = `hsl(${Math.random() * 360}, 65%, 70%)`
    ctx.fillText(code[i], 0, 0)
    ctx.restore()
  }
  // 干扰点
  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = `hsla(${Math.random() * 360}, 60%, 70%, 0.35)`
    ctx.fillRect(Math.random() * canvas.width, Math.random() * canvas.height, 2, 2)
  }
  return canvas.toDataURL('image/png')
}

// 获取验证码：真实后端返回 { captchaId, image }，mock 本地生成
export async function getCaptcha() {
  if (!BASE) {
    await delay(300)
    return { captchaId: 'mock', image: genMockCaptcha() }
  }
  return request('/api/captcha')
}

export async function login(username, password) {
  if (!BASE) {
    await delay(1400)
    return { token: 'fx_' + Math.random().toString(36).slice(2, 12), username, permission: 1 }
  }
  return request('/api/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}

// 后台管理员登录：仅 permission=0 可成功，否则后端返回 403
export async function adminLogin(username, password) {
  if (!BASE) {
    await delay(1400)
    // 本地模拟：仅 admin / admin123 视为管理员
    if (username === 'admin' && password === 'admin123') {
      return { token: 'fx_' + Math.random().toString(36).slice(2, 12), username, permission: 0 }
    }
    throw new Error('权限不足，仅管理员可登录后台')
  }
  return request('/api/admin/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}

// 注册：新增密码长度校验（5–15）与验证码校验；真实后端走 /api/register
// profile 为人物初始化资料：{ gender, age, spiritRoot, physique, avatar, initialTreasure, initialGongfa }
export async function register(username, password, captchaId, captcha, profile = {}) {
  if (!BASE) {
    await delay(1400)
    if (!username || !username.trim()) throw new Error('请填写道号')
    if (!password || password.length < 5 || password.length > 15) {
      throw new Error('法诀长度需为 5–15 位')
    }
    if (!captcha || captcha.toUpperCase() !== (mockCaptcha || '').toUpperCase()) {
      throw new Error('御阵符有误，请重新输入')
    }
    return { ok: true }
  }
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
