<template>
  <div class="stage">
    <div class="stats-shell">
      <div class="stats-watermark" aria-hidden="true">修</div>

      <!-- ===== 加载态 ===== -->
      <div v-if="loading" class="loading">
        <div class="spinner" aria-hidden="true"></div>
        <p>正在推演修为……</p>
      </div>

      <!-- ===== 主面板 ===== -->
      <div v-else class="panel">
        <!-- 顶部：道号 + 境界 -->
        <header class="head">
          <div class="identity">
            <p class="dao-name">{{ stats.username }}<span class="dao-tag">道友</span></p>
            <p class="realm-full">{{ stats.fullRealm }}</p>
            <p class="realm-sub">
              {{ stats.realm }} · {{ stats.stage }} · 第 {{ stats.totalLevel }} / 81 层修为
            </p>
          </div>
          <div class="head-right">
            <div class="resource-box">
              <div class="stone-box">
                <span class="stone-icon" aria-hidden="true">灵</span>
                <span class="stone-val">{{ stats.spiritStone }}</span>
                <span class="stone-unit">灵石</span>
              </div>
              <div class="qi-box">
                <span class="qi-icon" aria-hidden="true">气</span>
                <span class="qi-val">{{ stats.spiritQi }}</span>
                <span class="qi-unit">灵气</span>
              </div>
            </div>
            <button class="btn-up" :disabled="upgrading || stats.totalLevel >= 81" @click="onBreakthrough">
              <span>{{ upgrading ? '突破中…' : (stats.totalLevel >= 81 ? '已臻圆满' : '突破境界') }}</span>
            </button>
          </div>
        </header>

        <!-- 寿元 / 年龄 / 天数 -->
        <div class="life-bar">
          <span class="life-label">剩余寿元 <em>{{ stats.remainingLifespan }}</em> 年</span>
          <span class="life-spent">年岁 <em>{{ stats.age }}</em> · 已耗 <em>{{ stats.ageUsed }}</em> 天</span>
        </div>

        <!-- 修为进度 -->
        <div class="progress-wrap">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPct + '%' }"></div>
          </div>
          <div class="progress-labels">
            <span>练气</span><span>筑基</span><span>金丹</span><span>元婴</span><span>化神</span>
            <span>炼虚</span><span>合体</span><span>大乘</span><span>渡劫</span>
          </div>
        </div>

        <!-- 突破费用提示 -->
        <div v-if="stats.totalLevel < 81" class="cost-note">
          <span class="cost-label">突破至下一层需</span>
          <span class="cost-val">{{ stats.breakthroughCost }}</span>
          <span class="cost-unit">灵石</span>
          <span class="cost-sep">·</span>
          <span class="cost-val qi">{{ stats.breakthroughQiCost }}</span>
          <span class="cost-unit">灵气</span>
          <span class="cost-sep">·</span>
          <span class="cost-val day">{{ stats.breakthroughDayCost }}</span>
          <span class="cost-unit">天</span>
          <span class="cost-tag" :data-enough="canBreak">
            {{ canBreak ? '资源充足' : '资源不足' }}
          </span>
        </div>

        <!-- 获取资源（打坐 / 除妖 / 挖矿） -->
        <section class="earn">
          <h2 class="sec-title"><i class="orn"></i>获取资源<i class="orn r"></i></h2>
          <div class="earn-grid">
            <div class="earn-card">
              <div class="earn-head">
                <span class="earn-name">打坐修炼</span>
                <span class="earn-reward qi-txt">+20 灵气</span>
              </div>
              <p class="earn-desc">静心运功 · 耗 15 天 · 60 秒一次</p>
              <button class="earn-btn" :disabled="earning || meditateCd > 0" @click="onMeditate">
                {{ meditateCd > 0 ? meditateCd + 's 后可再练' : (earning === 'meditate' ? '修炼中…' : '开始打坐') }}
              </button>
            </div>
            <div class="earn-card">
              <div class="earn-head">
                <span class="earn-name">斩妖除魔</span>
                <span class="earn-reward">+30 灵气 · +100 灵石</span>
              </div>
              <p class="earn-desc">除妖卫道 · 耗 2 个月 · 5 分钟一次</p>
              <button class="earn-btn" :disabled="earning || huntCd > 0" @click="onHunt">
                {{ huntCd > 0 ? huntCd + 's 后可再战' : (earning === 'hunt' ? '除妖中…' : '斩妖除魔') }}
              </button>
            </div>
            <div class="earn-card">
              <div class="earn-head">
                <span class="earn-name">深入矿脉</span>
                <span class="earn-reward stone-txt">+150 灵石</span>
              </div>
              <p class="earn-desc">开采灵石矿 · 耗 1 个月 · 5 分钟一次</p>
              <button class="earn-btn" :disabled="earning || mineCd > 0" @click="onMine">
                {{ mineCd > 0 ? mineCd + 's 后可再采' : (earning === 'mine' ? '开采中…' : '深入矿脉') }}
              </button>
            </div>
          </div>
        </section>

        <!-- 五维属性 -->
        <section class="attrs">
          <h2 class="sec-title"><i class="orn"></i>五维属性<i class="orn r"></i></h2>
          <div class="attr-grid">
            <div v-for="a in attrList" :key="a.key" class="attr-card" :data-attr="a.key">
              <span class="attr-icon" aria-hidden="true">{{ a.icon }}</span>
              <div class="attr-body">
                <p class="attr-name">{{ a.name }}</p>
                <p class="attr-val">{{ stats.total[a.key] }}</p>
              </div>
              <p class="attr-unit">{{ a.unit }}</p>
            </div>
          </div>
        </section>

        <!-- 加成明细 -->
        <section class="breakdown">
          <h2 class="sec-title"><i class="orn"></i>加成来源<i class="orn r"></i></h2>

          <div class="src" v-for="s in sourceList" :key="s.key">
            <div class="src-head">
              <span class="src-name">{{ s.name }}</span>
              <span class="src-desc">{{ s.desc }}</span>
            </div>
            <div class="src-row">
              <div v-for="a in attrList" :key="a.key" class="src-cell">
                <span class="src-k">{{ a.icon }} {{ a.name }}</span>
                <span class="src-v">{{ stats.breakdown[s.key][a.key] }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 境界寿元上限 -->
        <p class="lifespan-note">当前境界寿元上限 <em>{{ stats.lifespanCap }}</em> 年</p>
      </div>

      <!-- 提示条 -->
      <p v-if="tip" class="tip">{{ tip }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStats, breakthrough, earnMeditate, earnHunt, earnMine } from '../api/stats'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const upgrading = ref(false)
const tip = ref('')
const earning = ref('') // 当前进行中的获取动作：'meditate' | 'hunt' | 'mine' | ''
const meditateCd = ref(0)
const huntCd = ref(0)
const mineCd = ref(0)
let cdTimer = null
const stats = ref({
  username: '', fullRealm: '', realm: '', stage: '', totalLevel: 1,
  lifespanCap: 0, spiritStone: 0, spiritQi: 0, age: 0, ageUsed: 0, remainingLifespan: 0,
  breakthroughCost: 0, breakthroughQiCost: 0, breakthroughDayCost: 0,
  total: { hp: 0, lifespan: 0, atk: 0, def: 0, spd: 0 },
  breakdown: {
    realm: {}, spiritRoot: {}, physique: {}, manual: {}, treasure: {}
  }
})

const attrList = [
  { key: 'hp', name: '生命', icon: '命', unit: '' },
  { key: 'lifespan', name: '寿元', icon: '寿', unit: '年' },
  { key: 'atk', name: '攻击', icon: '攻', unit: '' },
  { key: 'def', name: '防御', icon: '防', unit: '' },
  { key: 'spd', name: '速度', icon: '速', unit: '' }
]

const sourceList = computed(() => [
  { key: 'realm', name: '境界成长', desc: '修为每层累加' },
  { key: 'spiritRoot', name: '灵根', desc: stats.value.spiritRoot || '无' },
  { key: 'physique', name: '体质', desc: stats.value.physique || '凡人之躯' },
  { key: 'manual', name: '功法', desc: stats.value.manual || '未修' },
  { key: 'treasure', name: '法宝', desc: stats.value.treasure || '未持' }
])

const progressPct = computed(() => Math.round((stats.value.totalLevel / 81) * 100))

// 是否可突破（灵石、灵气、寿元三者皆足）
const canBreak = computed(() =>
  stats.value.spiritStone >= stats.value.breakthroughCost &&
  stats.value.spiritQi >= stats.value.breakthroughQiCost &&
  stats.value.remainingLifespan * 365 - stats.value.ageUsed >= stats.value.breakthroughDayCost
)

async function load() {
  loading.value = true
  try {
    stats.value = await getStats()
    if (stats.value.spiritStone != null) auth.setSpiritStone(stats.value.spiritStone)
  } catch (e) {
    tip.value = e.message || '推演失败'
  } finally {
    loading.value = false
  }
}

async function onBreakthrough() {
  if (upgrading.value) return
  upgrading.value = true
  tip.value = ''
  try {
    const data = await breakthrough()
    stats.value = data
    if (data.spiritStone != null) auth.setSpiritStone(data.spiritStone)
    const paid = data.costPaid != null
      ? `消耗 ${data.costPaid} 灵石、${data.qiCostPaid} 灵气，闭关 ${data.dayCostPaid} 天，`
      : ''
    tip.value = '突破成功！' + paid + '修为已至 ' + data.fullRealm
  } catch (e) {
    tip.value = e.message || '突破失败'
  } finally {
    upgrading.value = false
  }
}

// ===== 资源获取 =====
function startCooldown(kind, seconds) {
  if (kind === 'meditate') meditateCd.value = seconds
  else if (kind === 'hunt') huntCd.value = seconds
  else if (kind === 'mine') mineCd.value = seconds
}

async function onMeditate() {
  if (earning.value) return
  earning.value = 'meditate'
  tip.value = ''
  try {
    const data = await earnMeditate()
    applyEarn(data)
    tip.value = data.message || '打坐获得灵气'
    startCooldown('meditate', data.cooldown || 60)
  } catch (e) {
    tip.value = e.message || '修炼失败'
  } finally {
    earning.value = ''
  }
}

async function onHunt() {
  if (earning.value) return
  earning.value = 'hunt'
  tip.value = ''
  try {
    const data = await earnHunt()
    applyEarn(data)
    tip.value = data.message || '除妖获得灵气与灵石'
    startCooldown('hunt', data.cooldown || 300)
  } catch (e) {
    tip.value = e.message || '除妖失败'
  } finally {
    earning.value = ''
  }
}

async function onMine() {
  if (earning.value) return
  earning.value = 'mine'
  tip.value = ''
  try {
    const data = await earnMine()
    applyEarn(data)
    tip.value = data.message || '挖矿获得灵石'
    startCooldown('mine', data.cooldown || 300)
  } catch (e) {
    tip.value = e.message || '开采失败'
  } finally {
    earning.value = ''
  }
}

// 统一应用获取结果：同步灵石/灵气/天数/寿元
// 优先使用后端回传的精确值（ageUsed / remainingLifespan），
// 二者与寿元公式一致：消耗天数越多，剩余寿元越少（消耗即减去对应寿元）。
function applyEarn(data) {
  if (data.spiritStone != null) {
    stats.value.spiritStone = data.spiritStone
    auth.setSpiritStone(data.spiritStone)
  }
  if (data.spiritQi != null) stats.value.spiritQi = data.spiritQi
  if (data.ageUsed != null) stats.value.ageUsed = data.ageUsed
  if (data.remainingLifespan != null) stats.value.remainingLifespan = data.remainingLifespan
}

function goBack() {
  router.back()
}

onMounted(() => {
  load()
  // 冷却倒计时
  cdTimer = setInterval(() => {
    if (meditateCd.value > 0) meditateCd.value--
    if (huntCd.value > 0) huntCd.value--
    if (mineCd.value > 0) mineCd.value--
  }, 1000)
})

onUnmounted(() => {
  if (cdTimer) clearInterval(cdTimer)
})
</script>

<style scoped>
.stage { height: 100%; overflow-y: auto; position: relative; }
.stats-shell {
  position: relative;
  max-width: 820px;
  margin: 0 auto;
  padding: 40px 24px 60px;
}
.stats-watermark {
  position: fixed; right: 4%; top: 2%;
  font-size: 180px; font-weight: 900;
  color: rgba(139, 109, 255, 0.05);
  pointer-events: none; user-select: none;
  font-family: "Noto Serif SC", serif;
}

.loading { text-align: center; padding: 120px 0; color: var(--ink-dim); }
.spinner {
  width: 42px; height: 42px; margin: 0 auto 16px;
  border: 2px solid rgba(103,232,249,.25); border-top-color: var(--qi);
  border-radius: 50%; animation: spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.panel {
  position: relative;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(20,29,54,.7), rgba(13,20,38,.55));
  padding: 28px;
  box-shadow: 0 20px 60px rgba(0,0,0,.4);
}

/* 头部 */
.head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.head-right { display: flex; flex-direction: column; align-items: flex-end; gap: 12px; }
.dao-name { font-size: 26px; color: var(--gold-2); letter-spacing: 2px; }
.dao-tag { font-size: 13px; color: var(--ink-dim); margin-left: 10px; letter-spacing: 1px; }
.realm-full { font-size: 18px; color: var(--qi); margin-top: 8px; letter-spacing: 1px; }
.realm-sub { font-size: 13px; color: var(--ink-dim); margin-top: 4px; }

.stone-box {
  display: inline-flex; align-items: baseline; gap: 6px;
  padding: 8px 16px; border-radius: 999px;
  border: 1px solid rgba(233, 200, 120, .35);
  background: rgba(233, 200, 120, .06);
}
.stone-icon {
  display: inline-flex; width: 22px; height: 22px; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 12px; align-self: center;
  background: linear-gradient(135deg, var(--gold-2), var(--gold));
  color: #0c1322; font-weight: 700;
}
.stone-val { font-size: 20px; font-weight: 700; color: var(--gold-2); font-family: "Noto Serif SC", serif; }
.stone-unit { font-size: 12px; color: var(--ink-dim); }

/* 资源区（灵石 + 灵气） */
.resource-box { display: flex; gap: 10px; align-items: center; }
.qi-box {
  display: inline-flex; align-items: baseline; gap: 6px;
  padding: 8px 16px; border-radius: 999px;
  border: 1px solid rgba(103, 232, 249, .35);
  background: rgba(103, 232, 249, .06);
}
.qi-icon {
  display: inline-flex; width: 22px; height: 22px; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 12px; align-self: center;
  background: linear-gradient(135deg, var(--qi), var(--violet));
  color: #0c1322; font-weight: 700;
}
.qi-val { font-size: 20px; font-weight: 700; color: var(--qi); font-family: "Noto Serif SC", serif; }
.qi-unit { font-size: 12px; color: var(--ink-dim); }

/* 寿元 / 天数条 */
.life-bar {
  display: flex; align-items: center; justify-content: space-between;
  margin: 18px 0 22px; padding: 10px 16px;
  border-radius: 10px; border: 1px solid var(--line);
  background: rgba(20, 29, 54, .45);
}
.life-label { font-size: 13px; color: var(--ink-dim); letter-spacing: 1px; }
.life-label em { color: var(--gold); font-style: normal; font-size: 18px; font-family: "Noto Serif SC", serif; }
.life-spent { font-size: 13px; color: var(--ink-dim); letter-spacing: 1px; }
.life-spent em { color: var(--qi); font-style: normal; font-size: 16px; }

.btn-up {
  flex-shrink: 0;
  padding: 10px 20px;
  border: 1px solid var(--gold); color: var(--gold);
  background: rgba(233,200,120,.08); border-radius: 8px;
  cursor: pointer; font-family: inherit; font-size: 14px; letter-spacing: 2px;
  transition: all .2s;
}
.btn-up:hover:not(:disabled) { background: rgba(233,200,120,.2); box-shadow: 0 0 18px rgba(233,200,120,.35); }
.btn-up:disabled { opacity: .45; cursor: not-allowed; }

/* 进度条 */
.progress-wrap { margin: 26px 0 30px; }
.progress-bar {
  height: 8px; border-radius: 4px;
  background: rgba(140,170,220,.12); overflow: hidden;
}
.progress-fill {
  height: 100%; border-radius: 4px;
  background: linear-gradient(90deg, var(--qi), var(--violet));
  transition: width .4s ease;
}
.progress-labels {
  display: flex; justify-content: space-between;
  margin-top: 8px; font-size: 11px; color: var(--ink-dim);
}

/* 突破费用提示 */
.cost-note {
  display: flex; align-items: baseline; gap: 8px; justify-content: flex-end;
  margin-top: -12px; margin-bottom: 20px;
  font-size: 13px; color: var(--ink-dim); flex-wrap: wrap;
}
.cost-label { letter-spacing: 1px; }
.cost-val { font-size: 18px; font-weight: 700; color: var(--gold-2); font-family: "Noto Serif SC", serif; }
.cost-val.qi { color: var(--qi); }
.cost-val.day { color: var(--violet); }
.cost-unit { font-size: 12px; }
.cost-sep { color: var(--ink-dim); opacity: .6; }
.cost-tag {
  padding: 2px 10px; border-radius: 999px; font-size: 12px; letter-spacing: 1px;
  border: 1px solid; margin-left: 4px;
}
.cost-tag[data-enough="true"] { color: var(--qi); border-color: rgba(103, 232, 249, .4); }
.cost-tag[data-enough="false"] { color: #ff8a8a; border-color: rgba(255, 138, 138, .4); }

/* 获取资源 */
.earn { margin-bottom: 30px; }
.earn-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.earn-card {
  border: 1px solid var(--line); border-radius: 12px;
  padding: 18px; background: rgba(20, 29, 54, .45);
}
.earn-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; gap: 8px; flex-wrap: wrap; }
.earn-name { font-size: 16px; color: var(--ink); letter-spacing: 2px; }
.earn-reward { font-size: 14px; font-weight: 700; color: var(--gold-2); font-family: "Noto Serif SC", serif; }
.earn-reward.qi-txt { color: var(--qi); }
.earn-reward.stone-txt { color: var(--gold-2); }
.earn-desc { font-size: 12px; color: var(--ink-dim); margin-bottom: 14px; }
.earn-btn {
  width: 100%; padding: 9px 0; border-radius: 8px; cursor: pointer;
  font-family: inherit; font-size: 14px; letter-spacing: 2px;
  border: 1px solid var(--qi); color: var(--qi); background: rgba(103, 232, 249, .06);
  transition: .2s;
}
.earn-btn:hover:not(:disabled) { background: rgba(103, 232, 249, .16); box-shadow: 0 0 14px rgba(103, 232, 249, .25); }
.earn-btn:disabled { opacity: .5; cursor: not-allowed; }

/* 标题 */
.sec-title { display: flex; align-items: center; gap: 12px; font-size: 16px; color: var(--ink); letter-spacing: 2px; margin-bottom: 16px; }
.orn { display: inline-block; width: 26px; height: 1px; background: linear-gradient(90deg, transparent, var(--gold)); }
.orn.r { background: linear-gradient(90deg, var(--gold), transparent); }

/* 五维卡片 */
.attr-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.attr-card {
  position: relative;
  border: 1px solid var(--line); border-radius: 10px;
  padding: 18px 12px; text-align: center;
  background: rgba(20,29,54,.5);
  transition: transform .2s, box-shadow .2s;
}
.attr-card:hover { transform: translateY(-3px); box-shadow: 0 10px 24px rgba(0,0,0,.35); }
.attr-icon {
  display: inline-flex; width: 38px; height: 38px; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 16px; margin-bottom: 10px;
  border: 1px solid var(--line); color: var(--qi);
}
.attr-name { font-size: 13px; color: var(--ink-dim); }
.attr-val { font-size: 26px; font-weight: 700; color: var(--gold-2); margin-top: 4px; font-family: "Noto Serif SC", serif; }
.attr-unit { font-size: 11px; color: var(--ink-dim); margin-top: 4px; }

/* 加成明细 */
.breakdown { margin-top: 34px; }
.src { border: 1px solid var(--line); border-radius: 10px; padding: 14px 16px; margin-bottom: 12px; background: rgba(13,20,38,.4); }
.src-head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 10px; }
.src-name { font-size: 15px; color: var(--ink); letter-spacing: 1px; }
.src-desc { font-size: 12px; color: var(--ink-dim); }
.src-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.src-cell { text-align: center; }
.src-k { display: block; font-size: 12px; color: var(--ink-dim); }
.src-v { display: block; font-size: 18px; color: var(--qi); margin-top: 2px; font-family: "Noto Serif SC", serif; }

.lifespan-note { margin-top: 22px; text-align: right; font-size: 13px; color: var(--ink-dim); }
.lifespan-note em { color: var(--gold); font-style: normal; font-size: 16px; }

.tip {
  margin-top: 16px; text-align: center; color: var(--qi);
  font-size: 14px; letter-spacing: 1px;
}

/* 响应式 */
@media (max-width: 640px) {
  .attr-grid { grid-template-columns: repeat(2, 1fr); }
  .src-row { grid-template-columns: repeat(2, 1fr); }
  .earn-grid { grid-template-columns: 1fr; }
  .resource-box { flex-direction: column; align-items: flex-end; }
}
</style>
