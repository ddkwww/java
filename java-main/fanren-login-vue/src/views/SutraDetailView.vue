<template>
  <div class="stage">
    <div class="detail-shell">
      <!-- ===== 加载态 ===== -->
      <div v-if="loading" class="loading">
        <div class="spinner" aria-hidden="true"></div>
        <p>正在展卷……</p>
      </div>

      <!-- ===== 未寻得 ===== -->
      <div v-else-if="!manual" class="empty">
        <div class="cloud" aria-hidden="true">﹀﹀﹀</div>
        <p>未寻得此卷秘籍。</p>
        <p class="empty-hint">或已散佚，或编号有误，道友可返回藏经阁另觅他典。</p>
        <button class="back-btn" @click="goBack">返回藏经阁</button>
      </div>

      <!-- ===== 详情正文 ===== -->
      <article v-else class="detail">
        <div class="detail-watermark" aria-hidden="true">{{ manual.category.slice(0, 1) }}</div>

        <div class="top">
          <!-- 封面 -->
          <div class="cover-wrap">
            <img v-if="cover" :src="cover" :alt="manual.name" class="cover" />
            <div v-else class="cover fallback"><span>{{ manual.category.slice(0, 2) }}</span></div>
            <span class="cover-grad" aria-hidden="true"></span>
            <span class="grade-badge" :data-grade="manual.grade">{{ manual.grade }}阶</span>
          </div>

          <!-- 概要 -->
          <div class="info">
            <p class="cat">{{ manual.category }}</p>
            <h1 class="name">{{ manual.name }}</h1>
            <div class="meta">
              <span class="tag realm">{{ manual.realm }}</span>
              <span class="tag origin">出处 · {{ manual.origin }}</span>
              <span class="tag price">灵 · {{ priceText }}</span>
            </div>
            <p class="lead">{{ manual.desc }}</p>
          </div>
        </div>

        <!-- 详述面板 -->
        <div class="desc-panel">
          <h2 class="panel-title"><i class="orn"></i>功法详述<i class="orn r"></i></h2>
          <p class="desc">{{ manual.desc }}</p>
          <div class="line-grid">
            <div class="line"><span class="k">分　类</span><span class="v">{{ manual.category }}</span></div>
            <div class="line"><span class="k">品　阶</span><span class="v" :data-grade="manual.grade">{{ manual.grade }}阶</span></div>
            <div class="line"><span class="k">境　界</span><span class="v">{{ manual.realm }}</span></div>
            <div class="line"><span class="k">出　处</span><span class="v">{{ manual.origin }}</span></div>
          </div>

          <!-- 属性加成 -->
          <div v-if="hasBonus" class="bonus-block">
            <h3 class="bonus-title"><i class="orn"></i>修成加成<i class="orn r"></i></h3>
            <div class="bonus-grid">
              <div v-for="b in bonusList" :key="b.key" class="bonus-cell" :data-zero="b.value === 0">
                <span class="bonus-icon" aria-hidden="true">{{ b.icon }}</span>
                <span class="bonus-k">{{ b.name }}</span>
                <span class="bonus-v">{{ b.value > 0 ? '+' + b.value : '—' }}</span>
                <span class="bonus-unit">{{ b.unit }}</span>
              </div>
            </div>
          </div>

          <!-- 价格 + 购买 -->
          <div class="buy-block">
            <div class="buy-price">
              <span class="buy-icon" aria-hidden="true">灵</span>
              <span class="buy-val">{{ priceText }}</span>
              <span class="buy-unit">灵石</span>
            </div>
            <button class="buy-btn" :disabled="buying" @click="onBuy">
              <span>{{ buying ? '交易中…' : '购得此卷' }}</span>
            </button>
            <p v-if="buyTip" class="buy-tip">{{ buyTip }}</p>
          </div>
        </div>
      </article>

      <!-- ===== 同脉功法 ===== -->
      <section v-if="manual && related.length" class="related block">
        <h2 class="block-title"><i class="orn"></i>同脉功法 · {{ manual.category }}<i class="orn r"></i></h2>
        <div class="grid">
          <article
            v-for="r in related"
            :key="r.id"
            class="item"
            :data-grade="r.grade"
            role="button"
            tabindex="0"
            @click="goDetail(r.id)"
            @keyup.enter="goDetail(r.id)"
          >
            <i class="corner" aria-hidden="true"></i>
            <span class="enter-hint" aria-hidden="true">细览 ›</span>
            <div class="item-cover" :class="{ 'no-img': !coverOf(r) }">
              <img v-if="coverOf(r)" :src="coverOf(r)" :alt="r.name" loading="lazy" />
              <span v-else class="cover-fallback">{{ r.category.slice(0, 2) }}</span>
              <span class="cover-grad" aria-hidden="true"></span>
            </div>
            <h3 class="item-name">{{ r.name }}</h3>
            <p class="item-origin">{{ r.realm }} · {{ r.grade }}阶</p>
          </article>
        </div>
      </section>

      <!-- ===== 页脚 ===== -->
      <footer class="footer">
        <i class="orn"></i>
        <p>仙路争锋 · 逆天改命 —— 凡人修仙传</p>
        <p class="footer-dim">藏经阁 · 演示前端（详情接口：GET /api/manuals/{id}）</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchManualDetail, fetchManuals } from '../api/sutra'
