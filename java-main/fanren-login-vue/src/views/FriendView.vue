<template>
  <div class="fr-wrap">
    <header class="fr-head">
      <h1>好 友 录</h1>
      <p class="sub">以道号相寻 · 结交同门道友 · 好友请求实时送达</p>
    </header>

    <!-- 搜索添加 -->
    <section class="panel search-panel">
      <div class="search-bar">
        <input
          v-model="keyword"
          class="search-input"
          type="text"
          placeholder="输入道号搜索道友…"
          @keyup.enter="doSearch"
        />
        <button class="search-btn" @click="doSearch">搜 索</button>
      </div>

      <ul v-if="searchResults.length" class="result-list">
        <li v-for="u in searchResults" :key="u.username" class="result-row">
          <span class="r-dot" :class="{ on: u.online }"></span>
          <span class="r-name">{{ u.username }}</span>
          <span class="r-meta">{{ u.realm || '炼气' }} · 修为 {{ u.totalLevel || 1 }} 层</span>
          <button
            class="r-act"
            :class="actClass(u.relation)"
            :disabled="u.relation === 'pending_out' || u.relation === 'friend'"
            @click="onAct(u)"
          >{{ actText(u.relation) }}</button>
        </li>
      </ul>
      <p v-else-if="searched" class="hint">未找到匹配道号</p>
    </section>

    <!-- 收到的好友请求 -->
    <section class="panel">
      <h2 class="panel-title">收到的请求 <span class="badge" v-if="requests.length">{{ requests.length }}</span></h2>
      <ul v-if="requests.length" class="req-list">
        <li v-for="r in requests" :key="r.from" class="req-row">
          <span class="r-dot"></span>
          <span class="r-name">{{ r.from }}</span>
          <span class="r-meta">{{ r.realm || '炼气' }} · 修为 {{ r.totalLevel || 1 }} 层</span>
          <div class="req-acts">
            <button class="r-accept" @click="doAccept(r.from)">同 意</button>
            <button class="r-reject" @click="doReject(r.from)">拒 绝</button>
          </div>
        </li>
      </ul>
      <p v-else class="hint">暂无好友请求</p>
    </section>

    <!-- 我的好友 -->
    <section class="panel">
      <h2 class="panel-title">我的好友 <span class="badge" v-if="friends.length">{{ friends.length }}</span></h2>
      <ul v-if="friends.length" class="friend-list">
        <li v-for="f in friends" :key="f.username" class="friend-row" @click="goChat(f.username)">
          <span class="r-dot" :class="{ on: f.online }"></span>
          <span class="r-name">{{ f.username }}</span>
          <span class="r-meta">{{ f.realm || '炼气' }} · 修为 {{ f.totalLevel || 1 }} 层</span>
          <span class="online-tag" v-if="f.online">在线</span>
          <button class="r-chat" @click.stop="goChat(f.username)">聊天</button>
          <button class="r-remove" @click.stop="doRemove(f.username)">删除</button>
        </li>
      </ul>
      <p v-else class="hint">尚未结交好友，去上方搜索添加吧</p>
    </section>

    <transition name="toast">
      <div v-if="toastMsg" class="toast">{{ toastMsg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  searchFriends, addFriend, listFriends, listRequests, respondRequest, removeFriend
} from '../api/friend'

const router = useRouter()

const keyword = ref('')
const searched = ref(false)
const searchResults = ref([])
const friends = ref([])
const requests = ref([])
const toastMsg = ref('')

let reqTimer = null
let friendTimer = null
let toastTimer = null

function showToast(msg) {
  toastMsg.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastMsg.value = '' }, 2600)
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  searched.value = true
  try {
    const data = await searchFriends(kw)
    searchResults.value = data.users || []
  } catch (e) {
    showToast(e.message || '搜索失败')
  }
}

function actText(rel) {
  if (rel === 'friend') return '已是好友'
  if (rel === 'pending_out') return '请求已发送'
  if (rel === 'pending_in') return '接 受'
  return '添 加'
}
function actClass(rel) {
  if (rel === 'friend') return 'disabled'
  if (rel === 'pending_out') return 'disabled'
  if (rel === 'pending_in') return 'accept'
  return 'add'
}

// 搜索结果行操作：none->添加；pending_in->直接接受；其余禁用
async function onAct(u) {
  if (u.relation === 'pending_in') return doAccept(u.username)
  if (u.relation !== 'none') return
  try {
    const data = await addFriend(u.username)
    showToast(data.message || '已发送好友请求')
    // 乐观更新该结果状态
    u.relation = 'pending_out'
  } catch (e) {
    showToast(e.message || '添加失败')
  }
}

