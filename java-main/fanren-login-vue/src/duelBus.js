// 对战全局状态机：被挑战方轮询、邀请弹窗、对战动画、发起方状态轮询
// 全部在此常驻，挂在 App.vue 的 DuelListener 上，因此无论用户处于哪个路由页都会生效。
import { reactive } from 'vue'
import { useAuthStore } from './stores/auth'
import {
  challenge as apiChallenge,
  getPending,
  respond as apiRespond,
  getStatus
} from './api/duel'

const state = reactive({
  selfName: '',
  incoming: null, // 被挑战方当前待处理请求 { duelId, from, fromRealm, ... }
  battle: { visible: false, selfName: '', oppName: '', winner: '', log: [] },
  toast: ''
})

let pendingTimer = null
let statusTimer = null
let toastTimer = null

function selfName() {
  const auth = useAuthStore()
  return auth.user || localStorage.getItem('fx_user') || ''
}

function showToast(msg) {
  state.toast = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { state.toast = '' }, 3000)
}

// ===== 被挑战方：轮询待处理请求 =====
async function refreshPending() {
  if (state.battle.visible) return
  try {
    const data = await getPending()
    state.incoming = data.duel || null
  } catch (_) { /* 后端未连或异常，静默继续轮询 */ }
}

function startPendingPoll() {
  refreshPending()
  if (!pendingTimer) pendingTimer = setInterval(refreshPending, 2500)
}
function stopPendingPoll() {
  if (pendingTimer) { clearInterval(pendingTimer); pendingTimer = null }
}

// ===== 发起方：轮询对战状态 =====
async function pollStatus(duelId, oppName) {
  try {
    const s = await getStatus(duelId)
    if (s.status === 'FIGHTING') {
      stopStatus()
      openBattle(selfName(), oppName, s.winner, s.log)
    } else if (s.status === 'REJECTED') {
      stopStatus()
      showToast('对方拒绝对战')
    } else if (s.status === 'EXPIRED') {
      stopStatus()
      showToast('对战已过期')
    }
  } catch (_) { /* 继续轮询 */ }
}

function startStatusPoll(duelId, oppName) {
  stopStatus()
  pollStatus(duelId, oppName)
  statusTimer = setInterval(() => pollStatus(duelId, oppName), 2000)
}
function stopStatus() {
  if (statusTimer) { clearInterval(statusTimer); statusTimer = null }
}

function openBattle(self, opp, winner, log) {
  state.battle = { visible: true, selfName: self, oppName: opp, winner: winner || '', log: log || [] }
}
function closeBattle() {
  state.battle.visible = false
  state.battle.winner = ''
  state.battle.log = []
  stopStatus()
}

// ===== 对外动作 =====
async function challenge(target) {
  if (target === selfName()) return
  try {
    const res = await apiChallenge(target)
    // 发起对战扣 10 灵石，同步导航栏余额
    if (res.spiritStone != null) {
      const auth = useAuthStore()
      auth.setSpiritStone(res.spiritStone)
    }
    if (res.duelId) startStatusPoll(res.duelId, target)
  } catch (e) {
    showToast(e.message || '发起对战失败')
  }
}

async function accept() {
  const d = state.incoming
  if (!d) return
  try {
    const res = await apiRespond(d.duelId, true)
    state.incoming = null
    // 胜利方获得 10 灵石：若自己获胜，刷新导航栏余额
    if (res.winner === selfName()) {
      try {
        const auth = useAuthStore()
        await auth.fetchProfile()
      } catch (_) { /* 忽略 */ }
    }
    if (res.status === 'FIGHTING') openBattle(selfName(), d.from, res.winner, res.log)
  } catch (e) {
    showToast(e.message || '响应失败')
    state.incoming = null
  }
}

async function reject() {
  const d = state.incoming
  if (!d) return
  try { await apiRespond(d.duelId, false) } catch (_) {}
  state.incoming = null
}

export function useDuel() {
  return { state, startPendingPoll, stopPendingPoll, challenge, accept, reject, closeBattle, showToast }
}
