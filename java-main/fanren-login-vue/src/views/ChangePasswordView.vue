<template>
  <div class="stage">
    <div class="card">
      <h2>修改法诀</h2>
      <p class="hint">道友请于此更改进阶法诀（登录密码），以护道途周全</p>

      <div class="field">
        <span class="ico">🔒</span>
        <input
          v-model="oldPassword"
          :type="showOld ? 'text' : 'password'"
          placeholder="原法诀 / 原密码"
          autocomplete="current-password"
          @keyup.enter="onSubmit"
        >
        <span class="toggle" @click="showOld = !showOld">{{ showOld ? '隐' : '显' }}</span>
      </div>

      <div class="field">
        <span class="ico">🔑</span>
        <input
          v-model="newPassword"
          :type="showNew ? 'text' : 'password'"
          placeholder="新法诀（5–15 位）"
          autocomplete="new-password"
          @keyup.enter="onSubmit"
        >
        <span class="toggle" @click="showNew = !showNew">{{ showNew ? '隐' : '显' }}</span>
      </div>

      <div class="field">
        <span class="ico">🔑</span>
        <input
          v-model="confirm"
          :type="showNew ? 'text' : 'password'"
          placeholder="再次确认新法诀"
          autocomplete="new-password"
          @keyup.enter="onSubmit"
        >
      </div>

      <p class="err" v-if="errMsg">{{ errMsg }}</p>

      <button class="btn" type="button" :disabled="loading" @click="onSubmit">
        {{ loading ? '护道中…' : '确 认 修 改' }}
      </button>

      <button class="btn-ghost" type="button" @click="back">返回</button>
    </div>

    <Toast :message="toastMsg" :visible="toastVisible" />
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '../api/auth'
import Toast from '../components/Toast.vue'

const router = useRouter()

const oldPassword = ref('')
const newPassword = ref('')
const confirm = ref('')
const showOld = ref(false)
const showNew = ref(false)
const loading = ref(false)
const errMsg = ref('')
const toastMsg = ref('')
const toastVisible = ref(false)
let toastTimer = 0

function showToast(msg) {
  toastMsg.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastVisible.value = false }, 2200)
}

function back() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

async function onSubmit() {
  errMsg.value = ''
  if (!oldPassword.value) { errMsg.value = '请填写原密码'; return }
  if (!newPassword.value) { errMsg.value = '请填写新密码'; return }
  if (newPassword.value.length < 5 || newPassword.value.length > 15) {
    errMsg.value = '新密码长度需为 5–15 位'; return
  }
  if (newPassword.value === oldPassword.value) {
    errMsg.value = '新密码不能与原密码相同'; return
  }
  if (newPassword.value !== confirm.value) {
    errMsg.value = '两次输入的新密码不一致'; return
  }

  loading.value = true
  try {
    const d = await changePassword(oldPassword.value, newPassword.value)
    showToast(d.message || '密码已修改成功')
    setTimeout(() => router.push('/'), 1000)
  } catch (e) {
    errMsg.value = e.message || '修改失败，请再试一次'
  } finally {
    loading.value = false
  }
}

onUnmounted(() => clearTimeout(toastTimer))
</script>

<style scoped>
.stage{
  position:relative;z-index:2;height:100%;width:100%;
  display:flex;align-items:center;justify-content:center;padding:24px;
}
.card{
  width:min(420px,94vw);padding:34px 30px;border-radius:22px;
  background:linear-gradient(180deg,rgba(20,29,54,.62),rgba(7,10,20,.72));
  border:1px solid rgba(233,200,120,.18);
  box-shadow:0 30px 80px rgba(0,0,0,.55),0 0 0 1px rgba(233,200,120,.05) inset;
  backdrop-filter:blur(6px);animation:rise .9s cubic-bezier(.2,.8,.2,1) both;
}
@keyframes rise{from{opacity:0;transform:translateY(18px)}to{opacity:1;transform:none}}
.card h2{
  font-size:28px;letter-spacing:6px;margin-bottom:6px;text-align:center;
  background:linear-gradient(180deg,#fff,var(--qi));-webkit-background-clip:text;background-clip:text;color:transparent;
}
.hint{color:var(--ink-dim);font-size:13px;letter-spacing:2px;margin-bottom:24px;text-align:center}

.field{position:relative;margin-bottom:18px}
.field .ico{position:absolute;left:14px;top:50%;transform:translateY(-50%);font-size:16px;filter:grayscale(.2)}
.field input{
  width:100%;padding:14px 14px 14px 44px;font-size:15px;color:var(--ink);
  background:rgba(255,255,255,.04);border:1px solid var(--line);border-radius:12px;
  outline:none;transition:.25s;font-family:inherit;letter-spacing:1px;
}
.field input::placeholder{color:#5e6f93}
.field input:focus{
  border-color:rgba(103,232,249,.6);
  box-shadow:0 0 0 3px rgba(103,232,249,.12),0 0 22px rgba(103,232,249,.18);
  background:rgba(103,232,249,.05);
}
.field .toggle{
  position:absolute;right:12px;top:50%;transform:translateY(-50%);cursor:pointer;
  color:var(--ink-dim);user-select:none;font-size:13px;letter-spacing:1px;
}
.field .toggle:hover{color:var(--gold-2)}

.err{color:#ff8a8a;font-size:13px;letter-spacing:1px;margin:-4px 2px 14px;min-height:16px;text-align:center}

.btn{
  width:100%;padding:15px;border:none;border-radius:12px;cursor:pointer;
  font-family:inherit;font-size:17px;letter-spacing:8px;font-weight:700;color:#0c1322;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  position:relative;overflow:hidden;transition:.25s;
  box-shadow:0 10px 30px rgba(233,200,120,.30);
}
.btn:hover:not([disabled]){transform:translateY(-2px);box-shadow:0 16px 40px rgba(233,200,120,.45)}
.btn:active:not([disabled]){transform:translateY(0)}
.btn::after{
  content:"";position:absolute;top:0;left:-120%;width:60%;height:100%;
  background:linear-gradient(105deg,transparent,rgba(255,255,255,.65),transparent);
  transform:skewX(-20deg);animation:sheen 3.5s ease-in-out infinite;
}
@keyframes sheen{0%,60%{left:-120%}100%{left:160%}}
.btn[disabled]{opacity:.7;cursor:progress}

.btn-ghost{
  width:100%;margin-top:12px;padding:12px;border-radius:12px;cursor:pointer;
  font-family:inherit;font-size:14px;letter-spacing:3px;color:var(--gold);
  background:transparent;border:1px solid rgba(233,200,120,.32);transition:.2s;
}
.btn-ghost:hover{background:rgba(233,200,120,.12)}

@media(max-width:760px){
  .card{padding:30px 22px}
}
</style>
