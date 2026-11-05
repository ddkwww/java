<template>
  <div class="mp-wrap">
    <header class="mp-head">
      <h1>大富翁 · 灵石夺宝</h1>
      <div class="mp-head-right">
        <div class="mp-pool">
          <span class="pool-dot">灵</span>
          奖池 {{ state.pool }} 灵石 · 报名费 {{ state.entryFee }}
        </div>
        <button v-if="state.state === 'PLAYING'" class="btn-danger" @click="doEnd">结束本局</button>
      </div>
    </header>

    <!-- ===== 等待 / 报名阶段 ===== -->
    <section v-if="state.state === 'WAITING'" class="mp-lobby">
      <div class="lobby-tip">
        需 <b>4 名真实道友</b> 报名才能开局。报名即缴纳 <b>{{ state.entryFee }}</b> 灵石进入奖池，
        开局前可随时退出退回。
      </div>
      <div class="seat-row">
        <div v-for="(p, i) in state.players" :key="i" class="seat" :class="{ filled: p.username, me: p.isYou }">
          <div class="seat-avatar" :style="avatarStyle(i)">{{ avatarOf(i) }}</div>
          <div v-if="p.username" class="seat-name">{{ p.username }}</div>
          <div v-else class="seat-empty">虚位以待</div>
          <div v-if="p.username" class="seat-sub">{{ p.realm }}</div>
        </div>
      </div>

      <div class="lobby-actions">
        <button v-if="!you.joined" class="btn-primary" :disabled="myStone < state.entryFee" @click="doJoin">
          {{ myStone < state.entryFee ? '灵石不足' : '报名入桌' }}
        </button>
        <template v-else>
          <span class="joined-tag">已报名 · 座位 {{ you.seat + 1 }}</span>
          <button class="btn-ghost" @click="doLeave">退出退回灵石</button>
        </template>
        <button v-if="you.joined && seatCount === 4" class="btn-gold" @click="doStart">开局（4 人已齐）</button>
      </div>
      <p v-if="seatCount < 4" class="wait-hint">还差 {{ 4 - seatCount }} 人报名……可召唤道友一同前来。</p>
    </section>

    <!-- ===== 进行中 ===== -->
    <section v-else-if="state.state === 'PLAYING'" class="mp-play">
      <!-- 平面棋盘 -->
      <div class="board">
        <div
          v-for="t in state.board"
          :key="t.index"
          class="tile"
          :class="['t-' + t.type, { 'mine-own': t.owner === myName, 'mine-other': t.owner && t.owner !== myName }]"
          :style="tileStyle(t.index)"
        >
          <span class="t-icon">{{ tileIcon(t.type) }}</span>
          <span class="t-name">{{ t.name }}</span>
          <template v-if="t.property">
            <span class="t-prop">
              <span v-if="t.owner" class="t-owner">{{ t.owner }}</span>
              <span v-else class="t-free">未占领</span>
              <span v-if="t.owner" class="t-lv">Lv.{{ t.level }}</span>
            </span>
            <span v-if="t.owner" class="t-rent">租 {{ t.rent }}</span>
            <span v-else class="t-price">{{ t.price }} 灵石</span>
          </template>
          <span v-else class="t-desc">{{ shortDesc(t.type) }}</span>
          <span class="t-pawns">
            <span v-for="(p, i) in playersOnTile(t.index)" :key="i" class="pawn" :style="pawnStyle(p.seat)">
              {{ avatarOf(p.seat) }}
            </span>
          </span>
        </div>

        <!-- 中央面板：骰子 / 回合 / 按钮 -->
        <div class="board-center">
          <div class="ct-title">灵石夺宝</div>
          <div class="ct-pool">奖池 <b>{{ state.pool }}</b></div>
          <div class="ct-turn">
            <span class="ct-turn-avatar" :style="avatarStyle(state.currentSeat)">{{ avatarOf(state.currentSeat) }}</span>
            <span class="ct-turn-name">{{ currentName }}</span>
            <span v-if="you.isMyTurn" class="ct-turn-you">轮到你了</span>
            <span v-else class="ct-turn-wait">等待中…</span>
          </div>

          <div class="dice" :class="{ rolling: rolling }">
            <template v-if="diceFace >= 1">
              <span v-for="i in 9" :key="i" class="pip-slot">
                <span v-if="pips(diceFace).includes(i - 1)" class="pip"></span>
              </span>
            </template>
            <span v-else class="dice-q">?</span>
          </div>

          <button class="btn-gold btn-roll" :disabled="!you.canRoll || rolling" @click="doRoll">
            {{ you.isMyTurn ? '掷 骰 子' : '等待他人行动…' }}
          </button>
          <button class="btn-danger-outline" @click="doEnd">结束本局（提前结算）</button>

          <div class="ct-progress">已掷 {{ state.rollsDone }} / {{ state.maxRolls }} 次</div>
          <div v-if="state.lastEvent" class="ct-event" :class="state.lastEvent.delta >= 0 ? 'up' : 'down'">
            {{ state.lastEvent.text }}
          </div>
        </div>
      </div>

      <!-- 玩家资产条 -->
      <div class="players-bar">
        <div
          v-for="(p, i) in state.players"
          :key="i"
          class="pb"
          :class="{ me: p.isYou, cur: i === state.currentSeat, empty: !p.username }"
        >
          <span class="pb-avatar" :style="avatarStyle(i)">{{ avatarOf(i) }}</span>
          <span class="pb-name">{{ p.username || '—' }}</span>
          <span class="pb-stone">🪙 {{ p.gameStone }}</span>
          <span class="pb-own" v-if="p.username">矿 {{ ownedCount(p.username) }}</span>
        </div>
      </div>

      <!-- 日志 -->
      <div class="log-box">
        <div class="log-title">夺宝录</div>
        <div class="log-scroll">
          <p v-for="(l, i) in reversedLog" :key="i" class="log-line">
            <b>{{ l.username }}</b> {{ l.text }}
          </p>
        </div>
      </div>
    </section>

    <!-- ===== 结算 ===== -->
    <section v-else class="mp-end">
      <h2 class="end-title">灵石夺宝 · 终局</h2>
      <div class="rank-list">
        <div v-for="r in state.result" :key="r.username" class="rank-row" :class="'rk-' + r.rank">
          <span class="rk-no">{{ r.rank }}</span>
          <span class="rk-name">{{ r.username }}</span>
          <span class="rk-stone">局内 {{ r.gameStone }} 灵石</span>
          <span class="rk-pay">发放 {{ r.payout }}</span>
          <span class="rk-net" :class="r.net >= 0 ? 'up' : 'down'">
            {{ r.net >= 0 ? '+' : '' }}{{ r.net }} 灵石
          </span>
        </div>
      </div>
      <button class="btn-gold" @click="doReset">开启下一局</button>
    </section>

    <!-- ===== 占领 / 升级 选择弹窗 ===== -->
    <transition name="modal">
      <div v-if="state.pending && state.pending.isYou" class="decision-mask">
        <div class="decision-card">
          <div class="dc-icon" v-if="state.pending.type === 'OCCUPY'">⛏️</div>
          <div class="dc-icon" v-else>⬆️</div>
          <h3 class="dc-title" v-if="state.pending.type === 'OCCUPY'">
            是否占领「{{ state.pending.tileName }}」？
          </h3>
          <h3 class="dc-title" v-else>
            升级「{{ state.pending.tileName }}」？
          </h3>
          <p class="dc-desc" v-if="state.pending.type === 'OCCUPY'">
            占领需花费 <b>{{ state.pending.price }}</b> 局内灵石；
            此后其他道友落上此矿需支付租金 <b>{{ state.pending.rent }}</b>。
          </p>
          <p class="dc-desc" v-else>
            当前 Lv.{{ state.pending.level }}，升级需 <b>{{ state.pending.cost }}</b> 局内灵石，
            租金将提升至 <b>{{ state.pending.rent }}</b>。
          </p>
          <div class="dc-actions">
            <button v-if="state.pending.type === 'OCCUPY'" class="btn-gold" @click="doDecide('occupy')">占 领</button>
            <button v-else class="btn-gold" @click="doDecide('upgrade')">升 级</button>
            <button class="btn-ghost" @click="doDecide('skip')">{{ state.pending.type === 'OCCUPY' ? '放 弃' : '跳 过' }}</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="toast">
      <div v-if="toast.visible" class="g-toast" :class="toast.type">{{ toast.msg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { fetchGameState, joinGame, leaveGame, startGame, rollDice, resetGame, decideGame, endGame } from '../api/game'

const auth = useAuthStore()
const state = reactive({
  state: 'WAITING', entryFee: 10, pool: 0, currentSeat: 0,
  rollsDone: 0, maxRolls: 48, board: [], players: [], dice: null,
  lastEvent: null, log: [], you: { joined: false, seat: -1, isMyTurn: false, canRoll: false, canDecide: false },
  result: null, pending: null
})
const myStone = ref(0)
const rolling = ref(false)
const diceFace = ref(0)

// 人物形象（按座位）与配色
const AVATARS = ['🧙', '🧛', '🧝', '🥷']
const AVATAR_COLORS = ['#4cc9f0', '#b388ff', '#69db7c', '#ffb84d']
const TILE_ICONS = { START: '🏁', MINE: '⛏️', FORTUNE: '🎲', REST: '🏡', MARKET: '🛒', THUNDER: '⚡', TRIBUTE: '💰', SECRET: '🌀', PAUSE: '🧘' }

// 6×6 平面棋盘：20 格顺时针绕一圈（GO 在右下角）
const BOARD_COORDS = (() => {
  const m = new Array(20)
  for (let i = 0; i <= 5; i++) m[i] = { r: 5, c: 5 - i }         // 底行 5→0
  for (let i = 0; i < 5; i++) m[6 + i] = { r: 4 - i, c: 0 }       // 左列 4→0
  for (let i = 0; i < 5; i++) m[11 + i] = { r: 0, c: 1 + i }      // 顶行 1→5
  for (let i = 0; i < 4; i++) m[16 + i] = { r: 1 + i, c: 5 }      // 右列 1→4
  return m
})()

function tileStyle(index) {
  const c = BOARD_COORDS[index] || { r: 0, c: 0 }
  return { gridRow: c.r + 1, gridColumn: c.c + 1 }
}
function avatarOf(seat) { return AVATARS[seat % 4] }
function avatarStyle(seat) {
  return { borderColor: AVATAR_COLORS[seat % 4], color: AVATAR_COLORS[seat % 4] }
}
function pawnStyle(seat) {
  return { background: AVATAR_COLORS[seat % 4] + '33', borderColor: AVATAR_COLORS[seat % 4] }
}
function tileIcon(type) { return TILE_ICONS[type] || '•' }
function shortDesc(type) {
  const map = { FORTUNE: '机缘', REST: '安然', MARKET: '交易', THUNDER: '天劫', TRIBUTE: '灵石税', SECRET: '秘境', PAUSE: '闭关', START: '起点' }
  return map[type] || ''
}
function pips(n) {
  const map = { 1: [4], 2: [0, 8], 3: [0, 4, 8], 4: [0, 2, 6, 8], 5: [0, 2, 4, 6, 8], 6: [0, 2, 3, 5, 6, 8] }
  return map[n] || []
}

const toast = reactive({ visible: false, msg: '', type: '' })
let toastTimer = null
function showToast(msg, type = '') {
  toast.msg = msg; toast.type = type; toast.visible = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.visible = false }, 3200)
}

