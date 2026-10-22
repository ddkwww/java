// 藏经阁 · 功法秘籍接口
//
// === 后端接口契约（后端实现后自动启用，前端零改动）===
// GET /api/manuals?category=&realm=&grade=&q=
//   Header Authorization: Bearer <token>
//   成功 200：{ items: [{ id, name, category, grade, realm, desc, origin }] }
//     - 无参：返回全部秘籍
//     - q 非空：关键字搜索（名称/简介/分类/境界/出处），可叠加 分类/境界/品阶 筛选
//   GET /api/manuals/facets
//   成功 200：{ categories:[...], realms:[...], grades:[...] }（供筛选器使用）
// 后端未实现 / 未启动 → 降级本地藏经（mock），保证页面始终可用。
const BASE = import.meta.env.VITE_API_BASE || ''

// 从本地读取 token，自动附加到请求头（与 api/auth.js 保持一致）
function authHeader() {
  const token = localStorage.getItem('fx_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

// ------- 本地藏经（mock：与后端种子数据一致，后端未连时使用）-------
const MOCK_MANUALS = [
  { id: 1, name: '长春功', category: '主修功法', grade: '玄', realm: '练气期', desc: '七玄门基础功法，韩立初踏仙途所得，进境虽缓，根基却极稳，可延寿培元。', origin: '七玄门' },
  { id: 2, name: '青元剑诀', category: '主修功法', grade: '地', realm: '筑基期', desc: '青元观镇派剑诀，韩立主修功法，剑气化丝、以剑御敌，至元婴可布剑阵。', origin: '青元观' },
  { id: 3, name: '大衍诀', category: '主修功法', grade: '地', realm: '筑基期', desc: '大衍神君所创神识功法，可凝神识化身、参悟傀儡阵法，与青元剑诀相辅。', origin: '大衍神君' },
  { id: 4, name: '梵圣真魔功', category: '主修功法', grade: '天', realm: '元婴期', desc: '魔道无上真魔功，修成三头六臂法相，力大无穷，韩立后期主修。', origin: '真魔功' },
  { id: 5, name: '五雷正法', category: '主修功法', grade: '玄', realm: '金丹期', desc: '正道雷法总纲，引动天雷攻伐凌厉，破邪驱魔有奇效。', origin: '正道' },
  { id: 6, name: '三阳决', category: '主修功法', grade: '玄', realm: '筑基期', desc: '三阳宗火属性功法，进境迅捷，利于火系法术。', origin: '三阳宗' },
  { id: 7, name: '明王诀', category: '主修功法', grade: '地', realm: '元婴期', desc: '佛门护法功法，凝金身不坏，外力难伤。', origin: '佛门' },
  { id: 8, name: '素女轮回功', category: '主修功法', grade: '地', realm: '元婴期', desc: '阴罗宗魔道双修功法，采补夺元，歹毒非常。', origin: '阴罗宗' },
  { id: 9, name: '托天魔功', category: '主修功法', grade: '玄', realm: '金丹期', desc: '魔道托天魔功，炼体凝煞，力大无穷。', origin: '魔道' },
  { id: 10, name: '驱物术', category: '法术', grade: '黄', realm: '练气期', desc: '基础控物之术，以灵力摄物移形，入门必学。', origin: '通用' },
  { id: 11, name: '火弹术', category: '法术', grade: '黄', realm: '练气期', desc: '最常见火系攻击法术，凝火成弹，击中爆裂。', origin: '通用' },
  { id: 12, name: '雷闪术', category: '法术', grade: '黄', realm: '练气期', desc: '雷系小法术，倏忽突袭，扰敌心神。', origin: '通用' },
  { id: 13, name: '冰针术', category: '法术', grade: '玄', realm: '练气期', desc: '冰系法术，凝水成针，绵密难防。', origin: '通用' },
  { id: 14, name: '土甲术', category: '法术', grade: '黄', realm: '练气期', desc: '土系防御法术，凝甲护身，化解寻常攻势。', origin: '通用' },
  { id: 15, name: '火蟒术', category: '法术', grade: '玄', realm: '筑基期', desc: '进阶火系法术，化火为蟒，缠绞撕裂。', origin: '通用' },
  { id: 16, name: '清风诀', category: '法术', grade: '黄', realm: '练气期', desc: '轻身御风之术，身形飘忽，闪避迅捷。', origin: '通用' },
  { id: 17, name: '匿风遁', category: '法术', grade: '玄', realm: '筑基期', desc: '风系遁术，隐去声息身形，潜行无踪。', origin: '通用' },
  { id: 18, name: '血影遁', category: '秘术', grade: '玄', realm: '筑基期', desc: '血道逃命秘术，燃精血极速遁逃，反噬极重。', origin: '魔道' },
  { id: 19, name: '惊神刺', category: '秘术', grade: '玄', realm: '金丹期', desc: '神识攻击秘术，凝念成刺直击神魂，无形难防。', origin: '通用' },
  { id: 20, name: '血炼神光', category: '秘术', grade: '地', realm: '元婴期', desc: '魔道血道秘术，可祭炼法宝器灵，凶险异常。', origin: '魔道' },
  { id: 21, name: '煞丹术', category: '秘术', grade: '玄', realm: '金丹期', desc: '魔道凝煞成丹，借煞力增修为，戾气缠身。', origin: '魔道' },
  { id: 22, name: '融灵之术', category: '秘术', grade: '地', realm: '元婴期', desc: '将器灵融于己身，人宝合一，威能大涨。', origin: '通用' },
  { id: 23, name: '春黎剑阵', category: '神通', grade: '天', realm: '元婴期', desc: '青元剑诀配套剑阵，七十二口飞剑化春黎之景，困杀强敌。', origin: '青元剑诀' },
  { id: 24, name: '青蟠剑阵', category: '神通', grade: '天', realm: '元婴期', desc: '另一配套剑阵，剑蟠盘绕如龙，攻守兼备。', origin: '青元剑诀' },
  { id: 25, name: '疾风九变', category: '神通', grade: '天', realm: '化神期', desc: '风系遁法神通，瞬息九变，难锁定难追及。', origin: '风道' },
  { id: 26, name: '百脉炼宝诀', category: '神通', grade: '地', realm: '元婴期', desc: '以全身经脉温养本命法宝，宝随人长。', origin: '通用' },
  { id: 27, name: '缩地成寸', category: '神通', grade: '天', realm: '化神期', desc: '空间神通，一步千里，瞬息无踪。', origin: '空间' },
  { id: 28, name: '三头六臂', category: '神通', grade: '天', realm: '大乘期', desc: '梵圣真魔功法相神通，三头六臂各持神兵。', origin: '梵圣真魔功' },
  { id: 29, name: '筑基丹丹方', category: '炼丹', grade: '玄', realm: '练气期', desc: '筑基必用丹方，引灵气冲关，破境筑基。', origin: '炼丹' },
  { id: 30, name: '凝魂丹丹方', category: '炼丹', grade: '地', realm: '金丹期', desc: '凝练神魂、滋补识海，结丹前后修行必备。', origin: '炼丹' },
  { id: 31, name: '大衍丹丹方', category: '炼丹', grade: '地', realm: '元婴期', desc: '大衍神君所传，增识海神识，助参悟大道。', origin: '大衍神君' },
  { id: 32, name: '天火液炼方', category: '炼丹', grade: '玄', realm: '元婴期', desc: '以地火灵焰炼体之灵液，淬体坚骨。', origin: '炼丹' },
  { id: 33, name: '长春丹方', category: '炼丹', grade: '黄', realm: '练气期', desc: '疗伤培元之丹，气血双补，行走江湖常备。', origin: '炼丹' },
  { id: 34, name: '炼器总纲', category: '炼器', grade: '玄', realm: '筑基期', desc: '炼器入门总纲，识灵材、掌火候、祭器灵。', origin: '炼器' },
  { id: 35, name: '青竹蜂云剑炼制术', category: '炼器', grade: '地', realm: '金丹期', desc: '韩立本命飞剑炼制之术，七十二剑合一化云。', origin: '韩立' },
  { id: 36, name: '傀儡术总诀', category: '傀儡', grade: '玄', realm: '金丹期', desc: '机关傀儡之术，以灵石驱傀儡，代劳御敌、布阵探查。', origin: '傀儡' },
  { id: 46, name: '千竹教傀儡术', category: '傀儡', grade: '玄', realm: '金丹期', desc: '千竹教镇派傀儡绝学，灵石驱动机关傀儡替身御敌，精巧诡变。', origin: '千竹教' },
  { id: 37, name: '符箓基础', category: '符箓', grade: '黄', realm: '练气期', desc: '制符入门，引灵力入符，留法术于纸。', origin: '符箓' },
  { id: 38, name: '雷符术', category: '符箓', grade: '玄', realm: '筑基期', desc: '雷系符箓，掷出引天雷，一次性重创。', origin: '符箓' },
  { id: 39, name: '五行灵符', category: '符箓', grade: '地', realm: '金丹期', desc: '金木水火土五系高阶符箓，攻防一体。', origin: '符箓' },
  { id: 40, name: '阵法初解', category: '阵法', grade: '黄', realm: '练气期', desc: '阵法入门，布迷踪小阵，困敌隐匿。', origin: '阵法' },
  { id: 41, name: '幻阵术', category: '阵法', grade: '玄', realm: '筑基期', desc: '幻术大阵，乱敌五感，困于虚境。', origin: '阵法' },
  { id: 42, name: '困龙阵', category: '阵法', grade: '地', realm: '金丹期', desc: '困敌杀阵，引灵气成锁，锁龙困仙。', origin: '阵法' },
  { id: 43, name: '玄天斩灵术', category: '神通', grade: '天', realm: '炼虚期', desc: '借玄天之宝之威，斩灵断魂，无物不破。', origin: '玄天' },
  { id: 44, name: '虚空炼器法', category: '炼器', grade: '天', realm: '合体期', desc: '于虚空中祭炼法宝，纳空间之威。', origin: '炼器' },
  { id: 45, name: '渡劫秘要', category: '主修功法', grade: '天', realm: '渡劫期', desc: '渡劫飞升之秘要，引雷劫淬身，九死一生。', origin: '飞升' }
]

const REALM_ORDER = ['练气期', '筑基期', '金丹期', '元婴期', '化神期', '炼虚期', '合体期', '大乘期', '渡劫期']
const GRADE_ORDER = ['天', '地', '玄', '黄']

function gradeRank(g) {
  const i = GRADE_ORDER.indexOf(g)
  return i < 0 ? 9 : i
}

// 本地过滤 + 排序（与后端逻辑一致）
function mockFilter({ category, realm, grade, q } = {}) {
  const key = (q || '').trim().toLowerCase()
  return MOCK_MANUALS.filter((m) =>
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
  const categories = [...new Set(MOCK_MANUALS.map((m) => m.category))].sort((a, b) => a.localeCompare(b, 'zh'))
  const realms = REALM_ORDER.filter((r) => MOCK_MANUALS.some((m) => m.realm === r))
  const grades = GRADE_ORDER.filter((g) => MOCK_MANUALS.some((m) => m.grade === g))
  return { categories, realms, grades }
}

// 拉取功法秘籍：未配置后端 → 本地模式；已配置 → 优先真实接口；失败 → 降级本地藏经。
// 返回 { items, source }：source = 'remote' | 'local' | 'fallback'
export async function fetchManuals(params = {}) {
  if (!BASE) {
    return { items: mockFilter(params), source: 'local' }
  }
  try {
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
    return { items: data.items || [], source: 'remote' }
  } catch (_) {
    return { items: mockFilter(params), source: 'fallback' }
  }
}

// 拉取筛选维度（分类/境界/品阶）
export async function fetchFacets() {
  if (!BASE) {
    return { ...mockFacets(), source: 'local' }
  }
  try {
    const res = await fetch(`${BASE}/api/manuals/facets`, {
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

// 拉取单卷功法详情：未配置后端 → 本地模式；已配置 → 优先真实接口；失败 → 降级本地藏经。
// 返回 { manual, source }：source = 'remote' | 'local' | 'fallback'
// 注：真实库中 t_manual 主键非连续（如 49 / 68），故按 id 精确匹配；本地 mock 同样按 id 匹配。
export async function fetchManualDetail(id) {
  if (!BASE) {
    const m = MOCK_MANUALS.find((x) => String(x.id) === String(id))
    return { manual: m || null, source: 'local' }
  }
  try {
    const res = await fetch(`${BASE}/api/manuals/${id}`, {
      headers: { ...authHeader() }
    })
    if (!res.ok) throw new Error('HTTP ' + res.status)
    const data = await res.json()
    return { manual: data, source: 'remote' }
  } catch (_) {
    const m = MOCK_MANUALS.find((x) => String(x.id) === String(id))
    return { manual: m || null, source: 'fallback' }
  }
}

// ===== 后台管理 · 增删改（与 GET 一致：未配置后端走本地 mock，已配置走真实接口）=====
// 本地 mock 的写操作直接作用于 MOCK_MANUALS（模块级数组），本次会话内刷新列表可见，
// 整页刷新后复原（与 GET 的本地模式行为一致）。
function nextMockId() {
  return MOCK_MANUALS.reduce((max, x) => Math.max(max, Number(x.id) || 0), 0) + 1
}

// 新增功法秘籍：body { name, category, grade, realm, desc, origin, image }
export async function createManual(payload) {
  if (!BASE) {
    await delay(600)
    const item = { id: nextMockId(), ...payload }
    MOCK_MANUALS.push(item)
    return item
  }
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
  if (!BASE) {
    await delay(600)
    const idx = MOCK_MANUALS.findIndex((x) => String(x.id) === String(id))
    if (idx >= 0) {
      MOCK_MANUALS[idx] = { ...MOCK_MANUALS[idx], ...payload, id: MOCK_MANUALS[idx].id }
      return MOCK_MANUALS[idx]
    }
    throw new Error('未找到该卷秘籍')
  }
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
  if (!BASE) {
    await delay(400)
    const idx = MOCK_MANUALS.findIndex((x) => String(x.id) === String(id))
    if (idx >= 0) MOCK_MANUALS.splice(idx, 1)
    return { ok: true }
  }
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
