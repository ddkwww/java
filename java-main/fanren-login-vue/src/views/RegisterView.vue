<template>
  <div class="stage">
    <div class="panel">
        <!-- 左侧：海报视觉区（随所选头像切换专属海报） -->
        <aside class="poster-side">
          <div class="poster-layer" :class="{ on: !!currentPoster }">
            <img v-if="currentPoster" :key="currentPoster" :src="currentPoster" alt="角色海报" draggable="false" />
            <div v-else class="poster-empty">择男修或女修<br>开启仙缘</div>
          </div>
          <div class="poster-mask"></div>

          <div class="poster-caption">
            <div class="brand"><span class="seal">仙</span> 仙缘阁 · 入门拜帖</div>
            <h1 class="title">凡人<br>修仙传</h1>
            <p class="subtitle">
              {{ gender === '男' ? '男儿何不带吴钩 · 一剑光寒定九州'
               : gender === '女' ? '仙子凌波御风行 · 回眸人间万里晴'
               : '凡人亦可逆天改命' }}
            </p>
            <div class="realm-chip">当前修为 · {{ realm }}</div>
          </div>

          <div class="foot">
            {{ gender ? (avatar ? '道友形象已具 · 请完善根基' : '请择定化身头像') : '择男修或女修 · 可先观仙容' }}
          </div>
        </aside>

      <!-- 右侧：人物初始化表单 -->
      <section class="form-side">
        <form class="card" @submit.prevent="onSubmit" autocomplete="off">
          <h2>入门拜帖</h2>
          <p class="hint">开创化身，立道号，证仙缘</p>

          <!-- 化身：头像 + 性别 -->
          <div class="sec-title"><i></i>化身</div>
          <div class="avatar-row">
            <div class="avatar-preview" :style="avatarPreviewStyle">
              <span v-if="!avatar">未选<br>头像</span>
            </div>
            <div class="avatar-picker" :class="{ locked: !gender }">
              <div v-if="!gender" class="avatar-lock-tip">请先选择性别，方可择定化身</div>
              <div class="avatar-grid">
                <button
                  v-for="key in avatarList" :key="key" type="button"
                  class="avatar-cell" :class="{ active: avatar === key }"
                  :disabled="!gender" @click="avatar = key"
                >
                  <img :src="AVATAR_IMAGES[key]" alt="默认头像" draggable="false" />
                </button>
              </div>
              <label class="upload" :class="{ active: isUploaded }" :style="gender ? '' : 'pointer-events:none;opacity:.45'">
                <input type="file" accept="image/*" :disabled="!gender" hidden @change="onUpload" />
                {{ isUploaded ? '已用上传头像 · 点击更换' : '⇪ 上传自定义头像' }}
              </label>
            </div>
          </div>

          <div class="gender-row">
            <button type="button" class="gender-card" :class="{ active: gender === '男' }" @click="pickGender('男')">
              <span class="g-ico">⚔</span><span>男修</span>
            </button>
            <button type="button" class="gender-card" :class="{ active: gender === '女' }" @click="pickGender('女')">
              <span class="g-ico">❀</span><span>女修</span>
            </button>
          </div>

          <!-- 根基：修为/年龄/灵根/体质 -->
          <div class="sec-title"><i></i>根基</div>

          <div class="field readonly">
            <span class="ico">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg>
            </span>
            <input :value="realm" type="text" readonly disabled>
            <span class="lock" title="系统初始修为，不可修改">锁</span>
          </div>

          <div class="field">
            <span class="ico">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="4" y="2" width="16" height="20" rx="3"/><path d="M8 6h8M8 10h8M8 14h5"/></svg>
            </span>
            <input v-model.number="age" type="number" min="10" max="99" placeholder="年龄（10–99）" required>
          </div>

          <div class="chips">
            <button
              v-for="r in spiritRoots" :key="r.name" type="button"
              class="chip" :class="{ active: spiritRoot === r.name, special: r.name.includes('变异') }"
              @click="spiritRoot = r.name"
            >{{ r.name }}</button>
          </div>

          <!-- 灵根加成：攻/防/速 -->
          <div v-if="currentSpiritRoot" class="bonus-panel">
            <div class="bonus-title">灵根 · {{ currentSpiritRoot.name }} 加成</div>
            <div class="bonus-grid">
              <span class="bonus-tag">攻击 +{{ currentSpiritRoot.atk }}</span>
              <span class="bonus-tag">防御 +{{ currentSpiritRoot.def }}</span>
              <span class="bonus-tag">速度 +{{ currentSpiritRoot.spd }}</span>
            </div>
          </div>

          <div class="field">
            <span class="ico">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3c3 4 6 6 6 10a6 6 0 0 1-12 0c0-4 3-6 6-10z"/></svg>
            </span>
            <select v-model="physique" :style="{ color: physiqueColor }" @change="onPhysiqueChange">
              <option v-for="p in physiques" :key="p.name" :value="p.name" :style="{ color: p.color }">
                {{ p.name }}
              </option>
            </select>
          </div>

          <!-- 体质加成：生命/寿元/攻/防/速 -->
          <div v-if="physique" class="bonus-panel">
            <div class="bonus-title">体质 · {{ currentPhysique.name }} 加成</div>
            <div class="bonus-grid">
              <span class="bonus-tag">生命 +{{ currentPhysique.hp }}</span>
              <span class="bonus-tag life">寿元 +{{ currentPhysique.lifespan }}</span>
              <span class="bonus-tag">攻击 +{{ currentPhysique.atk }}</span>
              <span class="bonus-tag">防御 +{{ currentPhysique.def }}</span>
              <span class="bonus-tag">速度 +{{ currentPhysique.spd }}</span>
            </div>
          </div>

          <!-- 道号：账号/密码/验证码 -->
          <div class="sec-title"><i></i>道号</div>

          <div class="field">
            <span class="ico">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/></svg>
            </span>
            <input v-model="username" type="text" placeholder="道号 / 账号" required>
          </div>

          <div class="field">
            <span class="ico">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg>
            </span>
            <input v-model="password" :type="showPwd ? 'text' : 'password'" placeholder="法诀 / 密码（5–15 位）" title="法诀长度需为 5–15 位" required>
            <span class="toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐' : '显' }}</span>
          </div>

          <div class="field">
            <span class="ico">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="5" y="11" width="14" height="9" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg>
            </span>
            <input v-model="confirm" :type="showPwd ? 'text' : 'password'" placeholder="再诵法诀" required>
          </div>

          <div class="field captcha-field">
            <span class="ico">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3l7 3v5c0 4-3 7-7 9-4-2-7-5-7-9V6l7-3z"/><path d="M9 12l2 2 4-4"/></svg>
            </span>
            <input v-model="captcha" type="text" placeholder="御阵符 / 验证码" maxlength="4" autocomplete="off" required>
            <span class="captcha-box" :class="{ disabled: loading }" @click="refreshCaptcha" title="点击换一换">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else class="captcha-loading">生成中…</span>
              <span class="captcha-refresh" title="换一换">↻</span>
            </span>
          </div>

          <button class="btn" type="submit" :disabled="loading">
            {{ loading ? '立帖中…' : '入 门' }}
          </button>

          <p class="alt">已有仙缘？<router-link to="/login">返回登录</router-link></p>
        </form>

        <Toast :message="toastMsg" :visible="toastVisible" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getCaptcha } from '../api/auth'
