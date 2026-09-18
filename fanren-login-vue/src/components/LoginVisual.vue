<template>
  <section class="visual">
    <div class="brand"><span class="seal">仙</span> 仙缘阁 · 通行令</div>
    <div>
      <h1 class="title">凡人<br>修仙传</h1>
      <p class="subtitle">凡人亦可逆天改命</p>
      <div class="realms">
        <span
          v-for="(r, i) in realms"
          :key="r"
          :class="{ on: i === active }"
        >{{ r }}</span>
      </div>
    </div>
    <div class="foot">青云之巅 · 一念成仙</div>
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const realms = ['炼气', '筑基', '金丹', '元婴', '化神', '炼虚', '合体', '大乘', '渡劫']
const active = ref(0)
let timer = 0

onMounted(() => {
  timer = setInterval(() => {
    active.value = (active.value + 1) % realms.length
  }, 1600)
})

onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.visual{
  position:relative;flex:1.1;min-width:280px;
  padding:42px 38px;
  display:flex;flex-direction:column;justify-content:space-between;
  overflow:hidden;
  /* 如需韩立背景图：把图片放到 public/hanli.png，并在下方加一行 url('/hanli.png') center 22%/cover no-repeat, */
  background:
    linear-gradient(100deg,rgba(7,10,20,.82) 0%,rgba(7,10,20,.5) 40%,rgba(7,10,20,.12) 68%,rgba(7,10,20,.6) 100%),
    #070a14;
  background-color:#070a14;
}
.visual::before{ /* 右缘渐隐，与登录区融合 */
  content:"";position:absolute;top:0;right:0;bottom:0;width:34%;
  background:linear-gradient(90deg,transparent,rgba(7,10,20,.7));
  pointer-events:none;z-index:0;
}
.visual::after{ /* 底部压暗，托住文字 */
  content:"";position:absolute;left:0;right:0;bottom:0;height:42%;
  background:linear-gradient(0deg,rgba(7,10,20,.85),transparent);
  pointer-events:none;z-index:0;
}
.visual .brand{display:flex;align-items:center;gap:10px;font-size:14px;letter-spacing:3px;color:var(--gold)}
.visual .brand .seal{
  width:30px;height:30px;border-radius:7px;display:grid;place-items:center;
  background:linear-gradient(145deg,var(--gold),#b88a3c);color:#1a1206;font-weight:700;
  box-shadow:0 0 18px rgba(233,200,120,.45);
}
.visual .title{
  position:relative;z-index:1;
  font-size:clamp(40px,6vw,68px);font-weight:700;line-height:1.05;
  letter-spacing:6px;margin-top:8px;
  background:linear-gradient(180deg,#fff7e6,var(--gold) 55%,#b98e34);
  -webkit-background-clip:text;background-clip:text;color:transparent;
  text-shadow:0 0 40px rgba(233,200,120,.25);
}
.visual .subtitle{position:relative;z-index:1;color:var(--ink-dim);letter-spacing:4px;margin-top:14px;font-size:15px}
.realms{
  position:relative;z-index:1;display:flex;flex-wrap:wrap;gap:8px;margin-top:18px;
}
.realms span{
  font-size:12.5px;letter-spacing:2px;padding:6px 12px;border-radius:999px;
  border:1px solid var(--line);color:var(--ink-dim);
  background:rgba(255,255,255,.02);transition:.3s;
}
.realms span.on{color:var(--gold);border-color:rgba(233,200,120,.5);box-shadow:0 0 16px rgba(233,200,120,.25)}
.visual .foot{position:relative;z-index:1;color:var(--ink-dim);font-size:12px;letter-spacing:2px;opacity:.7}

@media(max-width:760px){
  .visual{min-height:200px}
}
</style>
