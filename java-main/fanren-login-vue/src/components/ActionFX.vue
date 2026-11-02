<template>
  <transition name="fx-fade">
    <div v-if="visible" class="fx-mask" @click.self="phase === 'gains' && onClose()">

      <!-- 阶段一：动作光效（按 type 切换三种动画） -->
      <div v-if="phase === 'burst'" class="fx-burst" :class="'fx--' + type">
        <div class="fx-scene">
          <!-- 打坐：灵气漩涡 -->
          <template v-if="type === 'meditate'">
            <span class="m-ring m1"></span>
            <span class="m-ring m2"></span>
            <span class="m-ring m3"></span>
            <span class="m-core"></span>
          </template>
          <!-- 斩妖：剑光横扫 -->
          <template v-else-if="type === 'hunt'">
            <span class="h-flash"></span>
            <span class="h-slash s1"></span>
            <span class="h-slash s2"></span>
          </template>
          <!-- 矿脉：灵石迸裂 -->
          <template v-else>
            <span class="n-gem"></span>
            <span class="n-shard sh1"></span>
            <span class="n-shard sh2"></span>
            <span class="n-shard sh3"></span>
            <span class="n-shard sh4"></span>
            <span class="n-burst"></span>
          </template>
        </div>
        <div class="fx-title">{{ title }}</div>
      </div>

      <!-- 阶段二：本次收获 -->
      <div v-else class="fx-gains">
        <div class="fx-gains-title">{{ title }}</div>
        <div class="fx-gains-sub">本次修炼所得</div>
        <ul class="fx-list">
          <li v-if="qiReward > 0" class="fx-item up">
            <span class="fx-icon qi">炁</span>
            <span class="fx-name">灵气</span>
            <span class="fx-delta">+{{ qiReward }}</span>
          </li>
          <li v-if="stoneReward > 0" class="fx-item up">
            <span class="fx-icon stone">灵</span>
            <span class="fx-name">灵石</span>
            <span class="fx-delta">+{{ stoneReward }}</span>
          </li>
        </ul>
        <button class="fx-btn" @click="onClose">继 续 修 行</button>
      </div>

    </div>
  </transition>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  type: { type: String, default: 'meditate' }, // 'meditate' | 'hunt' | 'mine'
  qiReward: { type: Number, default: 0 },
  stoneReward: { type: Number, default: 0 }
})
const emit = defineEmits(['close'])

const TITLES = { meditate: '打 坐 修 炼', hunt: '斩 妖 除 魔', mine: '深 入 矿 脉' }
const title = TITLES[props.type] || '修 行'

const phase = ref('burst')
let timer = null