import { AVATAR_IMAGES, defaultAvatars, avatarImage, avatarPoster } from '../assets/avatars'
import posterMale from '../assets/posters/poster-male.jpg'
import posterFemale from '../assets/posters/poster-female.jpg'
import Toast from '../components/Toast.vue'

const router = useRouter()
const auth = useAuthStore()

// ===== 人物初始化选项（参考游戏捏人模块）=====
const realm = '练气一层' // 修为：只读，系统初始

// 灵根：名称 → 攻/防/速加成（与后端 SpiritRootEnum 一致）
const spiritRoots = [
  { name: '金灵根', atk: 10, def: 5, spd: 0 },
  { name: '木灵根', atk: 0, def: 10, spd: 5 },
  { name: '水灵根', atk: 5, def: 10, spd: 0 },
  { name: '火灵根', atk: 15, def: 0, spd: 0 },
  { name: '土灵根', atk: 5, def: 15, spd: 0 },
  { name: '雷灵根·变异', atk: 20, def: 5, spd: 10 },
  { name: '冰灵根·变异', atk: 10, def: 15, spd: 0 },
  { name: '风灵根·变异', atk: 5, def: 0, spd: 20 }
]
// 体质：名称 → 生命/寿元/攻/防/速加成（与后端 PhysiqueEnum 一致）
const physiques = [
  { name: '凡人之躯', color: '#9aa4b8', hp: 0, lifespan: 0, atk: 0, def: 0, spd: 0 },
  { name: '先天道体', color: '#ffd56b', hp: 200, lifespan: 50, atk: 10, def: 10, spd: 5 },
  { name: '太阴之体', color: '#7fd4ff', hp: 150, lifespan: 30, atk: 15, def: 5, spd: 0 },
  { name: '纯阳之体', color: '#ff7a6b', hp: 150, lifespan: 30, atk: 10, def: 10, spd: 0 },
  { name: '九幽玄阴体', color: '#b48cff', hp: 250, lifespan: 80, atk: 20, def: 15, spd: 5 },
  { name: '风灵体质', color: '#6fcf97', hp: 100, lifespan: 20, atk: 5, def: 5, spd: 20 }
]

