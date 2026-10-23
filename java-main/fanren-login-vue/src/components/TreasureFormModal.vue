<template>
  <transition name="fade">
    <div v-if="visible" class="mask" @click.self="onClose">
      <div class="dialog">
        <header class="dlg-head">
          <h3>{{ editing ? '编辑法宝' : '新增法宝' }}</h3>
          <button class="x" @click="onClose" aria-label="关闭">✕</button>
        </header>

        <form class="dlg-body" @submit.prevent="onSubmit">
          <label class="fld">
            <span>名称 <i>*</i></span>
            <input v-model.trim="form.name" type="text" placeholder="如：青竹蜂云剑" maxlength="64" />
          </label>

          <div class="row2">
            <label class="fld">
              <span>类别 <i>*</i></span>
              <input
                v-model.trim="form.category"
                type="text" list="tre-cat-list" placeholder="飞剑 / 遁法 …" maxlength="32"
              />
              <datalist id="tre-cat-list">
                <option v-for="c in categories" :key="c" :value="c" />
              </datalist>
            </label>

            <label class="fld">
              <span>品阶 <i>*</i></span>
              <select v-model="form.grade">
                <option v-for="g in GRADES" :key="g" :value="g">{{ g }}阶</option>
              </select>
            </label>
          </div>

          <label class="fld">
            <span>适用境界 <i>*</i></span>
            <select v-model="form.realm">
              <option v-for="r in REALMS" :key="r" :value="r">{{ r }}</option>
            </select>
          </label>

          <label class="fld">
            <span>简介</span>
            <textarea v-model.trim="form.desc" rows="3" maxlength="512" placeholder="法宝效果、来历、要点……"></textarea>
          </label>

          <div class="row2">
            <label class="fld">
              <span>出处</span>
              <input v-model.trim="form.origin" type="text" placeholder="如：韩立" maxlength="64" />
            </label>
          </div>

          <label class="fld">
            <span>法宝插画</span>
            <div class="uploader" :class="{ 'has-img': previewSrc }">
              <div class="drop" @click="chooseFile">
                <img v-if="previewSrc" :src="previewSrc" class="thumb" alt="法宝插画预览" />
                <div v-else class="uphint">
                  <span class="upico">⤓</span>
                  <p>点击选择法宝图片</p>
                  <small>支持 jpg / png / gif / webp，≤ 10MB</small>
                </div>
              </div>
              <div class="upbar">
                <button type="button" class="upbtn" @click="chooseFile" :disabled="uploading">
                  {{ uploading ? '上传中…' : (previewSrc ? '更换图片' : '选择图片') }}
                </button>
                <button v-if="previewSrc" type="button" class="upclear" @click="clearImage">移除</button>
                <span v-if="previewSrc" class="upname">{{ imageName }}</span>
              </div>
              <input ref="fileInput" type="file" accept="image/*" class="file-hidden" @change="onPickFile" />
            </div>
          </label>

          <p v-if="error" class="err">{{ error }}</p>

          <footer class="dlg-foot">
            <button type="button" class="btn ghost" @click="onClose">取消</button>
            <button type="submit" class="btn" :disabled="props.saving">
              {{ props.saving ? '保存中…' : '保存' }}
            </button>
          </footer>
        </form>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { uploadTreasureImage } from '../api/treasure'

const props = defineProps({
  visible: { type: Boolean, default: false },
  editing: { type: Object, default: null },
  categories: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'submit'])

const API_BASE = import.meta.env.VITE_API_BASE || ''

const GRADES = ['天', '地', '玄', '黄']
const REALMS = ['练气期', '筑基期', '金丹期', '元婴期', '化神期', '炼虚期', '合体期', '大乘期', '渡劫期']

const empty = () => ({ name: '', category: '', grade: '玄', realm: '练气期', desc: '', origin: '', image: '' })
const form = ref(empty())
const error = ref('')
const fileInput = ref(null)
const uploading = ref(false)
const imageName = ref('')

// 预览地址：远程返回 /images/manuals/... 需拼接后端地址；blob:/http 直用；mock 模式留空走兜底
const previewSrc = computed(() => {
  const v = form.value.image
  if (!v) return ''
  if (v.startsWith('http') || v.startsWith('blob:')) return v
  return API_BASE ? API_BASE + v : null
})

// 打开时根据 editing 初始化表单（编辑=回填，新增=清空）
watch(() => [props.visible, props.editing], () => {
  if (props.visible) {
    error.value = ''
    uploading.value = false
    imageName.value = ''
    form.value = props.editing
      ? { name: '', category: '', grade: '玄', realm: '练气期', desc: '', origin: '', image: '', ...props.editing }
      : empty()
  }
}, { immediate: true })

function chooseFile() {
  if (fileInput.value) fileInput.value.click()
}

async function onPickFile(e) {
  const f = e.target.files && e.target.files[0]
  if (!f) return
  if (!f.type.startsWith('image/')) {
    error.value = '请选择图片文件'
    e.target.value = ''
    return
  }
  if (f.size > 10 * 1024 * 1024) {
    error.value = '图片体积不得超过 10MB'
    e.target.value = ''
    return
  }
  error.value = ''
  uploading.value = true
  try {
    const { url } = await uploadTreasureImage(f)
    form.value.image = url
    imageName.value = f.name
  } catch (err) {
    error.value = (err && err.message) || '图片上传失败'
  } finally {
    uploading.value = false
    e.target.value = ''
  }
}

function clearImage() {
  form.value.image = ''
  imageName.value = ''
}

function onClose() {
  emit('close')
}

