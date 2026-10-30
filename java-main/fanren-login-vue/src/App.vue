<template>
  <QiParticles />
  <div class="fog"></div>

  <div class="app-layout">
    <!-- ===== 持久导航栏（跨路由不重渲染，登录/注册/后台页隐藏） ===== -->
    <header v-if="showNav" class="navbar">
      <a v-if="!isHome" class="back-home" @click.prevent="goHome">
        <span class="arrow">‹</span> 返回首页
      </a>

      <div class="brand" @click="onBrand">
        <span class="seal-mini">藏</span>
        <span class="brand-name">凡人修仙 · 藏经阁</span>
      </div>

      <nav class="nav-links">
        <a :class="{ active: isSutras }" @click.prevent="goSutras">藏经阁</a>
        <a :class="{ active: isTreasures }" @click.prevent="goTreasures">法宝</a>
        <a :class="{ active: isStats }" @click.prevent="goStats">修为</a>
        <a :class="{ active: isSects }" @click.prevent="goSects">门派</a>
      </nav>

      <!-- 后台管理系统入口：仅管理员(permission=0)可见 -->
      <a v-if="auth.permission === 0" class="nav-admin" @click.prevent="goAdmin">后台管理系统</a>

      <div class="user-box">
        <span class="stone-chip" title="灵石" @click="goStats">
          <i class="stone-dot">灵</i>{{ auth.spiritStone }}
        </span>
        <span class="user-chip" @click="goStats">
          <i class="dot"></i>{{ auth.user || '无名' }} · {{ auth.profile?.realm || '炼气' }}
        </span>
        <button class="btn-exit" @click="onLogout" title="退出登录">退归山林</button>
      </div>
    </header>

    <!-- ===== 内容区：仅此区域随路由切换 ===== -->
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <component :is="Component" />
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import QiParticles from './components/QiParticles.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

// 登录 / 注册 / 后台登录 / 后台管理 不展示全局导航栏（各自有专属页面外壳）
const HIDDEN = ['login', 'register', 'admin-login', 'admin']
const showNav = computed(() => !HIDDEN.includes(route.name))
const isHome = computed(() => route.name === 'home')
const isSutras = computed(() => route.name === 'sutras' || route.name === 'manual')
const isTreasures = computed(() => route.name === 'treasures' || route.name === 'treasure')
const isStats = computed(() => route.name === 'stats')
const isSects = computed(() => route.name === 'sects')

const activeAnchor = ref('')

function goHome() { router.push('/') }
function goSutras() { router.push('/sutras') }
function goTreasures() { router.push('/treasures') }
function goStats() { router.push('/stats') }
function goSects() { router.push('/sects') }
function goAdmin() { router.push('/admin/login') }

function onBrand() {
  if (isHome.value) scrollTo('top')
  else router.push('/')
}

function onLogout() {
  auth.logout()
  router.push('/login')
}

