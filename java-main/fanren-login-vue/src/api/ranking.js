// 排行榜 API（天骄榜）
// 契约（后端 RankingController）：
//   GET  /api/ranking/tianjiao   Header Authorization: Bearer <token>
//       -> [ { rank, username, realm, totalLevel } ]  （按 totalLevel 降序，前 10 名）
const BASE = import.meta.env.VITE_API_BASE || ''

function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

export async function getTianjiao() {
  const res = await fetch(BASE + '/api/ranking/tianjiao', {
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    credentials: 'include'
  })
  if (!res.ok) {
    let msg = '获取天骄榜失败'
    try {
      const data = await res.json()
      msg = data.message || msg
    } catch (_) { /* ignore */ }
    throw new Error(msg)
  }
  return res.json()
}