import { manualImage } from '../assets/manuals'
import { buyManual } from '../api/stats'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const manual = ref(null)
const loading = ref(true)
const related = ref([])
const buying = ref(false)
const buyTip = ref('')

const API_BASE = import.meta.env.VITE_API_BASE || ''

// 取封面：优先接口 image 字段（数据库 t_manual.image，后端静态资源），
// 为相对路径则拼接后端地址；无图则用前端按名称映射的本地插画兜底。
function coverOf(it) {
  if (it && it.image) {
    return it.image.startsWith('http') ? it.image : API_BASE + it.image
  }
  return manualImage(it?.name)
}

async function load(id) {
  loading.value = true
  manual.value = null
  related.value = []
  const { manual: m } = await fetchManualDetail(id)
  manual.value = m
  loading.value = false
  if (m) {
    // 同脉功法：按分类取同门，排除自身，最多 6 卷
    const res = await fetchManuals({ category: m.category })
    related.value = (res.items || [])
      .filter((x) => String(x.id) !== String(id))
      .slice(0, 6)
  }
}

const cover = ref('')
watch(manual, (m) => { cover.value = m ? coverOf(m) : '' }, { immediate: true })

// ===== 属性加成展示 =====
// 后端返回 bonusHp / bonusLifespan / bonusAtk / bonusDef / bonusSpd；
// 未配置后端（mock）或字段缺失时，按品阶×境界自动估算，保证详情页始终有数值可看。
const bonusList = computed(() => {
  const m = manual.value
  if (!m) return []
  return [
    { key: 'hp', name: '生命', icon: '命', value: bonusVal(m, 'bonusHp', 'hp'), unit: '' },
    { key: 'lifespan', name: '寿元', icon: '寿', value: bonusVal(m, 'bonusLifespan', 'lifespan'), unit: '年' },
    { key: 'atk', name: '攻击', icon: '攻', value: bonusVal(m, 'bonusAtk', 'atk'), unit: '' },
    { key: 'def', name: '防御', icon: '防', value: bonusVal(m, 'bonusDef', 'def'), unit: '' },
    { key: 'spd', name: '速度', icon: '速', value: bonusVal(m, 'bonusSpd', 'spd'), unit: '' }
  ]
})

const hasBonus = computed(() => bonusList.value.some((b) => b.value > 0))

