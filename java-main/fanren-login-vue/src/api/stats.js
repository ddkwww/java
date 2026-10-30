// 数值系统 API：角色五维属性面板 + 境界提升 + 经济系统
// 所有数据必须来自后端，禁止 mock。
// 契约（后端 StatsController）：
//   GET  /api/stats/me            Header Authorization: Bearer <token>
//       -> { username, physique, spiritRoot, manual, treasure,
//            fullRealm, totalLevel, realm, realmLevel, subLevel, stage, lifespanCap,
//            spiritStone, spiritQi, age, ageUsed, remainingLifespan,
//            breakthroughCost, breakthroughQiCost, breakthroughDayCost,
//            total:{hp,lifespan,atk,def,spd}, breakdown:{realm,spiritRoot,physique,manual,treasure} }
//   POST /api/stats/breakthrough  Header Authorization: Bearer <token>
//       -> 同上，并附带 levelUp:true（灵石不足/灵气不足/寿元不足/渡劫圆满返回 400）
// 经济系统（EconomyController）：
//   POST /api/economy/buy/manual/{id}    -> 扣除灵石并装备功法
//   POST /api/economy/buy/treasure/{id}  -> 扣除灵石并装备法宝
//   POST /api/economy/earn/meditate      -> 打坐修炼（+灵气）
//   POST /api/economy/earn/hunt          -> 斩妖除魔（+灵气 +灵石）
//   POST /api/economy/earn/mine          -> 深入矿脉（+灵石）
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

// 查询当前角色属性面板
export async function getStats() {
  return request('/api/stats/me')
}

// 提升一个小境界（消耗灵石 + 灵气 + 天数）
export async function breakthrough() {
  return request('/api/stats/breakthrough', { method: 'POST' })
}

// 购买并装备功法
export async function buyManual(id) {
  return request(`/api/economy/buy/manual/${id}`, { method: 'POST' })
}

// 购买并装备法宝
export async function buyTreasure(id) {
  return request(`/api/economy/buy/treasure/${id}`, { method: 'POST' })
}

// 打坐修炼：+灵气，消耗 15 天，冷却 60 秒
export async function earnMeditate() {
  return request('/api/economy/earn/meditate', { method: 'POST' })
}

// 斩妖除魔：+灵气 +灵石，消耗 60 天（2 个月），冷却 5 分钟
export async function earnHunt() {
  return request('/api/economy/earn/hunt', { method: 'POST' })
}

// 挖矿：+灵石，消耗 30 天（1 个月），冷却 5 分钟
export async function earnMine() {
  return request('/api/economy/earn/mine', { method: 'POST' })
}
