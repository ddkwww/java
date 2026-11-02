<template>
  <transition name="fx-fade">
    <div v-if="visible" class="bx-mask">
      <!-- 阶段一：双方交锋 -->
      <div v-if="phase === 'clash'" class="bx-stage">
        <div class="bx-fighter self">
          <span class="bx-orb"></span>
          <span class="bx-name">{{ selfName || '我' }}</span>
        </div>

        <div class="bx-clash">
          <span class="bx-beam a"></span>
          <span class="bx-beam b"></span>
          <span class="bx-spark"></span>
          <span class="bx-ring"></span>
        </div>

        <div class="bx-fighter opp">
          <span class="bx-orb"></span>
          <span class="bx-name">{{ oppName || '对手' }}</span>
        </div>

        <div class="bx-title">对 战</div>
      </div>

      <!-- 阶段二：揭晓胜负 -->
      <div v-else class="bx-result">
        <div class="bx-result-title" :class="iWin ? 'win' : 'lose'">
          {{ iWin ? '胜 !' : '败' }}
        </div>
        <div class="bx-result-sub">
          {{ iWin ? '技高一筹，道心通明' : '技不如人，他日再战' }}
        </div>
        <div class="bx-versus">
          <span class="v-self">{{ selfName || '我' }}</span>
          <span class="v-vs">VS</span>
          <span class="v-opp">{{ oppName || '对手' }}</span>
        </div>
        <button class="bx-btn" @click="onClose">继 续</button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  selfName: { type: String, default: '' },
  oppName: { type: String, default: '' },
  winner: { type: String, default: '' } // 胜者用户名（与 selfName 比较得出胜负）
})
const emit = defineEmits(['close'])

const phase = ref('clash')
const iWin = computed(() => !!props.winner && props.winner === props.selfName)
let timer = null

watch(() => props.visible, (v) => {
  if (v) {
    phase.value = 'clash'
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => { phase.value = 'result' }, 2000)
  } else if (timer) {
    clearTimeout(timer)
    timer = null
  }
})

function onClose() {
  emit('close')
}

onBeforeUnmount(() => { if (timer) clearTimeout(timer) })
</script>

<style scoped>
.bx-mask {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  background: radial-gradient(circle at 50% 45%, rgba(40,20,10,.85), rgba(5,8,16,.96));
  backdrop-filter: blur(3px);
}
.fx-fade-enter-active, .fx-fade-leave-active { transition: opacity .35s ease; }
.fx-fade-enter-from, .fx-fade-leave-to { opacity: 0; }