function onSubmit() {
  const f = form.value
  if (!f.name) return (error.value = '请填写名称')
  if (!f.category) return (error.value = '请填写类别')
  if (!f.grade) return (error.value = '请选择品阶')
  if (!f.realm) return (error.value = '请选择适用境界')
  error.value = ''
  emit('submit', {
    name: f.name,
    category: f.category,
    grade: f.grade,
    realm: f.realm,
    desc: f.desc || '',
    origin: f.origin || '',
    image: f.image || ''
  })
}
</script>

<style scoped>
.mask{
  position:fixed;inset:0;z-index:50;display:grid;place-items:center;
  background:rgba(4,6,14,.62);backdrop-filter:blur(3px);
  animation:fade .2s ease;
}
.dialog{
  width:min(560px,94vw);max-height:90vh;overflow:auto;
  background:linear-gradient(180deg,var(--bg-2),var(--bg-1));
  border:1px solid var(--line);border-radius:18px;
  box-shadow:0 30px 80px rgba(0,0,0,.6);
}
.dlg-head{
  display:flex;align-items:center;justify-content:space-between;
  padding:18px 22px;border-bottom:1px solid var(--line);
}
.dlg-head h3{font-size:19px;letter-spacing:3px;color:var(--ink)}
.x{
  background:transparent;border:none;color:var(--ink-dim);font-size:18px;cursor:pointer;
  width:32px;height:32px;border-radius:8px;transition:.2s;
}
.x:hover{color:var(--gold);background:rgba(233,200,120,.1)}
.dlg-body{padding:20px 22px 22px}
.fld{display:block;margin-bottom:14px}
.fld > span{display:block;margin-bottom:6px;color:var(--ink-dim);font-size:13px;letter-spacing:2px}
.fld i{color:#ff7a7a;font-style:normal}
.fld input,.fld select,.fld textarea{
  width:100%;padding:11px 13px;font-size:14px;color:var(--ink);
  font-family:inherit;letter-spacing:.5px;
  background:rgba(255,255,255,.04);border:1px solid var(--line);border-radius:10px;
  outline:none;transition:.2s;
}
/* 下拉弹层选项：Windows 原生弹出层默认白底，option 会继承浅色文字导致看不清，
   显式指定深底浅字修复对比度（收起态的已选项仍由上方规则渲染，不受影响） */
.fld select option{
  background:#141d36;
  color:var(--ink);
}
.fld textarea{resize:vertical}
.fld input:focus,.fld select:focus,.fld textarea:focus{
  border-color:rgba(103,232,249,.55);
  box-shadow:0 0 0 3px rgba(103,232,249,.12);
  background:rgba(103,232,249,.04);
}
.row2{display:flex;gap:14px}
.row2 .fld{flex:1}

/* 图片上传 */
.uploader{border:1px solid var(--line);border-radius:12px;overflow:hidden;background:rgba(255,255,255,.03)}
.uploader.has-img{border-color:rgba(103,232,249,.35)}
.drop{
  position:relative;min-height:150px;display:grid;place-items:center;cursor:pointer;
  background:repeating-linear-gradient(45deg,rgba(255,255,255,.02) 0 12px,transparent 12px 24px);
  transition:.2s;
}
.drop:hover{background:rgba(103,232,249,.05)}
.thumb{width:100%;max-height:240px;object-fit:contain;display:block}
.uphint{text-align:center;color:var(--ink-dim);padding:24px}
.upico{display:block;font-size:30px;color:var(--gold);margin-bottom:8px}
.uphint p{font-size:14px;letter-spacing:2px;color:var(--ink)}
.uphint small{font-size:12px;opacity:.7;letter-spacing:1px}
.upbar{display:flex;align-items:center;gap:12px;padding:12px 14px;border-top:1px solid var(--line)}
.upbtn{
  padding:8px 16px;border-radius:9px;cursor:pointer;font-family:inherit;font-size:13px;letter-spacing:2px;
  border:1px solid rgba(233,200,120,.35);background:transparent;color:var(--gold);transition:.2s;
}
.upbtn:hover:not(:disabled){background:rgba(233,200,120,.12)}
.upbtn:disabled{opacity:.6;cursor:progress}
.upclear{
  padding:8px 16px;border-radius:9px;cursor:pointer;font-family:inherit;font-size:13px;letter-spacing:2px;
  border:1px solid rgba(255,120,120,.35);background:transparent;color:#ff9a9a;transition:.2s;
}
.upclear:hover{background:rgba(255,120,120,.12)}
.upname{font-size:12px;color:var(--ink-dim);letter-spacing:1px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.file-hidden{display:none}
.err{color:#ff9a9a;font-size:13px;letter-spacing:1px;margin:2px 0 12px}
.dlg-foot{display:flex;justify-content:flex-end;gap:12px;margin-top:6px}
.btn{
  padding:11px 26px;border:none;border-radius:10px;cursor:pointer;
  font-family:inherit;font-size:15px;letter-spacing:4px;font-weight:700;color:#0c1322;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  box-shadow:0 8px 24px rgba(233,200,120,.3);transition:.2s;
}
.btn:hover{transform:translateY(-1px);box-shadow:0 12px 30px rgba(233,200,120,.45)}
.btn[disabled]{opacity:.7;cursor:progress}
.btn.ghost{background:transparent;color:var(--ink-dim);border:1px solid var(--line);box-shadow:none}
.btn.ghost:hover{color:var(--ink);border-color:rgba(233,200,120,.35)}
.fade-enter-active,.fade-leave-active{transition:opacity .2s}
.fade-enter-from,.fade-leave-to{opacity:0}
@media(max-width:560px){.row2{flex-direction:column;gap:0}}
</style>
