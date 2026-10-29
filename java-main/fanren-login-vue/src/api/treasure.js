// 法宝阁 · 法宝接口
//
// === 后端接口契约（后端实现后自动启用，前端零改动）===
// GET /api/treasures?category=&realm=&grade=&q=
//   Header Authorization: Bearer <token>
//   成功 200：{ items: [{ id, name, category, grade, realm, desc, origin, image }] }
//     - 无参：返回全部法宝
//     - q 非空：关键字搜索（名称/简介/类别/境界/出处），可叠加 类别/境界/品阶 筛选
//   GET /api/treasures/facets
//   成功 200：{ categories:[...], realms:[...], grades:[...] }（供筛选器使用）
//   GET /api/treasures/{id} -> 单件详情；POST /api/treasures -> 新增；
//   PUT /api/treasures/{id} -> 更新；DELETE /api/treasures/{id} -> 删除
// 后端未实现 / 未启动 → 降级本地藏经（mock），保证页面始终可用。
const BASE = import.meta.env.VITE_API_BASE || ''

// 从本地读取 token，自动附加到请求头（与 api/auth.js 保持一致）
function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

// ------- 本地藏经（mock：与后端种子数据一致，后端未连时使用）-------
const MOCK_TREASURES = [
  { id: 1,  name: '青竹蜂云剑', category: '飞剑', grade: '地', realm: '金丹期', desc: '韩立本命飞剑，七十二口青竹小剑合一化云，剑随心动，群攻无双。', origin: '韩立' },
  { id: 2,  name: '风雷翅',     category: '遁法', grade: '天', realm: '元婴期', desc: '以风雷之力凝成双翅，振翅瞬息千里，遁速冠绝同阶。', origin: '韩立' },
  { id: 3,  name: '玄天斩灵剑', category: '飞剑', grade: '天', realm: '炼虚期', desc: '玄天之宝所化剑器，斩灵断魂，无物不破，魔道至宝。', origin: '玄天' },
  { id: 4,  name: '聚宝盆',     category: '奇物', grade: '玄', realm: '练气期', desc: '纳灵聚宝，置入灵石可缓慢滋生利息，散修攒家底之宝。', origin: '散修' },
  { id: 5,  name: '虚天鼎',     category: '奇物', grade: '天', realm: '化神期', desc: '虚天殿秘宝，内蕴须弥空间，曾封印古魔，威能莫测。', origin: '虚天殿' },
  { id: 6,  name: '乾蓝冰焰',   category: '攻伐', grade: '地', realm: '元婴期', desc: '极寒本源灵焰，触物成冰，焚敌于无形。', origin: '极寒之地' },
  { id: 7,  name: '风雷旗',     category: '攻伐', grade: '玄', realm: '金丹期', desc: '引动风雷的阵旗，布下可困敌削肉，声势骇人。', origin: '阵法' },
  { id: 8,  name: '圭灵盾',     category: '护身', grade: '地', realm: '元婴期', desc: '上古大能圭灵所遗护身盾，水火不侵，硬抗法宝。', origin: '圭灵' },
  { id: 9,  name: '七焰扇',     category: '攻伐', grade: '天', realm: '化神期', desc: '融七种灵焰于一扇，扇出焚天之火，威能惊世。', origin: '火道' },
  { id: 10, name: '墨蛟剑',     category: '飞剑', grade: '玄', realm: '筑基期', desc: '以墨蛟之骨炼制的飞剑，剑出带腥风，凌厉非常。', origin: '炼器' },
  { id: 11, name: '五行环',     category: '奇物', grade: '地', realm: '金丹期', desc: '金木水火土五环相生，可攻可守，变化无穷。', origin: '五行' },
  { id: 12, name: '天罗真甲',   category: '护身', grade: '天', realm: '炼虚期', desc: '天罗秘银织就的真甲，灵力流转间化解诸般攻伐。', origin: '天罗' },
  { id: 13, name: '雷珠',       category: '攻伐', grade: '玄', realm: '筑基期', desc: '封印雷力于宝珠，掷出引动天雷，一次性重创强敌。', origin: '雷道' },
  { id: 14, name: '黑风旗',     category: '遁法', grade: '黄', realm: '练气期', desc: '卷起黑风护身遁逃，价廉物美，江湖常备。', origin: '散修' },
  { id: 15, name: '纳物袋',     category: '储物', grade: '黄', realm: '练气期', desc: '须弥芥子之术，方寸之间藏百物，出行必备。', origin: '通用' },
  { id: 16, name: '化龙玺',     category: '奇物', grade: '天', realm: '合体期', desc: '上古龙族传承之玺，镇运纳福，亦可引动龙威压敌。', origin: '龙族' }
]

// 演示用：标注部分法宝为“今日新增”，使首页“法宝”区块有内容；
// 真实后端由实体 created_at 字段自动带入，此处仅对本地 mock 生效。
const TODAY_ISO = new Date().toISOString()
;[16, 15, 14, 12, 11, 9, 8, 6].forEach((id) => {
  const m = MOCK_TREASURES.find((x) => x.id === id)
  if (m) m.createdAt = TODAY_ISO
})

