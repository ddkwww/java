<template>
  <div class="stage">
    <div class="home-card">
      <div class="seal">仙</div>
      <h1>已入仙途</h1>
      <p>道友 <b>{{ auth.user || '无名' }}</b>，仙缘已证，灵台清明。</p>
      <p class="realm">当前境界 · <b>{{ auth.profile?.realm || '炼气' }}</b></p>
      <button class="btn" @click="onLogout">退归山林</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

onMounted(() => {
  auth.fetchProfile()
})

function onLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.stage{
  position:relative;z-index:2;
  height:100%;width:100%;
  display:flex;align-items:center;justify-content:center;
  padding:24px;
}
.home-card{
  width:min(420px,92vw);text-align:center;
  padding:48px 36px;border-radius:22px;
  border:1px solid var(--line);
  background:linear-gradient(180deg,rgba(20,29,54,.55),rgba(7,10,20,.65));
  backdrop-filter:blur(6px);
  box-shadow:0 30px 80px rgba(0,0,0,.55);
  animation:rise .9s cubic-bezier(.2,.8,.2,1) both;
}
@keyframes rise{from{opacity:0;transform:translateY(18px)}to{opacity:1;transform:none}}
.home-card .seal{
  width:48px;height:48px;margin:0 auto 18px;border-radius:12px;display:grid;place-items:center;
  background:linear-gradient(145deg,var(--gold),#b88a3c);color:#1a1206;font-weight:700;font-size:22px;
  box-shadow:0 0 22px rgba(233,200,120,.45);
}
.home-card h1{
  font-size:34px;letter-spacing:8px;margin-bottom:12px;
  background:linear-gradient(180deg,#fff7e6,var(--gold) 55%,#b98e34);
  -webkit-background-clip:text;background-clip:text;color:transparent;
}
.home-card p{color:var(--ink-dim);font-size:15px;letter-spacing:2px;margin-bottom:28px}
.home-card p b{color:var(--qi);font-weight:600}
.home-card .realm{margin-top:2px;font-size:13px;opacity:.85}
.btn{
  width:100%;padding:14px;border:none;border-radius:12px;cursor:pointer;
  font-family:inherit;font-size:16px;letter-spacing:6px;font-weight:700;color:#0c1322;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  box-shadow:0 10px 30px rgba(233,200,120,.30);transition:.25s;
}
.btn:hover{transform:translateY(-2px);box-shadow:0 16px 40px rgba(233,200,120,.45)}
</style>
