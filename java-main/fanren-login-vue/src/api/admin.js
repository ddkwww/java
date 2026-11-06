// 后台管理系统 · 用户管理接口
// === 后端接口契约（AdminController）===
// GET  /api/admin/users?keyword=&page=&size=
//   Header Authorization: Bearer <token>（仅管理员 permission=0）
//   成功 200：{ items:[{ id,username,permission,gender,age,spiritRoot,physique,realm,
//                         totalLevel,spiritStone,spiritQi,ageUsed,lifespan,sect,sectRank,createdAt }],
//                total, page, size, pages }
// POST /api/admin/users/{id}  body 部分字段（permission/gender/age/spiritRoot/physique/realm/
//                                totalLevel/spiritStone/spiritQi/lifespan/sect/sectRank/password）
//   成功 200：{ message, user }
const BASE = import.meta.env.VITE_API_BASE || ''

function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

// 分页列出用户（支持按用户名搜索）
export async function fetchUsers(params = {}) {
  const usp = new URLSearchParams()
  if (params.keyword) usp.set('keyword', params.keyword)
  if (params.page) usp.set('page', String(params.page))
  if (params.size) usp.set('size', String(params.size))
  const res = await fetch(`${BASE}/api/admin/users?${usp.toString()}`, {
    headers: { ...authHeader() }
  })
  if (!res.ok) {
    let msg = '读取失败'
    try { msg = (await res.json()).message || msg } catch (_) {}
    throw new Error(msg)
  }
  const data = await res.json()
  return {
    items: data.items || [],
    total: data.total || 0,
    page: data.page || 1,
    pages: data.pages || 1
  }
}

// 修改指定用户（部分字段覆盖）
export async function updateUser(id, payload) {
  const res = await fetch(`${BASE}/api/admin/users/${id}`, {
    method: 'POST',
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
