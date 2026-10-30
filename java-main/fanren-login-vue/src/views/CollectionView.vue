<template>
  <div class="stage">
    <div class="list-shell" ref="shell">

      <!-- ===== 顶部：搜索 ===== -->
      <section class="hero">
        <div class="watermark" aria-hidden="true">{{ isTreasure ? '宝' : '藏' }}</div>
        <h1 class="title">{{ isTreasure ? '法宝阁' : '藏 经 阁' }}</h1>
        <p class="subtitle">
          <i class="sub-line"></i>
          {{ isTreasure ? '珍奇宝物 · 按类寻踪' : '功法秘籍 · 按图索骥' }}
          <i class="sub-line r"></i>
        </p>

        <div class="search-bar">
          <span class="search-icon">寻</span>
          <input
            v-model="keyword"
            type="text"
            :placeholder="isTreasure ? '搜罗法宝、飞剑、奇物、遁法……' : '搜罗功法、秘术、神通、丹方……'"
            @keyup.enter="onSearch"
          />
          <button class="btn-search" @click="onSearch"><span>寻 典</span></button>
        </div>

        <div v-if="hotKeys.length" class="hot">
          <span class="hot-label">热门：</span>
          <a v-for="k in hotKeys" :key="k" @click.prevent="quickSearch(k)">{{ k }}</a>
        </div>

        <div v-if="sourceNote" class="source-note">{{ sourceNote }}</div>
      </section>

      <!-- ===== 列表（功法 / 法宝 共用） ===== -->
      <section class="block">
        <h2 class="block-title">
          <i class="orn"></i>
          {{ isTreasure ? '珍奇宝物总录' : '功法秘籍总录' }}
          <i class="orn r"></i>
        </h2>

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

        <!-- 卡片网格 -->
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
                :title="it.name + (isTreasure ? ' · 法宝插画' : ' · 秘籍插画')"
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
            <p class="item-price"><span class="price-icon" aria-hidden="true">灵</span>{{ priceOf(it) }} 灵石</p>
          </article>
        </div>
        <div v-else class="empty">
          <div class="cloud" aria-hidden="true">﹀﹀﹀</div>
          <p>{{ isTreasure ? '法宝阁中未见此宝。' : '藏经阁中未见此典。' }}</p>
          <p class="empty-hint">道友可调整筛选，或换个名目寻索。</p>
        </div>
      </section>

      <!-- ===== 页脚 ===== -->
      <footer class="footer">
        <i class="orn"></i>
        <p>仙路争锋 · 逆天改命 —— 凡人修仙传</p>
        <p class="footer-dim">{{ isTreasure ? '法宝阁 · 演示前端' : '藏经阁 · 演示前端' }}</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchManuals, fetchFacets } from '../api/sutra'
import { manualImage } from '../assets/manuals'
import { fetchTreasures, fetchTreasureFacets } from '../api/treasure'
import { treasureImage } from '../assets/treasures'

const route = useRoute()
const router = useRouter()

const isTreasure = computed(() => route.meta.type === 'treasure')
const API_BASE = import.meta.env.VITE_API_BASE || ''

const keyword = ref((route.query.q && String(route.query.q)) || '')
const items = ref([])
const facets = reactive({ categories: [], realms: [], grades: [] })
const activeCategory = ref('全部')
const activeGrade = ref('全部')
const activeRealm = ref('全部')
const sourceNote = ref('')
const searchStamp = ref(0)
const shell = ref(null)

const MANUAL_HOT = ['青元剑诀', '梵圣真魔功', '春黎剑阵', '大衍诀', '筑基丹丹方', '血影遁']
const TREASURE_HOT = ['青竹蜂云剑', '玄天斩灵剑', '风雷翅', '虚天鼎', '七焰扇', '聚宝盆']
const hotKeys = computed(() => (isTreasure.value ? TREASURE_HOT : MANUAL_HOT))

// 封面：优先接口 image（数据库字段，后端静态资源）；
// 相对路径则拼接后端地址；无图时回退本地打包清单（功法有、法宝暂无 → 显示分类兜底）。
function coverOf(it) {
  if (it && it.image) {
    return it.image.startsWith('http') ? it.image : API_BASE + it.image
  }
  return isTreasure.value ? treasureImage(it?.name) : manualImage(it?.name)
}