watch(() => props.visible, (v) => {
  if (v) {
    phase.value = 'burst'
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => { phase.value = 'gains' }, 1500)
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
.fx-mask {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  background: radial-gradient(circle at 50% 45%, rgba(40,30,10,.85), rgba(5,8,16,.96));
  backdrop-filter: blur(3px);
}
.fx-fade-enter-active, .fx-fade-leave-active { transition: opacity .35s ease; }
.fx-fade-enter-from, .fx-fade-leave-to { opacity: 0; }

.fx-burst { display: flex; flex-direction: column; align-items: center; gap: 20px; }
.fx-scene { position: relative; width: 220px; height: 220px; display: grid; place-items: center; }

/* ===== 打坐：青色灵气漩涡 ===== */
.fx--meditate .m-core {
  position: absolute; width: 54px; height: 54px; border-radius: 50%;
  background: radial-gradient(circle at 50% 38%, #eafcff, var(--qi) 55%, #2b7d8c 90%);
  box-shadow: 0 0 36px 8px rgba(103,232,249,.7), 0 0 70px 22px rgba(103,232,249,.35);
  animation: mCore 1.5s ease-in-out forwards;
}
.fx--meditate .m-ring { position: absolute; border-radius: 50%; border: 2px solid transparent; }
.fx--meditate .m1 { width: 100px; height: 100px; border-top-color: var(--qi); border-right-color: rgba(103,232,249,.4); animation: spin 1.5s linear forwards; }
.fx--meditate .m2 { width: 150px; height: 150px; border-bottom-color: var(--gold-2); border-left-color: rgba(233,200,120,.35); animation: spin 2.2s linear reverse forwards; }
.fx--meditate .m3 { width: 200px; height: 200px; border-top-color: rgba(139,109,255,.5); animation: spin 3s linear forwards; }
@keyframes mCore { 0% { transform: scale(.5); opacity: .5; } 60% { transform: scale(1.1); opacity: 1; } 100% { transform: scale(1); opacity: 1; } }
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 斩妖：剑光横扫 ===== */
.fx--hunt .h-flash {
  position: absolute; width: 220px; height: 220px; border-radius: 50%;
  background: radial-gradient(circle, rgba(255,90,90,.5), transparent 60%);
  animation: hFlash 1.5s ease-out forwards;
}
.fx--hunt .h-slash {
  position: absolute; height: 4px; width: 240px; border-radius: 4px;
  background: linear-gradient(90deg, transparent, #fff, rgba(255,120,160,.9));
  box-shadow: 0 0 22px 4px rgba(255,120,160,.8);
  transform: rotate(-32deg) scaleX(0); transform-origin: left center;
  animation: hSlash 1.5s ease-out forwards;
}
.fx--hunt .h-slash.s2 { transform: rotate(28deg) scaleX(0); animation-delay: .12s; background: linear-gradient(90deg, transparent, #fff, rgba(180,120,255,.9)); box-shadow: 0 0 22px 4px rgba(180,120,255,.8); }
@keyframes hFlash { 0% { opacity: 0; transform: scale(.4); } 25% { opacity: 1; } 100% { opacity: 0; transform: scale(1.2); } }
@keyframes hSlash { 0% { transform: rotate(-32deg) scaleX(0); opacity: 0; } 35% { transform: rotate(-32deg) scaleX(1); opacity: 1; } 100% { transform: rotate(-32deg) scaleX(1.15); opacity: 0; } }

/* ===== 矿脉：灵石迸裂 ===== */
.fx--mine .n-gem {
  position: absolute; width: 60px; height: 60px; transform: rotate(45deg);
  background: linear-gradient(135deg, #fff3c4, var(--gold) 50%, #b9842f);
  box-shadow: 0 0 30px 6px rgba(233,200,120,.7);
  animation: nGem 1.5s ease-in-out forwards;
}
.fx--mine .n-burst {
  position: absolute; width: 30px; height: 30px; border-radius: 50%;
  background: radial-gradient(circle, rgba(233,200,120,.9), transparent 70%);
  animation: nBurst 1.5s ease-out forwards;
}
.fx--mine .n-shard {
  position: absolute; width: 12px; height: 12px; transform: rotate(45deg);
  background: linear-gradient(135deg, #fff3c4, var(--gold)); opacity: 0;
}
.fx--mine .sh1 { animation: nFly1 1.5s .25s ease-out forwards; }
.fx--mine .sh2 { animation: nFly2 1.5s .25s ease-out forwards; }
.fx--mine .sh3 { animation: nFly3 1.5s .25s ease-out forwards; }
.fx--mine .sh4 { animation: nFly4 1.5s .25s ease-out forwards; }
@keyframes nGem { 0% { transform: rotate(45deg) scale(.4); opacity: .4; } 50% { transform: rotate(45deg) scale(1.15); opacity: 1; } 100% { transform: rotate(45deg) scale(1); opacity: 1; } }
@keyframes nBurst { 0% { transform: scale(.3); opacity: 0; } 30% { opacity: 1; } 100% { transform: scale(4); opacity: 0; } }
@keyframes nFly1 { 0% { opacity: 0; transform: rotate(45deg) translate(0,0); } 30% { opacity: 1; } 100% { opacity: 0; transform: rotate(45deg) translate(-90px,-70px); } }
@keyframes nFly2 { 0% { opacity: 0; transform: rotate(45deg) translate(0,0); } 30% { opacity: 1; } 100% { opacity: 0; transform: rotate(45deg) translate(90px,-60px); } }
@keyframes nFly3 { 0% { opacity: 0; transform: rotate(45deg) translate(0,0); } 30% { opacity: 1; } 100% { opacity: 0; transform: rotate(45deg) translate(-80px,80px); } }
@keyframes nFly4 { 0% { opacity: 0; transform: rotate(45deg) translate(0,0); } 30% { opacity: 1; } 100% { opacity: 0; transform: rotate(45deg) translate(85px,85px); } }

.fx-title {
  font-size: 46px; font-weight: 900; letter-spacing: 12px; text-indent: 12px;
  background: linear-gradient(180deg, #fff, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 22px rgba(233,200,120,.6));
  animation: titleIn .8s .2s cubic-bezier(.2,.8,.2,1) both;
}
@keyframes titleIn { from { opacity: 0; transform: scale(.7); } to { opacity: 1; transform: none; } }

/* ===== 阶段二：收获 ===== */
.fx-gains {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 28px 34px; border-radius: 18px; min-width: 320px;
  border: 1px solid rgba(233,200,120,.3);
  background: linear-gradient(180deg, rgba(20,29,54,.7), rgba(10,16,32,.82));
  box-shadow: 0 30px 80px rgba(0,0,0,.6), 0 0 0 1px rgba(233,200,120,.06) inset;
  animation: gainsIn .5s cubic-bezier(.2,.8,.2,1) both;
}
@keyframes gainsIn { from { opacity: 0; transform: translateY(24px) scale(.96); } to { opacity: 1; transform: none; } }
.fx-gains-title {
  font-size: 24px; letter-spacing: 8px;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 14px rgba(233,200,120,.4));
}
.fx-gains-sub { font-size: 13px; color: var(--ink-dim); letter-spacing: 2px; margin-bottom: 8px; }
.fx-list { list-style: none; padding: 0; margin: 6px 0 4px; width: 100%; display: grid; gap: 8px; }
.fx-item {
  display: flex; align-items: center; gap: 12px; padding: 10px 14px; border-radius: 10px;
  border: 1px solid var(--line); background: rgba(20,29,54,.5);
}
.fx-item.up { border-color: rgba(233,200,120,.4); box-shadow: 0 0 16px rgba(233,200,120,.12); }
.fx-icon {
  display: inline-flex; width: 34px; height: 34px; align-items: center; justify-content: center;
  border-radius: 50%; border: 1px solid var(--line); font-size: 14px;
}
.fx-icon.qi { color: var(--qi); }
.fx-icon.stone { color: var(--gold-2); }
.fx-name { flex: 1; color: var(--ink); letter-spacing: 1px; font-size: 15px; }
.fx-delta { font-size: 20px; font-weight: 700; color: var(--gold-2); font-family: "Noto Serif SC", serif; text-shadow: 0 0 12px rgba(233,200,120,.4); }
.fx-btn {
  margin-top: 14px; padding: 11px 34px; border: none; border-radius: 999px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 6px; font-weight: 700; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 10px 30px rgba(233,200,120,.3); transition: .2s;
}
.fx-btn:hover { transform: translateY(-2px); box-shadow: 0 16px 40px rgba(233,200,120,.45); }

@media (max-width: 640px) {
  .fx-title { font-size: 36px; }
  .fx-gains { min-width: 0; width: 86vw; padding: 24px 20px; }
}
</style>