async function doAccept(from) {
  try {
    await respondRequest(from, true)
    showToast('已添加为好友')
    await Promise.all([loadFriends(), loadRequests()])
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

async function doReject(from) {
  try {
    await respondRequest(from, false)
    showToast('已拒绝')
    await loadRequests()
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

async function doRemove(target) {
  try {
    await removeFriend(target)
    showToast('已删除好友')
    await loadFriends()
  } catch (e) {
    showToast(e.message || '删除失败')
  }
}

async function loadFriends() {
  try {
    const data = await listFriends()
    friends.value = data.friends || []
  } catch (_) { /* 静默 */ }
}

async function loadRequests() {
  try {
    const data = await listRequests()
    requests.value = data.requests || []
  } catch (_) { /* 静默 */ }
}

function goChat(username) {
  router.push({ path: '/chat', query: { peer: username } })
}

onMounted(() => {
  loadFriends()
  loadRequests()
  // 收到的请求实时轮询（3s），好友在线态 10s 刷新一次
  reqTimer = setInterval(loadRequests, 3000)
  friendTimer = setInterval(loadFriends, 10000)
})
onUnmounted(() => {
  if (reqTimer) clearInterval(reqTimer)
  if (friendTimer) clearInterval(friendTimer)
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<style scoped>
.fr-wrap { max-width: 760px; margin: 0 auto; padding: 28px 20px 60px; }
.fr-head { text-align: center; margin-bottom: 24px; }
.fr-head h1 {
  font-size: 34px; letter-spacing: 10px; text-indent: 10px; font-weight: 800;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 18px rgba(233,200,120,.45));
}
.fr-head .sub { color: var(--ink-dim); letter-spacing: 2px; font-size: 13px; margin-top: 8px; }

.panel {
  margin-bottom: 22px; padding: 20px 22px; border-radius: 16px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(20,29,54,.55), rgba(10,16,32,.7));
}
.panel-title {
  font-size: 16px; letter-spacing: 3px; color: var(--ink); margin: 0 0 14px;
  display: flex; align-items: center; gap: 8px;
}
.badge {
  font-size: 12px; padding: 1px 9px; border-radius: 999px; color: #0c1322; font-weight: 700;
  background: linear-gradient(135deg, var(--gold-2), var(--gold));
}
.hint { color: var(--ink-dim); font-size: 13px; letter-spacing: 1px; margin: 4px 0; }

/* 搜索 */
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.search-input {
  flex: 1; padding: 12px 16px; border-radius: 12px; color: var(--ink);
  border: 1px solid var(--line); background: rgba(8,12,24,.6); font-family: inherit; font-size: 15px;
  outline: none; transition: .2s;
}
.search-input:focus { border-color: rgba(233,200,120,.5); box-shadow: 0 0 14px rgba(233,200,120,.18); }
.search-btn {
  padding: 0 24px; border: none; border-radius: 12px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 3px; font-weight: 700; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 6px 18px rgba(233,200,120,.25); transition: .2s;
}
.search-btn:hover { transform: translateY(-1px); box-shadow: 0 12px 28px rgba(233,200,120,.4); }

/* 通用行 */
.result-list, .req-list, .friend-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 10px; }
.result-row, .req-row, .friend-row {
  display: flex; align-items: center; gap: 12px; padding: 12px 14px; border-radius: 12px;
  border: 1px solid var(--line); background: rgba(20,29,54,.45); transition: .2s;
}
.friend-row { cursor: pointer; }
.friend-row:hover { border-color: rgba(103,232,249,.4); box-shadow: 0 0 16px rgba(103,232,249,.12); }
.r-dot {
  flex: 0 0 8px; height: 8px; border-radius: 50%; background: #445; box-shadow: none;
}
.r-dot.on { background: var(--qi); box-shadow: 0 0 8px var(--qi); }
.r-name { font-size: 16px; color: var(--ink); letter-spacing: 1px; font-weight: 600; }
.r-meta { flex: 1; font-size: 13px; color: var(--ink-dim); letter-spacing: 1px; }
.online-tag { font-size: 12px; color: var(--qi); border: 1px solid rgba(103,232,249,.35); padding: 1px 8px; border-radius: 999px; }

.r-act, .r-accept, .r-reject, .r-remove {
  padding: 7px 16px; border-radius: 999px; cursor: pointer; font-family: inherit;
  font-size: 13px; letter-spacing: 2px; font-weight: 700; transition: .2s; border: none;
}
.r-act.add { color: #0c1322; background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44); }
.r-act.add:hover { transform: translateY(-1px); box-shadow: 0 10px 24px rgba(233,200,120,.4); }
.r-act.accept { color: #0c1322; background: linear-gradient(135deg, var(--qi), #4fb6cf); }
.r-act.disabled, .r-act:disabled { color: var(--ink-dim); background: transparent; border: 1px solid var(--line); cursor: default; }

.req-acts { display: flex; gap: 8px; }
.r-accept { color: #0c1322; background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44); }
.r-accept:hover { transform: translateY(-1px); }
.r-reject { color: #ff9a9a; background: transparent; border: 1px solid rgba(255,120,120,.4); }
.r-reject:hover { background: rgba(255,120,120,.12); }

.r-chat {
  color: #0c1322; background: linear-gradient(135deg, var(--qi), #4fb6cf);
}
.r-chat:hover { transform: translateY(-1px); box-shadow: 0 8px 20px rgba(103,232,249,.35); }

.r-remove { color: var(--ink-dim); background: transparent; border: 1px solid var(--line); }
.r-remove:hover { color: #ff9a9a; border-color: rgba(255,120,120,.4); }

/* toast */
.toast {
  position: fixed; left: 50%; bottom: 40px; transform: translateX(-50%); z-index: 9999;
  padding: 12px 22px; border-radius: 999px; font-size: 14px; letter-spacing: 2px;
  color: #0c1322; font-weight: 700;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 12px 40px rgba(0,0,0,.4);
}
.toast-enter-active, .toast-leave-active { transition: opacity .3s, transform .3s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(10px); }
</style>
