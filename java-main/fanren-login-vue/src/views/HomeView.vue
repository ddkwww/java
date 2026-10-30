<template>
  <div class="stage">
    <div class="home-shell" ref="shell">

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
      </section>

      <!-- ===== 当日新增功法 / 法宝（首页数据区块） ===== -->
      <section v-for="p in panels" :key="p.key" class="block">
        <h2 class="block-title"><i class="orn"></i>{{ p.title }}<i class="orn r"></i></h2>
        <p class="block-sub">
          {{ p.sub }} · 共 {{ p.items.length }} 卷
          <a class="more" @click.prevent="p.goAll()">查看全部 ›</a>
        </p>

        <div v-if="p.items.length" class="grid">
          <article
            v-for="(it, i) in p.items"
            :key="it.id"
            class="item"
            :data-grade="it.grade"
            :style="{ animationDelay: (i * 45) + 'ms' }"
            @click="p.goItem(it.id)"
          >
            <i class="corner" aria-hidden="true"></i>
            <span class="enter-hint" aria-hidden="true">细览 ›</span>
            <div class="item-cover" :class="{ 'no-img': !coverOf(it, p.key === 'treasure') }">
              <img
                v-if="coverOf(it, p.key === 'treasure')"
                :src="coverOf(it, p.key === 'treasure')"
                :alt="it.name"
                :title="it.name"
                loading="lazy"
              />
              <span v-else class="cover-fallback" aria-hidden="true">{{ (it.category || '').slice(0, 2) }}</span>
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
          <p>{{ p.empty }}</p>
          <p class="empty-hint">{{ p.emptyHint }}</p>
        </div>
      </section>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { fetchManuals } from '../api/sutra'
import { fetchTreasures } from '../api/treasure'
import { manualImage } from '../assets/manuals'
import { treasureImage } from '../assets/treasures'

const auth = useAuthStore()
const router = useRouter()

const API_BASE = import.meta.env.VITE_API_BASE || ''
const keyword = ref('')
const hotKeys = ['青元剑诀', '梵圣真魔功', '春黎剑阵', '大衍诀', '筑基丹丹方', '血影遁']

// 当日新增功法 / 法宝 数据
const todayManuals = ref([])
const todayTreasures = ref([])

// 是否为“今日新增”（与真实后端 created_at 字段一致）
function isToday(d) {
  if (!d) return false
  const t = new Date(d)
  const n = new Date()
  return t.getFullYear() === n.getFullYear() && t.getMonth() === n.getMonth() && t.getDate() === n.getDate()
}
function pickToday(items, limit = 6) {
  return items.filter((i) => isToday(i.createdAt)).slice(0, limit)
}

// 封面：优先接口 image（数据库字段 / 后端静态资源），否则回退本地打包清单
function coverOf(it, isTreasure) {
  if (it && it.image) {
    return it.image.startsWith('http') ? it.image : API_BASE + it.image
  }
  return isTreasure ? treasureImage(it?.name) : manualImage(it?.name)
}

// 两个首页数据区块（当日新增功法 / 法宝）
const panels = computed(() => [
  {
    key: 'manual',
    title: '当日新增功法',
    sub: '今日新入藏经阁的秘籍',
    empty: '今日藏经阁尚无新卷入藏。',
    emptyHint: '道友可前往藏经阁一观，或静候新典。',
    items: todayManuals.value,
    goAll: goSutras,
    goItem: goManual
  },
  {
    key: 'treasure',
    title: '法宝',
    sub: '今日新入法宝阁的灵珍',
    empty: '今日法宝阁尚无新宝入册。',
    emptyHint: '道友可前往法宝阁一观，或静候新宝。',
    items: todayTreasures.value,
    goAll: goTreasures,
    goItem: goTreasure
  }
])

function goSutras() { router.push('/sutras') }
function goTreasures() { router.push('/treasures') }
function goManual(id) { router.push({ name: 'manual', params: { id } }) }
function goTreasure(id) { router.push({ name: 'treasure', params: { id } }) }

// 首页搜索 → 跳转到藏经阁列表页，并带上关键字
function onSearch() {
  router.push({ path: '/sutras', query: keyword.value ? { q: keyword.value } : {} })
}
function quickSearch(k) {
  keyword.value = k
  onSearch()
}

function onLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(async () => {
  // 拉取道友信息（后端未启动时静默降级）
  try { await auth.fetchProfile() } catch (_) { /* 后端未连，用本地信息 */ }
  // 拉取当日新增功法 / 法宝
  const [m, t] = await Promise.all([fetchManuals(), fetchTreasures()])
  todayManuals.value = pickToday(m.items, 6)
  todayTreasures.value = pickToday(t.items, 6)
})
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

.block-sub{margin:-14px 0 22px;text-align:center;color:var(--ink-dim);font-size:13px;letter-spacing:2px}
.block-sub .more{
  margin-left:10px;color:var(--gold-2);cursor:pointer;
  border-bottom:1px dashed rgba(233,200,120,.4);padding-bottom:1px;
}
.block-sub .more:hover{color:var(--gold)}

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
.footer-dim{font-size:12px;opacity:.6;margin-top:10px}

@keyframes rise{from{opacity:0;transform:translateY(18px)}to{opacity:1;transform:none}}

/* 移动端 */
@media(max-width:760px){
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
