// 藏经阁 · 功法秘籍接口
// 所有数据必须来自后端，禁止 mock。
//
// === 后端接口契约 ===
// GET /api/manuals?category=&realm=&grade=&q=
//   Header Authorization: Bearer <token>
//   成功 200：{ items: [{ id, name, category, grade, realm, desc, origin }] }
//     - 无参：返回全部秘籍
//     - q 非空：关键字搜索（名称/简介/分类/境界/出处），可叠加 分类/境界/品阶 筛选
//   GET /api/manuals/facets
//   成功 200：{ categories:[...], realms:[...], grades:[...] }（供筛选器使用）
//   GET /api/manuals/{id} -> 单卷详情
//   POST /api/manuals -> 新增；PUT /api/manuals/{id} -> 更新；DELETE /api/manuals/{id} -> 删除
const BASE = import.meta.env.VITE_API_BASE || ''

// 从本地读取 token，自动附加到请求头（与 api/auth.js 保持一致）
function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

// 拉取功法秘籍列表，返回 { items }
export async function fetchManuals(params = {}) {
  const usp = new URLSearchParams()
  if (params.category && params.category !== '全部') usp.set('category', params.category)
  if (params.realm && params.realm !== '全部') usp.set('realm', params.realm)
  if (params.grade && params.grade !== '全部') usp.set('grade', params.grade)
  if (params.q) usp.set('q', params.q)
  const res = await fetch(`${BASE}/api/manuals?${usp.toString()}`, {
    headers: { ...authHeader() }
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const data = await res.json()
  return { items: data.items || [] }
}

// 拉取筛选维度（分类/境界/品阶）
export async function fetchFacets() {
  const res = await fetch(`${BASE}/api/manuals/facets`, {
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

// 拉取单卷功法详情，返回 { manual }
export async function fetchManualDetail(id) {
  const res = await fetch(`${BASE}/api/manuals/${id}`, {
    headers: { ...authHeader() }
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const data = await res.json()
  return { manual: data }
}

// 新增功法秘籍：body { name, category, grade, realm, desc, origin, image }
export async function createManual(payload) {
  const res = await fetch(`${BASE}/api/manuals`, {
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

// 更新功法秘籍：patch 模式，仅覆盖传入字段，保留 id
export async function updateManual(id, payload) {
  const res = await fetch(`${BASE}/api/manuals/${id}`, {
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

// 删除功法秘籍
export async function deleteManual(id) {
  const res = await fetch(`${BASE}/api/manuals/${id}`, {
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

// 功法插画上传：multipart POST 到 /api/manuals/upload，返回 { url }
export async function uploadManualImage(file) {
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
