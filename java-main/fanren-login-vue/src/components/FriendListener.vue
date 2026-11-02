<template>
  <div class="friend-listener">
    <!-- 收到好友请求弹窗（全局，任意页面都会弹出） -->
    <transition name="modal">
      <div v-if="shown" class="modal-mask">
        <div class="modal">
          <div class="m-title">好 友 请 求</div>
          <p class="m-text">
            <b>{{ shown.from }}</b> 请求加你为好友
            <span v-if="shown.realm" class="m-realm">（{{ shown.realm }}）</span>
          </p>
          <p class="m-tip">同意则结为道友，拒绝则对方将收到提示。</p>
          <div class="m-actions">
            <button class="m-accept" @click="accept">同 意</button>
            <button class="m-reject" @click="reject">拒 绝</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="toast">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { listRequests, respondRequest } from '../api/friend'

const requests = ref([])
const shownFrom = ref(null)
const toast = ref('')

let timer = null
let toastTimer = null

function showToast(msg) {
  toast.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2600)
}

// 当前弹窗要展示的那条请求（含详情）
const shown = ref(null)

async function poll() {
  try {
    const data = await listRequests()
    const list = data.requests || []
    requests.value = list
    // 已展示的请求若不在最新列表（已被处理/过期），清空以便展示下一条
    if (shownFrom.value && !list.some(r => r.from === shownFrom.value)) {
      shownFrom.value = null
      shown.value = null
    }
    // 没有正在展示的，就取出第一条展示
    if (!shownFrom.value && list.length) {
      shownFrom.value = list[0].from
      shown.value = list[0]
    }
  } catch (_) { /* 静默 */ }
}

async function accept() {
  const from = shownFrom.value
  if (!from) return
  try {
    await respondRequest(from, true)
    showToast('已添加为好友')
  } catch (e) {
    showToast(e.message || '操作失败')
  }
  shownFrom.value = null
  shown.value = null
}

async function reject() {
  const from = shownFrom.value
  if (!from) return
  try {
    await respondRequest(from, false)
    showToast('已拒绝')
  } catch (e) {
    showToast(e.message || '操作失败')
  }
  shownFrom.value = null
  shown.value = null
}

onMounted(() => {
  poll()
  timer = setInterval(poll, 3000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<style scoped>
.friend-listener { display: contents; }

.modal-mask {
  position: fixed; inset: 0; z-index: 9500;
  display: flex; align-items: center; justify-content: center;
  background: rgba(4,7,14,.6); backdrop-filter: blur(2px);
}
.modal {
  width: min(380px, 90vw); padding: 28px 26px; border-radius: 16px; text-align: center;
  border: 1px solid rgba(103,232,249,.4);
  background: linear-gradient(180deg, rgba(20,29,54,.92), rgba(10,16,32,.95));
  box-shadow: 0 30px 80px rgba(0,0,0,.6);
  animation: gainsIn .4s cubic-bezier(.2,.8,.2,1);
}
@keyframes gainsIn { from { opacity: 0; transform: translateY(20px) scale(.96); } to { opacity: 1; transform: none; } }
.m-title { font-size: 22px; letter-spacing: 8px; color: var(--qi); margin-bottom: 14px; }
.m-text { color: var(--ink); font-size: 15px; letter-spacing: 1px; }
.m-text b { color: var(--gold-2); }
.m-realm { color: var(--qi); font-size: 13px; }
.m-tip { color: var(--ink-dim); font-size: 12px; margin: 10px 0 18px; letter-spacing: 1px; }
.m-actions { display: flex; gap: 12px; }
.m-accept, .m-reject {
  flex: 1; padding: 11px; border-radius: 999px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 4px; font-weight: 700; transition: .2s; border: none;
}
.m-accept { color: #0c1322; background: linear-gradient(135deg, var(--qi), #4fb6cf); }
.m-accept:hover { transform: translateY(-1px); box-shadow: 0 12px 28px rgba(103,232,249,.4); }
.m-reject { border: 1px solid rgba(255,120,120,.4); background: transparent; color: #ff9a9a; }
.m-reject:hover { background: rgba(255,120,120,.12); }

.toast {
  position: fixed; left: 50%; bottom: 40px; transform: translateX(-50%); z-index: 9999;
  padding: 12px 22px; border-radius: 999px; font-size: 14px; letter-spacing: 2px;
  color: #0c1322; font-weight: 700;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 12px 40px rgba(0,0,0,.4);
}
.toast-enter-active, .toast-leave-active { transition: opacity .3s, transform .3s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(10px); }

.modal-enter-active, .modal-leave-active { transition: opacity .25s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
