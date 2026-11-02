// 好友系统 API（契约：后端 FriendController，均需 Header Authorization: Bearer <token>）
//   GET  /api/friend/search?kw=   -> { users:[{username,realm,totalLevel,spiritRoot,physique,online,relation}] }
//   POST /api/friend/add          {target} -> { message }
//   GET  /api/friend/list         -> { friends:[{username,realm,totalLevel,spiritRoot,physique,online}] }
//   GET  /api/friend/requests     -> { requests:[{from,realm,totalLevel,spiritRoot,physique}] }
//   POST /api/friend/respond      {from, accept} -> { message, status }
//   POST /api/friend/remove       {target} -> { message }
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

// 按名称模糊搜索用户（含 relation 标记）
export function searchFriends(kw) {
  return request('/api/friend/search?kw=' + encodeURIComponent(kw || ''))
}

// 发送好友请求（或对方已邀请则直接接受）
export function addFriend(target) {
  return request('/api/friend/add', {
    method: 'POST',
    body: JSON.stringify({ target })
  })
}

// 我的好友列表
export function listFriends() {
  return request('/api/friend/list')
}

// 收到的好友请求（轮询）
export function listRequests() {
  return request('/api/friend/requests')
}

// 同意/拒绝 收到的好友请求
export function respondRequest(from, accept) {
  return request('/api/friend/respond', {
    method: 'POST',
    body: JSON.stringify({ from, accept })
  })
}

// 删除好友
export function removeFriend(target) {
  return request('/api/friend/remove', {
    method: 'POST',
    body: JSON.stringify({ target })
  })
}