/* ===== 阶段一：交锋 ===== */
.bx-stage {
  position: relative; width: min(560px, 92vw); height: 300px;
  display: grid; grid-template-columns: 1fr auto 1fr; align-items: center;
}
.bx-fighter { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.bx-fighter .bx-orb {
  width: 70px; height: 70px; border-radius: 50%;
  animation: bxFloat 1.6s ease-in-out infinite;
}
.bx-fighter.self .bx-orb {
  background: radial-gradient(circle at 50% 38%, #eafcff, var(--qi) 55%, #2b7d8c 90%);
  box-shadow: 0 0 36px 8px rgba(103,232,249,.7);
  transform: translateX(40px);
}
.bx-fighter.opp .bx-orb {
  background: radial-gradient(circle at 50% 38%, #fff3c4, var(--gold) 55%, #b9842f 90%);
  box-shadow: 0 0 36px 8px rgba(233,200,120,.7);
  transform: translateX(-40px);
}
.bx-name { font-size: 15px; letter-spacing: 2px; color: var(--ink); }
@keyframes bxFloat { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }
.bx-fighter.self { animation: bxPushSelf 2s ease-in-out forwards; }
.bx-fighter.opp { animation: bxPushOpp 2s ease-in-out forwards; }
@keyframes bxPushSelf { 0% { transform: translateX(0); } 50% { transform: translateX(28px); } 100% { transform: translateX(0); } }
@keyframes bxPushOpp { 0% { transform: translateX(0); } 50% { transform: translateX(-28px); } 100% { transform: translateX(0); } }

.bx-clash { position: relative; width: 90px; height: 90px; display: grid; place-items: center; }
.bx-beam {
  position: absolute; height: 4px; border-radius: 4px; opacity: 0;
}
.bx-beam.a {
  width: 120px; left: -10px;
  background: linear-gradient(90deg, transparent, var(--qi), #fff);
  box-shadow: 0 0 18px 3px rgba(103,232,249,.8);
  animation: bxBeamA 2s ease-in-out forwards;
}
.bx-beam.b {
  width: 120px; right: -10px;
  background: linear-gradient(270deg, transparent, var(--gold), #fff);
  box-shadow: 0 0 18px 3px rgba(233,200,120,.8);
  animation: bxBeamB 2s ease-in-out forwards;
}
@keyframes bxBeamA { 0% { transform: scaleX(0); opacity: 0; } 45% { transform: scaleX(1); opacity: 1; } 60% { opacity: 1; } 100% { opacity: 0; transform: scaleX(1.2); } }
@keyframes bxBeamB { 0% { transform: scaleX(0); opacity: 0; } 45% { transform: scaleX(1); opacity: 1; } 60% { opacity: 1; } 100% { opacity: 0; transform: scaleX(1.2); } }
.bx-spark {
  position: absolute; width: 30px; height: 30px; border-radius: 50%;
  background: radial-gradient(circle, #fff, rgba(255,255,255,0) 70%);
  opacity: 0; animation: bxSpark 2s ease-out .8s forwards;
}
@keyframes bxSpark { 0% { transform: scale(.3); opacity: 0; } 50% { transform: scale(2.6); opacity: 1; } 100% { transform: scale(3.4); opacity: 0; } }
.bx-ring {
  position: absolute; width: 30px; height: 30px; border-radius: 50%;
  border: 2px solid rgba(255,255,255,.8); opacity: 0;
  animation: bxRing 2s ease-out .8s forwards;
}
@keyframes bxRing { 0% { transform: scale(.2); opacity: .9; } 100% { transform: scale(6); opacity: 0; } }

.bx-title {
  position: absolute; bottom: 18px; left: 0; right: 0; text-align: center;
  font-size: 44px; font-weight: 900; letter-spacing: 14px; text-indent: 14px;
  background: linear-gradient(180deg, #fff, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 20px rgba(233,200,120,.6));
  animation: titleIn .8s .2s cubic-bezier(.2,.8,.2,1) both;
}
@keyframes titleIn { from { opacity: 0; transform: scale(.7); } to { opacity: 1; transform: none; } }

/* ===== 阶段二：结果 ===== */
.bx-result {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  padding: 30px 38px; border-radius: 18px; min-width: 320px;
  border: 1px solid rgba(233,200,120,.3);
  background: linear-gradient(180deg, rgba(20,29,54,.7), rgba(10,16,32,.82));
  box-shadow: 0 30px 80px rgba(0,0,0,.6);
  animation: gainsIn .5s cubic-bezier(.2,.8,.2,1) both;
}
@keyframes gainsIn { from { opacity: 0; transform: translateY(24px) scale(.96); } to { opacity: 1; transform: none; } }
.bx-result-title { font-size: 64px; font-weight: 900; letter-spacing: 8px; }
.bx-result-title.win {
  background: linear-gradient(180deg, #fff, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 24px rgba(233,200,120,.7));
}
.bx-result-title.lose { color: #8fa0c0; filter: drop-shadow(0 0 14px rgba(143,160,192,.4)); }
.bx-result-sub { font-size: 14px; color: var(--ink-dim); letter-spacing: 2px; }
.bx-versus { display: flex; align-items: center; gap: 12px; margin: 6px 0 4px; color: var(--ink); }
.bx-versus .v-vs { color: var(--gold-2); font-size: 13px; letter-spacing: 1px; }
.bx-versus .v-opp { color: var(--gold-2); }
.bx-btn {
  margin-top: 12px; padding: 11px 34px; border: none; border-radius: 999px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 6px; font-weight: 700; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 10px 30px rgba(233,200,120,.3); transition: .2s;
}
.bx-btn:hover { transform: translateY(-2px); box-shadow: 0 16px 40px rgba(233,200,120,.45); }

@media (max-width: 640px) {
  .bx-title { font-size: 34px; }
  .bx-result { width: 86vw; padding: 26px 18px; }
}
</style>