// ===== 表单状态 =====
const username = ref('')
const password = ref('')
const confirm = ref('')
const captcha = ref('')
const captchaId = ref('')
const captchaImage = ref('')
const showPwd = ref(false)
const loading = ref(false)

const gender = ref('')                       // '' | '男' | '女'
const avatar = ref('')                       // 'avatar:male-1'…或上传的 dataURL
const age = ref(null)
const spiritRoot = ref('')                   // 选中的灵根名称
const physique = ref(physiques[0].name)      // 选中的体质名称

const toastMsg = ref('')
const toastVisible = ref(false)
let toastTimer = 0

// 头像：已选性别时只展示对应性别默认头像，否则展示全部
const avatarList = computed(() => (gender.value ? defaultAvatars(gender.value) : Object.keys(AVATAR_IMAGES)))
const isUploaded = computed(() => avatar.value.startsWith('data:'))
const avatarPreviewStyle = computed(() => {
  const url = avatarImage(avatar.value)
  return url ? { backgroundImage: `url(${url})` } : {}
})

// 左侧海报：优先显示已选头像的专属海报；未选头像时回退性别默认海报；未选性别则为空
const currentPoster = computed(() => {
  const p = avatarPoster(avatar.value)
  if (p) return p
  if (gender.value === '女') return posterFemale
  if (gender.value === '男') return posterMale
  return null
})

// 当前体质/灵根的展示色与加成（驱动下拉字色与加成展示）
const currentPhysique = computed(() => physiques.find(p => p.name === physique.value) || physiques[0])
const physiqueColor = computed(() => currentPhysique.value.color)
const currentSpiritRoot = computed(() => spiritRoots.find(r => r.name === spiritRoot.value) || null)

onMounted(() => {
  const saved = localStorage.getItem('fx_user')
  if (saved) username.value = saved
  refreshCaptcha()
})

function show(msg) {
  toastMsg.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toastVisible.value = false), 2200)
}

async function refreshCaptcha() {
  try {
    const data = await getCaptcha()
    captchaId.value = data.captchaId
    captchaImage.value = data.image
  } catch (e) {
    show('御阵符获取失败，请重试')
  }
}

// 选择性别：同步左侧海报；强制「先性别后头像」——切换性别时清空不属于该性别的已选头像
function pickGender(g) {
  gender.value = g
  if (!isUploaded.value && avatar.value && !defaultAvatars(g).includes(avatar.value)) {
    avatar.value = ''
  }
}

// 体质：仅「凡人之躯」可择定，其余仅供观览天命。下拉可展开查看，但选定其他体质即时复位
function onPhysiqueChange() {
  if (physique.value !== '凡人之躯') {
    show('天命未至，体质暂不可改，仅凡人之躯可择')
    physique.value = '凡人之躯'
  }
}