const seatCount = computed(() => state.players.filter(p => p.username).length)
const currentName = computed(() => {
  const p = state.players[state.currentSeat]
  return p && p.username ? p.username : '—'
})
const reversedLog = computed(() => [...(state.log || [])].reverse())
const you = computed(() => state.you || { joined: false, seat: -1, isMyTurn: false, canRoll: false, canDecide: false })
const myName = computed(() => {
  const p = (state.players || []).find(x => x.isYou)
  return p ? p.username : ''
})

function playersOnTile(idx) {
  return (state.players || []).filter(p => p.username && p.position === idx)
}
function ownedCount(username) {
  return (state.board || []).filter(t => t.owner === username).length
}

async function refresh() {
  try {
    const s = await fetchGameState()
    Object.assign(state, s)
    if (!rolling.value && s.dice && s.dice.value) diceFace.value = s.dice.value
    myStone.value = auth.spiritStone
  } catch (e) {
    showToast('牌桌状态获取失败：' + (e.message || '网络异常'), 'err')
  }
}

async function doJoin() {
  try { await joinGame(); showToast('报名成功，已缴纳 ' + state.entryFee + ' 灵石', 'ok'); await refresh(); await auth.fetchProfile() }
  catch (e) { showToast(e.message || '报名失败', 'err') }
}
async function doLeave() {
  try { await leaveGame(); showToast('已退出，灵石已退回', 'ok'); await refresh(); await auth.fetchProfile() }
  catch (e) { showToast(e.message || '退出失败', 'err') }
}
async function doStart() {
  try { await startGame(); showToast('开局！', 'ok'); await refresh() }
  catch (e) { showToast(e.message || '开局失败', 'err') }
}
async function doRoll() {
  if (!you.value.canRoll || rolling.value) return
  rolling.value = true
  const rollTimer = setInterval(() => { diceFace.value = Math.floor(Math.random() * 6) + 1 }, 90)
  let actual = 0
  try {
    const s = await rollDice()
    if (s && s.dice && s.dice.value) actual = s.dice.value
  } catch (e) {
    clearInterval(rollTimer); rolling.value = false
    showToast(e.message || '掷骰失败', 'err')
    return
  }
  setTimeout(() => {
    clearInterval(rollTimer)
    if (actual) diceFace.value = actual
    rolling.value = false
    refresh()
  }, 900)
}
async function doDecide(action) {
  try { await decideGame(action); await refresh() }
  catch (e) { showToast(e.message || '操作失败', 'err') }
}
async function doEnd() {
  if (!window.confirm('确定提前结算本局吗？将按当前局内灵石排名瓜分奖池。')) return
  try { await endGame(); showToast('本局已结算', 'ok'); await refresh() }
  catch (e) { showToast(e.message || '结束失败', 'err') }
}
async function doReset() {
  try { await resetGame(); showToast('牌桌已重置', 'ok'); await refresh() }
  catch (e) { showToast(e.message || '重置失败', 'err') }
}

