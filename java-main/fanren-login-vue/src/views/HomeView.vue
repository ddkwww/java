<template>
  <div class="stage">
    <div class="home-shell" ref="shell">

      <!-- ===== 导航栏 ===== -->
      <header class="navbar">
        <div class="brand" @click="scrollTo('top')">
          <span class="seal-mini">藏</span>
          <span class="brand-name">凡人修仙 · 藏经阁</span>
        </div>
        <nav class="nav-links">
          <a :class="{ active: activeNav === 'repository' }" @click.prevent="scrollTo('repository')">藏经阁</a>
          <a :class="{ active: activeNav === 'cave' }" @click.prevent="scrollTo('cave')">洞府</a>
          <a :class="{ active: activeNav === 'dao' }" @click.prevent="scrollTo('dao')">论道</a>
        </nav>

        <!-- 后台管理系统入口 -->
        <a class="nav-admin" @click.prevent="goAdmin">后台管理系统</a>

        <div class="user-box">
          <span class="user-chip" @click="scrollTo('cave')">
            <i class="dot"></i>{{ auth.user || '无名' }} · {{ auth.profile?.realm || '炼气' }}
          </span>
          <button class="btn-exit" @click="onLogout" title="退出登录">退归山林</button>
        </div>
      </header>

      <!-- ===== 顶部：搜索 ===== -->
      <section class="hero" id="top">
        <div class="watermark" aria-hidden="true">藏</div>
        <h1 class="title">藏 经 阁</h1>
        <p class="subtitle"><i class="sub-line"></i>功法秘籍 · 按图索骥<i class="sub-line r"></i></p>

        <div class="search-bar">
          <span class="search-icon">寻</span>
          <input
            v-model="keyword"
            type="text"
            :placeholder="'搜罗功法、秘术、神通、丹方……'"
            @keyup.enter="onSearch"
          />
          <button class="btn-search" @click="onSearch"><span>寻 典</span></button>
        </div>

        <div class="hot">
          <span class="hot-label">热门：</span>
          <a v-for="k in hotKeys" :key="k" @click.prevent="quickSearch(k)">{{ k }}</a>
        </div>

        <p v-if="sourceNote" class="source-note">{{ sourceNote }}</p>
      </section>

      <!-- ===== 藏经阁（功法秘籍总录） ===== -->
      <section class="block" id="repository">
        <h2 class="block-title"><i class="orn"></i>藏经阁 · 功法秘籍总录<i class="orn r"></i></h2>

        <!-- 筛选器：分类 / 品阶 / 境界 -->
        <div class="filters">
          <div class="filter-row">
            <span class="filter-label">分类</span>
            <div class="chips">
              <button
                v-for="c in ['全部', ...facets.categories]"
                :key="c"
                :class="['chip', { active: activeCategory === c }]"
                @click="activeCategory = c"
              >{{ c }}</button>
            </div>
          </div>

          <div class="filter-row">
            <span class="filter-label">品阶</span>
            <div class="chips">
              <button
                v-for="g in ['全部', ...facets.grades]"
                :key="g"
                :class="['chip', 'grade-' + g, { active: activeGrade === g }]"
                @click="activeGrade = g"
              >{{ g === '全部' ? '全部' : g + '阶' }}</button>
            </div>
          </div>

          <div class="filter-row">
            <span class="filter-label">境界</span>
            <select class="realm-select" v-model="activeRealm">
              <option value="全部">全部境界</option>
              <option v-for="r in facets.realms" :key="r" :value="r">{{ r }}</option>
            </select>
            <span class="count">共 {{ items.length }} 卷</span>
          </div>
        </div>

        <!-- 秘籍卡片网格 -->
        <div v-if="items.length" class="grid">
          <article
            v-for="(it, i) in items"
            :key="searchStamp + '-' + it.id"
            class="item"
            :data-grade="it.grade"
            :style="{ animationDelay: (i * 45) + 'ms' }"
            role="button"
            tabindex="0"
            @click="goDetail(it.id)"
            @keyup.enter="goDetail(it.id)"
          >
            <i class="corner" aria-hidden="true"></i>
            <span class="enter-hint" aria-hidden="true">细览 ›</span>
            <div class="item-cover" :class="{ 'no-img': !coverOf(it) }">
              <img
                v-if="coverOf(it)"
                :src="coverOf(it)"
                :alt="it.name"
                :title="it.name + ' · 秘籍插画'"
                loading="lazy"
              />
              <span v-else class="cover-fallback" aria-hidden="true">{{ it.category.slice(0, 2) }}</span>
              <span class="cover-grad" aria-hidden="true"></span>
            </div>
            <div class="item-head">
              <span class="grade">{{ it.grade }}阶</span>
              <span class="type">{{ it.category }}</span>
              <span class="realm-tag">{{ it.realm }}</span>
            </div>
            <h3 class="item-name">{{ it.name }}</h3>
            <p class="item-desc">{{ it.desc }}</p>
            <p class="item-origin">出处 · {{ it.origin }}</p>
          </article>
        </div>
        <div v-else class="empty">
          <div class="cloud" aria-hidden="true">﹀﹀﹀</div>
          <p>藏经阁中未见此典。</p>
          <p class="empty-hint">道友可调整筛选，或换个名目寻索。</p>
        </div>
      </section>

      <!-- ===== 洞府（道友信息） ===== -->
      <section class="block cave" id="cave">
        <h2 class="block-title"><i class="orn"></i>洞府<i class="orn r"></i></h2>
        <div class="cave-card">
          <div class="seal">藏</div>
          <div class="cave-info">
            <p class="dao-name">道友 <b>{{ auth.user || '无名' }}</b></p>
            <p class="realm">当前境界 · <b>{{ auth.profile?.realm || '炼气' }}</b></p>
            <p class="cave-desc">洞府灵气充裕，可安心打坐修炼。仙途漫漫，道友且行且惜。</p>
          </div>
          <button class="btn-exit big" @click="onLogout">退归山林</button>
        </div>
      </section>

      <!-- ===== 论道（页脚） ===== -->
      <footer class="footer" id="dao">
        <i class="orn"></i>
        <p>仙路争锋 · 逆天改命 —— 凡人修仙传</p>
        <p class="footer-dim">藏经阁 · 演示前端（后端接口：/api/manuals · /api/manuals/facets）</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { fetchManuals, fetchFacets } from '../api/sutra'
