// 对战系统 API：在线用户、人物详情、发起/接收/响应/状态
// 契约（后端 DuelController，均需 Header Authorization: Bearer <token>）：
//   GET  /api/duel/online          -> { users:[{username,realm,totalLevel,spiritRoot,physique}] }
//   GET  /api/duel/profile?user=    -> 完整属性面板（同 /api/stats/me 结构）
//   POST /api/duel/challenge       {target} -> {duelId, status}
//   GET  /api/duel/pending         -> { duel:{duelId,from,fromRealm,fromTotalLevel,fromSpiritRoot,status} | null }
//   POST /api/duel/respond         {duelId, accept} -> {status, winner?}
//   GET  /api/duel/status?duelId=  -> {status, from, to, winner?}
const BASE = import.meta.env.VITE_API_BASE || ''

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

// 当前在线用户（排除自己）
export function getOnline() {
  return request('/api/duel/online')
}

// 心跳：刷新自身在线时间戳（全局定时器调用）
export function heartbeat() {
  return request('/api/duel/heartbeat')
}

// 某人完整属性面板（详情抽屉）
export function getProfile(username) {
  return request('/api/duel/profile?user=' + encodeURIComponent(username))
}

// 向某用户发起对战，返回 duelId
export function challenge(target) {
  return request('/api/duel/challenge', {
    method: 'POST',
    body: JSON.stringify({ target })
  })
}

// 被挑战方轮询：待处理请求
export function getPending() {
  return request('/api/duel/pending')
}

// 被挑战方响应：同意(accept=true) 或 拒绝(accept=false)
export function respond(duelId, accept) {
  return request('/api/duel/respond', {
    method: 'POST',
    body: JSON.stringify({ duelId, accept })
  })
}

// 挑战方轮询：对战状态
export function getStatus(duelId) {
  return request('/api/duel/status?duelId=' + encodeURIComponent(duelId))
}
