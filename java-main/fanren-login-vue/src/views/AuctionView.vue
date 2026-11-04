<template>
  <div class="auction-view">
    <!-- ===== 顶部标题 ===== -->
    <header class="auction-head">
      <div class="title-wrap">
        <span class="seal">拍</span>
        <div>
          <h1>拍卖行</h1>
          <p class="sub">每 10 分钟开启一场 · 在线修士同场竞拍 · 起拍 100 灵石</p>
        </div>
      </div>
      <div class="stone-chip" title="我的灵石">
        <i class="stone-dot">灵</i>{{ myStone }}<span class="stone-unit">灵石</span>
      </div>
    </header>

    <!-- ===== 等待 / 结束：开拍倒计时 ===== -->
    <section v-if="!isRunning" class="await">
      <div class="await-ring">
        <span class="await-num">{{ nextMin }}</span><span class="await-sep">:</span><span class="await-num">{{ nextSec }}</span>
      </div>
      <p class="await-tip">
        <template v-if="status?.state === 'ENDED'">本场已结束 · 距下一场开拍</template>
        <template v-else>距本场开拍</template>
      </p>

      <!-- 开拍前：参与报名 -->
      <div class="join-box">
        <button v-if="!participated" class="join-btn" @click="joinAuction">🙋 参与本场拍卖</button>
        <div v-else class="joined">
          <span class="joined-badge">✅ 已参与 · 共 {{ participantCount }} 人报名</span>
          <a class="joined-cancel" @click="leaveAuction">取消</a>
        </div>
        <p class="join-tip">开拍后会自动跳转到拍卖行并提醒你；也可在任意页面等待开拍通知。</p>
      </div>

      <p class="await-hint">开拍后将按品阶由低到高（黄→玄→地→天）依次拍卖 5 件，每次加价后停留 5 秒，无人再加价即归最高出价者。</p>
    </section>

    <!-- ===== 竞拍中 ===== -->
    <section v-else class="hall">
      <!-- 左：当前拍品 -->
      <div class="lot-card">
        <div class="lot-cover" @click="openDetail(currentLot)" :class="{ 'no-img': !coverOf(currentLot) }">
          <img v-if="coverOf(currentLot)" :src="coverOf(currentLot)" :alt="currentLot.name" />
          <span v-else class="cover-fallback">{{ currentLot.category.slice(0, 2) }}</span>
          <span class="cover-grad"></span>
          <span class="badge-type" :data-type="currentLot.type">{{ currentLot.type === 'manual' ? '功法' : '法宝' }}</span>
          <span class="grade-badge" :data-grade="currentLot.grade">{{ currentLot.grade }}阶</span>
        </div>

        <div class="lot-info">
          <p class="lot-cat">{{ currentLot.category }} · {{ currentLot.realm }}</p>
          <h2 class="lot-name" @click="openDetail(currentLot)">{{ currentLot.name }}</h2>
          <p class="lot-origin">出处 · {{ currentLot.origin }}</p>

          <div class="price-row">
            <div class="price-now">
              <span class="price-label">当前价</span>
              <span class="price-val">{{ currentLot.currentPrice }}</span>
              <span class="price-unit">灵石</span>
            </div>
            <div class="price-start">
              <span>起拍 100</span>
              <span class="muted">· 原售 {{ currentLot.price }}</span>
            </div>
          </div>

          <p class="leader" :class="{ me: isHighest }">
            <template v-if="!currentLot.highestBidder">尚无出价，等待应价……</template>
            <template v-else-if="isHighest">您当前领先（{{ currentLot.highestBidder }}）</template>
            <template v-else>最高出价：{{ currentLot.highestBidder }}</template>
          </p>

          <!-- 5 秒倒计时 -->
          <div class="timer">
            <div class="timer-bar"><span :style="{ width: timerPct + '%' }"></span></div>
            <span class="timer-num">{{ timerSec }}s</span>
          </div>

          <!-- 加价档位 -->
          <div class="bid-btns">
            <button v-for="inc in increments" :key="inc"
                    class="bid-btn"
                    :disabled="bidding || isHighest || !canAfford(currentLot.currentPrice + inc)"
                    @click="bid(inc)">
              +{{ inc }}
            </button>
          </div>
          <p v-if="isHighest" class="bid-hint">您已是最高出价者，等待他人加价或倒计时归零。</p>
          <p v-else-if="currentLot.currentPrice + 10 > myStone" class="bid-hint warn">灵石不足以加价，请先去「修炼 / 挖矿 / 除妖」攒灵石。</p>
        </div>
      </div>

      <!-- 右：出价记录 + 本场队列 -->
      <aside class="side">
        <div class="panel log-panel">
          <h3 class="panel-title"><i class="orn"></i>出价记录<i class="orn r"></i></h3>
          <div class="log-list">
            <p v-if="!currentLot.bidLog?.length" class="log-empty">暂无出价，快来应价！</p>
            <div v-for="(e, i) in currentLot.bidLog" :key="i" class="log-item" :class="{ me: e.bidder === auth.user }">
              <span class="log-bidder">{{ e.bidder === auth.user ? '你' : e.bidder }}</span>
              <span class="log-act">加价 +{{ e.increment }}，当前 <b>{{ e.amount }}</b> 灵石</span>
              <span class="log-time">{{ fmtTime(e.time) }}</span>
            </div>
          </div>
        </div>

        <div class="panel queue-panel">
          <h3 class="panel-title"><i class="orn"></i>本场队列（{{ queue.length }}）<i class="orn r"></i></h3>
          <p v-if="!queue.length" class="log-empty">已是本场最后一件。</p>
          <ul class="queue-list">
            <li v-for="(q, i) in queue" :key="q.type + q.id" class="queue-item" @click="openDetail(q)">
              <span class="q-order">{{ i + 2 }}</span>
              <span class="q-grade" :data-grade="q.grade">{{ q.grade }}</span>
              <span class="q-name">{{ q.name }}</span>
              <span class="q-type" :data-type="q.type">{{ q.type === 'manual' ? '功法' : '法宝' }}</span>
            </li>
          </ul>
        </div>
      </aside>
    </section>

    <!-- ===== 物品属性弹窗 ===== -->
    <transition name="fade">
      <div v-if="detailItem" class="modal-mask" @click.self="closeDetail">
        <div class="modal">
          <button class="modal-close" @click="closeDetail">✕</button>
          <div class="modal-top">
            <div class="modal-cover" :class="{ 'no-img': !coverOf(detailItem) }">
              <img v-if="coverOf(detailItem)" :src="coverOf(detailItem)" :alt="detailItem.name" />
              <span v-else class="cover-fallback">{{ detailItem.category.slice(0, 2) }}</span>
              <span class="grade-badge" :data-grade="detailItem.grade">{{ detailItem.grade }}阶</span>
            </div>
            <div class="modal-info">
              <p class="modal-cat">{{ detailItem.category }} · {{ detailItem.realm }}</p>
              <h2 class="modal-name">{{ detailItem.name }}</h2>
              <div class="modal-tags">
                <span class="tag origin">出处 · {{ detailItem.origin }}</span>
                <span class="tag price">原售 {{ detailItem.price }} 灵石</span>
              </div>
              <p class="modal-desc">{{ detailItem.desc }}</p>
            </div>
          </div>

          <div v-if="hasBonus(detailItem)" class="bonus-block">
            <h3 class="bonus-title"><i class="orn"></i>修成加成<i class="orn r"></i></h3>
            <div class="bonus-grid">
              <div v-for="b in bonusList(detailItem)" :key="b.key" class="bonus-cell" :data-zero="b.value === 0">
                <span class="bonus-icon">{{ b.icon }}</span>
                <span class="bonus-k">{{ b.name }}</span>
                <span class="bonus-v">{{ b.value > 0 ? '+' + b.value : '—' }}</span>
                <span class="bonus-unit">{{ b.unit }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- ===== Toast ===== -->
    <transition name="toast">
      <div v-if="toastVisible" class="toast">{{ toastMsg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { fetchAuctionStatus, placeBid, participate } from '../api/auction'

const auth = useAuthStore()
const API_BASE = import.meta.env.VITE_API_BASE || ''

const status = ref(null)
const now = ref(Date.now())
const bidding = ref(false)
const detailItem = ref(null)
const toastMsg = ref('')
const toastVisible = ref(false)

// 参与本场拍卖（开拍前报名）
const participated = ref(false)
const participantCount = ref(0)

const increments = [10, 50, 100, 500]

// 轮询 / 时钟
let pollTimer = null
let clockTimer = null
let firstLoad = true
let prevOpen = -1
let prevSeq = -1
let toastTimer = null

const currentLot = computed(() => status.value?.currentLot || null)
const queue = computed(() => status.value?.queue || [])
const isRunning = computed(() => status.value?.state === 'RUNNING' && !!currentLot.value)
const myStone = computed(() => {
  const s = status.value?.yourStone
  return s != null ? s : (auth.spiritStone || 0)
})
const isHighest = computed(() => !!currentLot.value && currentLot.value.highestBidder === auth.user)

const timerSec = computed(() => {
  if (!currentLot.value) return 0
  return Math.max(0, Math.ceil((currentLot.value.lotEndsAt - now.value) / 1000))
})
const timerPct = computed(() => {
  if (!currentLot.value) return 0
  const remain = Math.max(0, currentLot.value.lotEndsAt - now.value)
  return Math.min(100, (remain / 5000) * 100)
})

// 下一场倒计时
const nextMin = computed(() => {
  const sec = Math.max(0, Math.floor((status.value?.nextOpenAt - now.value) / 1000))
  return String(Math.floor(sec / 60)).padStart(2, '0')
})
const nextSec = computed(() => {
  const sec = Math.max(0, Math.floor((status.value?.nextOpenAt - now.value) / 1000))
  return String(sec % 60).padStart(2, '0')
})

function coverOf(it) {
  if (!it || !it.image) return ''
  return it.image.startsWith('http') ? it.image : API_BASE + it.image
}

function canAfford(amount) {
  return myStone.value >= amount
}

function fmtTime(ms) {
  const d = new Date(ms)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

async function load() {
  try {
    const s = await fetchAuctionStatus()
    status.value = s
    if (s.yourStone != null) auth.setSpiritStone(s.yourStone)
    if (s.participated != null) participated.value = s.participated
    if (s.participantCount != null) participantCount.value = s.participantCount
    // 开拍通知
    if (firstLoad) {
      prevOpen = s.openCount
      prevSeq = s.lastResult ? s.lastResult.seq : -1
      firstLoad = false
    } else {
      if (s.openCount > prevOpen) {
        prevOpen = s.openCount
        showToast('🔔 拍卖行已开启！本场拍品按品阶由低到高依次竞拍')
      }
      if (s.lastResult && s.lastResult.seq !== prevSeq) {
        prevSeq = s.lastResult.seq
        const r = s.lastResult
        if (r.passed) showToast(`《${r.lotName}》流拍，无人出价`)
        else if (r.winner === auth.user) showToast(`🎉 你以 ${r.price} 灵石拍得《${r.lotName}》！`)
        else showToast(`《${r.lotName}》被 ${r.winner} 以 ${r.price} 灵石拍得`)
      }
    }
  } catch (_) { /* 后端未连，静默 */ }
}

async function bid(inc) {
  if (!currentLot.value || bidding.value || isHighest.value) return
  const lot = currentLot.value
  const amount = lot.currentPrice + inc
  if (!canAfford(amount)) {
    showToast('灵石不足，无法加价')
    return
  }
  bidding.value = true
  try {
    const data = await placeBid(lot.type, lot.id, amount)
    status.value = data
    if (data.yourStone != null) auth.setSpiritStone(data.yourStone)
  } catch (e) {
    showToast(e.message || '出价失败')
  } finally {
    bidding.value = false
  }
}

function openDetail(item) { detailItem.value = item }
function closeDetail() { detailItem.value = null }

// ===== 参与 / 取消参与本场拍卖 =====
async function joinAuction() {
  try {
    const r = await participate(true)
    participated.value = r.participated
    participantCount.value = r.participantCount
    localStorage.setItem('auction_participated', 'true')
    showToast('已报名本场拍卖，开拍会自动提醒你')
  } catch (_) { showToast('报名失败，请稍后再试') }
}
async function leaveAuction() {
  try {
    const r = await participate(false)
    participated.value = r.participated
    participantCount.value = r.participantCount
    localStorage.setItem('auction_participated', 'false')
  } catch (_) { /* 忽略 */ }
}

function showToast(msg) {
  toastMsg.value = msg
  toastVisible.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastVisible.value = false }, 4200)
}

// ===== 属性弹窗数值 =====
function bonusList(it) {
  if (!it) return []
  return [
    { key: 'hp', name: '生命', icon: '命', value: num(it.bonusHp), unit: '' },
    { key: 'lifespan', name: '寿元', icon: '寿', value: num(it.bonusLifespan), unit: '年' },
    { key: 'atk', name: '攻击', icon: '攻', value: num(it.bonusAtk), unit: '' },
    { key: 'def', name: '防御', icon: '防', value: num(it.bonusDef), unit: '' },
    { key: 'spd', name: '速度', icon: '速', value: num(it.bonusSpd), unit: '' }
  ]
}
function hasBonus(it) { return bonusList(it).some((b) => b.value > 0) }
function num(v) { return v == null ? 0 : Number(v) || 0 }

onMounted(() => {
  load()
  pollTimer = setInterval(load, 1500)
  clockTimer = setInterval(() => { now.value = Date.now() }, 200)
})
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (clockTimer) clearInterval(clockTimer)
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<style scoped>
.auction-view {
  position: relative; z-index: 2;
  height: 100%; overflow-y: auto; overflow-x: hidden;
  padding: 22px 28px 40px;
  scroll-behavior: smooth;
}
.auction-view::-webkit-scrollbar { width: 8px; }
.auction-view::-webkit-scrollbar-thumb { border-radius: 8px; background: linear-gradient(180deg, rgba(233,200,120,.5), rgba(233,200,120,.2)); }

/* 顶部 */
.auction-head {
  display: flex; align-items: center; justify-content: space-between;
  max-width: 1100px; margin: 0 auto 22px;
}
.title-wrap { display: flex; align-items: center; gap: 14px; }
.seal {
  width: 46px; height: 46px; border-radius: 12px; display: grid; place-items: center;
  background: linear-gradient(145deg, var(--gold), #b88a3c); color: #1a1206;
  font-weight: 700; font-size: 22px; box-shadow: 0 0 18px rgba(233,200,120,.4);
}
.auction-head h1 {
  font-size: 26px; letter-spacing: 6px; margin: 0;
  background: linear-gradient(180deg,#fff7e6,var(--gold) 60%,#b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.auction-head .sub { margin: 4px 0 0; font-size: 13px; letter-spacing: 1px; color: var(--ink-dim); }
.stone-chip {
  display: flex; align-items: center; gap: 6px; padding: 7px 14px; border-radius: 999px;
  border: 1px solid rgba(233,200,120,.35); color: var(--gold-2); font-weight: 700; font-size: 16px;
  background: rgba(233,200,120,.06);
}
.stone-dot { width: 18px; height: 18px; border-radius: 50%; display: grid; place-items: center;
  background: linear-gradient(135deg,var(--gold-2),var(--gold)); color: #0c1322; font-size: 11px; font-weight: 700; }
.stone-unit { font-size: 12px; color: var(--ink-dim); font-weight: 400; margin-left: 2px; }

/* 等待态 */
.await { text-align: center; padding: 70px 20px; color: var(--ink-dim); }
.await-ring {
  display: inline-flex; align-items: baseline; gap: 4px; margin-bottom: 14px;
  font-family: "Noto Serif SC", serif; font-size: 64px; font-weight: 700; letter-spacing: 2px;
  background: linear-gradient(180deg,#fff7e6,var(--gold) 60%,#b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.await-sep { opacity: .5; }
.await-tip { font-size: 16px; letter-spacing: 3px; color: var(--ink); margin-bottom: 8px; }

/* 参与报名 */
.join-box { margin: 18px auto 6px; }
.join-btn {
  padding: 13px 38px; border-radius: 999px; cursor: pointer; font-family: inherit;
  font-size: 17px; font-weight: 700; letter-spacing: 2px; color: #0c1322;
  background: linear-gradient(135deg, var(--qi), #59c6ff); border: none;
  box-shadow: 0 0 22px rgba(103,232,249,.4); transition: .15s;
}
.join-btn:hover { transform: translateY(-2px); box-shadow: 0 0 30px rgba(103,232,249,.65); }
.joined { display: inline-flex; align-items: center; gap: 12px; padding: 10px 20px; border-radius: 999px;
  border: 1px solid rgba(233,200,120,.45); background: rgba(233,200,120,.1); }
.joined-badge { font-size: 15px; font-weight: 700; letter-spacing: 1px; color: var(--gold-2); }
.joined-cancel { font-size: 13px; color: var(--ink-dim); cursor: pointer; letter-spacing: 1px; text-decoration: underline; }
.joined-cancel:hover { color: var(--gold); }
.join-tip { font-size: 12px; color: var(--ink-dim); letter-spacing: 1px; margin: 10px 0 0; }

.await-hint { font-size: 13px; letter-spacing: 1px; opacity: .8; max-width: 560px; margin: 14px auto 0; line-height: 1.8; }

/* 大厅 */
.hall {
  display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(280px, 1fr); gap: 22px;
  max-width: 1100px; margin: 0 auto; align-items: start;
}

/* 当前拍品卡 */
.lot-card {
  display: flex; gap: 22px; padding: 22px; border-radius: 18px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(20,29,54,.55), rgba(7,10,20,.65));
  backdrop-filter: blur(6px);
}
.lot-cover {
  position: relative; width: 200px; height: 264px; flex: none; border-radius: 14px; overflow: hidden;
  border: 1px solid var(--line); background: rgba(20,29,54,.6); cursor: pointer;
}
.lot-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.lot-cover.no-img { display: grid; place-items: center;
  background: radial-gradient(70% 90% at 50% 110%, rgba(233,200,120,.16), transparent 70%), linear-gradient(180deg, rgba(20,29,54,.9), rgba(7,10,20,.9)); }
.cover-fallback { font-size: 52px; font-weight: 700; letter-spacing: 6px; text-indent: 6px;
  background: linear-gradient(180deg,#fff7e6,var(--gold) 60%,#b98e34); -webkit-background-clip: text; background-clip: text; color: transparent; opacity: .85; }
.cover-grad { position: absolute; inset: 0; pointer-events: none; background: linear-gradient(180deg, transparent 50%, rgba(7,10,20,.6)); }
.badge-type {
  position: absolute; left: 10px; top: 10px; z-index: 2; padding: 3px 10px; border-radius: 8px;
  font-size: 13px; letter-spacing: 1px; font-weight: 700; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold));
}
.badge-type[data-type="treasure"] { background: linear-gradient(135deg, #8be6f6, var(--qi)); }
.grade-badge {
  position: absolute; right: 10px; bottom: 10px; z-index: 2; padding: 4px 11px; border-radius: 8px;
  font-weight: 700; font-size: 14px; letter-spacing: 1px; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold));
}
.grade-badge[data-grade="天"] { color: #0c1322; background: linear-gradient(135deg, var(--gold-2), var(--gold)); }
.grade-badge[data-grade="地"] { color: #fff; background: rgba(139,109,255,.85); }
.grade-badge[data-grade="玄"] { color: #062024; background: linear-gradient(135deg, #8be6f6, var(--qi)); }
.grade-badge[data-grade="黄"] { color: var(--ink-dim); background: rgba(139,155,189,.25); border: 1px solid var(--line); }

.lot-info { flex: 1; min-width: 0; }
.lot-cat { color: var(--gold); font-size: 13px; letter-spacing: 3px; margin: 0 0 6px; }
.lot-name { font-size: 30px; letter-spacing: 3px; margin: 0 0 8px; cursor: pointer;
  background: linear-gradient(180deg,#fff7e6,var(--gold) 60%,#b98e34); -webkit-background-clip: text; background-clip: text; color: transparent; }
.lot-origin { font-size: 13px; color: var(--ink-dim); margin: 0 0 16px; letter-spacing: 1px; }

.price-row { display: flex; align-items: flex-end; gap: 18px; margin-bottom: 12px; flex-wrap: wrap; }
.price-now { display: flex; align-items: baseline; gap: 8px; }
.price-label { font-size: 13px; color: var(--ink-dim); letter-spacing: 2px; }
.price-val { font-size: 34px; font-weight: 700; color: var(--gold-2); font-family: "Noto Serif SC", serif;
  filter: drop-shadow(0 0 16px rgba(233,200,120,.25)); }
.price-unit { font-size: 13px; color: var(--ink-dim); }
.price-start { font-size: 13px; color: var(--gold); letter-spacing: 1px; }
.price-start .muted { color: var(--ink-dim); }

.leader { font-size: 14px; letter-spacing: 1px; color: var(--qi); margin: 0 0 12px; }
.leader.me { color: var(--gold-2); font-weight: 700; }

.timer { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.timer-bar { flex: 1; height: 8px; border-radius: 999px; background: rgba(7,10,20,.6); overflow: hidden; border: 1px solid var(--line); }
.timer-bar span { display: block; height: 100%; border-radius: 999px;
  background: linear-gradient(90deg, var(--gold-2), var(--gold)); transition: width .2s linear;
  box-shadow: 0 0 10px rgba(233,200,120,.5); }
.timer-num { font-size: 15px; font-weight: 700; color: var(--gold-2); min-width: 34px; text-align: right; }

.bid-btns { display: flex; gap: 10px; flex-wrap: wrap; }
.bid-btn {
  flex: 1; min-width: 76px; padding: 12px 0; border-radius: 12px; cursor: pointer;
  font-family: inherit; font-size: 18px; font-weight: 700; letter-spacing: 1px; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold)); border: none;
  transition: .15s;
}
.bid-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 0 20px rgba(233,200,120,.5); }
.bid-btn:disabled { opacity: .4; cursor: not-allowed; }
.bid-hint { font-size: 12px; color: var(--ink-dim); margin: 10px 0 0; letter-spacing: 1px; }
.bid-hint.warn { color: #ffb36b; }

/* 侧栏 */
.side { display: flex; flex-direction: column; gap: 22px; }
.panel { padding: 18px; border-radius: 16px; border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(20,29,54,.5), rgba(7,10,20,.6)); }
.panel-title { display: flex; align-items: center; gap: 14px; justify-content: center;
  color: var(--ink); font-size: 16px; letter-spacing: 4px; font-weight: 700; margin: 0 0 14px; }
.orn { position: relative; flex: none; width: 70px; height: 1px; }
.orn::before { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg, transparent, rgba(233,200,120,.6)); }
.orn::after { content: ''; position: absolute; right: -4px; top: -3px; width: 7px; height: 7px; transform: rotate(45deg);
  background: var(--gold); box-shadow: 0 0 8px rgba(233,200,120,.6); }
.orn.r::before { background: linear-gradient(90deg, rgba(233,200,120,.6), transparent); }
.orn.r::after { right: auto; left: -4px; }

.log-list { max-height: 260px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.log-list::-webkit-scrollbar { width: 6px; }
.log-list::-webkit-scrollbar-thumb { border-radius: 6px; background: rgba(233,200,120,.3); }
.log-empty { font-size: 13px; color: var(--ink-dim); text-align: center; padding: 14px 0; letter-spacing: 1px; }
.log-item { display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 10px;
  border: 1px solid var(--line); background: rgba(7,10,20,.4); font-size: 13px; }
.log-item.me { border-color: rgba(233,200,120,.45); background: rgba(233,200,120,.08); }
.log-bidder { font-weight: 700; color: var(--gold-2); flex: none; }
.log-item.me .log-bidder { color: var(--gold); }
.log-act { color: var(--ink-dim); flex: 1; }
.log-act b { color: var(--ink); }
.log-time { color: var(--ink-dim); font-size: 11px; flex: none; opacity: .8; }

.queue-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.queue-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 10px;
  border: 1px solid var(--line); background: rgba(7,10,20,.4); cursor: pointer; transition: .15s; }
.queue-item:hover { border-color: rgba(233,200,120,.35); transform: translateX(2px); }
.q-order { width: 20px; height: 20px; border-radius: 50%; display: grid; place-items: center;
  font-size: 12px; color: var(--gold-2); border: 1px solid rgba(233,200,120,.4); flex: none; }
.q-grade { font-size: 12px; font-weight: 700; padding: 2px 8px; border-radius: 6px; flex: none; }
.q-grade[data-grade="天"] { color: #0c1322; background: linear-gradient(135deg,var(--gold-2),var(--gold)); }
.q-grade[data-grade="地"] { color: #fff; background: rgba(139,109,255,.85); }
.q-grade[data-grade="玄"] { color: #062024; background: linear-gradient(135deg,#8be6f6,var(--qi)); }
.q-grade[data-grade="黄"] { color: var(--ink-dim); background: rgba(139,155,189,.25); border: 1px solid var(--line); }
.q-name { flex: 1; font-size: 14px; color: var(--ink); letter-spacing: 1px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.q-type { font-size: 11px; padding: 2px 8px; border-radius: 6px; flex: none; color: #0c1322;
  background: linear-gradient(135deg,var(--gold-2),var(--gold)); }
.q-type[data-type="treasure"] { background: linear-gradient(135deg,#8be6f6,var(--qi)); }

/* 弹窗 */
.modal-mask { position: fixed; inset: 0; z-index: 50; display: grid; place-items: center;
  background: rgba(3,5,12,.72); backdrop-filter: blur(3px); padding: 20px; }
.modal { position: relative; width: min(560px, 94vw); max-height: 88vh; overflow-y: auto;
  padding: 26px; border-radius: 18px; border: 1px solid rgba(233,200,120,.3);
  background: linear-gradient(180deg, rgba(20,29,54,.96), rgba(7,10,20,.98)); }
.modal-close { position: absolute; right: 14px; top: 14px; width: 30px; height: 30px; border-radius: 50%;
  border: 1px solid var(--line); background: transparent; color: var(--ink-dim); cursor: pointer; font-size: 14px; }
.modal-close:hover { color: var(--gold); border-color: rgba(233,200,120,.4); }
.modal-top { display: flex; gap: 18px; }
.modal-cover { position: relative; width: 140px; height: 184px; flex: none; border-radius: 12px; overflow: hidden;
  border: 1px solid var(--line); background: rgba(20,29,54,.6); }
.modal-cover img { width: 100%; height: 100%; object-fit: cover; }
.modal-cover.no-img { display: grid; place-items: center;
  background: radial-gradient(70% 90% at 50% 110%, rgba(233,200,120,.16), transparent 70%), linear-gradient(180deg, rgba(20,29,54,.9), rgba(7,10,20,.9)); }
.modal-info { flex: 1; min-width: 0; }
.modal-cat { color: var(--gold); font-size: 13px; letter-spacing: 3px; margin: 0 0 6px; }
.modal-name { font-size: 26px; letter-spacing: 2px; margin: 0 0 12px;
  background: linear-gradient(180deg,#fff7e6,var(--gold) 60%,#b98e34); -webkit-background-clip: text; background-clip: text; color: transparent; }
.modal-tags { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.tag { padding: 4px 12px; border-radius: 999px; font-size: 12px; letter-spacing: 1px; border: 1px solid var(--line); color: var(--ink-dim); }
.tag.origin { color: var(--gold); border-color: rgba(233,200,120,.3); }
.tag.price { color: var(--gold-2); border-color: rgba(233,200,120,.45); font-weight: 700; }
.modal-desc { font-size: 14px; line-height: 1.9; color: var(--ink-dim); }

.bonus-block { margin-top: 20px; padding-top: 18px; border-top: 1px dashed rgba(233,200,120,.2); }
.bonus-title { display: flex; align-items: center; gap: 14px; justify-content: center;
  color: var(--gold); font-size: 15px; letter-spacing: 3px; font-weight: 700; margin: 0 0 16px; }
.bonus-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; }
.bonus-cell { display: flex; flex-direction: column; align-items: center; gap: 3px; padding: 12px 6px; border-radius: 12px;
  border: 1px solid rgba(103,232,249,.18); background: radial-gradient(120% 120% at 50% 0%, rgba(103,232,249,.08), rgba(7,10,20,.4)); }
.bonus-cell[data-zero="true"] { opacity: .45; }
.bonus-icon { display: inline-flex; width: 28px; height: 28px; align-items: center; justify-content: center; border-radius: 50%;
  font-size: 13px; border: 1px solid rgba(103,232,249,.3); color: var(--qi); }
.bonus-k { font-size: 11px; color: var(--ink-dim); }
.bonus-v { font-size: 18px; font-weight: 700; color: var(--gold-2); }
.bonus-unit { font-size: 10px; color: var(--ink-dim); }

/* Toast */
.toast { position: fixed; left: 50%; top: 84px; transform: translateX(-50%); z-index: 60;
  padding: 12px 24px; border-radius: 999px; font-size: 15px; letter-spacing: 1px; color: #0c1322; font-weight: 700;
  background: linear-gradient(135deg, var(--gold-2), var(--gold)); box-shadow: 0 8px 30px rgba(233,200,120,.45);
  max-width: 90vw; }
.toast-enter-active, .toast-leave-active { transition: all .35s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, -16px); }
.fade-enter-active, .fade-leave-active { transition: opacity .25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 移动端 */
@media (max-width: 880px) {
  .hall { grid-template-columns: 1fr; }
  .lot-card { flex-direction: column; }
  .lot-cover { width: 100%; height: 220px; }
}
@media (max-width: 760px) {
  .auction-view { padding: 16px 14px 32px; }
  .bonus-grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