import { manualImage } from '../assets/manuals'

const auth = useAuthStore()
const router = useRouter()

const keyword = ref('')
const items = ref([])
const facets = reactive({ categories: [], realms: [], grades: [] })
const activeCategory = ref('全部')
const activeGrade = ref('全部')
const activeRealm = ref('全部')
const sourceNote = ref('')
const hotKeys = ['青元剑诀', '梵圣真魔功', '春黎剑阵', '大衍诀', '筑基丹丹方', '血影遁']
const searchStamp = ref(0)     // 每次刷新自增，让卡片重新播放入场动画
const activeNav = ref('')
const shell = ref(null)

const SECTIONS = ['repository', 'cave', 'dao']

// 取功法封面：优先接口返回的 image 字段（数据库 t_manual.image），
// 为相对路径时拼接后端地址；接口未带（本地 mock 模式）则用前端清单兜底。
const API_BASE = import.meta.env.VITE_API_BASE || ''
function coverOf(it) {
  if (it && it.image) {
    return it.image.startsWith('http') ? it.image : API_BASE + it.image
  }
  return manualImage(it?.name)
}

onMounted(async () => {
  // 拉取道友信息（后端未启动时静默降级）
  try { await auth.fetchProfile() } catch (_) { /* 后端未连，用本地信息 */ }
  // 拉取筛选维度 + 首屏全部秘籍
  const f = await fetchFacets()
  facets.categories = f.categories
  facets.realms = f.realms
  facets.grades = f.grades
  await loadManuals()
  // scrollspy
  if (shell.value) shell.value.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onUnmounted(() => {
  if (shell.value) shell.value.removeEventListener('scroll', onScroll)
})

// 筛选变化即重新拉取
watch([activeCategory, activeGrade, activeRealm], () => loadManuals())

async function loadManuals() {
  const res = await fetchManuals({
    category: activeCategory.value,
    grade: activeGrade.value,
    realm: activeRealm.value,
    q: keyword.value
  })
  items.value = res.items
  searchStamp.value++
  sourceNote.value =
    res.source === 'remote' ? ''
      : res.source === 'fallback' ? '（后端未连，暂阅本地藏经）'
      : '（本地藏经模式）'
}

function onSearch() {
  loadManuals()
  scrollTo('repository')
}

function quickSearch(k) {
  keyword.value = k
  onSearch()
}

function onScroll() {
  const el = shell.value
  if (!el) return
  const probe = el.scrollTop + el.clientHeight * 0.35
  let current = ''
  for (const id of SECTIONS) {
    const node = document.getElementById(id)
    if (node && node.offsetTop <= probe) current = id
  }
  activeNav.value = current
}

function scrollTo(id) {
  if (id === 'top') {
    shell.value?.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goDetail(id) {
  router.push({ name: 'manual', params: { id } })
}

function goAdmin() {
  router.push('/admin/login')
}

function onLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.stage{
  position:relative;z-index:2;height:100%;width:100%;
}
.home-shell{
  height:100%;overflow-y:auto;overflow-x:hidden;
  scroll-behavior:smooth;
}
/* 金色细滚动条 */
.home-shell::-webkit-scrollbar{width:8px}
.home-shell::-webkit-scrollbar-track{background:rgba(7,10,20,.4)}
.home-shell::-webkit-scrollbar-thumb{
  border-radius:8px;background:linear-gradient(180deg,rgba(233,200,120,.5),rgba(233,200,120,.2));
}
.home-shell::-webkit-scrollbar-thumb:hover{background:rgba(233,200,120,.65)}

/* ===== 导航栏 ===== */
.navbar{
  position:sticky;top:0;z-index:10;
  display:flex;align-items:center;gap:24px;
  padding:0 28px;height:64px;
  background:rgba(7,10,20,.72);
  backdrop-filter:blur(12px);
  border-bottom:1px solid var(--line);
}
.navbar::after{
  content:'';position:absolute;left:0;right:0;bottom:-1px;height:1px;pointer-events:none;
  background:linear-gradient(90deg,transparent,rgba(233,200,120,.35) 50%,transparent);
}
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
.btn-exit.big{padding:13px 26px;font-size:16px;letter-spacing:6px}

/* ===== 顶部搜索 ===== */
.hero{
  position:relative;text-align:center;overflow:hidden;
  padding:88px 24px 56px;
}
.watermark{
  position:absolute;left:50%;top:38%;transform:translate(-50%,-50%);
  font-size:min(340px,60vw);font-weight:700;line-height:1;
  background:linear-gradient(180deg,rgba(233,200,120,.10),rgba(233,200,120,.02) 70%);
  -webkit-background-clip:text;background-clip:text;color:transparent;
  pointer-events:none;user-select:none;
  animation:floaty 9s ease-in-out infinite alternate;
}
@keyframes floaty{
  from{transform:translate(-50%,-52%)}
  to{transform:translate(-50%,-46%)}
}
.title{
  position:relative;
  font-size:52px;letter-spacing:22px;text-indent:22px;
  background:linear-gradient(180deg,#fff7e6,var(--gold) 55%,#b98e34);
  -webkit-background-clip:text;background-clip:text;color:transparent;
  filter:drop-shadow(0 0 26px rgba(233,200,120,.22));
  animation:rise .9s cubic-bezier(.2,.8,.2,1) both, glow 4s 1s ease-in-out infinite;
}
@keyframes glow{
  0%,100%{filter:drop-shadow(0 0 18px rgba(233,200,120,.16))}
  50%{filter:drop-shadow(0 0 34px rgba(233,200,120,.34))}
}
.subtitle{
  position:relative;display:flex;align-items:center;justify-content:center;gap:16px;
  margin-top:12px;color:var(--ink-dim);font-size:15px;letter-spacing:8px;
  animation:rise .9s .1s cubic-bezier(.2,.8,.2,1) both;
}
.sub-line{
  width:70px;height:1px;flex:none;
  background:linear-gradient(90deg,transparent,rgba(139,155,189,.5));
}
.sub-line.r{background:linear-gradient(90deg,rgba(139,155,189,.5),transparent)}
.search-bar{
  position:relative;margin:40px auto 0;width:min(680px,92vw);
  display:flex;align-items:center;gap:6px;
  padding:8px 8px 8px 18px;border-radius:999px;
  border:1px solid var(--line);
  background:linear-gradient(180deg,rgba(20,29,54,.65),rgba(7,10,20,.75));
  backdrop-filter:blur(8px);
  box-shadow:0 18px 50px rgba(0,0,0,.45), 0 0 0 1px rgba(233,200,120,.05) inset;
  transition:.25s;
  animation:rise .9s .2s cubic-bezier(.2,.8,.2,1) both;
}
.search-bar:focus-within{
  border-color:rgba(233,200,120,.45);
  box-shadow:0 18px 50px rgba(0,0,0,.45), 0 0 30px rgba(233,200,120,.15);
}
.search-icon{
  color:var(--gold);font-size:15px;letter-spacing:0;opacity:.9;
  width:22px;text-align:center;
}
.search-bar input{
  flex:1;border:none;outline:none;background:transparent;
  color:var(--ink);font-family:inherit;font-size:16px;letter-spacing:1px;
  padding:10px 6px;
}
.search-bar input::placeholder{color:rgba(139,155,189,.65)}
.btn-search{
  position:relative;border:none;border-radius:999px;cursor:pointer;overflow:hidden;
  padding:12px 30px;font-family:inherit;font-size:15px;font-weight:700;letter-spacing:6px;
  color:#0c1322;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  box-shadow:0 8px 26px rgba(233,200,120,.30);transition:.25s;
}
.btn-search span{position:relative;z-index:1}
.btn-search::before{
  content:'';position:absolute;top:0;left:-80%;width:50%;height:100%;
  background:linear-gradient(105deg,transparent,rgba(255,255,255,.5),transparent);
  transform:skewX(-20deg);transition:left .55s ease;
}
.btn-search:hover{transform:translateY(-1px);box-shadow:0 12px 34px rgba(233,200,120,.45)}
.btn-search:hover::before{left:130%}
.hot{
  position:relative;margin-top:20px;display:flex;justify-content:center;align-items:center;gap:14px;flex-wrap:wrap;
  animation:rise .9s .3s cubic-bezier(.2,.8,.2,1) both;
}
.hot-label{color:var(--ink-dim);font-size:13px;letter-spacing:2px}
.hot a{
  color:var(--qi);font-size:13px;letter-spacing:1px;cursor:pointer;
  padding:4px 12px;border-radius:999px;border:1px solid transparent;transition:.2s;
}
.hot a:hover{
  border-color:rgba(103,232,249,.35);background:rgba(103,232,249,.08);
}
.source-note{
  position:relative;margin-top:14px;color:var(--ink-dim);font-size:12px;letter-spacing:2px;opacity:.8;
}

/* ===== 区块 ===== */
.block{padding:34px 28px 10px;max-width:1080px;margin:0 auto}
.block-title{
  display:flex;align-items:center;gap:16px;justify-content:center;
  color:var(--ink);font-size:20px;letter-spacing:8px;font-weight:700;margin-bottom:26px;
}
.orn{position:relative;flex:none;width:110px;height:1px}
.orn::before{
  content:'';position:absolute;inset:0;
  background:linear-gradient(90deg,transparent,rgba(233,200,120,.6));
}
.orn::after{
  content:'';position:absolute;right:-4px;top:-3px;
  width:7px;height:7px;transform:rotate(45deg);
  background:var(--gold);box-shadow:0 0 8px rgba(233,200,120,.6);
}
.orn.r::before{background:linear-gradient(90deg,rgba(233,200,120,.6),transparent)}
.orn.r::after{right:auto;left:-4px}

/* ===== 筛选器 ===== */
.filters{
  margin-bottom:26px;
  padding:18px 20px;border-radius:16px;
  border:1px solid var(--line);
  background:linear-gradient(180deg,rgba(20,29,54,.45),rgba(7,10,20,.55));
  backdrop-filter:blur(6px);
}
.filter-row{
  display:flex;align-items:flex-start;gap:14px;
  padding:8px 0;
}
.filter-row + .filter-row{border-top:1px dashed var(--line)}
.filter-label{
  flex:none;width:48px;padding-top:6px;
  color:var(--gold);font-size:14px;letter-spacing:3px;font-weight:700;
}
.chips{display:flex;flex-wrap:wrap;gap:8px;flex:1}
.chip{
  padding:6px 14px;border-radius:999px;cursor:pointer;font-family:inherit;
  border:1px solid var(--line);
  background:rgba(20,29,54,.4);color:var(--ink-dim);
  font-size:13px;letter-spacing:1px;transition:.2s;
}
.chip:hover{color:var(--gold-2);border-color:rgba(233,200,120,.3)}
.chip.active{
  color:#0c1322;font-weight:700;
  background:linear-gradient(135deg,var(--gold-2),var(--gold));
  border-color:transparent;
}
/* 品阶 chip 选中配色（天金 / 地紫 / 玄青 / 黄灰）*/
.chip.grade-天.active{background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#0c1322}
.chip.grade-地.active{background:rgba(139,109,255,.8);color:#fff}
.chip.grade-玄.active{background:linear-gradient(135deg,#8be6f6,var(--qi));color:#062024}
.chip.grade-黄.active{background:rgba(139,155,189,.4);color:var(--ink)}
.realm-select{
  font-family:inherit;font-size:13px;letter-spacing:1px;
  padding:7px 14px;border-radius:10px;cursor:pointer;
  border:1px solid var(--line);color:var(--ink);
  background:rgba(7,10,20,.7);outline:none;transition:.2s;
}
.realm-select:focus{border-color:rgba(233,200,120,.45)}
.realm-select option{background:#0d1426;color:var(--ink)}
.count{
  margin-left:auto;align-self:center;
  color:var(--ink-dim);font-size:13px;letter-spacing:2px;
}

/* 卡片网格 */
.grid{
  display:grid;grid-template-columns:repeat(auto-fill,minmax(264px,1fr));gap:18px;
}
.item{
  position:relative;overflow:hidden;
  padding:20px 20px 18px;border-radius:16px;
  border:1px solid var(--line);
  background:linear-gradient(180deg,rgba(20,29,54,.55),rgba(7,10,20,.65));
  backdrop-filter:blur(6px);
  transition:transform .25s, border-color .25s, box-shadow .25s;
  animation:rise .7s cubic-bezier(.2,.8,.2,1) both;
  cursor:pointer;
}
/* 悬停时右上角浮现「细览 ›」引导点击 */
.enter-hint{
  position:absolute;top:12px;right:14px;
  font-size:13px;letter-spacing:2px;color:var(--gold-2);
  background:rgba(7,10,20,.6);border:1px solid rgba(233,200,120,.3);
  padding:2px 10px;border-radius:999px;
  opacity:0;transform:translateY(-4px);transition:.25s;
  z-index:2;
}
.item:hover .enter-hint{opacity:1;transform:translateY(0)}
.item::before{
  content:'';position:absolute;left:0;top:14px;bottom:14px;width:3px;border-radius:3px;
  background:var(--ink-dim);opacity:.5;transition:.25s;
}
.item:hover{
  transform:translateY(-4px);
  border-color:rgba(233,200,120,.35);
  box-shadow:0 16px 40px rgba(0,0,0,.5), 0 0 24px rgba(233,200,120,.12);
}
.item:hover::before{top:8px;bottom:8px;opacity:1}
/* ===== 卡片封面插画 ===== */
.item-cover{
  position:relative;
  margin:-20px -20px 14px;      /* 铺满卡片顶部（卡片内边距 20px） */
  height:150px;overflow:hidden;
  background:rgba(20,29,54,.6);
}
.item-cover img{
  width:100%;height:100%;object-fit:cover;display:block;
  transform:scale(1.02);
  transition:transform .5s cubic-bezier(.2,.8,.2,1);
}
.item:hover .item-cover img{transform:scale(1.09)}
.cover-grad{
  position:absolute;inset:0;pointer-events:none;
  background:linear-gradient(180deg,rgba(7,10,20,.05) 40%,rgba(7,10,20,.55) 82%,rgba(7,10,20,.8));
}
/* 无插画时的分类兜底底纹 */
.item-cover.no-img{
  display:grid;place-items:center;
  background:
    radial-gradient(70% 90% at 50% 110%,rgba(233,200,120,.16),transparent 70%),
    linear-gradient(180deg,rgba(20,29,54,.9),rgba(7,10,20,.9));
}
.cover-fallback{
  font-size:44px;font-weight:700;letter-spacing:6px;text-indent:6px;
  background:linear-gradient(180deg,#fff7e6,var(--gold) 60%,#b98e34);
  -webkit-background-clip:text;background-clip:text;color:transparent;
  opacity:.85;
}
.item-head{display:flex;align-items:center;gap:8px;margin-bottom:12px;flex-wrap:wrap}
.grade{
  font-size:14px;letter-spacing:2px;padding:3px 10px;border-radius:6px;font-weight:700;
}
.type{
  font-size:14px;letter-spacing:2px;color:var(--ink-dim);
  border:1px solid var(--line);padding:2px 9px;border-radius:6px;
}
.realm-tag{
  font-size:13px;letter-spacing:1px;color:var(--qi);
  border:1px solid rgba(103,232,249,.3);padding:2px 8px;border-radius:6px;
}
.item-name{font-size:19px;letter-spacing:2px;color:var(--ink);margin-bottom:8px}
.item-desc{font-size:15px;line-height:1.8;color:var(--ink-dim);letter-spacing:.5px}
.item-origin{
  margin-top:10px;font-size:14px;letter-spacing:1px;color:var(--gold);
  opacity:.8;
}
/* 悬停右上角菱形光点 */
.corner{
  position:absolute;top:12px;right:14px;width:8px;height:8px;
  transform:rotate(45deg) scale(0);transition:transform .3s .05s;
  background:linear-gradient(135deg,var(--gold-2),var(--gold));
  box-shadow:0 0 10px rgba(233,200,120,.7);
}
.item:hover .corner{transform:rotate(45deg) scale(1)}

/* 品阶配色（徽章 + 左侧竖条）*/
.item[data-grade="天"] .grade{color:#0c1322;background:linear-gradient(135deg,var(--gold-2),var(--gold))}
.item[data-grade="天"]::before{background:var(--gold)}
.item[data-grade="地"] .grade{color:#fff;background:rgba(139,109,255,.75)}
.item[data-grade="地"]::before{background:var(--violet)}
.item[data-grade="玄"] .grade{color:#062024;background:linear-gradient(135deg,#8be6f6,var(--qi))}
.item[data-grade="玄"]::before{background:var(--qi)}
.item[data-grade="黄"] .grade{color:var(--ink-dim);background:rgba(139,155,189,.2);border:1px solid var(--line)}
.item[data-grade="黄"]::before{background:var(--ink-dim)}
.item[data-grade="天"]::after{
  content:'';position:absolute;inset:0;pointer-events:none;
  background:radial-gradient(60% 40% at 80% 0%,rgba(233,200,120,.10),transparent 70%);
}

/* 空结果 */
.empty{text-align:center;padding:48px 0 30px;color:var(--ink-dim);letter-spacing:3px}
.cloud{
  font-size:22px;letter-spacing:10px;opacity:.45;margin-bottom:14px;
  color:var(--ink-dim);
  animation:floaty2 5s ease-in-out infinite alternate;
}
@keyframes floaty2{from{transform:translateX(-8px)}to{transform:translateX(8px)}}
.empty-hint{font-size:13px;opacity:.75;margin-top:10px}

/* ===== 洞府 ===== */
.cave-card{
  position:relative;overflow:hidden;
  display:flex;align-items:center;gap:28px;flex-wrap:wrap;
  padding:34px 34px;border-radius:20px;
  border:1px solid var(--line);
  background:linear-gradient(180deg,rgba(20,29,54,.55),rgba(7,10,20,.65));
  backdrop-filter:blur(6px);box-shadow:0 30px 80px rgba(0,0,0,.45);
}
.cave-card::before{
  content:'';position:absolute;inset:0;pointer-events:none;
  background:radial-gradient(45% 60% at 12% 20%,rgba(233,200,120,.08),transparent 70%);
}
.cave-card .seal{
  width:56px;height:56px;border-radius:14px;display:grid;place-items:center;
  background:linear-gradient(145deg,var(--gold),#b88a3c);color:#1a1206;font-weight:700;font-size:26px;
  box-shadow:0 0 22px rgba(233,200,120,.45);
  transition:.4s;
}
.cave-card:hover .seal{transform:rotate(-8deg) scale(1.05);box-shadow:0 0 34px rgba(233,200,120,.6)}
.cave-info{flex:1;min-width:220px}
.dao-name{color:var(--ink-dim);font-size:15px;letter-spacing:2px}
.dao-name b{color:var(--qi);font-weight:600;font-size:17px}
.realm{color:var(--ink-dim);font-size:13px;letter-spacing:2px;margin-top:6px}
.realm b{color:var(--gold-2)}
.cave-desc{color:var(--ink-dim);font-size:13px;line-height:1.9;letter-spacing:1px;margin-top:12px;opacity:.85}

/* ===== 页脚 ===== */
.footer{
  text-align:center;padding:52px 20px 40px;
  color:var(--ink-dim);font-size:14px;letter-spacing:4px;
}
.footer .orn{margin:0 auto 22px;display:block;width:160px}
.footer .orn::before{background:linear-gradient(90deg,transparent,rgba(233,200,120,.45),transparent)}
.footer-dim{font-size:12px;opacity:.6;margin-top:10px;letter-spacing:2px}

@keyframes rise{from{opacity:0;transform:translateY(18px)}to{opacity:1;transform:none}}

/* 移动端 */
@media(max-width:760px){
  .navbar{padding:0 16px;gap:10px;height:58px}
  .brand-name{display:none}
  .nav-links a{padding:7px 9px;letter-spacing:1px;font-size:13px}
  .nav-admin{padding:6px 11px;font-size:12px;letter-spacing:1px}
  .user-chip{display:none}
  .title{font-size:36px;letter-spacing:12px;text-indent:12px}
  .subtitle{letter-spacing:4px}
  .sub-line{width:36px}
  .orn{width:50px}
  .cave-card{flex-direction:column;text-align:center}
  .realm-select{width:100%}
  .count{margin-left:0;width:100%;text-align:right}
  .watermark{font-size:70vw}
}
</style>