// 锚点滚动：仅首页顶部
function scrollTo(id) {
  if (id === 'top') {
    document.getElementById('top')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

function onScroll() {
  activeAnchor.value = ''
}

onMounted(async () => {
  // 拉取道友信息（后端未启动时静默降级），供导航栏显示境界与灵石
  try {
    await auth.fetchProfile()
  } catch (_) { /* 后端未连，用本地信息 */ }
  window.addEventListener('scroll', onScroll, true) // 捕获：监听内部滚动容器
  onScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll, true)
})

// 路由切换后（内容区重渲染）重新计算高亮
watch(() => route.fullPath, () => nextTick(onScroll))
</script>

<style scoped>
/* 流光雾气 */
.fog{
  position:fixed;inset:-20%;z-index:0;pointer-events:none;
  background:
    radial-gradient(40% 30% at 20% 80%,rgba(103,232,249,.10),transparent 70%),
    radial-gradient(45% 35% at 85% 20%,rgba(139,109,255,.12),transparent 70%);
  filter:blur(8px);
  animation:drift 18s ease-in-out infinite alternate;
}
@keyframes drift{from{transform:translate3d(-2%,1%,0) scale(1)}to{transform:translate3d(2%,-1%,0) scale(1.06)}}

.app-layout{
  position:relative;z-index:2;
  display:flex;flex-direction:column;
  height:100%;width:100%;
}
.app-main{
  flex:1;min-height:0;
  display:flex;flex-direction:column;
}

/* ===== 导航栏（持久，仅内部内容区随路由变化） ===== */
.navbar{
  position:sticky;top:0;z-index:20;
  display:flex;align-items:center;gap:24px;
  padding:0 28px;height:64px;flex:none;
  background:rgba(7,10,20,.72);
  backdrop-filter:blur(12px);
  border-bottom:1px solid var(--line);
}
.navbar::after{
  content:'';position:absolute;left:0;right:0;bottom:-1px;height:1px;pointer-events:none;
  background:linear-gradient(90deg,transparent,rgba(233,200,120,.35) 50%,transparent);
}
.back-home{
  display:flex;align-items:center;gap:4px;padding:8px 14px;border-radius:10px;cursor:pointer;
  font-size:14px;letter-spacing:2px;color:var(--gold);
  border:1px solid rgba(233,200,120,.35);background:transparent;transition:.2s;white-space:nowrap;
}
.back-home:hover{background:rgba(233,200,120,.12);box-shadow:0 0 16px rgba(233,200,120,.25)}
.back-home .arrow{font-size:18px;line-height:1}

.brand{display:flex;align-items:center;gap:10px;cursor:pointer;user-select:none}
.seal-mini{
  width:30px;height:30px;border-radius:8px;display:grid;place-items:center;
  background:linear-gradient(145deg,var(--gold),#b88a3c);color:#1a1206;font-weight:700;font-size:15px;
  box-shadow:0 0 14px rgba(233,200,120,.40);
  animation:breath 3.2s ease-in-out infinite;
}
@keyframes breath{
  0%,100%{box-shadow:0 0 10px rgba(233,200,120,.35)}
  50%{box-shadow:0 0 22px rgba(233,200,120,.65)}
}
.brand-name{
  font-size:17px;letter-spacing:2px;font-weight:700;
  background:linear-gradient(180deg,#fff7e6,var(--gold) 60%,#b98e34);
  -webkit-background-clip:text;background-clip:text;color:transparent;
}
.nav-links{display:flex;gap:4px;margin-left:8px}
.nav-links a{
  position:relative;padding:8px 14px;border-radius:10px;cursor:pointer;
  color:var(--ink-dim);font-size:15px;letter-spacing:3px;transition:.2s;
  border:1px solid transparent;
}
.nav-links a:hover{
  color:var(--gold-2);background:rgba(233,200,120,.08);
  border-color:rgba(233,200,120,.25);
}
.nav-links a.active{color:var(--gold-2)}
.nav-links a.active::after{
  content:'';position:absolute;left:50%;transform:translateX(-50%);bottom:2px;
  width:16px;height:2px;border-radius:2px;
  background:linear-gradient(90deg,transparent,var(--gold),transparent);
}
.nav-admin{
  margin-left:6px;padding:8px 16px;border-radius:999px;cursor:pointer;
  font-size:14px;letter-spacing:2px;user-select:none;transition:.2s;
  color:#0c1322;font-weight:700;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  border:1px solid transparent;
  box-shadow:0 4px 16px rgba(233,200,120,.25);
}
.nav-admin:hover{
  transform:translateY(-1px);
  box-shadow:0 8px 24px rgba(233,200,120,.45);
}
.user-box{margin-left:auto;display:flex;align-items:center;gap:12px}
.stone-chip{
  display:flex;align-items:center;gap:6px;
  padding:6px 12px;border-radius:999px;cursor:pointer;
  border:1px solid rgba(233,200,120,.35);color:var(--gold-2);
  font-size:14px;font-weight:700;letter-spacing:1px;
  background:rgba(233,200,120,.06);transition:.2s;
}
.stone-chip:hover{border-color:rgba(233,200,120,.6);box-shadow:0 0 12px rgba(233,200,120,.25)}
.stone-dot{
  width:18px;height:18px;border-radius:50%;display:grid;place-items:center;
  background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#0c1322;font-size:11px;font-weight:700;
}
.user-chip{
  display:flex;align-items:center;gap:8px;
  padding:7px 14px;border-radius:999px;cursor:pointer;
  border:1px solid var(--line);color:var(--ink);font-size:14px;letter-spacing:1px;
  background:rgba(20,29,54,.45);transition:.2s;
}
.user-chip:hover{border-color:rgba(103,232,249,.4)}
.user-chip .dot{
  width:7px;height:7px;border-radius:50%;
  background:var(--qi);box-shadow:0 0 8px var(--qi);
  animation:breath 2.6s ease-in-out infinite;
}
.btn-exit{
  padding:7px 14px;border-radius:10px;cursor:pointer;
  border:1px solid rgba(233,200,120,.35);background:transparent;
  color:var(--gold);font-family:inherit;font-size:13px;letter-spacing:2px;transition:.2s;
}
.btn-exit:hover{
  background:rgba(233,200,120,.12);box-shadow:0 0 16px rgba(233,200,120,.25);
}

/* 移动端 */
@media(max-width:760px){
  .navbar{padding:0 16px;gap:10px;height:58px}
  .brand-name{display:none}
  .nav-links a{padding:7px 9px;letter-spacing:1px;font-size:13px}
  .nav-admin{padding:6px 11px;font-size:12px;letter-spacing:1px}
  .user-chip{display:none}
  .stone-chip{padding:5px 9px;font-size:12px}
  .back-home{padding:7px 10px;font-size:12px}
}
</style>
