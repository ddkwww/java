<template>
  <div class="stage">
    <div class="sect-shell" ref="shell">

      <!-- ============ 已加入门派：门派主页 ============ -->
      <div v-if="mySect && mySect.joined" class="sect-home">
        <!-- 顶部 -->
        <section class="hero">
          <div class="watermark" aria-hidden="true">门</div>
          <h1 class="title">{{ mySect.sectName }}</h1>
          <p class="subtitle"><i class="sub-line"></i>{{ mySect.camp }} · {{ mySect.region || '修仙界' }}<i class="sub-line r"></i></p>

          <!-- 弟子级别 + 月俸 -->
          <div class="rank-bar">
            <div class="rank-chip">
              <span class="rank-tag">弟子级别</span>
              <span class="rank-val">{{ mySect.sectRank }}</span>
            </div>
            <div class="salary-chip">
              <span class="salary-tag">月俸</span>
              <span class="salary-val">
                <i class="s-ico">灵</i>{{ mySect.stoneSalary }}
                <i class="s-ico q">气</i>{{ mySect.qiSalary }}
              </span>
            </div>
          </div>

          <p class="sect-desc">{{ mySect.desc }}</p>
          <p class="sect-sign" v-if="mySect.signature">镇派绝学 · 《{{ mySect.signature }}》<template v-if="mySect.representative"> · 代表人物 {{ mySect.representative }}</template></p>

          <!-- 领取月俸 -->
          <div class="salary-claim">
            <button class="btn-salary" :disabled="salaryCd > 0" @click="onClaimSalary">
              {{ salaryCd > 0 ? salaryCd + 's 后可再领月俸' : '领取月俸' }}
            </button>
          </div>
        </section>

        <!-- 三大场所 -->
        <section class="block">
          <h2 class="block-title"><i class="orn"></i>宗门重地<i class="orn r"></i></h2>
          <div class="places-grid">
            <article v-for="p in mySect.places" :key="p.key" class="place-card" :data-place="p.key">
              <span class="place-icon" aria-hidden="true">{{ p.icon }}</span>
              <h3 class="place-name">{{ p.name }}</h3>
              <p class="place-desc">{{ p.desc }}</p>
            </article>
          </div>
        </section>

        <!-- 退出按钮 -->
        <div class="leave-wrap">
          <button class="btn-leave" @click="onLeave">退出此门</button>
        </div>
      </div>

      <!-- ============ 未加入门派：门派谱（供加入） ============ -->
      <template v-else>
        <section class="hero">
          <div class="watermark" aria-hidden="true">门</div>
          <h1 class="title">门 派 谱</h1>
          <p class="subtitle"><i class="sub-line"></i>正魔散修 · 诸派林立<i class="sub-line r"></i></p>

          <div class="search-bar">
            <span class="search-icon">寻</span>
            <input
              v-model="keyword"
              type="text"
              placeholder="搜罗门派、势力、地域、代表人物……"
              @keyup.enter="loadItems"
            />
            <button class="btn-search" @click="loadItems"><span>寻 派</span></button>
          </div>

          <div v-if="camps.length" class="chips">
            <button
              class="chip"
              :class="{ active: activeCamp === '全部' }"
              @click="pickCamp('全部')"
            >全部</button>
            <button
              v-for="c in camps" :key="c"
              class="chip"
              :class="{ active: activeCamp === c }"
              @click="pickCamp(c)"
            >{{ c }}</button>
          </div>
        </section>

        <section class="block">
          <h2 class="block-title"><i class="orn"></i>诸派总录<i class="orn r"></i></h2>
          <p class="block-sub">共 {{ items.length }} 派 · 择一拜入，开启宗门之路</p>

          <div v-if="items.length" class="grid">
            <article
              v-for="(it, i) in items"
              :key="it.id"
              class="sect-card"
              :data-camp="it.camp"
              :style="{ animationDelay: (i * 45) + 'ms' }"
            >
              <i class="corner" aria-hidden="true"></i>
              <div class="sect-head">
                <span class="sect-seal" aria-hidden="true">{{ it.name.slice(0, 1) }}</span>
                <div class="sect-title">
                  <h3 class="sect-name">{{ it.name }}</h3>
                  <span class="sect-camp">{{ it.camp }}</span>
                </div>
              </div>
              <p class="sect-desc">{{ it.desc }}</p>
              <div class="sect-meta">
                <span class="meta-item" v-if="it.region"><i>域</i>{{ it.region }}</span>
                <span class="meta-item" v-if="it.signature"><i>绝</i>{{ it.signature }}</span>
                <span class="meta-item" v-if="it.representative"><i>英</i>{{ it.representative }}</span>
              </div>
              <button
                v-if="it.camp !== '散修'"
                class="btn-join"
                :disabled="joiningId === it.id"
                @click="onJoin(it)"
              >{{ joiningId === it.id ? '拜入中…' : '拜入此门' }}</button>
              <span v-else class="tag-loose">散修无门，无需拜入</span>
            </article>
          </div>
          <div v-else class="empty">
            <div class="cloud" aria-hidden="true">﹀﹀﹀</div>
            <p>未寻得此门派</p>
            <p class="empty-hint">道友可换个关键词，或查看全部门派</p>
          </div>
        </section>
      </template>

      <!-- 提示条 -->
      <p v-if="tip" class="tip">{{ tip }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import {
  fetchSects,
  fetchSectFacets,
  fetchMySect,
  joinSect,
  leaveSect,
  claimSalary
} from '../api/sects'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const keyword = ref('')
const items = ref([])
const camps = ref([])
const activeCamp = ref('全部')
const mySect = ref(null) // 当前用户门派信息（joined=true 时进入门派主页）
const joiningId = ref(null)
const tip = ref('')
const salaryCd = ref(0)
const shell = ref(null)
let salaryTimer = null

async function loadMySect() {
  try {
    const data = await fetchMySect()
    mySect.value = data
    salaryCd.value = data.salaryCd || 0
  } catch (e) {
    mySect.value = { joined: false }
  }
}

async function loadFacets() {
  const f = await fetchSectFacets()
  camps.value = f.camps || []
}

async function loadItems() {
  const params = { camp: activeCamp.value, q: keyword.value }
  const res = await fetchSects(params)
  items.value = res.items
}

function pickCamp(c) {
  activeCamp.value = c
  loadItems()
}

async function onJoin(it) {
  if (joiningId.value) return
  joiningId.value = it.id
  tip.value = ''
  try {
    const data = await joinSect(it.id)
    mySect.value = data
    salaryCd.value = data.salaryCd || 0
    tip.value = data.message || '已拜入「' + it.name + '」'
  } catch (e) {
    tip.value = e.message || '拜入失败'
  } finally {
    joiningId.value = null
  }
}

async function onLeave() {
  tip.value = ''
  try {
    const data = await leaveSect()
    mySect.value = { joined: false }
    tip.value = data.message || '已退出此门'
    await loadItems()
  } catch (e) {
    tip.value = e.message || '退出失败'
  }
}

async function onClaimSalary() {
  tip.value = ''
  try {
    const data = await claimSalary()
    tip.value = data.message || '已领取月俸'
    salaryCd.value = data.cooldown || 0
    if (data.spiritStone != null) auth.setSpiritStone(data.spiritStone)
  } catch (e) {
    tip.value = e.message || '领取失败'
  }
}

onMounted(async () => {
  await loadMySect()
  // 未加入时才加载门派列表
  if (!(mySect.value && mySect.value.joined)) {
    await loadFacets()
    await loadItems()
  }
  // 月俸冷却倒计时
  salaryTimer = setInterval(() => {
    if (salaryCd.value > 0) salaryCd.value--
  }, 1000)
})

onUnmounted(() => {
  if (salaryTimer) clearInterval(salaryTimer)
})
</script>

<style scoped>
.stage{position:relative;z-index:2;height:100%;width:100%}
.sect-shell{height:100%;overflow-y:auto;overflow-x:hidden;scroll-behavior:smooth}
.sect-shell::-webkit-scrollbar{width:8px}
.sect-shell::-webkit-scrollbar-track{background:rgba(7,10,20,.4)}
.sect-shell::-webkit-scrollbar-thumb{
  border-radius:8px;background:linear-gradient(180deg,rgba(233,200,120,.5),rgba(233,200,120,.2));
}

/* ===== 顶部 ===== */
.hero{position:relative;text-align:center;overflow:hidden;padding:72px 24px 36px}
.watermark{
  position:absolute;left:50%;top:38%;transform:translate(-50%,-50%);
  font-size:min(300px,56vw);font-weight:700;line-height:1;
  background:linear-gradient(180deg,rgba(139,109,255,.12),rgba(139,109,255,.02) 70%);
  -webkit-background-clip:text;background-clip:text;color:transparent;
  pointer-events:none;animation:floaty 9s ease-in-out infinite alternate;
}
@keyframes floaty{from{transform:translate(-50%,-52%)}to{transform:translate(-50%,-46%)}}
.title{
  position:relative;font-size:46px;letter-spacing:18px;text-indent:18px;
  background:linear-gradient(180deg,#fff7e6,var(--gold) 55%,#b98e34);
  -webkit-background-clip:text;background-clip:text;color:transparent;
  filter:drop-shadow(0 0 26px rgba(233,200,120,.22));
  animation:rise .9s cubic-bezier(.2,.8,.2,1) both;
}
.subtitle{
  position:relative;display:flex;align-items:center;justify-content:center;gap:16px;
  margin-top:12px;color:var(--ink-dim);font-size:15px;letter-spacing:6px;
  animation:rise .9s .1s cubic-bezier(.2,.8,.2,1) both;
}
.sub-line{width:70px;height:1px;flex:none;background:linear-gradient(90deg,transparent,rgba(139,155,189,.5))}
.sub-line.r{background:linear-gradient(90deg,rgba(139,155,189,.5),transparent)}
.search-bar{
  position:relative;margin:34px auto 0;width:min(640px,92vw);
  display:flex;align-items:center;gap:6px;
  padding:8px 8px 8px 18px;border-radius:999px;
  border:1px solid var(--line);
  background:linear-gradient(180deg,rgba(20,29,54,.65),rgba(7,10,20,.75));
  backdrop-filter:blur(8px);
  box-shadow:0 18px 50px rgba(0,0,0,.45);
  transition:.25s;animation:rise .9s .2s cubic-bezier(.2,.8,.2,1) both;
}
.search-bar:focus-within{border-color:rgba(139,109,255,.45);box-shadow:0 18px 50px rgba(0,0,0,.45),0 0 30px rgba(139,109,255,.15)}
.search-icon{color:var(--gold);font-size:15px;width:22px;text-align:center}
.search-bar input{
  flex:1;border:none;outline:none;background:transparent;
  color:var(--ink);font-family:inherit;font-size:16px;letter-spacing:1px;padding:10px 6px;
}
.search-bar input::placeholder{color:rgba(139,155,189,.65)}
.btn-search{
  border:none;border-radius:999px;cursor:pointer;padding:12px 30px;
  font-family:inherit;font-size:15px;font-weight:700;letter-spacing:6px;color:#0c1322;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  box-shadow:0 8px 26px rgba(233,200,120,.30);transition:.25s;
}
.btn-search:hover{transform:translateY(-1px);box-shadow:0 12px 34px rgba(233,200,120,.45)}

/* 阵营筛选 */
.chips{
  position:relative;margin-top:20px;display:flex;justify-content:center;align-items:center;gap:12px;flex-wrap:wrap;
  animation:rise .9s .3s cubic-bezier(.2,.8,.2,1) both;
}
.chip{
  padding:8px 18px;border-radius:999px;cursor:pointer;
  font-family:inherit;font-size:14px;letter-spacing:2px;color:var(--ink-dim);
  border:1px solid var(--line);background:rgba(255,255,255,.02);transition:.2s;
}
.chip:hover{color:var(--ink);border-color:rgba(139,109,255,.5)}
.chip.active{
  color:var(--violet);border-color:rgba(139,109,255,.6);
  background:rgba(139,109,255,.08);box-shadow:0 0 14px rgba(139,109,255,.25);
}

/* ===== 弟子级别 + 月俸 ===== */
.rank-bar{
  position:relative;margin-top:28px;display:flex;justify-content:center;align-items:stretch;gap:16px;flex-wrap:wrap;
  animation:rise .9s .2s cubic-bezier(.2,.8,.2,1) both;
}
.rank-chip,.salary-chip{
  display:flex;flex-direction:column;align-items:center;gap:6px;
  padding:16px 28px;border-radius:14px;min-width:160px;
  border:1px solid var(--line);background:linear-gradient(180deg,rgba(20,29,54,.6),rgba(7,10,20,.7));
}
.rank-tag,.salary-tag{font-size:12px;letter-spacing:3px;color:var(--ink-dim)}
.rank-val{font-size:22px;letter-spacing:3px;color:var(--violet);font-weight:700;font-family:"Noto Serif SC",serif}
.salary-val{font-size:18px;color:var(--gold-2);font-weight:700;display:flex;align-items:center;gap:10px;font-family:"Noto Serif SC",serif}
.s-ico{
  display:inline-flex;width:20px;height:20px;border-radius:50%;align-items:center;justify-content:center;
  font-size:11px;font-style:normal;margin-right:3px;
  background:linear-gradient(135deg,var(--gold-2),var(--gold));color:#0c1322;font-weight:700;
}
.s-ico.q{background:linear-gradient(135deg,var(--qi),var(--violet));color:#0c1322}

.sect-desc{
  position:relative;margin:20px auto 0;max-width:620px;
  font-size:14px;line-height:1.9;color:var(--ink-dim);letter-spacing:.5px;
  animation:rise .9s .25s cubic-bezier(.2,.8,.2,1) both;
}
.sect-sign{
  position:relative;margin-top:12px;font-size:13px;letter-spacing:2px;color:var(--gold);
  animation:rise .9s .3s cubic-bezier(.2,.8,.2,1) both;
}
.salary-claim{margin-top:24px;animation:rise .9s .35s cubic-bezier(.2,.8,.2,1) both}
.btn-salary{
  padding:12px 38px;border-radius:999px;cursor:pointer;
  font-family:inherit;font-size:15px;font-weight:700;letter-spacing:4px;color:#0c1322;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  border:1px solid transparent;box-shadow:0 8px 26px rgba(233,200,120,.30);transition:.25s;
}
.btn-salary:hover:not(:disabled){transform:translateY(-1px);box-shadow:0 12px 34px rgba(233,200,120,.45)}
.btn-salary:disabled{opacity:.5;cursor:not-allowed}

/* ===== 列表 ===== */
.block{padding:20px 28px 60px;max-width:1080px;margin:0 auto}
.block-title{
  display:flex;align-items:center;gap:16px;justify-content:center;
  color:var(--ink);font-size:20px;letter-spacing:8px;font-weight:700;margin-bottom:8px;
}
.orn{position:relative;flex:none;width:110px;height:1px}
.orn::before{content:'';position:absolute;inset:0;background:linear-gradient(90deg,transparent,rgba(233,200,120,.6))}
.orn::after{content:'';position:absolute;right:-4px;top:-3px;width:7px;height:7px;transform:rotate(45deg);background:var(--gold)}
.orn.r::before{background:linear-gradient(90deg,rgba(233,200,120,.6),transparent)}
.orn.r::after{right:auto;left:-4px}
.block-sub{text-align:center;color:var(--ink-dim);font-size:13px;letter-spacing:2px;margin-bottom:24px}

.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:18px}
.sect-card{
  position:relative;overflow:hidden;padding:24px 24px 20px;border-radius:16px;
  border:1px solid var(--line);
  background:linear-gradient(180deg,rgba(20,29,54,.55),rgba(7,10,20,.65));
  backdrop-filter:blur(6px);
  transition:transform .25s,border-color .25s,box-shadow .25s;
  animation:rise .7s cubic-bezier(.2,.8,.2,1) both;
}
.sect-card:hover{transform:translateY(-4px);border-color:rgba(139,109,255,.4);box-shadow:0 16px 40px rgba(0,0,0,.5),0 0 24px rgba(139,109,255,.12)}
.sect-card::before{
  content:'';position:absolute;left:0;top:16px;bottom:16px;width:3px;border-radius:3px;background:var(--ink-dim);opacity:.5;transition:.25s;
}
.sect-card:hover::before{top:8px;bottom:8px;opacity:1}
.corner{
  position:absolute;top:12px;right:14px;width:8px;height:8px;
  transform:rotate(45deg) scale(0);transition:transform .3s .05s;
  background:linear-gradient(135deg,var(--gold-2),var(--gold));box-shadow:0 0 10px rgba(233,200,120,.7);
}
.sect-card:hover .corner{transform:rotate(45deg) scale(1)}

/* 阵营配色（左侧竖条）*/
.sect-card[data-camp="正道"]::before{background:#6fcf97}
.sect-card[data-camp="魔道"]::before{background:#ff7a6b}
.sect-card[data-camp="中立"]::before{background:var(--qi)}
.sect-card[data-camp="散修"]::before{background:var(--ink-dim)}

.sect-head{display:flex;align-items:center;gap:14px;margin-bottom:14px}
.sect-seal{
  width:48px;height:48px;flex:none;border-radius:12px;display:grid;place-items:center;
  background:linear-gradient(145deg,var(--violet),#5b3fd6);color:#fff;font-weight:700;font-size:22px;
  box-shadow:0 0 18px rgba(139,109,255,.35);
}
.sect-title{min-width:0}
.sect-name{font-size:20px;letter-spacing:2px;color:var(--ink);font-weight:700}
.sect-camp{
  display:inline-block;margin-top:6px;padding:2px 10px;border-radius:999px;
  font-size:12px;letter-spacing:2px;border:1px solid rgba(139,109,255,.35);color:var(--violet);
  background:rgba(139,109,255,.08);
}
.sect-desc{font-size:14px;line-height:1.9;color:var(--ink-dim);letter-spacing:.5px}
.sect-meta{display:flex;flex-wrap:wrap;gap:8px;margin-top:14px}
.meta-item{
  display:inline-flex;align-items:center;gap:5px;
  font-size:12px;letter-spacing:1px;color:var(--ink-dim);
  border:1px solid var(--line);padding:3px 10px;border-radius:999px;
}
.meta-item i{font-style:normal;color:var(--gold);font-size:11px}

/* 加入按钮 */
.btn-join{
  width:100%;margin-top:16px;padding:10px 0;border-radius:10px;cursor:pointer;
  font-family:inherit;font-size:14px;letter-spacing:3px;color:var(--violet);
  border:1px solid rgba(139,109,255,.5);background:rgba(139,109,255,.08);transition:.2s;
}
.btn-join:hover:not(:disabled){background:rgba(139,109,255,.18);box-shadow:0 0 16px rgba(139,109,255,.3)}
.btn-join:disabled{opacity:.5;cursor:not-allowed}
.tag-loose{
  display:block;margin-top:16px;text-align:center;font-size:13px;letter-spacing:2px;
  color:var(--ink-dim);border:1px dashed var(--line);padding:10px 0;border-radius:10px;
}

/* 三大场所 */
.places-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}
.place-card{
  position:relative;text-align:center;padding:28px 22px;border-radius:16px;
  border:1px solid var(--line);
  background:linear-gradient(180deg,rgba(20,29,54,.5),rgba(7,10,20,.65));
  transition:transform .25s,border-color .25s,box-shadow .25s;
  animation:rise .7s cubic-bezier(.2,.8,.2,1) both;
}
.place-card:hover{transform:translateY(-4px);border-color:rgba(233,200,120,.4);box-shadow:0 16px 40px rgba(0,0,0,.5)}
.place-icon{
  display:inline-flex;width:56px;height:56px;border-radius:50%;align-items:center;justify-content:center;
  font-size:24px;color:#fff;margin-bottom:16px;
  background:linear-gradient(145deg,var(--gold),#b88a3c);box-shadow:0 0 20px rgba(233,200,120,.4);
}
.place-card[data-place="alchemy"] .place-icon{background:linear-gradient(145deg,var(--qi),#2a9fb8)}
.place-card[data-place="practice"] .place-icon{background:linear-gradient(145deg,var(--violet),#5b3fd6)}
.place-name{font-size:20px;letter-spacing:4px;color:var(--ink);font-weight:700;margin-bottom:10px}
.place-desc{font-size:13px;line-height:1.9;color:var(--ink-dim);letter-spacing:.5px}

/* 退出 */
.leave-wrap{text-align:center;padding-bottom:50px}
.btn-leave{
  padding:10px 30px;border-radius:10px;cursor:pointer;
  font-family:inherit;font-size:14px;letter-spacing:3px;color:#ff8a8a;
  border:1px solid rgba(255,138,138,.4);background:rgba(255,138,138,.06);transition:.2s;
}
.btn-leave:hover{background:rgba(255,138,138,.14);box-shadow:0 0 16px rgba(255,138,138,.25)}

/* 空结果 */
.empty{text-align:center;padding:48px 0 30px;color:var(--ink-dim);letter-spacing:3px}
.cloud{font-size:22px;letter-spacing:10px;opacity:.45;margin-bottom:14px;color:var(--ink-dim);animation:floaty2 5s ease-in-out infinite alternate}
@keyframes floaty2{from{transform:translateX(-8px)}to{transform:translateX(8px)}}
.empty-hint{font-size:13px;opacity:.75;margin-top:10px}

/* 提示 */
.tip{margin:16px auto;text-align:center;color:var(--qi);font-size:14px;letter-spacing:1px;max-width:640px}

@keyframes rise{from{opacity:0;transform:translateY(18px)}to{opacity:1;transform:none}}

@media(max-width:760px){
  .title{font-size:34px;letter-spacing:10px;text-indent:10px}
  .grid{grid-template-columns:1fr}
  .places-grid{grid-template-columns:1fr}
  .sub-line{width:36px}
  .orn{width:50px}
  .rank-chip,.salary-chip{min-width:130px}
}
</style>
