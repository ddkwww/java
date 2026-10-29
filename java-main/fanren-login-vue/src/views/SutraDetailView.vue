<template>
  <div class="stage">
    <div class="detail-shell">
      <!-- ===== 加载态 ===== -->
      <div v-if="loading" class="loading">
        <div class="spinner" aria-hidden="true"></div>
        <p>正在展卷……</p>
      </div>

      <!-- ===== 未寻得 ===== -->
      <div v-else-if="!manual" class="empty">
        <div class="cloud" aria-hidden="true">﹀﹀﹀</div>
        <p>未寻得此卷秘籍。</p>
        <p class="empty-hint">或已散佚，或编号有误，道友可返回藏经阁另觅他典。</p>
        <button class="back-btn" @click="goBack">返回藏经阁</button>
      </div>

      <!-- ===== 详情正文 ===== -->
      <article v-else class="detail">
        <div class="detail-watermark" aria-hidden="true">{{ manual.category.slice(0, 1) }}</div>

        <div class="top">
          <!-- 封面 -->
          <div class="cover-wrap">
            <img v-if="cover" :src="cover" :alt="manual.name" class="cover" />
            <div v-else class="cover fallback"><span>{{ manual.category.slice(0, 2) }}</span></div>
            <span class="cover-grad" aria-hidden="true"></span>
            <span class="grade-badge" :data-grade="manual.grade">{{ manual.grade }}阶</span>
          </div>

          <!-- 概要 -->
          <div class="info">
            <p class="cat">{{ manual.category }}</p>
            <h1 class="name">{{ manual.name }}</h1>
            <div class="meta">
              <span class="tag realm">{{ manual.realm }}</span>
              <span class="tag origin">出处 · {{ manual.origin }}</span>
            </div>
            <p class="lead">{{ manual.desc }}</p>
          </div>
        </div>

        <!-- 详述面板 -->
        <div class="desc-panel">
          <h2 class="panel-title"><i class="orn"></i>功法详述<i class="orn r"></i></h2>
          <p class="desc">{{ manual.desc }}</p>
          <div class="line-grid">
            <div class="line"><span class="k">分　类</span><span class="v">{{ manual.category }}</span></div>
            <div class="line"><span class="k">品　阶</span><span class="v" :data-grade="manual.grade">{{ manual.grade }}阶</span></div>
            <div class="line"><span class="k">境　界</span><span class="v">{{ manual.realm }}</span></div>
            <div class="line"><span class="k">出　处</span><span class="v">{{ manual.origin }}</span></div>
          </div>
        </div>
      </article>

      <!-- ===== 同脉功法 ===== -->
      <section v-if="manual && related.length" class="related block">
        <h2 class="block-title"><i class="orn"></i>同脉功法 · {{ manual.category }}<i class="orn r"></i></h2>
        <div class="grid">
          <article
            v-for="r in related"
            :key="r.id"
            class="item"
            :data-grade="r.grade"
            role="button"
            tabindex="0"
            @click="goDetail(r.id)"
            @keyup.enter="goDetail(r.id)"
          >
            <i class="corner" aria-hidden="true"></i>
            <span class="enter-hint" aria-hidden="true">细览 ›</span>
            <div class="item-cover" :class="{ 'no-img': !coverOf(r) }">
              <img v-if="coverOf(r)" :src="coverOf(r)" :alt="r.name" loading="lazy" />
              <span v-else class="cover-fallback">{{ r.category.slice(0, 2) }}</span>
              <span class="cover-grad" aria-hidden="true"></span>
            </div>
            <h3 class="item-name">{{ r.name }}</h3>
            <p class="item-origin">{{ r.realm }} · {{ r.grade }}阶</p>
          </article>
        </div>
      </section>

      <!-- ===== 页脚 ===== -->
      <footer class="footer">
        <i class="orn"></i>
        <p>仙路争锋 · 逆天改命 —— 凡人修仙传</p>
        <p class="footer-dim">藏经阁 · 演示前端（详情接口：GET /api/manuals/{id}）</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchManualDetail, fetchManuals } from '../api/sutra'
import { manualImage } from '../assets/manuals'

