<template>
  <div class="chat-wrap">
    <!-- 好友列表 -->
    <aside class="chat-list">
      <header class="cl-head">好 友</header>
      <ul class="cl-ul">
        <li
          v-for="f in friends"
          :key="f.username"
          class="cl-row"
          :class="{ active: peer === f.username }"
          @click="openChat(f.username)"
        >
          <span class="cl-dot" :class="{ on: f.online }"></span>
          <span class="cl-name">{{ f.username }}</span>
          <span class="cl-realm">{{ f.realm || '炼气' }}</span>
          <span v-if="unread[f.username]" class="cl-badge">{{ unread[f.username] }}</span>
        </li>
      </ul>
      <p v-if="!friends.length" class="cl-empty">尚无好友，先去添加吧</p>
    </aside>

    <!-- 聊天窗口 -->
    <section class="chat-main">
      <template v-if="peer">
        <header class="cm-head">
          <span class="cm-name">{{ peer }}</span>
          <span class="cm-tip">与道友论道</span>
        </header>

        <div ref="msgBox" class="cm-body">
          <p v-if="loading" class="cm-loading">正在取回传讯…</p>
          <template v-else>
            <p v-if="!messages.length" class="cm-empty">尚未论道，先发一句问候吧</p>
            <div
              v-for="(m, i) in messages"
              :key="i"
              class="msg"
              :class="m.mine ? 'mine' : 'theirs'"
            >
              <template v-if="m.type === 'GIFT'">
                <div class="msg-gift">
                  <span class="gift-emoji">🎁</span>
                  <span class="gift-text">
                    {{ m.mine ? '你赠予了' : (m.from + ' 赠予你') }}
                    <b class="gift-num">{{ m.stone }}</b> 灵石
                  </span>
                </div>
              </template>
              <template v-else>
                <div class="msg-bubble" :class="{ err: m.err }">{{ m.content }}</div>
              </template>
              <div class="msg-meta">{{ m.time }}</div>
            </div>
          </template>
        </div>

        <footer class="cm-input">
          <button class="cm-gift-btn" title="赠送灵石" @click="openGift">赠 灵</button>
          <input
            v-model="draft"
            class="cm-field"
            type="text"
            placeholder="输入传讯内容…"
            @keyup.enter="doSend"
          />
          <button class="cm-send" :disabled="sending || !draft.trim()" @click="doSend">发 送</button>
        </footer>
      </template>

      <div v-else class="cm-placeholder">
        <div class="ph-seal">传</div>
        <p>选择左侧好友开始论道</p>
      </div>
    </section>

    <!-- 赠送灵石弹窗 -->
    <div v-if="giftOpen" class="gift-mask" @click.self="closeGift">
      <div class="gift-panel">
        <h3 class="gp-title">赠 灵</h3>
        <p class="gp-peer">赠予道友 <b>{{ peer }}</b></p>
        <p class="gp-balance">我的灵石：<b class="gp-num">{{ myStone }}</b></p>
        <input
          v-model.number="giftAmount"
          class="gp-field"
          type="number"
          min="1"
          placeholder="输入赠送数量"
          @keyup.enter="doGift"
        />
        <p v-if="giftErr" class="gp-err">{{ giftErr }}</p>
        <div class="gp-actions">
          <button class="gp-cancel" @click="closeGift">取 消</button>
          <button class="gp-ok" :disabled="gifting" @click="doGift">确 认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { listFriends } from '../api/friend'
import { sendMessage, getHistory, getUnread, markRead, giftStone } from '../api/chat'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()

const friends = ref([])
const peer = ref('')
const messages = ref([])
const draft = ref('')
const loading = ref(false)
const sending = ref(false)
const unread = ref({})
const msgBox = ref(null)

// 赠灵弹窗状态
const giftOpen = ref(false)
const giftAmount = ref(null)
const giftErr = ref('')
const gifting = ref(false)

