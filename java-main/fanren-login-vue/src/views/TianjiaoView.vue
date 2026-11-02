<template>
  <div class="tj-wrap">
    <div class="tj-head">
      <h1 class="tj-title">天 骄 榜</h1>
      <p class="tj-sub">群雄并起 · 以境界论高低 · 每 60 秒自动刷新</p>
    </div>

    <!-- 加载/错误态 -->
    <div v-if="loading && list.length === 0" class="tj-state">榜单加载中…</div>
    <div v-else-if="error" class="tj-state tj-err">{{ error }}</div>

    <!-- 榜单（前100，超高时滚动） -->
    <ul v-else class="tj-list">
      <li
        v-for="row in list"
        :key="row.username"
        class="tj-row"
        :class="['r' + row.rank, { me: row.username === me }]"
      >
        <span class="tj-rank">{{ row.rank }}</span>
        <div class="tj-info">
          <span class="tj-name">
            {{ row.username }}
            <em v-if="row.username === me" class="tj-me-tag">吾</em>
          </span>
          <span class="tj-realm">{{ row.realm || '炼气一层' }}</span>
        </div>
        <span class="tj-level">修为 {{ row.totalLevel }} 层</span>
      </li>

      <li v-if="list.length === 0" class="tj-row tj-empty">暂无上榜修士</li>
    </ul>

    <p v-if="lastUpdated" class="tj-foot">最近刷新：{{ lastUpdatedText }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getTianjiao } from '../api/ranking'

const auth = useAuthStore()
const me = computed(() => auth.user || '')
const list = ref([])
const loading = ref(false)
const error = ref('')
const lastUpdated = ref(null)

const lastUpdatedText = computed(() => {
  if (!lastUpdated.value) return ''
  const d = lastUpdated.value
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getHours()}:${p(d.getMinutes())}:${p(d.getSeconds())}`
})

async function load() {
  loading.value = true
  try {
    const data = await getTianjiao()
    if (Array.isArray(data)) list.value = data
    error.value = ''
  } catch (e) {
    error.value = e.message || '获取天骄榜失败'
  } finally {
    loading.value = false
    lastUpdated.value = new Date()
  }
}

let timer = null
onMounted(() => {
  load()
  timer = setInterval(load, 60000) // 每分钟刷新
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.tj-wrap {
  width: min(720px, 100%);
  margin: 0 auto;
  padding: 26px 18px 40px;
}
.tj-head { text-align: center; margin-bottom: 22px; }
.tj-title {
  font-family: "Noto Serif SC", serif;
  font-size: 40px; letter-spacing: 16px; text-indent: 16px; font-weight: 900;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 18px rgba(233,200,120,.45));
  margin: 0;
}
.tj-sub { color: var(--ink-dim); letter-spacing: 2px; font-size: 13px; margin-top: 8px; }

.tj-state { text-align: center; color: var(--ink-dim); padding: 40px 0; letter-spacing: 2px; }
.tj-err { color: #ff9a9a; }

.tj-list {
  list-style: none; padding: 0; margin: 0; display: grid; gap: 10px;
  max-height: calc(100vh - 240px); overflow-y: auto; padding-right: 6px;
}
.tj-list::-webkit-scrollbar { width: 8px; }
.tj-list::-webkit-scrollbar-thumb { background: rgba(233,200,120,.25); border-radius: 8px; }
.tj-list::-webkit-scrollbar-thumb:hover { background: rgba(233,200,120,.45); }
.tj-row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px; border-radius: 14px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(20,29,54,.55), rgba(10,16,32,.7));
  transition: .2s;
}
.tj-row:hover { border-color: rgba(103,232,249,.35); box-shadow: 0 0 18px rgba(103,232,249,.12); }

.tj-rank {
  flex: 0 0 42px; height: 42px; display: grid; place-items: center;
  border-radius: 50%; font-family: "Noto Serif SC", serif; font-weight: 800; font-size: 18px;
  color: #0c1322; background: linear-gradient(135deg, #5b6b8c, #39476b);
}
.tj-row.r1 .tj-rank { background: linear-gradient(135deg, #ffe9a8, var(--gold) 55%, #c79a44); box-shadow: 0 0 18px rgba(233,200,120,.5); }
.tj-row.r2 .tj-rank { background: linear-gradient(135deg, #e9eef7, #aeb9cc); }
.tj-row.r3 .tj-rank { background: linear-gradient(135deg, #f0c8a0, #c98a5a); }

.tj-info { flex: 1; display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.tj-name {
  font-size: 17px; color: var(--ink); letter-spacing: 1px; font-weight: 600;
  display: inline-flex; align-items: center; gap: 8px;
}
.tj-me-tag {
  font-style: normal; font-size: 11px; padding: 1px 7px; border-radius: 999px;
  color: #0c1322; background: var(--qi); box-shadow: 0 0 10px rgba(103,232,249,.4);
}
.tj-realm { font-size: 13px; color: var(--qi); letter-spacing: 1px; }
.tj-row.r1 .tj-realm { color: var(--gold-2); }

.tj-level {
  flex: 0 0 auto; font-family: "Noto Serif SC", serif; font-size: 14px;
  color: var(--ink-dim); letter-spacing: 1px;
}

.tj-row.me {
  border-color: rgba(103,232,249,.55);
  box-shadow: 0 0 22px rgba(103,232,249,.18);
  background: linear-gradient(180deg, rgba(30,52,82,.6), rgba(14,26,46,.78));
}

.tj-empty { justify-content: center; color: var(--ink-dim); }
.tj-foot { text-align: center; color: var(--ink-dim); font-size: 12px; letter-spacing: 1px; margin-top: 18px; }

@media (max-width: 640px) {
  .tj-title { font-size: 32px; letter-spacing: 10px; text-indent: 10px; }
  .tj-level { font-size: 12px; }
}
</style>