const route = useRoute()
const router = useRouter()

const manual = ref(null)
const loading = ref(true)
const related = ref([])
const sourceNote = ref('')

const API_BASE = import.meta.env.VITE_API_BASE || ''

// 取封面：优先接口 image 字段（数据库 t_manual.image，后端静态资源），
// 为相对路径则拼接后端地址；无图则用前端按名称映射的本地插画兜底。
function coverOf(it) {
  if (it && it.image) {
    return it.image.startsWith('http') ? it.image : API_BASE + it.image
  }
  return manualImage(it?.name)
}

async function load(id) {
  loading.value = true
  manual.value = null
  related.value = []
  sourceNote.value = ''
  const { manual: m, source } = await fetchManualDetail(id)
  manual.value = m
  loading.value = false
  if (m) {
    sourceNote.value =
      source === 'remote' ? '' : source === 'fallback' ? '（后端未连，暂阅本地藏经）' : '（本地藏经模式）'
    // 同脉功法：按分类取同门，排除自身，最多 6 卷
    const res = await fetchManuals({ category: m.category })
    related.value = (res.items || [])
      .filter((x) => String(x.id) !== String(id))
      .slice(0, 6)
  }
}

const cover = ref('')
watch(manual, (m) => { cover.value = m ? coverOf(m) : '' }, { immediate: true })

function goDetail(id) {
  router.push({ name: 'manual', params: { id } })
}

function goBack() {
  router.push({ name: 'home' })
}

onMounted(() => load(route.params.id))
watch(() => route.params.id, (id) => { if (id) load(id) })
</script>

<style scoped>
.stage {
  position: relative;
  z-index: 2;
  height: 100%;
  width: 100%;
}
.detail-shell {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  scroll-behavior: smooth;
  padding-top: 20px;
  padding-bottom: 30px;
}
.detail-shell::-webkit-scrollbar { width: 8px; }
.detail-shell::-webkit-scrollbar-track { background: rgba(7, 10, 20, .4); }
.detail-shell::-webkit-scrollbar-thumb {
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(233, 200, 120, .5), rgba(233, 200, 120, .2));
}

/* ===== 顶栏（已统一由全局导航栏承载，此处仅保留详情内容） ===== */

