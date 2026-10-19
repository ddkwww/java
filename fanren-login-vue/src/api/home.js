// 灵境阁 · 首页数据接口
//
// === 后端接口契约（后端实现后自动启用，前端零改动）===
// GET /api/search?q=<关键词>
//   Header Authorization: Bearer <token>
//   成功 200：{ items: [{ id, name, type, grade, desc }] }
//     - id:    条目唯一标识
//     - name:  名称（功法 / 法宝 / 丹药 / 灵草）
//     - type:  分类（功法 | 法宝 | 丹药 | 灵草）
//     - grade: 品阶（天 | 地 | 玄 | 黄）
//     - desc:  一句话简介
//   q 为空时返回推荐列表
//   失败 401 未登录（token 失效）；5xx 服务异常（前端降级为本地藏经）
const BASE = import.meta.env.VITE_API_BASE || ''

// 从本地读取 token，自动附加到请求头（与 api/auth.js 保持一致）
function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

// ------- 本地藏经（mock 数据：后端未实现 / 未启动时使用）-------
const MOCK_ITEMS = [
  { id: 1,  name: '青元剑诀', type: '功法', grade: '玄', desc: '青元观镇派剑法，剑气如虹，御敌于百步之外。' },
  { id: 2,  name: '大衍诀',   type: '功法', grade: '地', desc: '上古辅修心法，神识暴涨，参悟功法事半功倍。' },
  { id: 3,  name: '象甲功',   type: '功法', grade: '黄', desc: '土属性炼体功法，皮糙肉厚，寻常刀剑难伤。' },
  { id: 4,  name: '罗烟步',   type: '功法', grade: '黄', desc: '身法轻灵，踏烟而行，逃命追敌两相宜。' },
  { id: 5,  name: '婴变之术', type: '功法', grade: '天', desc: '传说中的夺舍秘法，因果深重，慎之慎之。' },
  { id: 6,  name: '青竹蜂云剑', type: '法宝', grade: '地', desc: '七十二柄青竹剑所化剑阵，群攻无双。' },
  { id: 7,  name: '风雷翅',   type: '法宝', grade: '天', desc: '风雷之力凝于一翅，振翅千里，遁速绝伦。' },
  { id: 8,  name: '玄天斩灵剑', type: '法宝', grade: '天', desc: '斩灵断魂，剑出必见血，魔道至宝。' },
  { id: 9,  name: '聚宝盆',   type: '法宝', grade: '玄', desc: '纳灵聚宝，置入灵石可缓慢滋生利息。' },
  { id: 10, name: '筑基丹',   type: '丹药', grade: '玄', desc: '炼气修士梦寐以求，破境筑基的至要之物。' },
  { id: 11, name: '凝魂丹',   type: '丹药', grade: '地', desc: '稳固神魂，凝练神识，结丹期前不可错过。' },
  { id: 12, name: '黄龙丹',   type: '丹药', grade: '黄', desc: '疗伤圣药，气血双补，行走江湖常备。' },
  { id: 13, name: '千年灵乳', type: '灵草', grade: '地', desc: '钟天地灵气而生，服用可增数十年修为。' },
  { id: 14, name: '紫猴花',   type: '灵草', grade: '玄', desc: '炼制筑基丹的主药，生于极寒之巅。' },
  { id: 15, name: '金髓芝',   type: '灵草', grade: '黄', desc: '年份不足的灵芝，药力平和，宜于入门。' },
  { id: 16, name: '五色灵乳', type: '灵草', grade: '天', desc: '传说之物，五行俱全，可遇而不可求。' }
]

function mockSearch(q) {
  const key = (q || '').trim().toLowerCase()
  if (!key) return MOCK_ITEMS.slice(0, 8)
  return MOCK_ITEMS.filter(
    (i) =>
      i.name.toLowerCase().includes(key) ||
      i.type.includes(key) ||
      i.grade.includes(key) ||
      i.desc.includes(key)
  )
}

// 万象搜索：未配置后端 → 本地模式；已配置 → 优先真实接口，
// 失败（后端未启动/未实现）→ 降级本地藏经，保证页面始终可用。
// 返回 { items, source }：source = 'remote' | 'local' | 'fallback'
export async function searchAll(q) {
  if (!BASE) {
    return { items: mockSearch(q), source: 'local' }
  }
  try {
    const res = await fetch(`${BASE}/api/search?q=${encodeURIComponent(q || '')}`, {
      headers: { ...authHeader() }
    })
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const data = await res.json()
    return { items: data.items || [], source: 'remote' }
  } catch (_) {
    return { items: mockSearch(q), source: 'fallback' }
  }
}