// 价格：优先接口 price 字段；缺失时按品阶×境界×类别估算兜底
const PRICE_POOL = { 天: 2000, 地: 800, 玄: 300, 黄: 100 }
const PRICE_REALM = { 练气期: 1, 筑基期: 1.5, 金丹期: 2.5, 元婴期: 4, 化神期: 6, 炼虚期: 8, 合体期: 10, 大乘期: 12, 渡劫期: 15 }
const PRICE_MANUAL_CAT = { 主修功法: 1.0, 神通: 1.2, 炼丹: 0.8, 符箓: 0.8, 阵法: 0.8, 傀儡: 0.8, 炼器: 0.8, 法术: 0.6, 秘术: 0.6 }
const PRICE_TREASURE_CAT = { 飞剑: 1.0, 攻伐: 1.0, 护身: 1.0, 遁法: 0.9, 奇物: 1.1, 储物: 0.5 }

function priceOf(it) {
  if (!it) return 0
  if (it.price != null) return it.price
  const pool = (PRICE_POOL[it.grade] || 100) * (PRICE_REALM[it.realm] || 1)
  const coef = isTreasure.value
    ? (PRICE_TREASURE_CAT[it.category] || 1.0)
    : (PRICE_MANUAL_CAT[it.category] || 1.0)
  return Math.round(pool * coef)
}

function setSourceNote(source) {
  sourceNote.value =
    source === 'remote' ? ''
      : source === 'fallback' ? '（后端未连，暂阅本地藏经）'
      : '（本地藏经模式）'
}

async function loadFacets() {
  if (isTreasure.value) {
    const tf = await fetchTreasureFacets()
    facets.categories = tf.categories
    facets.realms = tf.realms
    facets.grades = tf.grades
  } else {
    const f = await fetchFacets()
    facets.categories = f.categories
    facets.realms = f.realms
    facets.grades = f.grades
  }
}

async function loadItems() {
  const params = {
    category: activeCategory.value,
    grade: activeGrade.value,
    realm: activeRealm.value,
    q: keyword.value
  }
  if (isTreasure.value) {
    const res = await fetchTreasures(params)
    items.value = res.items
    setSourceNote(res.source)
  } else {
    const res = await fetchManuals(params)
    items.value = res.items
    setSourceNote(res.source)
  }
  searchStamp.value++
}

function onSearch() {
  loadItems()
}
function quickSearch(k) {
  keyword.value = k
  onSearch()
}

function goDetail(id) {
  router.push(isTreasure.value
    ? { name: 'treasure', params: { id } }
    : { name: 'manual', params: { id } })
}

