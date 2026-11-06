<template>
  <transition name="fade">
    <div v-if="visible" class="mask" @click.self="onClose">
      <div class="dialog">
        <header class="dlg-head">
          <h3>编辑用户 · {{ editing ? editing.username : '' }}</h3>
          <button class="x" @click="onClose" aria-label="关闭">✕</button>
        </header>

        <form class="dlg-body" @submit.prevent="onSubmit">
          <!-- 权限 -->
          <label class="fld">
            <span>权限</span>
            <select v-model.number="form.permission" :disabled="!canEditPermission">
              <option :value="0">管理员 (0)</option>
              <option :value="1">普通用户 (1)</option>
            </select>
            <small v-if="!canEditPermission" class="hint">不能修改自己的权限</small>
          </label>

          <!-- 人物资料 -->
          <div class="row2">
            <label class="fld">
              <span>性别</span>
              <select v-model="form.gender">
                <option value="">未设置</option>
                <option value="男">男</option>
                <option value="女">女</option>
              </select>
            </label>
            <label class="fld">
              <span>年龄</span>
              <input v-model.number="form.age" type="number" min="10" max="99" placeholder="10–99" />
            </label>
          </div>

          <div class="row2">
            <label class="fld">
              <span>灵根</span>
              <select v-model="form.spiritRoot">
                <option value="">未设置</option>
                <option v-for="r in SPIRIT_ROOTS" :key="r" :value="r">{{ r }}</option>
              </select>
            </label>
            <label class="fld">
              <span>体质</span>
              <select v-model="form.physique">
                <option value="">未设置</option>
                <option v-for="p in PHYSIQUES" :key="p" :value="p">{{ p }}</option>
              </select>
            </label>
          </div>

          <div class="row2">
            <label class="fld">
              <span>境界</span>
              <select v-model="form.realm">
                <option v-for="r in REALMS" :key="r" :value="r">{{ r }}</option>
              </select>
            </label>
            <label class="fld">
              <span>修为总层数 (1–81)</span>
              <input v-model.number="form.totalLevel" type="number" min="1" max="81" />
            </label>
          </div>

          <!-- 经济数值 -->
          <div class="row2">
            <label class="fld">
              <span>灵石</span>
              <input v-model.number="form.spiritStone" type="number" min="0" />
            </label>
            <label class="fld">
              <span>灵气</span>
              <input v-model.number="form.spiritQi" type="number" min="0" />
            </label>
          </div>

          <div class="row2">
            <label class="fld">
              <span>剩余寿元</span>
              <input v-model.number="form.lifespan" type="number" min="0" />
            </label>
            <label class="fld">
              <span>已耗天数</span>
              <input v-model.number="form.ageUsed" type="number" min="0" />
            </label>
          </div>

          <div class="row2">
            <label class="fld">
              <span>门派</span>
              <input v-model.trim="form.sect" type="text" placeholder="未加入" maxlength="64" />
            </label>
            <label class="fld">
              <span>弟子级别</span>
              <input v-model.trim="form.sectRank" type="text" placeholder="外门 / 内门 / 真传" maxlength="16" />
            </label>
          </div>

          <!-- 重置密码 -->
          <label class="fld">
            <span>重置密码 <i class="opt">(留空则不修改)</i></span>
            <input v-model="form.password" type="text" placeholder="5–15 位新密码" maxlength="15" />
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
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  visible: { type: Boolean, default: false },
  editing: { type: Object, default: null },
  saving: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'submit'])

const auth = useAuthStore()

const SPIRIT_ROOTS = ['金', '木', '水', '火', '土', '雷·变异', '冰·变异', '风·变异']
const PHYSIQUES = ['凡人之躯', '先天道体', '太阴之体', '纯阳之体', '九幽玄阴体', '风灵体质']
const REALMS = ['练气一层', '练气二层', '练气三层', '练气四层', '练气五层', '练气六层', '练气七层', '练气八层', '练气九层',
  '筑基期', '金丹期', '元婴期', '化神期', '炼虚期', '合体期', '大乘期', '渡劫期']

const form = ref({})
const error = ref('')

// 是否可改权限：管理员不能改自己的权限
const canEditPermission = computed(() => {
  const e = props.editing
  if (!e) return true
  return e.username !== (auth.user || '')
})

const empty = () => ({
  permission: 1, gender: '', age: 18, spiritRoot: '', physique: '',
  realm: '练气一层', totalLevel: 1, spiritStone: 0, spiritQi: 0,
  lifespan: 100, ageUsed: 0, sect: '', sectRank: '', password: ''
})

watch(() => [props.visible, props.editing], () => {
  if (props.visible) {
    error.value = ''
    form.value = props.editing
      ? { ...empty(), ...props.editing, password: '' }
      : empty()
  }
}, { immediate: true })

function onClose() {
  emit('close')
}

function onSubmit() {
  const f = form.value
  if (f.age != null && (f.age < 10 || f.age > 99)) return (error.value = '年龄需在 10–99 之间')
  if (f.totalLevel != null && (f.totalLevel < 1 || f.totalLevel > 81)) return (error.value = '修为总层数需在 1–81 之间')
  if (f.spiritStone != null && f.spiritStone < 0) return (error.value = '灵石不能为负')
  if (f.spiritQi != null && f.spiritQi < 0) return (error.value = '灵气不能为负')
  if (f.lifespan != null && f.lifespan < 0) return (error.value = '寿元不能为负')
  if (f.password && (f.password.length < 5 || f.password.length > 15)) return (error.value = '新密码需 5–15 位')
  error.value = ''

  const payload = {
    permission: f.permission,
    gender: f.gender || '',
    age: f.age,
    spiritRoot: f.spiritRoot || '',
    physique: f.physique || '',
    realm: f.realm,
    totalLevel: f.totalLevel,
    spiritStone: f.spiritStone,
    spiritQi: f.spiritQi,
    lifespan: f.lifespan,
    ageUsed: f.ageUsed,
    sect: f.sect || '',
    sectRank: f.sectRank || ''
  }
  // 仅当填写了新密码才传 password
  if (f.password) payload.password = f.password

  emit('submit', payload)
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
.fld i.opt{color:var(--ink-dim);font-style:normal;opacity:.6;font-size:12px}
.fld input,.fld select{
  width:100%;padding:11px 13px;font-size:14px;color:var(--ink);
  font-family:inherit;letter-spacing:.5px;
  background:rgba(255,255,255,.04);border:1px solid var(--line);border-radius:10px;
  outline:none;transition:.2s;
}
.fld select option{background:#141d36;color:var(--ink)}
.fld input:focus,.fld select:focus{
  border-color:rgba(103,232,249,.55);
  box-shadow:0 0 0 3px rgba(103,232,249,.12);
  background:rgba(103,232,249,.04);
}
.fld input:disabled,.fld select:disabled{opacity:.55;cursor:not-allowed}
.hint{display:block;color:#ffb36b;font-size:12px;letter-spacing:1px;margin-top:6px}
.row2{display:flex;gap:14px}
.row2 .fld{flex:1}
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
