<template>
  <section class="form-side">
    <form class="card" @submit.prevent="onSubmit" autocomplete="off">
      <h2>踏入仙途</h2>
      <p class="hint">请输入道号与法诀，以证仙缘</p>

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
        <input v-model="password" :type="showPwd ? 'text' : 'password'" placeholder="法诀 / 密码" required>
        <span class="toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐' : '显' }}</span>
      </div>

      <div class="row">
        <label><input type="checkbox" v-model="remember"> 铭记道号</label>
        <a href="#" @click.prevent>遗忘法诀？</a>
      </div>

      <p class="alt">尚无仙缘？<router-link to="/register">前往入门</router-link></p>

      <button class="btn" type="submit" :disabled="loading">
        {{ loading ? '渡劫中…' : '登 仙' }}
      </button>
    </form>

    <Toast :message="toastMsg" :visible="toastVisible" />
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import Toast from './Toast.vue'

const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const remember = ref(true)
const showPwd = ref(false)
const loading = ref(false)
const toastMsg = ref('')
const toastVisible = ref(false)
let toastTimer = 0

// 若曾"铭记道号"，回填账号
onMounted(() => {
  const saved = localStorage.getItem('fx_user')
  if (saved) username.value = saved
})

function show(msg) {
  toastMsg.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toastVisible.value = false), 2200)
}

async function onSubmit() {
  if (!username.value.trim()) {
    show('请填写道号')
    return
  }
  if (!password.value) {
    show('请填写法诀')
    return
  }
  loading.value = true
  try {
    await auth.login(username.value.trim(), password.value, { remember: remember.value })
    show('仙缘已证 · 欢迎归来，' + username.value.trim())
    setTimeout(() => router.push('/'), 600)
  } catch (e) {
    show(e.message || '渡劫失败，请再试一次')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => clearTimeout(toastTimer))
</script>

<style scoped>
.form-side{
  flex:1;min-width:300px;display:grid;place-items:center;padding:36px;
  background:linear-gradient(180deg,rgba(20,29,54,.55),rgba(7,10,20,.65));
  position:relative;
}
.card{width:100%;max-width:330px;animation:rise .9s cubic-bezier(.2,.8,.2,1) both}
@keyframes rise{from{opacity:0;transform:translateY(18px)}to{opacity:1;transform:none}}
.card h2{font-size:30px;letter-spacing:6px;margin-bottom:6px;
  background:linear-gradient(180deg,#fff,var(--qi));-webkit-background-clip:text;background-clip:text;color:transparent}
.card .hint{color:var(--ink-dim);font-size:13px;letter-spacing:2px;margin-bottom:26px}
.field{position:relative;margin-bottom:18px}
.field .ico{position:absolute;left:14px;top:50%;transform:translateY(-50%);color:var(--ink-dim)}
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
.field .toggle{position:absolute;right:12px;top:50%;transform:translateY(-50%);cursor:pointer;color:var(--ink-dim);user-select:none}
.row{display:flex;align-items:center;justify-content:space-between;margin:4px 2px 22px;font-size:13px}
.row label{display:flex;align-items:center;gap:8px;color:var(--ink-dim);cursor:pointer;letter-spacing:1px}
.row input{accent-color:var(--gold);width:15px;height:15px}
.row a{color:var(--qi);text-decoration:none;opacity:.85}
.row a:hover{opacity:1;text-shadow:0 0 12px rgba(103,232,249,.5)}
.alt{margin-top:16px;text-align:center;font-size:13px;letter-spacing:1px;color:var(--ink-dim)}
.alt a{color:var(--qi);text-decoration:none}
.alt a:hover{text-shadow:0 0 12px rgba(103,232,249,.5)}

.btn{
  width:100%;padding:15px;border:none;border-radius:12px;cursor:pointer;
  font-family:inherit;font-size:17px;letter-spacing:8px;font-weight:700;color:#0c1322;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  position:relative;overflow:hidden;transition:.25s;
  box-shadow:0 10px 30px rgba(233,200,120,.30);
}
.btn:hover{transform:translateY(-2px);box-shadow:0 16px 40px rgba(233,200,120,.45)}
.btn:active{transform:translateY(0)}
.btn::after{ /* 扫光 */
  content:"";position:absolute;top:0;left:-120%;width:60%;height:100%;
  background:linear-gradient(105deg,transparent,rgba(255,255,255,.65),transparent);
  transform:skewX(-20deg);animation:sheen 3.5s ease-in-out infinite;
}
@keyframes sheen{0%,60%{left:-120%}100%{left:160%}}
.btn[disabled]{opacity:.7;cursor:progress}

@media(max-width:760px){
  .form-side{padding:30px}
}
</style>
