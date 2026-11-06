// 好友聊天 API（契约：后端 ChatController，均需 Header Authorization: Bearer <token>）
//   POST /api/chat/send     {to, content} -> { from, to, content, time }
//   GET  /api/chat/history?peer=   -> { messages:[{from,to,content,time,mine,type,stone}] }
//   GET  /api/chat/unread          -> { counts:{friend: n}, total }
//   POST /api/chat/read     {peer} -> { message }
//   POST /api/chat/gift     {to, amount} -> { message, spiritStone, to, amount }
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

// 发送消息
export function sendMessage(to, content) {
  return request('/api/chat/send', {
    method: 'POST',
    body: JSON.stringify({ to, content })
  })
}

// 与某好友的历史消息
export function getHistory(peer) {
  return request('/api/chat/history?peer=' + encodeURIComponent(peer || ''))
}

// 未读消息数（每个好友 + 总数）
export function getUnread() {
  return request('/api/chat/unread')
}

// 标记与某好友会话已读
export function markRead(peer) {
  return request('/api/chat/read', {
    method: 'POST',
    body: JSON.stringify({ peer })
  })
}

// 向好友赠送灵石（扣己加彼，留存 GIFT 消息）
export function giftStone(to, amount) {
  return request('/api/chat/gift', {
    method: 'POST',
    body: JSON.stringify({ to, amount })
  })
}
