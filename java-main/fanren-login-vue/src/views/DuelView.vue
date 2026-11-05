<template>
  <div class="duel-wrap">
    <header class="duel-head">
      <h1>论 道 争 锋</h1>
      <p class="sub">
        当前在线道友共 <b class="online-count">{{ onlineUsers.length }}</b> 人（全部显示，无人数上限）· 每 5 秒刷新 · 点击查看详情，亦可发起切磋
      </p>
    </header>

    <!-- 在线用户列表 -->
    <section class="online-grid">
      <article
        v-for="u in onlineUsers"
        :key="u.username"
        class="user-card"
        @click="viewDetail(u.username)"
      >
        <div class="uc-top">
          <span class="uc-name">{{ u.username }}</span>
          <span class="uc-realm">{{ u.realm || '练气' }}</span>
        </div>
        <div class="uc-chips">
          <span class="chip root">{{ u.spiritRoot || '灵根未知' }}</span>
          <span class="chip phys">{{ u.physique || '体质未知' }}</span>
        </div>
        <div class="uc-foot">
          <span class="uc-lv">修为 {{ u.totalLevel || 1 }} 层</span>
          <button class="uc-fight" @click.stop="onChallenge(u.username)">对 战</button>
        </div>
      </article>

      <p v-if="!onlineUsers.length" class="empty">暂无其他在线道友，稍候片刻或邀请同门登录。</p>
    </section>

    <!-- 人物详情抽屉 -->
    <transition name="drawer">
      <div v-if="detailOpen" class="drawer-mask" @click.self="closeDetail">
        <aside class="drawer">
          <button class="drawer-close" @click="closeDetail">✕</button>
          <div v-if="detailLoading" class="drawer-loading">正在参悟此人命格…</div>
          <template v-else-if="detail">
            <h2 class="d-name">{{ detail.username }}</h2>
            <p class="d-realm">{{ detail.fullRealm || detail.realm }}</p>

            <div class="d-tags">
              <span class="chip root">{{ detail.spiritRoot || '灵根未知' }}</span>
              <span class="chip phys">{{ detail.physique || '体质未知' }}</span>
              <span class="chip sect">{{ detail.sect || '散修' }}</span>
            </div>

            <div class="d-section">
              <h3>五维属性</h3>
              <ul class="stat-list">
                <li><span>生命</span><b>{{ detail.total?.hp ?? 0 }}</b></li>
                <li><span>攻击</span><b>{{ detail.total?.atk ?? 0 }}</b></li>
                <li><span>防御</span><b>{{ detail.total?.def ?? 0 }}</b></li>
                <li><span>速度</span><b>{{ detail.total?.spd ?? 0 }}</b></li>
                <li><span>寿元</span><b>{{ detail.remainingLifespan ?? detail.total?.lifespan ?? 0 }}</b></li>
              </ul>
            </div>

            <div class="d-section">
              <h3>修持</h3>
              <p class="d-line">主修功法：<em>{{ detail.manual || '无' }}</em></p>
              <p class="d-line">本命法宝：<em>{{ detail.treasure || '无' }}</em></p>
              <p class="d-line">灵石 / 灵气：<em>{{ detail.spiritStone ?? 0 }} / {{ detail.spiritQi ?? 0 }}</em></p>
            </div>

            <button class="d-fight" @click="onChallenge(detail.username)">向此人发起对战</button>
          </template>
        </aside>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useDuel } from '../duelBus'
import { getOnline, getProfile } from '../api/duel'

// 接收请求、弹窗、对战动画、发起方状态轮询全部交由全局 duelBus 处理
const { challenge, showToast } = useDuel()

const onlineUsers = ref([])
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

let onlineTimer = null

async function refreshOnline() {
  try {
    const data = await getOnline()
    onlineUsers.value = data.users || []
  } catch (_) { /* 后端未连，静默 */ }
}

async function viewDetail(username) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const data = await getProfile(username)
    detail.value = data
  } catch (e) {
    showToast(e.message || '获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detailOpen.value = false
}

function onChallenge(target) {
  challenge(target)
}

onMounted(() => {
  refreshOnline()
  onlineTimer = setInterval(refreshOnline, 5000)
})

onUnmounted(() => {
  if (onlineTimer) clearInterval(onlineTimer)
})
</script>

<style scoped>
.duel-wrap { max-width: 1080px; margin: 0 auto; padding: 28px 20px 60px; }
.duel-head { text-align: center; margin-bottom: 26px; }
.duel-head h1 {
  font-size: 34px; letter-spacing: 10px; text-indent: 10px; font-weight: 800;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 18px rgba(233,200,120,.45));
}
.duel-head .sub { color: var(--ink-dim); letter-spacing: 2px; font-size: 13px; margin-top: 8px; }
.duel-head .sub .online-count {
  color: var(--gold-2); font-weight: 800; font-size: 16px;
  padding: 0 4px; text-shadow: 0 0 12px rgba(233,200,120,.4);
}

