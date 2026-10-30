// 门派 · 修仙势力接口
// 所有数据必须来自后端，禁止 mock。
//
// === 后端接口契约 ===
// GET /api/sects?camp=&q=
//   Header Authorization: Bearer <token>
//   成功 200：{ items: [{ id, name, camp, region, desc, signature, representative, image }] }
//     - 无参：返回全部门派
//     - q 非空：关键字搜索（名称/阵营/地域/简介/镇派功法/代表人物）
//     - camp 非空：按阵营筛选
//   GET /api/sects/facets -> { camps:[...] }
//   GET /api/sects/{id} -> 单派详情（含 places 三场所 + joined 加入状态 + sectRank/月俸）
//   GET /api/sects/my -> { joined, sectName, sectRank, stoneSalary, qiSalary, salaryCd, places, ... }
//   POST /api/sects/join/{id} -> 加入门派
//   POST /api/sects/leave -> 退出门派
//   POST /api/sects/salary -> 领取月俸
const BASE = import.meta.env.VITE_API_BASE || ''

function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

async function request(url, options = {}) {
  const res = await fetch(BASE + url, {
    headers: { 'Content-Type': 'application/json', ...authHeader() },
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

// 拉取门派列表，返回 { items }
export async function fetchSects(params = {}) {
  const usp = new URLSearchParams()
  if (params.camp && params.camp !== '全部') usp.set('camp', params.camp)
  if (params.q) usp.set('q', params.q)
  const res = await fetch(`${BASE}/api/sects?${usp.toString()}`, {
    headers: { ...authHeader() }
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const data = await res.json()
  return { items: data.items || [] }
}

// 拉取筛选维度（阵营）
export async function fetchSectFacets() {
  const res = await fetch(`${BASE}/api/sects/facets`, {
    headers: { ...authHeader() }
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const data = await res.json()
  return { camps: data.camps || [] }
}

// 拉取单派详情，返回 { sect }
export async function fetchSectDetail(id) {
  return request(`/api/sects/${id}`)
}

// 查询当前用户门派信息
export async function fetchMySect() {
  return request('/api/sects/my')
}

// 加入门派
export async function joinSect(id) {
  return request(`/api/sects/join/${id}`, { method: 'POST' })
}

// 退出门派
export async function leaveSect() {
  return request('/api/sects/leave', { method: 'POST' })
}

// 领取月俸
export async function claimSalary() {
  return request('/api/sects/salary', { method: 'POST' })
}