// 字段名 -> 数值；缺失时用估算公式兜底
const GRADE_MULT = { 天: 8, 地: 4, 玄: 2, 黄: 1 }
const REALM_COEF = { 练气期: 1, 筑基期: 1.5, 金丹期: 2.5, 元婴期: 4, 化神期: 6, 炼虚期: 8, 合体期: 10, 大乘期: 12, 渡劫期: 15 }
const CAT_RATIO = {
  主修功法: { hp: 0.40, lifespan: 0.30, atk: 0.30, def: 0, spd: 0 },
  法术: { hp: 0, lifespan: 0, atk: 0.70, def: 0, spd: 0.30 },
  秘术: { hp: 0.20, lifespan: 0, atk: 0.50, def: 0, spd: 0.30 },
  神通: { hp: 0.20, lifespan: 0, atk: 0.60, def: 0, spd: 0.20 },
  炼丹: { hp: 0.50, lifespan: 0.50, atk: 0, def: 0, spd: 0 },
  符箓: { hp: 0.50, lifespan: 0.50, atk: 0, def: 0, spd: 0 },
  阵法: { hp: 0.30, lifespan: 0.20, atk: 0, def: 0.50, spd: 0 },
  傀儡: { hp: 0.40, lifespan: 0.20, atk: 0, def: 0.40, spd: 0 },
  炼器: { hp: 0.40, lifespan: 0.20, atk: 0, def: 0.40, spd: 0 }
}
const GRADE_POOL = { 天: 800, 地: 320, 玄: 120, 黄: 40 }

function bonusVal(m, field, dim) {
  if (m[field] != null) return Number(m[field]) || 0
  // 估算：品阶池 × 境界系数 × 类别比例
  const pool = (GRADE_POOL[m.grade] || 40) * (REALM_COEF[m.realm] || 1)
  const ratio = (CAT_RATIO[m.category] || CAT_RATIO['法术'])[dim] || 0
  return Math.round(pool * ratio)
}

function goDetail(id) {
  router.push({ name: 'manual', params: { id } })
}

function goBack() {
  router.push({ name: 'home' })
}

// ===== 价格展示 =====
// 后端返回 price 字段；缺失时按品阶×境界×类别估算兜底
const PRICE_POOL = { 天: 2000, 地: 800, 玄: 300, 黄: 100 }
const PRICE_CAT = { 主修功法: 1.0, 神通: 1.2, 炼丹: 0.8, 符箓: 0.8, 阵法: 0.8, 傀儡: 0.8, 炼器: 0.8, 法术: 0.6, 秘术: 0.6 }

const priceText = computed(() => {
  const m = manual.value
  if (!m) return '0'
  if (m.price != null) return String(m.price)
  const pool = (PRICE_POOL[m.grade] || 100) * (REALM_COEF[m.realm] || 1)
  const coef = PRICE_CAT[m.category] || 1.0
  return String(Math.round(pool * coef))
})

async function onBuy() {
  if (buying.value) return
  buying.value = true
  buyTip.value = ''
  try {
    const data = await buyManual(manual.value.id)
    if (data.spiritStone != null) auth.setSpiritStone(data.spiritStone)
    buyTip.value = data.message || '购得成功'
  } catch (e) {
    buyTip.value = e.message || '购买失败'
  } finally {
    buying.value = false
  }
}

onMounted(() => load(route.params.id))
watch(() => route.params.id, (id) => { if (id) load(id) })
</script>

<style scoped>
.stage {
  position: relative;
  z-index: 2;
  height: 100%;
  width: 100%;
}
.detail-shell {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  scroll-behavior: smooth;
  padding-top: 20px;
  padding-bottom: 30px;
}
.detail-shell::-webkit-scrollbar { width: 8px; }
.detail-shell::-webkit-scrollbar-track { background: rgba(7, 10, 20, .4); }
.detail-shell::-webkit-scrollbar-thumb {
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(233, 200, 120, .5), rgba(233, 200, 120, .2));
}

/* ===== 顶栏（已统一由全局导航栏承载，此处仅保留详情内容） ===== */