const myStone = computed(() => auth.spiritStone)

let pollTimer = null

const selfName = computed(() => localStorage.getItem('fx_user') || '')

function scrollBottom() {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  })
}

async function loadFriends() {
  try {
    const data = await listFriends()
    friends.value = data.friends || []
  } catch (_) { /* 静默 */ }
}

async function loadUnread() {
  try {
    const data = await getUnread()
    unread.value = data.counts || {}
  } catch (_) { /* 静默 */ }
}

async function openChat(name) {
  peer.value = name
  draft.value = ''
  loading.value = true
  try {
    const data = await getHistory(name)
    messages.value = data.messages || []
    // 进入即标记已读，清该好友未读角标
    await markRead(name)
    unread.value[name] = 0
  } catch (e) {
    messages.value = []
  } finally {
    loading.value = false
    scrollBottom()
  }
}

async function refreshHistory() {
  if (!peer.value) return
  try {
    const data = await getHistory(peer.value)
    const list = data.messages || []
    // 仅当消息变多时刷新，避免覆盖正在输入的滚动位置
    if (list.length !== messages.value.length) {
      messages.value = list
      scrollBottom()
    }
    // 有新的对方消息则顺带标记已读
    if (list.some(m => !m.mine)) {
      await markRead(peer.value)
      unread.value[peer.value] = 0
    }
  } catch (_) { /* 静默 */ }
}

async function doSend() {
  const text = draft.value.trim()
  if (!text || !peer.value || sending.value) return
  sending.value = true
  try {
    const res = await sendMessage(peer.value, text)
    messages.value.push({
      from: res.from, to: res.to, content: res.content,
      time: res.time, mine: true
    })
    draft.value = ''
    scrollBottom()
  } catch (e) {
    // 显示发送失败（不打断流程）
    messages.value.push({ from: selfName.value, to: peer.value, content: '（发送失败：' + (e.message || '') + '）', time: '', mine: true, err: true })
  } finally {
    sending.value = false
  }
}

function openGift() {
  if (!peer.value) return
  giftAmount.value = null
  giftErr.value = ''
  giftOpen.value = true
}

function closeGift() {
  giftOpen.value = false
  giftAmount.value = null
  giftErr.value = ''
}

async function doGift() {
  if (!peer.value || gifting.value) return
  const n = Math.floor(Number(giftAmount.value))
  if (!Number.isFinite(n) || n <= 0) { giftErr.value = '请输入大于 0 的整数'; return }
  if (n > myStone.value) { giftErr.value = '灵石不足，当前仅 ' + myStone.value; return }
  giftErr.value = ''
  gifting.value = true
  try {
    const res = await giftStone(peer.value, n)
    // 同步导航栏余额
    if (res.spiritStone != null) auth.setSpiritStone(res.spiritStone)
    // 推送一条赠送消息（本地即时展示）
    messages.value.push({
      from: selfName.value, to: peer.value, content: '🎁 赠送 ' + n + ' 灵石',
      type: 'GIFT', stone: n, time: res.time || '', mine: true
    })
    closeGift()
    scrollBottom()
  } catch (e) {
    giftErr.value = e.message || '赠送失败'
  } finally {
    gifting.value = false
  }
}