// 上传头像：本地压缩为 128×128 JPEG dataURL（避免超大图入库）
function onUpload(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  if (!/^image\//.test(file.type)) return show('请选择图片文件')
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      const size = 128
      const canvas = document.createElement('canvas')
      canvas.width = size
      canvas.height = size
      const ctx = canvas.getContext('2d')
      const scale = Math.max(size / img.width, size / img.height)
      const w = img.width * scale
      const h = img.height * scale
      ctx.drawImage(img, (size - w) / 2, (size - h) / 2, w, h)
      avatar.value = canvas.toDataURL('image/jpeg', 0.85)
      show('头像已就位')
    }
    img.onerror = () => show('图片读取失败，请换一张')
    img.src = reader.result
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

async function onSubmit() {
  if (!gender.value) return show('请先择定男修或女修')
  if (!avatar.value) return show('请选择或上传头像')
  const n = Number(age.value)
  if (!n || n < 10 || n > 99) return show('年龄需在 10–99 之间')
  if (!spiritRoot.value) return show('请择定灵根')
  if (!username.value.trim()) return show('请填写道号')
  if (!password.value) return show('请填写法诀')
  if (password.value.length < 5 || password.value.length > 15) {
    return show('法诀长度需为 5–15 位')
  }
  if (password.value !== confirm.value) return show('两遍法诀不一致')
  if (!captcha.value.trim()) return show('请填写御阵符')

  loading.value = true
  try {
    await auth.register(username.value.trim(), password.value, captchaId.value, captcha.value.trim(), {
      gender: gender.value,
      age: n,
      spiritRoot: spiritRoot.value,
      physique: physique.value,
      avatar: avatar.value
    })
    show('入门已成 · 请以此道号登录')
    setTimeout(() => router.push('/login'), 1200)
  } catch (e) {
    show(e.message || '入门失败，请再试一次')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onUnmounted(() => clearTimeout(toastTimer))
</script>

<style scoped>
.stage{
  position:relative;z-index:2;
  height:100%;width:100%;
  display:flex;align-items:center;justify-content:center;
  padding:20px;
}
.panel{
  display:flex;width:min(1120px,96vw);height:min(760px,94vh);
  border-radius:22px;overflow:hidden;
  border:1px solid var(--line);
  box-shadow:0 30px 80px rgba(0,0,0,.55),0 0 0 1px rgba(233,200,120,.05) inset;
  backdrop-filter:blur(6px);
}

/* ===== 左侧海报区 ===== */
.poster-side{
  position:relative;flex:none;width:38%;min-width:300px;
  display:flex;flex-direction:column;justify-content:space-between;
  overflow:hidden;background:#070a14;
}
.poster-layer{position:absolute;inset:0;opacity:0;transition:opacity .7s ease}
.poster-layer.on{opacity:1}
.poster-layer img{width:100%;height:100%;object-fit:cover;display:block}
.poster-mask{
  position:absolute;inset:0;pointer-events:none;
  background:
    linear-gradient(90deg,rgba(7,10,20,.25),rgba(7,10,20,.75) 88%),
    linear-gradient(0deg,rgba(7,10,20,.9),transparent 55%);
}
.poster-caption{position:relative;z-index:1;padding:38px 34px;display:flex;flex-direction:column;gap:0}
.poster-caption .brand{display:flex;align-items:center;gap:10px;font-size:13px;letter-spacing:3px;color:var(--gold)}
.poster-caption .brand .seal{
  width:30px;height:30px;border-radius:7px;display:grid;place-items:center;
  background:linear-gradient(145deg,var(--gold),#b88a3c);color:#1a1206;font-weight:700;
  box-shadow:0 0 18px rgba(233,200,120,.45);
}
.poster-caption .title{
  font-size:clamp(34px,4.2vw,52px);font-weight:700;line-height:1.08;
  letter-spacing:6px;margin-top:10px;
  background:linear-gradient(180deg,#fff7e6,var(--gold) 55%,#b98e34);
  -webkit-background-clip:text;background-clip:text;color:transparent;
  text-shadow:0 0 40px rgba(233,200,120,.25);
}
.poster-caption .subtitle{color:var(--ink-dim);letter-spacing:3px;margin-top:12px;font-size:13px}
.realm-chip{
  margin-top:18px;align-self:flex-start;
  font-size:12.5px;letter-spacing:2px;padding:7px 14px;border-radius:999px;
  border:1px solid rgba(233,200,120,.4);color:var(--gold);
  background:rgba(233,200,120,.08);box-shadow:0 0 16px rgba(233,200,120,.2);
}
.poster-side .foot{position:relative;z-index:1;padding:0 34px 30px;color:var(--ink-dim);font-size:12px;letter-spacing:2px;opacity:.75}
.poster-empty{
  position:absolute;inset:0;display:grid;place-items:center;text-align:center;
  color:var(--ink-dim);font-size:15px;letter-spacing:4px;line-height:2;opacity:.7;
}

/* ===== 右侧表单区 ===== */
.form-side{
  flex:1;min-width:0;position:relative;
  overflow-y:auto;
  background:linear-gradient(180deg,rgba(20,29,54,.55),rgba(7,10,20,.65));
  padding:34px 40px 30px;
}
.form-side::-webkit-scrollbar{width:8px}
.form-side::-webkit-scrollbar-thumb{background:rgba(140,170,220,.25);border-radius:4px}
.card{width:100%;max-width:560px;margin:0 auto;animation:rise .9s cubic-bezier(.2,.8,.2,1) both}
@keyframes rise{from{opacity:0;transform:translateY(18px)}to{opacity:1;transform:none}}
.card h2{font-size:28px;letter-spacing:6px;margin-bottom:6px;
  background:linear-gradient(180deg,#fff,var(--qi));-webkit-background-clip:text;background-clip:text;color:transparent}
.card .hint{color:var(--ink-dim);font-size:13px;letter-spacing:2px;margin-bottom:20px}

/* 分节标题 */
.sec-title{
  display:flex;align-items:center;gap:10px;
  margin:22px 0 14px;color:var(--gold);font-size:14px;letter-spacing:4px;font-weight:700;
}
.sec-title i{width:22px;height:1px;background:linear-gradient(90deg,var(--gold),transparent)}
.sec-title:first-of-type{margin-top:4px}

/* 头像选择 */
.avatar-row{display:flex;gap:18px;align-items:flex-start}
.avatar-preview{
  width:84px;height:84px;flex:none;border-radius:50%;
  border:1px solid var(--line);background:rgba(255,255,255,.03) center/cover no-repeat;
  display:grid;place-items:center;text-align:center;
  font-size:12px;color:var(--ink-dim);letter-spacing:2px;line-height:1.7;
  box-shadow:0 0 0 4px rgba(233,200,120,.08),0 8px 24px rgba(0,0,0,.4);
}
.avatar-picker{flex:1;min-width:0}
.avatar-grid{display:grid;grid-template-columns:repeat(6,1fr);gap:8px;transition:opacity .25s}
.avatar-picker.locked .avatar-grid{opacity:.4;pointer-events:none;filter:grayscale(.6)}
.avatar-lock-tip{
  font-size:13px;letter-spacing:2px;color:var(--ink-dim);text-align:center;
  padding:14px;border:1px dashed var(--line);border-radius:12px;margin-bottom:12px;
  background:rgba(255,255,255,.02);
}
.avatar-cell{
  aspect-ratio:1;border-radius:50%;padding:0;cursor:pointer;overflow:hidden;
  border:2px solid var(--line);background:transparent;transition:.2s;
}
.avatar-cell img{width:100%;height:100%;object-fit:cover;display:block}
.avatar-cell:hover{border-color:rgba(103,232,249,.5)}
.avatar-cell.active{border-color:var(--gold);box-shadow:0 0 16px rgba(233,200,120,.45)}
.upload{
  display:block;margin-top:10px;text-align:center;cursor:pointer;
  padding:8px 12px;border-radius:10px;font-size:12.5px;letter-spacing:2px;
  color:var(--ink-dim);border:1px dashed var(--line);transition:.2s;
}
.upload:hover{color:var(--qi);border-color:rgba(103,232,249,.5)}
.upload.active{color:var(--gold);border-color:rgba(233,200,120,.5);border-style:solid;background:rgba(233,200,120,.06)}

/* 性别 */
.gender-row{display:flex;gap:14px;margin-top:16px}
.gender-card{
  flex:1;padding:13px 10px;border-radius:12px;cursor:pointer;
  display:flex;align-items:center;justify-content:center;gap:10px;
  font-family:inherit;font-size:16px;letter-spacing:6px;color:var(--ink-dim);
  border:1px solid var(--line);background:rgba(255,255,255,.03);transition:.25s;
}
.gender-card .g-ico{font-size:18px}
.gender-card:hover{border-color:rgba(103,232,249,.5);color:var(--ink)}
.gender-card.active{
  color:var(--gold);border-color:rgba(233,200,120,.6);
  background:rgba(233,200,120,.08);box-shadow:0 0 18px rgba(233,200,120,.25);
}

/* 灵根 chips */
.chips{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:18px}
.chip{
  padding:8px 13px;border-radius:999px;cursor:pointer;
  font-family:inherit;font-size:13px;letter-spacing:1px;color:var(--ink-dim);
  border:1px solid var(--line);background:rgba(255,255,255,.02);transition:.2s;
}
.chip:hover{color:var(--ink);border-color:rgba(103,232,249,.5)}
.chip.active{color:var(--qi);border-color:rgba(103,232,249,.6);background:rgba(103,232,249,.08);box-shadow:0 0 14px rgba(103,232,249,.2)}
.chip.special{border-style:dashed}
.chip.special.active{color:var(--violet);border-color:rgba(139,109,255,.6);background:rgba(139,109,255,.08);box-shadow:0 0 14px rgba(139,109,255,.25)}

/* 灵根/体质加成面板 */
.bonus-panel{
  margin:-6px 0 18px;padding:12px 14px;border-radius:12px;
  border:1px solid rgba(103,232,249,.22);background:rgba(103,232,249,.05);
  animation:fadeIn .3s ease both;
}
.bonus-title{font-size:12.5px;letter-spacing:2px;color:var(--qi);margin-bottom:9px;font-weight:600}
.bonus-grid{display:flex;flex-wrap:wrap;gap:7px}
.bonus-tag{
  font-size:12px;letter-spacing:1px;padding:4px 10px;border-radius:999px;
  color:var(--ink);border:1px solid rgba(103,232,249,.35);background:rgba(103,232,249,.08);
}
.bonus-tag.life{color:#ffd56b;border-color:rgba(255,213,107,.4);background:rgba(255,213,107,.08)}
@keyframes fadeIn{from{opacity:0;transform:translateY(4px)}to{opacity:1;transform:none}}

/* 双列下拉 */
.duo{display:grid;grid-template-columns:1fr 1fr;gap:12px}

/* 输入域（沿用原风格） */
.field{position:relative;margin-bottom:18px}
.field .ico{position:absolute;left:14px;top:50%;transform:translateY(-50%);color:var(--ink-dim);pointer-events:none}
.field input,.field select{
  width:100%;padding:14px 14px 14px 44px;font-size:15px;color:var(--ink);
  background:rgba(255,255,255,.04);border:1px solid var(--line);border-radius:12px;
  outline:none;transition:.25s;font-family:inherit;letter-spacing:1px;
  appearance:none;
}
.field select{cursor:pointer}
.field select option{background:#0d1426;color:var(--ink)}
.field input::placeholder{color:#5e6f93}
.field input:focus,.field select:focus{
  border-color:rgba(103,232,249,.6);
  box-shadow:0 0 0 3px rgba(103,232,249,.12),0 0 22px rgba(103,232,249,.18);
  background:rgba(103,232,249,.05);
}
.field.readonly input,.field.readonly select{
  color:var(--gold);cursor:not-allowed;
  background:rgba(233,200,120,.05);border-color:rgba(233,200,120,.3);
}
.field .lock{
  position:absolute;right:12px;top:50%;transform:translateY(-50%);
  font-size:11px;letter-spacing:2px;padding:3px 8px;border-radius:6px;
  color:var(--ink-dim);border:1px solid var(--line);cursor:help;user-select:none;
}
.field .toggle{position:absolute;right:12px;top:50%;transform:translateY(-50%);cursor:pointer;color:var(--ink-dim);user-select:none}
.field .toggle:hover{color:var(--qi)}
.captcha-field input{padding-right:128px}
.captcha-box{
  position:absolute;right:10px;top:50%;transform:translateY(-50%);
  display:flex;align-items:center;gap:3px;cursor:pointer;
}
.captcha-box.disabled{opacity:.55;pointer-events:none}
.captcha-box img{height:34px;border-radius:8px;border:1px solid var(--line);display:block}
.captcha-loading{font-size:12px;color:var(--ink-dim);white-space:nowrap}
.captcha-refresh{
  width:20px;height:20px;display:grid;place-items:center;
  font-size:16px;color:var(--qi);user-select:none;
}
.captcha-refresh:hover{text-shadow:0 0 12px rgba(103,232,249,.6)}
.btn{
  width:100%;padding:15px;border:none;border-radius:12px;cursor:pointer;
  font-family:inherit;font-size:17px;letter-spacing:8px;font-weight:700;color:#0c1322;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  position:relative;overflow:hidden;transition:.25s;
  box-shadow:0 10px 30px rgba(233,200,120,.30);
}
.btn:hover{transform:translateY(-2px);box-shadow:0 16px 40px rgba(233,200,120,.45)}
.btn:active{transform:translateY(0)}
.btn[disabled]{opacity:.7;cursor:progress}
.alt{margin-top:16px;text-align:center;font-size:13px;letter-spacing:1px;color:var(--ink-dim)}
.alt a{color:var(--qi);text-decoration:none}
.alt a:hover{text-shadow:0 0 12px rgba(103,232,249,.5)}

@media(max-width:920px){
  .panel{flex-direction:column;height:auto;max-height:none}
  .poster-side{width:100%;min-height:230px}
  .poster-caption{padding:24px 24px 18px}
  .poster-side .foot{padding:0 24px 18px}
  .form-side{overflow:visible;padding:28px 24px}
}
</style>