/* ===== 加载 / 空 ===== */
.loading, .empty {
  text-align: center;
  padding: 90px 20px;
  color: var(--ink-dim);
  letter-spacing: 3px;
}
.spinner {
  width: 42px; height: 42px; margin: 0 auto 18px;
  border-radius: 50%;
  border: 3px solid rgba(233, 200, 120, .18);
  border-top-color: var(--gold);
  animation: spin .9s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.cloud {
  font-size: 22px; letter-spacing: 10px; opacity: .45; margin-bottom: 14px;
  color: var(--ink-dim);
  animation: floaty2 5s ease-in-out infinite alternate;
}
@keyframes floaty2 { from { transform: translateX(-8px); } to { transform: translateX(8px); } }
.empty-hint { font-size: 13px; opacity: .75; margin-top: 10px; }
.back-btn {
  margin-top: 22px;
  padding: 11px 26px; border-radius: 10px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 4px; color: var(--gold);
  border: 1px solid rgba(233, 200, 120, .35); background: transparent;
  transition: .2s;
}
.back-btn:hover { background: rgba(233, 200, 120, .12); box-shadow: 0 0 16px rgba(233, 200, 120, .25); }

/* ===== 详情正文 ===== */
.detail { position: relative; max-width: 980px; margin: 0 auto; padding: 34px 28px 10px; }
.detail-watermark {
  position: absolute; right: 4%; top: 30px;
  font-size: min(220px, 36vw); font-weight: 700; line-height: 1;
  background: linear-gradient(180deg, rgba(233, 200, 120, .08), rgba(233, 200, 120, .02) 70%);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  pointer-events: none; user-select: none; z-index: 0;
}
.detail > * { position: relative; z-index: 1; }

.top { display: flex; gap: 30px; flex-wrap: wrap; }
.cover-wrap {
  position: relative;
  width: 280px; height: 360px; flex: none;
  border-radius: 18px; overflow: hidden;
  border: 1px solid var(--line);
  background: rgba(20, 29, 54, .6);
  box-shadow: 0 24px 60px rgba(0, 0, 0, .5);
}
.cover { width: 100%; height: 100%; object-fit: cover; display: block; }
.cover.fallback {
  display: grid; place-items: center;
  background:
    radial-gradient(70% 90% at 50% 110%, rgba(233, 200, 120, .16), transparent 70%),
    linear-gradient(180deg, rgba(20, 29, 54, .9), rgba(7, 10, 20, .9));
}
.cover.fallback span {
  font-size: 64px; font-weight: 700; letter-spacing: 8px; text-indent: 8px;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent; opacity: .85;
}
.cover-grad {
  position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(180deg, rgba(7, 10, 20, .05) 40%, rgba(7, 10, 20, .55) 82%, rgba(7, 10, 20, .8));
}
.grade-badge {
  position: absolute; left: 14px; bottom: 14px; z-index: 2;
  padding: 4px 12px; border-radius: 8px; font-weight: 700; font-size: 15px; letter-spacing: 1px;
}
.grade-badge[data-grade="天"] { color: #0c1322; background: linear-gradient(135deg, var(--gold-2), var(--gold)); }
.grade-badge[data-grade="地"] { color: #fff; background: rgba(139, 109, 255, .8); }
.grade-badge[data-grade="玄"] { color: #062024; background: linear-gradient(135deg, #8be6f6, var(--qi)); }
.grade-badge[data-grade="黄"] { color: var(--ink-dim); background: rgba(139, 155, 189, .25); border: 1px solid var(--line); }

.info { flex: 1; min-width: 260px; padding-top: 6px; }
.cat {
  color: var(--gold); font-size: 14px; letter-spacing: 4px; margin-bottom: 12px;
}
.name {
  font-size: 42px; letter-spacing: 4px; line-height: 1.2;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 22px rgba(233, 200, 120, .18));
  margin-bottom: 18px;
}
.meta { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 22px; }
.tag {
  padding: 5px 14px; border-radius: 999px; font-size: 14px; letter-spacing: 1px;
  border: 1px solid var(--line); color: var(--ink-dim);
}
.tag.realm { color: var(--qi); border-color: rgba(103, 232, 249, .3); }
.tag.origin { color: var(--gold); border-color: rgba(233, 200, 120, .3); }
.tag.price { color: var(--gold-2); border-color: rgba(233, 200, 120, .45); font-weight: 700; }
.lead {
  font-size: 16px; line-height: 1.95; letter-spacing: .5px; color: var(--ink-dim);
  border-left: 3px solid var(--gold); padding-left: 16px;
}

/* ===== 详述面板 ===== */
.desc-panel {
  margin-top: 30px;
  padding: 26px 28px; border-radius: 18px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(20, 29, 54, .5), rgba(7, 10, 20, .6));
  backdrop-filter: blur(6px);
}
.panel-title {
  display: flex; align-items: center; gap: 16px; justify-content: center;
  color: var(--ink); font-size: 18px; letter-spacing: 6px; font-weight: 700; margin-bottom: 20px;
}
.orn { position: relative; flex: none; width: 90px; height: 1px; }
.orn::before {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(90deg, transparent, rgba(233, 200, 120, .6));
}
.orn::after {
  content: ''; position: absolute; right: -4px; top: -3px;
  width: 7px; height: 7px; transform: rotate(45deg);
  background: var(--gold); box-shadow: 0 0 8px rgba(233, 200, 120, .6);
}
.orn.r::before { background: linear-gradient(90deg, rgba(233, 200, 120, .6), transparent); }
.orn.r::after { right: auto; left: -4px; }
.desc {
  font-size: 16px; line-height: 2.1; letter-spacing: .5px; color: var(--ink);
  margin-bottom: 22px;
}
.line-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px;
}
.line {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border-radius: 12px;
  border: 1px solid var(--line); background: rgba(7, 10, 20, .4);
}
.line .k { color: var(--gold); font-size: 14px; letter-spacing: 2px; flex: none; }
.line .v { color: var(--ink); font-size: 15px; letter-spacing: 1px; }
.line .v[data-grade="天"] { color: var(--gold-2); }
.line .v[data-grade="地"] { color: #b9a6ff; }
.line .v[data-grade="玄"] { color: var(--qi); }
.line .v[data-grade="黄"] { color: var(--ink-dim); }

/* ===== 属性加成区块 ===== */
.bonus-block {
  margin-top: 24px;
  padding-top: 22px;
  border-top: 1px dashed rgba(233, 200, 120, .2);
}
.bonus-title {
  display: flex; align-items: center; gap: 16px; justify-content: center;
  color: var(--gold); font-size: 16px; letter-spacing: 4px; font-weight: 700; margin-bottom: 18px;
}
.bonus-grid {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px;
}
.bonus-cell {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 14px 8px; border-radius: 12px;
  border: 1px solid rgba(103, 232, 249, .18);
  background: radial-gradient(120% 120% at 50% 0%, rgba(103, 232, 249, .08), rgba(7, 10, 20, .4));
  transition: transform .2s, box-shadow .2s;
}
.bonus-cell:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(0, 0, 0, .35); }
.bonus-cell[data-zero="true"] { opacity: .45; }
.bonus-icon {
  display: inline-flex; width: 32px; height: 32px; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 14px; margin-bottom: 2px;
  border: 1px solid rgba(103, 232, 249, .3); color: var(--qi);
}
.bonus-k { font-size: 12px; color: var(--ink-dim); letter-spacing: 1px; }
.bonus-v { font-size: 22px; font-weight: 700; color: var(--gold-2); font-family: "Noto Serif SC", serif; }
.bonus-unit { font-size: 11px; color: var(--ink-dim); }

/* ===== 价格 + 购买区块 ===== */
.buy-block {
  margin-top: 24px;
  padding-top: 22px;
  border-top: 1px dashed rgba(233, 200, 120, .2);
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
}
.buy-price {
  display: inline-flex; align-items: baseline; gap: 6px;
  padding: 8px 16px; border-radius: 999px;
  border: 1px solid rgba(233, 200, 120, .35);
  background: rgba(233, 200, 120, .06);
}
.buy-icon {
  display: inline-flex; width: 22px; height: 22px; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 12px; align-self: center;
  background: linear-gradient(135deg, var(--gold-2), var(--gold));
  color: #0c1322; font-weight: 700;
}
.buy-val { font-size: 20px; font-weight: 700; color: var(--gold-2); font-family: "Noto Serif SC", serif; }
.buy-unit { font-size: 12px; color: var(--ink-dim); }
.buy-btn {
  padding: 10px 22px; border-radius: 10px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 3px; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold));
  border: none; font-weight: 700;
  transition: .2s;
}
.buy-btn:hover:not(:disabled) { box-shadow: 0 0 20px rgba(233, 200, 120, .5); transform: translateY(-1px); }
.buy-btn:disabled { opacity: .5; cursor: not-allowed; }
.buy-tip { width: 100%; font-size: 13px; color: var(--qi); letter-spacing: 1px; }

