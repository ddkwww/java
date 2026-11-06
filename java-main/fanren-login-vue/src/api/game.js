// 小游戏接口（大富翁·灵石夺宝）
// 所有数据来自后端，禁止 mock。
//
// === 后端接口契约 ===
// GET  /api/game/monopoly/state                -> 牌桌快照
// POST /api/game/monopoly/join                 -> 报名（扣 ENTRY_FEE 灵石进奖池）
// POST /api/game/monopoly/leave                -> 退出（退回灵石，仅等待阶段）
// POST /api/game/monopoly/start                -> 满 4 人开局
// POST /api/game/monopoly/roll                 -> 当前玩家掷骰
// POST /api/game/monopoly/reset                -> 结算后重置牌桌
const BASE = import.meta.env.VITE_API_BASE || ''

function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

// 拉取牌桌状态（轮询）
export async function fetchGameState() {
  const res = await fetch(`${BASE}/api/game/monopoly/state`, { headers: { ...authHeader() } })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  return res.json()
}

// 报名 / 退出 / 开局 / 掷骰 / 重置：统一 POST 空体
async function post(action) {
  const res = await fetch(`${BASE}/api/game/monopoly/${action}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader() }
  })
  if (!res.ok) {
    let msg = '操作失败'
    try { msg = (await res.json()).message || msg } catch (_) {}
    throw new Error(msg)
  }
  return res.json()
}

export const joinGame = () => post('join')
export const leaveGame = () => post('leave')
export const startGame = () => post('start')
export const rollDice = () => post('roll')
export const resetGame = () => post('reset')

// 占领/升级选择：action = 'occupy' | 'skip' | 'upgrade'
export async function decideGame(action) {
  const res = await fetch(`${BASE}/api/game/monopoly/decide`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: JSON.stringify({ action })
  })
  if (!res.ok) {
    let msg = '操作失败'
    try { msg = (await res.json()).message || msg } catch (_) {}
    throw new Error(msg)
  }
  return res.json()
}

// 发起结束投票
export const proposeEndGame = () => post('propose-end')

// 结束投票表态：agree = true 赞成 / false 反对
export async function voteEndGame(agree) {
  const res = await fetch(`${BASE}/api/game/monopoly/vote-end`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: JSON.stringify({ agree })
  })
  if (!res.ok) {
    let msg = '操作失败'
    try { msg = (await res.json()).message || msg } catch (_) {}
    throw new Error(msg)
  }
  return res.json()
}