const REALM_ORDER = ['练气期', '筑基期', '金丹期', '元婴期', '化神期', '炼虚期', '合体期', '大乘期', '渡劫期']
const GRADE_ORDER = ['天', '地', '玄', '黄']

function gradeRank(g) {
  const i = GRADE_ORDER.indexOf(g)
  return i < 0 ? 9 : i
}

// 本地过滤 + 排序（与后端逻辑一致）
function mockFilter({ category, realm, grade, q } = {}) {
  const key = (q || '').trim().toLowerCase()
  return MOCK_TREASURES.filter((m) =>
    (!category || category === '全部' || m.category === category) &&
    (!realm || realm === '全部' || m.realm === realm) &&
    (!grade || grade === '全部' || m.grade === grade) &&
    (!key || [m.name, m.category, m.grade, m.realm, m.origin, m.desc]
      .some((f) => (f || '').toLowerCase().includes(key)))
  ).sort((a, b) =>
    gradeRank(a.grade) - gradeRank(b.grade) ||
    a.category.localeCompare(b.category, 'zh') ||
    a.name.localeCompare(b.name, 'zh'))
}

function mockFacets() {
  const categories = [...new Set(MOCK_TREASURES.map((m) => m.category))].sort((a, b) => a.localeCompare(b, 'zh'))
  const realms = REALM_ORDER.filter((r) => MOCK_TREASURES.some((m) => m.realm === r))
  const grades = GRADE_ORDER.filter((g) => MOCK_TREASURES.some((m) => m.grade === g))
  return { categories, realms, grades }
}

// 拉取法宝：未配置后端 → 本地模式；已配置 → 优先真实接口；失败 → 降级本地藏经。
// 返回 { items, source }：source = 'remote' | 'local' | 'fallback'
export async function fetchTreasures(params = {}) {
  if (!BASE) {
    return { items: mockFilter(params), source: 'local' }
  }
  try {
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
    return { items: data.items || [], source: 'remote' }
  } catch (_) {
    return { items: mockFilter(params), source: 'fallback' }
  }
}

// 拉取筛选维度（类别/境界/品阶）
export async function fetchTreasureFacets() {
  if (!BASE) {
    return { ...mockFacets(), source: 'local' }
  }
  try {
    const res = await fetch(`${BASE}/api/treasures/facets`, {
      headers: { ...authHeader() }
    })
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const data = await res.json()
    return {
      categories: data.categories || [],
      realms: data.realms || [],
      grades: data.grades || [],
      source: 'remote'
    }
  } catch (_) {
    return { ...mockFacets(), source: 'fallback' }
  }
}

// 拉取单件法宝详情
export async function fetchTreasureDetail(id) {
  if (!BASE) {
    const m = MOCK_TREASURES.find((x) => String(x.id) === String(id))
    return { treasure: m || null, source: 'local' }
  }
  try {
    const res = await fetch(`${BASE}/api/treasures/${id}`, {
      headers: { ...authHeader() }
    })
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const data = await res.json()
    return { treasure: data, source: 'remote' }
  } catch (_) {
    const m = MOCK_TREASURES.find((x) => String(x.id) === String(id))
    return { treasure: m || null, source: 'fallback' }
  }
}

// ===== 后台管理 · 增删改（与 GET 一致：未配置后端走本地 mock，已配置走真实接口）=====
function nextMockId() {
  return MOCK_TREASURES.reduce((max, x) => Math.max(max, Number(x.id) || 0), 0) + 1
}

// 新增法宝：body { name, category, grade, realm, desc, origin, image }
export async function createTreasure(payload) {
  if (!BASE) {
    await delay(600)
    const item = { id: nextMockId(), createdAt: new Date().toISOString(), ...payload }
    MOCK_TREASURES.push(item)
    return item
  }
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
  if (!BASE) {
    await delay(600)
    const idx = MOCK_TREASURES.findIndex((x) => String(x.id) === String(id))
    if (idx >= 0) {
      MOCK_TREASURES[idx] = { ...MOCK_TREASURES[idx], ...payload, id: MOCK_TREASURES[idx].id }
      return MOCK_TREASURES[idx]
    }
    throw new Error('未找到该件法宝')
  }
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
  if (!BASE) {
    await delay(400)
    const idx = MOCK_TREASURES.findIndex((x) => String(x.id) === String(id))
    if (idx >= 0) MOCK_TREASURES.splice(idx, 1)
    return { ok: true }
  }
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

// ===== 法宝插画上传 =====
// 真实后端：复用功法插画上传接口 multipart POST 到 /api/manuals/upload，返回 { url }；
// mock 模式（未配置后端）：生成本地预览 URL（不落库，与整体 mock 行为一致）。
export async function uploadTreasureImage(file) {
  if (!BASE) {
    return { url: URL.createObjectURL(file) }
  }
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

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