onMounted(() => {
  loadFriends()
  loadUnread()
  // 若路由带 ?peer= 则直接打开对应会话
  const q = route.query.peer
  if (q) openChat(String(q))
  pollTimer = setInterval(() => {
    refreshHistory()
    loadUnread()
  }, 2000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.chat-wrap {
  display: flex; max-width: 900px; margin: 0 auto; height: calc(100vh - 96px);
  padding: 24px 20px; gap: 18px; box-sizing: border-box;
}
/* 好友列表 */
.chat-list {
  flex: 0 0 240px; display: flex; flex-direction: column;
  border: 1px solid var(--line); border-radius: 16px; overflow: hidden;
  background: linear-gradient(180deg, rgba(20,29,54,.55), rgba(10,16,32,.7));
}
.cl-head {
  padding: 16px 18px; font-size: 16px; letter-spacing: 4px; color: var(--ink);
  border-bottom: 1px solid var(--line);
}
.cl-ul { list-style: none; padding: 8px; margin: 0; overflow-y: auto; flex: 1; }
.cl-row {
  display: flex; align-items: center; gap: 10px; padding: 11px 12px;
  border-radius: 10px; cursor: pointer; transition: .15s;
}
.cl-row:hover { background: rgba(103,232,249,.08); }
.cl-row.active { background: rgba(103,232,249,.14); border: 1px solid rgba(103,232,249,.3); }
.cl-dot { flex: 0 0 8px; height: 8px; border-radius: 50%; background: #445; }
.cl-dot.on { background: var(--qi); box-shadow: 0 0 8px var(--qi); }
.cl-name { color: var(--ink); font-size: 15px; letter-spacing: 1px; font-weight: 600; }
.cl-realm { margin-left: auto; color: var(--ink-dim); font-size: 12px; }
.cl-badge {
  min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px;
  display: grid; place-items: center; font-size: 11px; font-weight: 700; color: #0c1322;
  background: linear-gradient(135deg, #ff6b6b, #e04040);
}
.cl-empty { color: var(--ink-dim); font-size: 13px; text-align: center; margin: 20px 0; }

/* 聊天主区 */
.chat-main { flex: 1; display: flex; flex-direction: column; border: 1px solid var(--line); border-radius: 16px; overflow: hidden; background: linear-gradient(180deg, rgba(20,29,54,.4), rgba(10,16,32,.6)); }
.cm-head { padding: 16px 20px; border-bottom: 1px solid var(--line); display: flex; align-items: baseline; gap: 12px; }
.cm-name { color: var(--gold-2); font-size: 18px; letter-spacing: 2px; font-weight: 700; }
.cm-tip { color: var(--ink-dim); font-size: 12px; letter-spacing: 2px; }

.cm-body { flex: 1; overflow-y: auto; padding: 18px 20px; display: flex; flex-direction: column; gap: 12px; }
.cm-loading, .cm-empty { color: var(--ink-dim); font-size: 13px; letter-spacing: 1px; text-align: center; margin: auto; }

.msg { display: flex; flex-direction: column; max-width: 70%; }
.msg.mine { align-self: flex-end; align-items: flex-end; }
.msg.theirs { align-self: flex-start; align-items: flex-start; }
.msg-bubble {
  padding: 10px 14px; border-radius: 14px; font-size: 15px; letter-spacing: 1px; line-height: 1.5;
  word-break: break-word; white-space: pre-wrap;
}
.msg.mine .msg-bubble {
  color: #0c1322; font-weight: 600;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 55%, #c79a44);
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 16px rgba(233,200,120,.25);
}
.msg.theirs .msg-bubble {
  color: var(--ink);
  background: rgba(103,232,249,.10);
  border: 1px solid rgba(103,232,249,.25);
  border-bottom-left-radius: 4px;
}
.msg-meta { font-size: 11px; color: var(--ink-dim); margin-top: 4px; letter-spacing: 1px; }

.cm-input { display: flex; gap: 10px; padding: 14px 16px; border-top: 1px solid var(--line); }
.cm-field {
  flex: 1; padding: 12px 16px; border-radius: 12px; color: var(--ink);
  border: 1px solid var(--line); background: rgba(8,12,24,.6); font-family: inherit; font-size: 15px;
  outline: none; transition: .2s;
}
.cm-field:focus { border-color: rgba(233,200,120,.5); box-shadow: 0 0 14px rgba(233,200,120,.18); }
.cm-send {
  padding: 0 26px; border: none; border-radius: 12px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 3px; font-weight: 700; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 6px 18px rgba(233,200,120,.25); transition: .2s;
}
.cm-send:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 12px 28px rgba(233,200,120,.4); }
.cm-send:disabled { opacity: .5; cursor: default; }

/* 赠灵按钮 */
.cm-gift-btn {
  padding: 0 18px; border-radius: 12px; cursor: pointer;
  font-family: inherit; font-size: 14px; letter-spacing: 2px; font-weight: 700; color: var(--qi);
  border: 1px solid rgba(103,232,249,.4); background: transparent; transition: .2s;
}
.cm-gift-btn:hover { background: rgba(103,232,249,.12); box-shadow: 0 0 16px rgba(103,232,249,.3); }

/* 礼物气泡 */
.msg-gift {
  display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-radius: 14px;
  font-size: 15px; letter-spacing: 1px; color: var(--ink);
  background: linear-gradient(135deg, rgba(233,200,120,.18), rgba(233,200,120,.08));
  border: 1px solid rgba(233,200,120,.4);
  box-shadow: 0 4px 16px rgba(233,200,120,.18);
}
.msg.mine .msg-gift { border-bottom-right-radius: 4px; }
.msg.theirs .msg-gift { border-bottom-left-radius: 4px; }
.gift-emoji { font-size: 22px; }
.gift-num { color: var(--gold); font-size: 17px; margin: 0 2px; }

/* 赠灵弹窗 */
.gift-mask {
  position: fixed; inset: 0; z-index: 100; display: grid; place-items: center;
  background: rgba(4,8,18,.6); backdrop-filter: blur(3px);
}
.gift-panel {
  width: 320px; padding: 24px 22px; border-radius: 18px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(22,32,58,.96), rgba(10,16,32,.98));
  box-shadow: 0 12px 40px rgba(0,0,0,.5);
  display: flex; flex-direction: column; gap: 14px;
}
.gp-title { margin: 0; color: var(--gold); font-size: 20px; letter-spacing: 6px; text-align: center; }
.gp-peer { margin: 0; color: var(--ink); font-size: 14px; letter-spacing: 1px; }
.gp-peer b { color: var(--gold-2); }
.gp-balance { margin: 0; color: var(--ink-dim); font-size: 13px; letter-spacing: 1px; }
.gp-num { color: var(--qi); font-size: 15px; }
.gp-field {
  padding: 12px 14px; border-radius: 12px; color: var(--ink); font-family: inherit; font-size: 16px;
  border: 1px solid var(--line); background: rgba(8,12,24,.6); outline: none; transition: .2s;
}
.gp-field:focus { border-color: rgba(233,200,120,.5); box-shadow: 0 0 14px rgba(233,200,120,.18); }
.gp-err { margin: 0; color: #ff6b6b; font-size: 12px; letter-spacing: 1px; }
.gp-actions { display: flex; gap: 12px; margin-top: 4px; }
.gp-cancel, .gp-ok {
  flex: 1; padding: 11px 0; border-radius: 12px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 3px; font-weight: 700; transition: .2s;
}
.gp-cancel { color: var(--ink-dim); border: 1px solid var(--line); background: transparent; }
.gp-cancel:hover { background: rgba(255,255,255,.05); }
.gp-ok {
  border: none; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 6px 18px rgba(233,200,120,.25);
}
.gp-ok:hover:not(:disabled) { transform: translateY(-1px); }
.gp-ok:disabled { opacity: .5; cursor: default; }

.cm-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; color: var(--ink-dim); }
.ph-seal {
  width: 64px; height: 64px; border-radius: 16px; display: grid; place-items: center;
  font-size: 30px; color: #1a1206; font-weight: 700;
  background: linear-gradient(145deg, var(--gold), #b88a3c);
  box-shadow: 0 0 24px rgba(233,200,120,.4);
}

@media (max-width: 760px) {
  .chat-wrap { flex-direction: column; height: auto; }
  .chat-list { flex: none; max-height: 200px; }
}
</style>