let pollTimer = null
onMounted(async () => {
  await auth.fetchProfile().catch(() => {})
  await refresh()
  pollTimer = setInterval(refresh, 3000)
})
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer); if (toastTimer) clearTimeout(toastTimer) })
</script>

<style scoped>
.mp-wrap { max-width: 1100px; margin: 0 auto; padding: 24px 22px 36px; height: 100%; overflow-y: auto; }
.mp-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; flex-wrap: wrap; }
.mp-head h1 { font-size: 26px; letter-spacing: 3px; font-weight: 700;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34); -webkit-background-clip: text; background-clip: text; color: transparent; }
.mp-head-right { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.mp-pool { display: flex; align-items: center; gap: 8px; padding: 8px 16px; border-radius: 999px;
  border: 1px solid rgba(233,200,120,.35); color: var(--gold-2); font-size: 14px; letter-spacing: 1px; background: rgba(233,200,120,.06); }
.pool-dot { width: 18px; height: 18px; border-radius: 50%; display: grid; place-items: center;
  background: linear-gradient(135deg, var(--gold-2), var(--gold)); color: #0c1322; font-size: 11px; font-weight: 700; }

/* 等待区 */
.lobby-tip { padding: 14px 18px; border-radius: 14px; border: 1px solid var(--line); background: rgba(20,29,54,.5);
  color: var(--ink-dim); font-size: 14px; line-height: 1.8; margin-bottom: 18px; }
.lobby-tip b { color: var(--gold-2); }
.seat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.seat { padding: 22px 14px; border-radius: 16px; text-align: center; border: 1px dashed rgba(140,170,220,.3);
  background: rgba(13,20,38,.5); transition: .2s; min-height: 150px; display: flex; flex-direction: column; justify-content: center; align-items: center; gap: 8px; }
.seat.filled { border-style: solid; border-color: rgba(103,232,249,.4); background: linear-gradient(160deg, rgba(20,29,54,.7), rgba(13,20,38,.7)); }
.seat.me { border-color: var(--gold); box-shadow: 0 0 18px rgba(233,200,120,.25); }
.seat-avatar { width: 52px; height: 52px; border-radius: 50%; display: grid; place-items: center; font-size: 28px; border: 2px solid; background: rgba(13,20,38,.6); }
.seat-name { font-size: 18px; color: var(--gold-2); font-weight: 700; letter-spacing: 1px; }
.seat-empty { font-size: 14px; color: var(--ink-dim); }
.seat-sub { font-size: 12px; color: var(--qi); }
.lobby-actions { display: flex; align-items: center; gap: 14px; margin-top: 22px; flex-wrap: wrap; }
.joined-tag { color: var(--qi); font-size: 15px; letter-spacing: 1px; }
.wait-hint { margin-top: 14px; color: var(--ink-dim); font-size: 13px; }

/* ===== 平面棋盘 ===== */
.board {
  display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); grid-template-rows: repeat(6, minmax(0, 1fr));
  width: min(92vw, 720px); aspect-ratio: 1; margin: 0 auto; gap: 5px; padding: 5px;
  border-radius: 18px; background: rgba(8,12,24,.6); border: 1px solid rgba(233,200,120,.2);
}
.tile {
  position: relative; display: flex; flex-direction: column; align-items: center; justify-content: flex-start;
  gap: 2px; padding: 4px 2px; border-radius: 8px; overflow: hidden; font-size: 11px;
  border: 1px solid var(--line); background: linear-gradient(160deg, rgba(20,29,54,.8), rgba(13,20,38,.8));
  min-width: 0; min-height: 0;
}
.t-icon { font-size: 16px; line-height: 1; }
.t-name { font-weight: 700; letter-spacing: 1px; color: var(--ink); font-size: 12px; }
.t-desc { font-size: 10px; color: var(--ink-dim); letter-spacing: 1px; }
.t-prop { display: flex; align-items: center; gap: 4px; font-size: 10px; }
.t-owner { color: var(--gold-2); max-width: 56px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.t-free { color: var(--ink-dim); }
.t-lv { color: var(--qi); }
.t-rent { font-size: 10px; color: #7CFFB0; }
.t-price { font-size: 10px; color: var(--gold-2); }
.t-pawns { display: flex; gap: 2px; flex-wrap: wrap; justify-content: center; margin-top: auto; }
.pawn { width: 20px; height: 20px; border-radius: 50%; display: grid; place-items: center; font-size: 12px; border: 1.5px solid; }

.t-START { border-color: rgba(233,200,120,.6); background: linear-gradient(160deg, rgba(233,200,120,.28), rgba(13,20,38,.8)); }
.t-MINE { border-color: rgba(103,232,249,.45); }
.t-FORTUNE { border-color: rgba(139,109,255,.45); }
.t-THUNDER { border-color: rgba(255,107,107,.5); }
.t-TRIBUTE { border-color: rgba(255,180,80,.4); }
.t-SECRET { border-color: rgba(103,232,249,.3); }
.t-PAUSE { border-color: rgba(140,170,220,.35); }
.t-REST { border-color: rgba(140,170,220,.2); }
.t-MARKET { border-color: rgba(120,220,160,.4); }
.tile.mine-own { box-shadow: inset 0 0 12px rgba(103,232,249,.35); }
.tile.mine-other { box-shadow: inset 0 0 12px rgba(255,180,80,.28); }

/* 中央面板 */
.board-center {
  grid-row: 2 / 6; grid-column: 2 / 6; display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; padding: 12px; border-radius: 14px; text-align: center;
  background: radial-gradient(circle at 50% 20%, rgba(233,200,120,.10), rgba(8,12,24,.7));
}
.ct-title { font-size: 20px; font-weight: 800; letter-spacing: 4px; color: var(--gold); }
.ct-pool { font-size: 13px; color: var(--ink-dim); letter-spacing: 1px; }
.ct-pool b { color: var(--gold-2); font-size: 16px; }
.ct-turn { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--ink); }
.ct-turn-avatar { width: 30px; height: 30px; border-radius: 50%; display: grid; place-items: center; font-size: 17px; border: 2px solid; background: rgba(13,20,38,.6); }
.ct-turn-name { font-weight: 700; color: var(--gold-2); letter-spacing: 1px; }
.ct-turn-you { color: var(--qi); font-size: 12px; }
.ct-turn-wait { color: var(--ink-dim); font-size: 12px; }
.ct-progress { font-size: 11px; color: var(--ink-dim); }
.ct-event { margin-top: 2px; padding: 6px 10px; border-radius: 8px; font-size: 11px; line-height: 1.5; background: rgba(20,29,54,.55); max-width: 100%; }
.ct-event.up { color: #7CFFB0; } .ct-event.down { color: #ff8a8a; }

/* 骰子 */
.dice { width: 62px; height: 62px; margin: 2px auto; border-radius: 14px; display: grid; grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(3, 1fr);
  padding: 10px; background: linear-gradient(145deg, #1a2240, #0d1426); border: 1px solid var(--line);
  box-shadow: inset 0 0 16px rgba(0,0,0,.4); }
.pip-slot { display: grid; place-items: center; }
.pip { width: 9px; height: 9px; border-radius: 50%; background: var(--gold-2); box-shadow: 0 0 6px rgba(233,200,120,.5); }
.dice-q { grid-column: 1 / 4; grid-row: 1 / 4; display: grid; place-items: center; font-size: 30px; font-weight: 800; color: var(--ink-dim); }
.dice.rolling { animation: tumble .9s ease; }
@keyframes tumble {
  0% { transform: rotate(0) scale(1); }
  25% { transform: rotate(120deg) scale(1.15); }
  50% { transform: rotate(240deg) scale(.92); }
  75% { transform: rotate(360deg) scale(1.1); }
  100% { transform: rotate(360deg) scale(1); }
}

/* 玩家资产条 */
.players-bar { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin: 16px auto 0; max-width: 720px; }
.pb { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 12px; border: 1px solid var(--line); background: rgba(13,20,38,.5); font-size: 13px; }
.pb.empty { opacity: .4; }
.pb.me { border-color: var(--gold); }
.pb.cur { background: rgba(103,232,249,.12); border-color: rgba(103,232,249,.5); }
.pb-avatar { width: 28px; height: 28px; border-radius: 50%; display: grid; place-items: center; font-size: 16px; border: 2px solid; flex-shrink: 0; }
.pb-name { flex: 1; color: var(--ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pb-stone { color: var(--gold-2); }
.pb-own { color: var(--qi); font-size: 11px; }

.log-box { margin-top: 16px; padding: 14px 16px; border-radius: 14px; border: 1px solid var(--line); background: rgba(13,20,38,.5); }
.log-title { font-size: 14px; color: var(--gold-2); letter-spacing: 2px; margin-bottom: 8px; }
.log-scroll { max-height: 160px; overflow-y: auto; display: flex; flex-direction: column; gap: 4px; }
.log-line { font-size: 13px; color: var(--ink-dim); line-height: 1.6; }
.log-line b { color: var(--qi); }

/* 结算 */
.mp-end { text-align: center; padding: 30px 0; }
.end-title { font-size: 26px; letter-spacing: 3px; color: var(--gold-2); margin-bottom: 20px; }
.rank-list { display: flex; flex-direction: column; gap: 10px; max-width: 560px; margin: 0 auto 22px; }
.rank-row { display: grid; grid-template-columns: 40px 1fr 120px 90px 100px; align-items: center; gap: 10px; padding: 12px 16px; border-radius: 12px;
  border: 1px solid var(--line); background: linear-gradient(160deg, rgba(20,29,54,.7), rgba(13,20,38,.7)); font-size: 14px; }
.rk-1 { border-color: var(--gold); box-shadow: 0 0 18px rgba(233,200,120,.3); }
.rk-no { font-size: 20px; font-weight: 800; color: var(--ink-dim); }
.rk-1 .rk-no { color: var(--gold); }
.rk-name { color: var(--gold-2); font-weight: 700; letter-spacing: 1px; }
.rk-stone { color: var(--ink-dim); }
.rk-pay { color: var(--qi); }
.rk-net.up { color: #7CFFB0; } .rk-net.down { color: #ff8a8a; }

/* 选择弹窗 */
.decision-mask { position: fixed; inset: 0; z-index: 9600; display: flex; align-items: center; justify-content: center;
  background: rgba(4,7,14,.6); backdrop-filter: blur(2px); }
.decision-card { width: min(400px, 92vw); padding: 26px 24px; border-radius: 18px; text-align: center;
  background: linear-gradient(180deg, rgba(16,23,44,.98), rgba(8,12,24,.98)); border: 1px solid rgba(233,200,120,.35);
  box-shadow: 0 24px 80px rgba(0,0,0,.6); }
.dc-icon { font-size: 40px; margin-bottom: 8px; }
.dc-title { font-size: 20px; color: var(--gold-2); letter-spacing: 2px; margin-bottom: 12px; }
.dc-desc { font-size: 14px; color: var(--ink-dim); line-height: 1.8; margin-bottom: 20px; }
.dc-desc b { color: var(--gold-2); }
.dc-actions { display: flex; gap: 12px; justify-content: center; }

/* 按钮 */
.btn-primary, .btn-gold, .btn-ghost, .btn-danger, .btn-danger-outline, .btn-roll { padding: 11px 22px; border-radius: 12px; cursor: pointer; font-family: inherit; font-size: 15px; letter-spacing: 2px; transition: .2s; border: none; }
.btn-primary { background: rgba(103,232,249,.14); border: 1px solid rgba(103,232,249,.4); color: var(--qi); }
.btn-primary:hover:not(:disabled) { background: rgba(103,232,249,.24); }
.btn-primary:disabled { opacity: .5; cursor: not-allowed; }
.btn-gold { color: #0c1322; font-weight: 700; background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44); box-shadow: 0 4px 16px rgba(233,200,120,.25); }
.btn-gold:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 8px 24px rgba(233,200,120,.45); }
.btn-gold:disabled { opacity: .55; cursor: not-allowed; }
.btn-ghost { background: transparent; border: 1px solid rgba(233,200,120,.35); color: var(--gold); }
.btn-ghost:hover { background: rgba(233,200,120,.12); }
.btn-danger { background: linear-gradient(135deg, #ff8a8a, #e04040); color: #fff; font-weight: 700; box-shadow: 0 4px 16px rgba(224,64,64,.3); }
.btn-danger:hover { transform: translateY(-1px); }
.btn-danger-outline { background: transparent; border: 1px solid rgba(255,138,138,.5); color: #ff8a8a; font-size: 12px; padding: 7px 14px; letter-spacing: 1px; }
.btn-danger-outline:hover { background: rgba(255,138,138,.12); }
.btn-roll { font-size: 16px; padding: 12px 30px; }

/* toast */
.g-toast { position: fixed; left: 50%; top: 84px; transform: translateX(-50%); z-index: 200;
  padding: 12px 24px; border-radius: 999px; font-size: 15px; letter-spacing: 1px; color: #0c1322; font-weight: 700;
  background: linear-gradient(135deg, var(--gold-2), var(--gold)); box-shadow: 0 8px 30px rgba(233,200,120,.45); max-width: 92vw; text-align: center; }
.g-toast.err { background: linear-gradient(135deg, #ff8a8a, #e04040); color: #fff; }
.g-toast.ok { background: linear-gradient(135deg, #7CFFB0, #37c97a); color: #04210f; }
.g-toast-enter-active, .g-toast-leave-active { transition: all .35s ease; }
.g-toast-enter-from, .g-toast-leave-to { opacity: 0; transform: translate(-50%, -16px); }

.modal-enter-active, .modal-leave-active { transition: opacity .25s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }

@media (max-width: 820px) {
  .seat-row { grid-template-columns: repeat(2, 1fr); }
  .players-bar { grid-template-columns: repeat(2, 1fr); }
  .rank-row { grid-template-columns: 32px 1fr 80px; grid-auto-rows: auto; }
  .rk-stone, .rk-pay { display: none; }
}
</style>
