// 法宝阁 · 法宝接口
// 所有数据必须来自后端，禁止 mock。
//
// === 后端接口契约 ===
// GET /api/treasures?category=&realm=&grade=&q=
//   Header Authorization: Bearer <token>
//   成功 200：{ items: [{ id, name, category, grade, realm, desc, origin, image }] }
//     - 无参：返回全部法宝
//     - q 非空：关键字搜索（名称/简介/类别/境界/出处），可叠加 类别/境界/品阶 筛选
//   GET /api/treasures/facets
//   成功 200：{ categories:[...], realms:[...], grades:[...] }（供筛选器使用）
//   GET /api/treasures/{id} -> 单件详情；POST /api/treasures -> 新增；
//   PUT /api/treasures/{id} -> 更新；DELETE /api/treasures/{id} -> 删除
const BASE = import.meta.env.VITE_API_BASE || ''

// 从本地读取 token，自动附加到请求头（与 api/auth.js 保持一致）
function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

// 拉取法宝列表，返回 { items }
export async function fetchTreasures(params = {}) {
  const usp = new URLSearchParams()
  if (params.category && params.category !== '全部') usp.set('category', params.category)
  if (params.realm && params.realm !== '全部') usp.set('realm', params.realm)
  if (params.grade && params.grade !== '全部') usp.set('grade', params.grade)
  if (params.q) usp.set('q', params.q)
  const res = await fetch(`${BASE}/api/treasures?${usp.toString()}`, {
    headers: { ...authHeader() }
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const data = await res.json()
  return { items: data.items || [] }
}

// 拉取筛选维度（类别/境界/品阶）
export async function fetchTreasureFacets() {
  const res = await fetch(`${BASE}/api/treasures/facets`, {
    headers: { ...authHeader() }
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const data = await res.json()
  return {
    categories: data.categories || [],
    realms: data.realms || [],
    grades: data.grades || []
  }
}

// 拉取单件法宝详情，返回 { treasure }
export async function fetchTreasureDetail(id) {
  const res = await fetch(`${BASE}/api/treasures/${id}`, {
    headers: { ...authHeader() }
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const data = await res.json()
  return { treasure: data }
}

// 新增法宝：body { name, category, grade, realm, desc, origin, image }
export async function createTreasure(payload) {
  const res = await fetch(`${BASE}/api/treasures`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: JSON.stringify(payload)
  })
  if (!res.ok) {
    let msg = '创建失败'
    try { msg = (await res.json()).message || msg } catch (_) {}
    throw new Error(msg)
  }
  return res.json()
}

// 更新法宝：patch 模式，仅覆盖传入字段，保留 id
export async function updateTreasure(id, payload) {
  const res = await fetch(`${BASE}/api/treasures/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: JSON.stringify(payload)
  })
  if (!res.ok) {
    let msg = '更新失败'
    try { msg = (await res.json()).message || msg } catch (_) {}
    throw new Error(msg)
  }
  return res.json()
}

// 删除法宝
export async function deleteTreasure(id) {
  const res = await fetch(`${BASE}/api/treasures/${id}`, {
    method: 'DELETE',
    headers: { ...authHeader() }
  })
  if (!res.ok) {
    let msg = '删除失败'
    try { msg = (await res.json()).message || msg } catch (_) {}
    throw new Error(msg)
  }
  return { ok: true }
}

// 法宝插画上传：复用功法插画上传接口 multipart POST 到 /api/manuals/upload，返回 { url }
export async function uploadTreasureImage(file) {
  const fd = new FormData()
  fd.append('file', file)
  const res = await fetch(`${BASE}/api/manuals/upload`, {
    method: 'POST',
    headers: { ...authHeader() },
    body: fd
  })
  if (!res.ok) {
    let msg = '图片上传失败'
    try { msg = (await res.json()).message || msg } catch (_) {}
    throw new Error(msg)
  }
  const data = await res.json()
  return { url: data.url }
}