/* ===== 加载 / 空 ===== */
.loading, .empty {
  text-align: center;
  padding: 90px 20px;
  color: var(--ink-dim);
  letter-spacing: 3px;
}
.spinner {
  width: 42px; height: 42px; margin: 0 auto 18px;
  border-radius: 50%;
  border: 3px solid rgba(233, 200, 120, .18);
  border-top-color: var(--gold);
  animation: spin .9s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.cloud {
  font-size: 22px; letter-spacing: 10px; opacity: .45; margin-bottom: 14px;
  color: var(--ink-dim);
  animation: floaty2 5s ease-in-out infinite alternate;
}
@keyframes floaty2 { from { transform: translateX(-8px); } to { transform: translateX(8px); } }
.empty-hint { font-size: 13px; opacity: .75; margin-top: 10px; }
.back-btn {
  margin-top: 22px;
  padding: 11px 26px; border-radius: 10px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 4px; color: var(--gold);
  border: 1px solid rgba(233, 200, 120, .35); background: transparent;
  transition: .2s;
}
.back-btn:hover { background: rgba(233, 200, 120, .12); box-shadow: 0 0 16px rgba(233, 200, 120, .25); }

/* ===== 详情正文 ===== */
.detail { position: relative; max-width: 980px; margin: 0 auto; padding: 34px 28px 10px; }
.detail-watermark {
  position: absolute; right: 4%; top: 30px;
  font-size: min(220px, 36vw); font-weight: 700; line-height: 1;
  background: linear-gradient(180deg, rgba(233, 200, 120, .08), rgba(233, 200, 120, .02) 70%);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  pointer-events: none; user-select: none; z-index: 0;
}
.detail > * { position: relative; z-index: 1; }

.top { display: flex; gap: 30px; flex-wrap: wrap; }
.cover-wrap {
  position: relative;
  width: 280px; height: 360px; flex: none;
  border-radius: 18px; overflow: hidden;
  border: 1px solid var(--line);
  background: rgba(20, 29, 54, .6);
  box-shadow: 0 24px 60px rgba(0, 0, 0, .5);
}
.cover { width: 100%; height: 100%; object-fit: cover; display: block; }
.cover.fallback {
  display: grid; place-items: center;
  background:
    radial-gradient(70% 90% at 50% 110%, rgba(233, 200, 120, .16), transparent 70%),
    linear-gradient(180deg, rgba(20, 29, 54, .9), rgba(7, 10, 20, .9));
}
.cover.fallback span {
  font-size: 64px; font-weight: 700; letter-spacing: 8px; text-indent: 8px;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent; opacity: .85;
}
.cover-grad {
  position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(180deg, rgba(7, 10, 20, .05) 40%, rgba(7, 10, 20, .55) 82%, rgba(7, 10, 20, .8));
}
.grade-badge {
  position: absolute; left: 14px; bottom: 14px; z-index: 2;
  padding: 4px 12px; border-radius: 8px; font-weight: 700; font-size: 15px; letter-spacing: 1px;
}
.grade-badge[data-grade="天"] { color: #0c1322; background: linear-gradient(135deg, var(--gold-2), var(--gold)); }
.grade-badge[data-grade="地"] { color: #fff; background: rgba(139, 109, 255, .8); }
.grade-badge[data-grade="玄"] { color: #062024; background: linear-gradient(135deg, #8be6f6, var(--qi)); }
.grade-badge[data-grade="黄"] { color: var(--ink-dim); background: rgba(139, 155, 189, .25); border: 1px solid var(--line); }

.info { flex: 1; min-width: 260px; padding-top: 6px; }
.cat {
  color: var(--gold); font-size: 14px; letter-spacing: 4px; margin-bottom: 12px;
}
.name {
  font-size: 42px; letter-spacing: 4px; line-height: 1.2;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 22px rgba(233, 200, 120, .18));
  margin-bottom: 18px;
}
.meta { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 22px; }
.tag {
  padding: 5px 14px; border-radius: 999px; font-size: 14px; letter-spacing: 1px;
  border: 1px solid var(--line); color: var(--ink-dim);
}
.tag.realm { color: var(--qi); border-color: rgba(103, 232, 249, .3); }
.tag.origin { color: var(--gold); border-color: rgba(233, 200, 120, .3); }
.lead {
  font-size: 16px; line-height: 1.95; letter-spacing: .5px; color: var(--ink-dim);
  border-left: 3px solid var(--gold); padding-left: 16px;
}

/* ===== 详述面板 ===== */
.desc-panel {
  margin-top: 30px;
  padding: 26px 28px; border-radius: 18px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(20, 29, 54, .5), rgba(7, 10, 20, .6));
  backdrop-filter: blur(6px);
}
.panel-title {
  display: flex; align-items: center; gap: 16px; justify-content: center;
  color: var(--ink); font-size: 18px; letter-spacing: 6px; font-weight: 700; margin-bottom: 20px;
}
.orn { position: relative; flex: none; width: 90px; height: 1px; }
.orn::before {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(90deg, transparent, rgba(233, 200, 120, .6));
}
.orn::after {
  content: ''; position: absolute; right: -4px; top: -3px;
  width: 7px; height: 7px; transform: rotate(45deg);
  background: var(--gold); box-shadow: 0 0 8px rgba(233, 200, 120, .6);
}
.orn.r::before { background: linear-gradient(90deg, rgba(233, 200, 120, .6), transparent); }
.orn.r::after { right: auto; left: -4px; }
.desc {
  font-size: 16px; line-height: 2.1; letter-spacing: .5px; color: var(--ink);
  margin-bottom: 22px;
}
.line-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px;
}
.line {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border-radius: 12px;
  border: 1px solid var(--line); background: rgba(7, 10, 20, .4);
}
.line .k { color: var(--gold); font-size: 14px; letter-spacing: 2px; flex: none; }
.line .v { color: var(--ink); font-size: 15px; letter-spacing: 1px; }
.line .v[data-grade="天"] { color: var(--gold-2); }
.line .v[data-grade="地"] { color: #b9a6ff; }
.line .v[data-grade="玄"] { color: var(--qi); }
.line .v[data-grade="黄"] { color: var(--ink-dim); }

/* ===== 同脉功法 ===== */
.block { padding: 30px 28px 10px; max-width: 1080px; margin: 0 auto; }
.block-title {
  display: flex; align-items: center; gap: 16px; justify-content: center;
  color: var(--ink); font-size: 20px; letter-spacing: 8px; font-weight: 700; margin-bottom: 26px;
}
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(264px, 1fr)); gap: 18px; }
.item {
  position: relative; overflow: hidden;
  padding: 0 0 16px; border-radius: 16px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(20, 29, 54, .55), rgba(7, 10, 20, .65));
  backdrop-filter: blur(6px); cursor: pointer;
  transition: transform .25s, border-color .25s, box-shadow .25s;
}
.item:hover {
  transform: translateY(-4px);
  border-color: rgba(233, 200, 120, .35);
  box-shadow: 0 16px 40px rgba(0, 0, 0, .5), 0 0 24px rgba(233, 200, 120, .12);
}
.corner {
  position: absolute; top: 12px; right: 14px; width: 8px; height: 8px;
  transform: rotate(45deg) scale(0); transition: transform .3s .05s;
  background: linear-gradient(135deg, var(--gold-2), var(--gold));
  box-shadow: 0 0 10px rgba(233, 200, 120, .7);
}
.item:hover .corner { transform: rotate(45deg) scale(1); }
.enter-hint {
  position: absolute; top: 12px; right: 14px; z-index: 2;
  font-size: 13px; letter-spacing: 2px; color: var(--gold-2);
  background: rgba(7, 10, 20, .6); border: 1px solid rgba(233, 200, 120, .3);
  padding: 2px 10px; border-radius: 999px;
  opacity: 0; transform: translateY(-4px); transition: .25s;
}
.item:hover .enter-hint { opacity: 1; transform: translateY(0); }
.item-cover {
  position: relative; margin-bottom: 14px; height: 150px; overflow: hidden;
  background: rgba(20, 29, 54, .6);
}
.item-cover img { width: 100%; height: 100%; object-fit: cover; display: block; transform: scale(1.02); transition: transform .5s; }
.item:hover .item-cover img { transform: scale(1.09); }
.item-cover.no-img {
  display: grid; place-items: center;
  background:
    radial-gradient(70% 90% at 50% 110%, rgba(233, 200, 120, .16), transparent 70%),
    linear-gradient(180deg, rgba(20, 29, 54, .9), rgba(7, 10, 20, .9));
}
.cover-fallback {
  font-size: 44px; font-weight: 700; letter-spacing: 6px; text-indent: 6px;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent; opacity: .85;
}
.cover-grad {
  position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(180deg, rgba(7, 10, 20, .05) 40%, rgba(7, 10, 20, .55) 82%, rgba(7, 10, 20, .8));
}
.item-name { font-size: 18px; letter-spacing: 2px; color: var(--ink); margin: 0 18px 8px; }
.item-origin { font-size: 13px; letter-spacing: 1px; color: var(--gold); opacity: .8; margin: 0 18px; }

/* ===== 页脚 ===== */
.footer { text-align: center; padding: 46px 20px 30px; color: var(--ink-dim); font-size: 14px; letter-spacing: 4px; }
.footer .orn { margin: 0 auto 18px; display: block; width: 160px; }
.footer .orn::before { background: linear-gradient(90deg, transparent, rgba(233, 200, 120, .45), transparent); }
.footer-dim { font-size: 12px; opacity: .6; margin-top: 10px; letter-spacing: 2px; }

/* 移动端 */
@media (max-width: 760px) {
  .topbar { padding: 0 16px; gap: 10px; height: 58px; }
  .brand-name { display: none; }
  .source-note { display: none; }
  .detail { padding: 22px 16px 10px; }
  .top { gap: 18px; justify-content: center; }
  .cover-wrap { width: 220px; height: 280px; }
  .name { font-size: 32px; }
  .block { padding: 24px 16px 10px; }
  .orn { width: 50px; }
}
</style>
