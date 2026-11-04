// 拍卖行接口
// 所有数据必须来自后端，禁止 mock。
//
// === 后端接口契约 ===
// GET /api/auction/status
//   Header Authorization: Bearer <token>
//   成功 200：{ state, nextOpenAt, openCount, yourStone,
//               currentLot:{ type,id,name,category,grade,realm,image,price,desc,origin,
//                            bonusHp..bonusLifespan, currentPrice, highestBidder, lotEndsAt, bidLog[] },
//               queue:[...], remaining, lastResult:{lotName,winner,price,success,passed,seq}|null }
// POST /api/auction/bid
//   Header Authorization: Bearer <token>
//   body { type:"manual"|"treasure", id:Long, amount:Int }
//   成功 200：返回最新 status 快照；失败 400（非当前拍品/加价档位非法/已是最高出价）
const BASE = import.meta.env.VITE_API_BASE || ''

function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

// 拉取拍卖行当前状态（轮询用）
export async function fetchAuctionStatus() {
  const res = await fetch(`${BASE}/api/auction/status`, { headers: { ...authHeader() } })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  return res.json()
}

// 出价：type + id + 目标总价 amount（当前价 + 加价档位）
export async function placeBid(type, id, amount) {
  const res = await fetch(`${BASE}/api/auction/bid`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: JSON.stringify({ type, id, amount })
  })
  if (!res.ok) {
    let msg = '出价失败'
    try { msg = (await res.json()).message || msg } catch (_) {}
    throw new Error(msg)
  }
  return res.json()
}

// 报名 / 取消参与下一场拍卖：join=true 报名，false 取消
// 返回 { participated, participantCount }
export async function participate(join) {
  const res = await fetch(`${BASE}/api/auction/participate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: JSON.stringify({ join })
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  return res.json()
}
