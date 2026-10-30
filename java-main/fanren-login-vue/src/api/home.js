// 灵境阁 · 首页数据接口
// 所有数据必须来自后端，禁止 mock。
//
// === 后端接口契约 ===
// GET /api/search?q=<关键词>
//   Header Authorization: Bearer <token>
//   成功 200：{ items: [{ id, name, type, grade, desc }] }
//     - id:    条目唯一标识
//     - name:  名称（功法 / 法宝 / 丹药 / 灵草）
//     - type:  分类（功法 | 法宝 | 丹药 | 灵草）
//     - grade: 品阶（天 | 地 | 玄 | 黄）
//     - desc:  一句话简介
//   q 为空时返回推荐列表
const BASE = import.meta.env.VITE_API_BASE || ''

// 从本地读取 token，自动附加到请求头（与 api/auth.js 保持一致）
function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

// 万象搜索，返回 { items }
export async function searchAll(q) {
  const res = await fetch(`${BASE}/api/search?q=${encodeURIComponent(q || '')}`, {
    headers: { ...authHeader() }
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const data = await res.json()
  return { items: data.items || [] }
}
