// 数值系统 API：角色五维属性面板 + 境界提升 + 经济系统
// 契约（后端 StatsController）：
//   GET  /api/stats/me            Header Authorization: Bearer <token>
//       -> { username, physique, spiritRoot, manual, treasure,
//            fullRealm, totalLevel, realm, realmLevel, subLevel, stage, lifespanCap,
//            spiritStone, breakthroughCost,
//            total:{hp,lifespan,atk,def,spd}, breakdown:{realm,spiritRoot,physique,manual,treasure} }
//   POST /api/stats/breakthrough  Header Authorization: Bearer <token>
//       -> 同上，并附带 levelUp:true（灵石不足/渡劫圆满返回 400）
// 经济系统（EconomyController）：
//   POST /api/economy/buy/manual/{id}    -> 扣除灵石并装备功法
//   POST /api/economy/buy/treasure/{id}  -> 扣除灵石并装备法宝
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
  if (!BASE) return mockStats()
  return request('/api/stats/me')
}

// 提升一个小境界（消耗灵石）
export async function breakthrough() {
  if (!BASE) return mockBreakthrough()
  return request('/api/stats/breakthrough', { method: 'POST' })
}

// 购买并装备功法
export async function buyManual(id) {
  if (!BASE) return { message: '演示模式：购买成功', spiritStone: 1000 }
  return request(`/api/economy/buy/manual/${id}`, { method: 'POST' })
}

// 购买并装备法宝
export async function buyTreasure(id) {
  if (!BASE) return { message: '演示模式：购买成功', spiritStone: 1000 }
  return request(`/api/economy/buy/treasure/${id}`, { method: 'POST' })
}

// 打坐修炼：+灵气，消耗 15 天，冷却 60 秒
export async function earnMeditate() {
  if (!BASE) return { message: '演示模式：打坐获得 20 灵气', qiReward: 20, spiritQi: 120, dayCost: 15, ageUsed: 15, remainingLifespan: 100, cooldown: 60 }
  return request('/api/economy/earn/meditate', { method: 'POST' })
}

// 斩妖除魔：+灵气 +灵石，消耗 60 天（2 个月），冷却 5 分钟
export async function earnHunt() {
  if (!BASE) return { message: '演示模式：除妖获得 30 灵气、100 灵石', qiReward: 30, stoneReward: 100, spiritQi: 130, spiritStone: 1100, dayCost: 60, ageUsed: 60, remainingLifespan: 100, cooldown: 300 }
  return request('/api/economy/earn/hunt', { method: 'POST' })
}

// 挖矿：+灵石，消耗 30 天（1 个月），冷却 5 分钟
export async function earnMine() {
  if (!BASE) return { message: '演示模式：挖矿获得 150 灵石', stoneReward: 150, spiritStone: 1150, dayCost: 30, ageUsed: 30, remainingLifespan: 100, cooldown: 300 }
  return request('/api/economy/earn/mine', { method: 'POST' })
}

// ===== 本地 mock（未配置后端时兜底展示，便于开箱即跑） =====
function mockStats() {
  return {
    username: localStorage.getItem('fx_user') || '无名',
    physique: '先天道体',
    spiritRoot: '金',
    manual: '长春功',
    treasure: '青竹蜂云剑',
    fullRealm: '练气期 初期 一层',
    totalLevel: 1,
    realm: '练气期',
    realmLevel: 1,
    subLevel: 1,
    stage: '初期',
    lifespanCap: 100,
    spiritStone: 1000,
    breakthroughCost: 20,
    total: { hp: 150, lifespan: 100, atk: 28, def: 23, spd: 15 },
    breakdown: {
      realm: { hp: 150, lifespan: 100, atk: 18, def: 18, spd: 15 },
      spiritRoot: { hp: 0, lifespan: 0, atk: 10, def: 5, spd: 0 },
      physique: { hp: 0, lifespan: 0, atk: 0, def: 0, spd: 0 },
      manual: { hp: 0, lifespan: 0, atk: 0, def: 0, spd: 0 },
      treasure: { hp: 0, lifespan: 0, atk: 0, def: 0, spd: 0 }
    }
  }
}

function mockBreakthrough() {
  return { ...mockStats(), levelUp: true }
}