@media (max-width: 640px) {
  .bonus-grid { grid-template-columns: repeat(3, 1fr); }
}

/* ===== 同脉功法 ===== */
.block { padding: 30px 28px 10px; max-width: 1080px; margin: 0 auto; }
.block-title {
  display: flex; align-items: center; gap: 16px; justify-content: center;
  color: var(--ink); font-size: 20px; letter-spacing: 8px; font-weight: 700; margin-bottom: 26px;
}
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(264px, 1fr)); gap: 18px; }
.item {
  position: relative; overflow: hidden;
  padding: 0 0 16px; border-radius: 16px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(20, 29, 54, .55), rgba(7, 10, 20, .65));
  backdrop-filter: blur(6px); cursor: pointer;
  transition: transform .25s, border-color .25s, box-shadow .25s;
}
.item:hover {
  transform: translateY(-4px);
  border-color: rgba(233, 200, 120, .35);
  box-shadow: 0 16px 40px rgba(0, 0, 0, .5), 0 0 24px rgba(233, 200, 120, .12);
}
.corner {
  position: absolute; top: 12px; right: 14px; width: 8px; height: 8px;
  transform: rotate(45deg) scale(0); transition: transform .3s .05s;
  background: linear-gradient(135deg, var(--gold-2), var(--gold));
  box-shadow: 0 0 10px rgba(233, 200, 120, .7);
}
.item:hover .corner { transform: rotate(45deg) scale(1); }
.enter-hint {
  position: absolute; top: 12px; right: 14px; z-index: 2;
  font-size: 13px; letter-spacing: 2px; color: var(--gold-2);
  background: rgba(7, 10, 20, .6); border: 1px solid rgba(233, 200, 120, .3);
  padding: 2px 10px; border-radius: 999px;
  opacity: 0; transform: translateY(-4px); transition: .25s;
}
.item:hover .enter-hint { opacity: 1; transform: translateY(0); }
.item-cover {
  position: relative; margin-bottom: 14px; height: 150px; overflow: hidden;
  background: rgba(20, 29, 54, .6);
}
.item-cover img { width: 100%; height: 100%; object-fit: cover; display: block; transform: scale(1.02); transition: transform .5s; }
.item:hover .item-cover img { transform: scale(1.09); }
.item-cover.no-img {
  display: grid; place-items: center;
  background:
    radial-gradient(70% 90% at 50% 110%, rgba(233, 200, 120, .16), transparent 70%),
    linear-gradient(180deg, rgba(20, 29, 54, .9), rgba(7, 10, 20, .9));
}
.cover-fallback {
  font-size: 44px; font-weight: 700; letter-spacing: 6px; text-indent: 6px;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent; opacity: .85;
}
.cover-grad {
  position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(180deg, rgba(7, 10, 20, .05) 40%, rgba(7, 10, 20, .55) 82%, rgba(7, 10, 20, .8));
}
.item-name { font-size: 18px; letter-spacing: 2px; color: var(--ink); margin: 0 18px 8px; }
.item-origin { font-size: 13px; letter-spacing: 1px; color: var(--gold); opacity: .8; margin: 0 18px; }

/* ===== 页脚 ===== */
.footer { text-align: center; padding: 46px 20px 30px; color: var(--ink-dim); font-size: 14px; letter-spacing: 4px; }
.footer .orn { margin: 0 auto 18px; display: block; width: 160px; }
.footer .orn::before { background: linear-gradient(90deg, transparent, rgba(233, 200, 120, .45), transparent); }
.footer-dim { font-size: 12px; opacity: .6; margin-top: 10px; letter-spacing: 2px; }

/* 移动端 */
@media (max-width: 760px) {
  .topbar { padding: 0 16px; gap: 10px; height: 58px; }
  .brand-name { display: none; }
  .source-note { display: none; }
  .detail { padding: 22px 16px 10px; }
  .top { gap: 18px; justify-content: center; }
  .cover-wrap { width: 220px; height: 280px; }
  .name { font-size: 32px; }
  .block { padding: 24px 16px 10px; }
  .orn { width: 50px; }
}
</style>