onMounted(async () => {
  await loadFacets()
  await loadItems()
  if (shell.value) shell.value.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onUnmounted(() => {
  if (shell.value) shell.value.removeEventListener('scroll', onScroll)
})

watch([activeCategory, activeGrade, activeRealm], () => loadItems())

// 藏经阁 / 法宝阁共用本组件：Vue Router 在 /sutras ↔ /treasures 间切换时会复用组件实例，
// onMounted 不再执行，列表不会重载（切到法宝阁仍显示功法）。此处监听 meta.type 变化，
// 切换即重置筛选并重新拉取对应数据，确保内容随导航栏切换而改变。
watch(() => route.meta.type, async () => {
  activeCategory.value = '全部'
  activeGrade.value = '全部'
  activeRealm.value = '全部'
  keyword.value = (route.query.q && String(route.query.q)) || ''
  await loadFacets()
  await loadItems()
})

function onScroll() {
  // 列表页无需 scrollspy 高亮，保留占位以便后续扩展
}
</script>

<style scoped>
.stage{position:relative;z-index:2;height:100%;width:100%}
.list-shell{height:100%;overflow-y:auto;overflow-x:hidden;scroll-behavior:smooth}
.list-shell::-webkit-scrollbar{width:8px}
.list-shell::-webkit-scrollbar-track{background:rgba(7,10,20,.4)}
.list-shell::-webkit-scrollbar-thumb{
  border-radius:8px;background:linear-gradient(180deg,rgba(233,200,120,.5),rgba(233,200,120,.2));
}

/* 源说明（列表页置于搜索区下方居中展示） */
.source-note{margin:16px 0 0;text-align:center;color:var(--ink-dim);font-size:12px;letter-spacing:2px;opacity:.8}

/* ===== 顶部搜索 ===== */
.hero{position:relative;text-align:center;overflow:hidden;padding:88px 24px 56px}
.watermark{
  position:absolute;left:50%;top:38%;transform:translate(-50%,-50%);
  font-size:min(340px,60vw);font-weight:700;line-height:1;
  background:linear-gradient(180deg,rgba(233,200,120,.10),rgba(233,200,120,.02) 70%);
  -webkit-background-clip:text;background-clip:text;color:transparent;
  pointer-events:none;user-select:none;animation:floaty 9s ease-in-out infinite alternate;
}
@keyframes floaty{from{transform:translate(-50%,-52%)}to{transform:translate(-50%,-46%)}}
.title{
  position:relative;font-size:52px;letter-spacing:22px;text-indent:22px;
  background:linear-gradient(180deg,#fff7e6,var(--gold) 55%,#b98e34);
  -webkit-background-clip:text;background-clip:text;color:transparent;
  filter:drop-shadow(0 0 26px rgba(233,200,120,.22));
  animation:rise .9s cubic-bezier(.2,.8,.2,1) both, glow 4s 1s ease-in-out infinite;
}
@keyframes glow{0%,100%{filter:drop-shadow(0 0 18px rgba(233,200,120,.16))}50%{filter:drop-shadow(0 0 34px rgba(233,200,120,.34))}}
.subtitle{
  position:relative;display:flex;align-items:center;justify-content:center;gap:16px;
  margin-top:12px;color:var(--ink-dim);font-size:15px;letter-spacing:8px;
  animation:rise .9s .1s cubic-bezier(.2,.8,.2,1) both;
}
.sub-line{width:70px;height:1px;flex:none;background:linear-gradient(90deg,transparent,rgba(139,155,189,.5))}
.sub-line.r{background:linear-gradient(90deg,rgba(139,155,189,.5),transparent)}
.search-bar{
  position:relative;margin:40px auto 0;width:min(680px,92vw);display:flex;align-items:center;gap:6px;
  padding:8px 8px 8px 18px;border-radius:999px;border:1px solid var(--line);
  background:linear-gradient(180deg,rgba(20,29,54,.65),rgba(7,10,20,.75));backdrop-filter:blur(8px);
  box-shadow:0 18px 50px rgba(0,0,0,.45),0 0 0 1px rgba(233,200,120,.05) inset;transition:.25s;
  animation:rise .9s .2s cubic-bezier(.2,.8,.2,1) both;
}
.search-bar:focus-within{border-color:rgba(233,200,120,.45);box-shadow:0 18px 50px rgba(0,0,0,.45),0 0 30px rgba(233,200,120,.15)}
.search-icon{color:var(--gold);font-size:15px;letter-spacing:0;opacity:.9;width:22px;text-align:center}
.search-bar input{flex:1;border:none;outline:none;background:transparent;color:var(--ink);font-family:inherit;font-size:16px;letter-spacing:1px;padding:10px 6px}
.search-bar input::placeholder{color:rgba(139,155,189,.65)}
.btn-search{
  position:relative;border:none;border-radius:999px;cursor:pointer;overflow:hidden;
  padding:12px 30px;font-family:inherit;font-size:15px;font-weight:700;letter-spacing:6px;color:#0c1322;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);box-shadow:0 8px 26px rgba(233,200,120,.30);transition:.25s;
}
.btn-search span{position:relative;z-index:1}
.btn-search::before{content:'';position:absolute;top:0;left:-80%;width:50%;height:100%;background:linear-gradient(105deg,transparent,rgba(255,255,255,.5),transparent);transform:skewX(-20deg);transition:left .55s ease}
.btn-search:hover{transform:translateY(-1px);box-shadow:0 12px 34px rgba(233,200,120,.45)}
.btn-search:hover::before{left:130%}
.hot{position:relative;margin-top:20px;display:flex;justify-content:center;align-items:center;gap:14px;flex-wrap:wrap;animation:rise .9s .3s cubic-bezier(.2,.8,.2,1) both}
.hot-label{color:var(--ink-dim);font-size:13px;letter-spacing:2px}
.hot a{color:var(--qi);font-size:13px;letter-spacing:1px;cursor:pointer;padding:4px 12px;border-radius:999px;border:1px solid transparent;transition:.2s}
.hot a:hover{border-color:rgba(103,232,249,.35);background:rgba(103,232,249,.08)}

/* ===== 区块 ===== */
.block{padding:34px 28px 10px;max-width:1080px;margin:0 auto}
.block-title{display:flex;align-items:center;gap:16px;justify-content:center;color:var(--ink);font-size:20px;letter-spacing:8px;font-weight:700;margin-bottom:26px}
.orn{position:relative;flex:none;width:110px;height:1px}
.orn::before{content:'';position:absolute;inset:0;background:linear-gradient(90deg,transparent,rgba(233,200,120,.6))}
.orn::after{content:'';position:absolute;right:-4px;top:-3px;width:7px;height:7px;transform:rotate(45deg);background:var(--gold);box-shadow:0 0 8px rgba(233,200,120,.6)}
.orn.r::before{background:linear-gradient(90deg,rgba(233,200,120,.6),transparent)}
.orn.r::after{right:auto;left:-4px}

/* ===== 筛选器 ===== */
.filters{margin-bottom:26px;padding:18px 20px;border-radius:16px;border:1px solid var(--line);background:linear-gradient(180deg,rgba(20,29,54,.45),rgba(7,10,20,.55));backdrop-filter:blur(6px)}
.filter-row{display:flex;align-items:flex-start;gap:14px;padding:8px 0}
.filter-row + .filter-row{border-top:1px dashed var(--line)}
.filter-label{flex:none;width:48px;padding-top:6px;color:var(--gold);font-size:14px;letter-spacing:3px;font-weight:700}
.chips{display:flex;flex-wrap:wrap;gap:8px;flex:1}
.chip{padding:6px 14px;border-radius:999px;cursor:pointer;font-family:inherit;border:1px solid var(--line);background:rgba(20,29,54,.4);color:var(--ink-dim);font-size:13px;letter-spacing:1px;transition:.2s}
.chip:hover{color:var(--gold-2);border-color:rgba(233,200,120,.3)}
.chip.active{color:#0c1322;font-weight:700;background:linear-gradient(135deg,var(--gold-2),var(--gold));border-color:transparent}
.chip.grade-天.active{background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#0c1322}
.chip.grade-地.active{background:rgba(139,109,255,.8);color:#fff}
.chip.grade-玄.active{background:linear-gradient(135deg,#8be6f6,var(--qi));color:#062024}
.chip.grade-黄.active{background:rgba(139,155,189,.4);color:var(--ink)}
.realm-select{font-family:inherit;font-size:13px;letter-spacing:1px;padding:7px 14px;border-radius:10px;cursor:pointer;border:1px solid var(--line);color:var(--ink);background:rgba(7,10,20,.7);outline:none;transition:.2s}
.realm-select:focus{border-color:rgba(233,200,120,.45)}
.realm-select option{background:#0d1426;color:var(--ink)}
.count{margin-left:auto;align-self:center;color:var(--ink-dim);font-size:13px;letter-spacing:2px}

/* 卡片网格 */
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(264px,1fr));gap:18px}
.item{position:relative;overflow:hidden;padding:20px 20px 18px;border-radius:16px;border:1px solid var(--line);background:linear-gradient(180deg,rgba(20,29,54,.55),rgba(7,10,20,.65));backdrop-filter:blur(6px);transition:transform .25s,border-color .25s,box-shadow .25s;animation:rise .7s cubic-bezier(.2,.8,.2,1) both;cursor:pointer}
.enter-hint{position:absolute;top:12px;right:14px;font-size:13px;letter-spacing:2px;color:var(--gold-2);background:rgba(7,10,20,.6);border:1px solid rgba(233,200,120,.3);padding:2px 10px;border-radius:999px;opacity:0;transform:translateY(-4px);transition:.25s;z-index:2}
.item:hover .enter-hint{opacity:1;transform:translateY(0)}
.item::before{content:'';position:absolute;left:0;top:14px;bottom:14px;width:3px;border-radius:3px;background:var(--ink-dim);opacity:.5;transition:.25s}
.item:hover{transform:translateY(-4px);border-color:rgba(233,200,120,.35);box-shadow:0 16px 40px rgba(0,0,0,.5),0 0 24px rgba(233,200,120,.12)}
.item:hover::before{top:8px;bottom:8px;opacity:1}
.item-cover{position:relative;margin:-20px -20px 14px;height:150px;overflow:hidden;background:rgba(20,29,54,.6)}
.item-cover img{width:100%;height:100%;object-fit:cover;display:block;transform:scale(1.02);transition:transform .5s cubic-bezier(.2,.8,.2,1)}
.item:hover .item-cover img{transform:scale(1.09)}
.cover-grad{position:absolute;inset:0;pointer-events:none;background:linear-gradient(180deg,rgba(7,10,20,.05) 40%,rgba(7,10,20,.55) 82%,rgba(7,10,20,.8))}
.item-cover.no-img{display:grid;place-items:center;background:radial-gradient(70% 90% at 50% 110%,rgba(233,200,120,.16),transparent 70%),linear-gradient(180deg,rgba(20,29,54,.9),rgba(7,10,20,.9))}
.cover-fallback{font-size:44px;font-weight:700;letter-spacing:6px;text-indent:6px;background:linear-gradient(180deg,#fff7e6,var(--gold) 60%,#b98e34);-webkit-background-clip:text;background-clip:text;color:transparent;opacity:.85}
.item-head{display:flex;align-items:center;gap:8px;margin-bottom:12px;flex-wrap:wrap}
.grade{font-size:14px;letter-spacing:2px;padding:3px 10px;border-radius:6px;font-weight:700}
.type{font-size:14px;letter-spacing:2px;color:var(--ink-dim);border:1px solid var(--line);padding:2px 9px;border-radius:6px}
.realm-tag{font-size:13px;letter-spacing:1px;color:var(--qi);border:1px solid rgba(103,232,249,.3);padding:2px 8px;border-radius:6px}
.item-name{font-size:19px;letter-spacing:2px;color:var(--ink);margin-bottom:8px}
.item-desc{font-size:15px;line-height:1.8;color:var(--ink-dim);letter-spacing:.5px}
.item-origin{margin-top:10px;font-size:14px;letter-spacing:1px;color:var(--gold);opacity:.8}
.item-price{margin-top:8px;display:flex;align-items:center;gap:6px;font-size:15px;color:var(--gold-2);font-weight:700;letter-spacing:1px}
.price-icon{display:inline-flex;width:18px;height:18px;align-items:center;justify-content:center;border-radius:50%;font-size:11px;background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#0c1322;font-weight:700}
.corner{position:absolute;top:12px;right:14px;width:8px;height:8px;transform:rotate(45deg) scale(0);transition:transform .3s .05s;background:linear-gradient(135deg,var(--gold-2),var(--gold));box-shadow:0 0 10px rgba(233,200,120,.7)}
.item:hover .corner{transform:rotate(45deg) scale(1)}
.item[data-grade="天"] .grade{color:#0c1322;background:linear-gradient(135deg,var(--gold-2),var(--gold))}
.item[data-grade="天"]::before{background:var(--gold)}
.item[data-grade="地"] .grade{color:#fff;background:rgba(139,109,255,.75)}
.item[data-grade="地"]::before{background:var(--violet)}
.item[data-grade="玄"] .grade{color:#062024;background:linear-gradient(135deg,#8be6f6,var(--qi))}
.item[data-grade="玄"]::before{background:var(--qi)}
.item[data-grade="黄"] .grade{color:var(--ink-dim);background:rgba(139,155,189,.2);border:1px solid var(--line)}
.item[data-grade="黄"]::before{background:var(--ink-dim)}
.item[data-grade="天"]::after{content:'';position:absolute;inset:0;pointer-events:none;background:radial-gradient(60% 40% at 80% 0%,rgba(233,200,120,.10),transparent 70%)}

/* 空结果 */
.empty{text-align:center;padding:48px 0 30px;color:var(--ink-dim);letter-spacing:3px}
.cloud{font-size:22px;letter-spacing:10px;opacity:.45;margin-bottom:14px;color:var(--ink-dim);animation:floaty2 5s ease-in-out infinite alternate}
@keyframes floaty2{from{transform:translateX(-8px)}to{transform:translateX(8px)}}
.empty-hint{font-size:13px;opacity:.75;margin-top:10px}

/* ===== 页脚 ===== */
.footer{text-align:center;padding:52px 20px 40px;color:var(--ink-dim);font-size:14px;letter-spacing:4px}
.footer .orn{margin:0 auto 22px;display:block;width:160px}
.footer .orn::before{background:linear-gradient(90deg,transparent,rgba(233,200,120,.45),transparent)}
.footer-dim{font-size:12px;opacity:.6;margin-top:10px;letter-spacing:2px}

@keyframes rise{from{opacity:0;transform:translateY(18px)}to{opacity:1;transform:none}}

/* 移动端 */
@media(max-width:760px){
  .brand-name{display:none}
  .source-note{display:none}
  .title{font-size:36px;letter-spacing:12px;text-indent:12px}
  .subtitle{letter-spacing:4px}
  .sub-line{width:36px}
  .orn{width:50px}
  .block{padding:24px 16px 10px}
  .realm-select{width:100%}
  .count{margin-left:0;width:100%;text-align:right}
  .watermark{font-size:70vw}
}
</style>