.online-grid {
  display: grid; gap: 16px;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
}
.user-card {
  cursor: pointer; padding: 16px 18px; border-radius: 14px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(20,29,54,.55), rgba(10,16,32,.7));
  transition: .2s;
}
.user-card:hover {
  border-color: rgba(233,200,120,.5);
  box-shadow: 0 0 22px rgba(233,200,120,.18);
  transform: translateY(-2px);
}
.uc-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.uc-name { font-size: 17px; font-weight: 700; color: var(--ink); letter-spacing: 1px; }
.uc-realm { font-size: 12px; color: var(--gold-2); border: 1px solid rgba(233,200,120,.35); padding: 2px 8px; border-radius: 999px; }
.uc-chips { display: flex; flex-wrap: wrap; gap: 6px; margin: 12px 0; }
.chip { font-size: 12px; padding: 3px 9px; border-radius: 999px; border: 1px solid var(--line); color: var(--ink-dim); }
.chip.root { color: var(--qi); border-color: rgba(103,232,249,.35); }
.chip.phys { color: #c9a6ff; border-color: rgba(139,109,255,.35); }
.chip.sect { color: var(--gold-2); border-color: rgba(233,200,120,.3); }
.uc-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; }
.uc-lv { font-size: 13px; color: var(--ink-dim); letter-spacing: 1px; }
.uc-fight {
  padding: 8px 18px; border: none; border-radius: 999px; cursor: pointer;
  font-family: inherit; font-size: 14px; letter-spacing: 3px; font-weight: 700; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 6px 18px rgba(233,200,120,.25); transition: .2s;
}
.uc-fight:hover { transform: translateY(-1px); box-shadow: 0 12px 28px rgba(233,200,120,.4); }
.empty { grid-column: 1 / -1; text-align: center; color: var(--ink-dim); padding: 50px 0; letter-spacing: 2px; }

/* ===== 详情抽屉 ===== */
.drawer-mask {
  position: fixed; inset: 0; z-index: 9000;
  display: flex; justify-content: flex-end;
  background: rgba(4,7,14,.55); backdrop-filter: blur(2px);
}
.drawer {
  position: relative; width: min(420px, 92vw); height: 100%;
  padding: 28px 26px; overflow-y: auto;
  background: linear-gradient(180deg, rgba(14,20,38,.98), rgba(8,12,24,.98));
  border-left: 1px solid rgba(233,200,120,.25);
  box-shadow: -20px 0 60px rgba(0,0,0,.5);
  animation: drawerIn .3s cubic-bezier(.2,.8,.2,1);
}
@keyframes drawerIn { from { transform: translateX(40px); opacity: .4; } to { transform: none; opacity: 1; } }
.drawer-close {
  position: absolute; top: 16px; right: 16px; width: 32px; height: 32px;
  border-radius: 50%; border: 1px solid var(--line); background: transparent;
  color: var(--ink-dim); cursor: pointer; font-size: 14px; transition: .2s;
}
.drawer-close:hover { color: var(--gold); border-color: rgba(233,200,120,.5); }
.drawer-loading { color: var(--ink-dim); text-align: center; margin-top: 60px; letter-spacing: 2px; }
.d-name { font-size: 24px; font-weight: 800; color: var(--ink); letter-spacing: 2px; }
.d-realm { color: var(--gold-2); letter-spacing: 2px; margin: 4px 0 16px; }
.d-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 20px; }
.d-section { margin-bottom: 20px; }
.d-section h3 { font-size: 14px; color: var(--ink-dim); letter-spacing: 3px; margin-bottom: 10px; border-left: 3px solid var(--gold); padding-left: 8px; }
.stat-list { list-style: none; padding: 0; margin: 0; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.stat-list li { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 10px; border: 1px solid var(--line); background: rgba(20,29,54,.5); }
.stat-list span { color: var(--ink-dim); font-size: 13px; letter-spacing: 1px; }
.stat-list b { color: var(--gold-2); font-size: 18px; font-family: "Noto Serif SC", serif; }
.d-line { color: var(--ink); font-size: 14px; margin: 8px 0; letter-spacing: 1px; }
.d-line em { color: var(--qi); font-style: normal; }
.d-fight {
  width: 100%; margin-top: 8px; padding: 12px; border: none; border-radius: 999px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 4px; font-weight: 700; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 10px 30px rgba(233,200,120,.3); transition: .2s;
}
.d-fight:hover { transform: translateY(-2px); box-shadow: 0 16px 40px rgba(233,200,120,.45); }

.drawer-enter-active, .drawer-leave-active { transition: opacity .25s; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
</style>
