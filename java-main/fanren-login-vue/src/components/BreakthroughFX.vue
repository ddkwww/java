<template>
  <transition name="fx-fade">
    <div v-if="visible" class="fx-mask" @click.self="phase === 'gains' && onClose()">

      <!-- 阶段一：境界突破光效 -->
      <div v-if="phase === 'burst'" class="fx-burst">
        <div class="orb">
          <span class="ring r3"></span>
          <span class="ring r2"></span>
          <span class="ring r1"></span>
          <span class="core"></span>
        </div>
        <div class="fx-title">突 破 ！</div>
        <div class="fx-realm">{{ newRealm }}</div>
      </div>

      <!-- 阶段二：属性提升明细 -->
      <div v-else class="fx-gains">
        <div class="fx-gains-title">境 界 突 破</div>
        <div class="fx-gains-sub">修为精进 · 属性提升如下</div>
        <ul class="fx-list">
          <li
            v-for="d in diffs"
            :key="d.key"
            class="fx-item"
            :class="{ up: d.delta > 0 }"
          >
            <span class="fx-icon">{{ d.icon }}</span>
            <span class="fx-name">{{ d.name }}</span>
            <span class="fx-delta">
              {{ d.delta > 0 ? '+' + d.delta : (d.delta < 0 ? d.delta : '无变化') }}<i v-if="d.delta > 0" class="fx-unit">{{ d.unit }}</i>
            </span>
          </li>
        </ul>
        <button class="fx-btn" @click="onClose">继 续 修 行</button>
      </div>

    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  before: { type: Object, default: () => ({}) },
  after: { type: Object, default: () => ({}) },
  attrList: { type: Array, default: () => [] },
  newRealm: { type: String, default: '' }
})
const emit = defineEmits(['close'])

const phase = ref('burst')   // 'burst'（光效）→ 'gains'（属性提升）
let timer = null

// 五维属性的本次增量 = 突破后 - 突破前
const diffs = computed(() =>
  props.attrList.map(a => ({
    ...a,
    delta: (Number(props.after[a.key]) || 0) - (Number(props.before[a.key]) || 0)
  }))
)

// 遮罩出现即从头播放：先光效，约 1.9s 后切到属性提升
watch(() => props.visible, (v) => {
  if (v) {
    phase.value = 'burst'
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => { phase.value = 'gains' }, 1900)
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
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 50% 45%, rgba(40,30,10,.85), rgba(5,8,16,.96));
  backdrop-filter: blur(3px);
}
.fx-fade-enter-active, .fx-fade-leave-active { transition: opacity .35s ease; }
.fx-fade-enter-from, .fx-fade-leave-to { opacity: 0; }

/* ===== 阶段一：光效 ===== */
.fx-burst { display: flex; flex-direction: column; align-items: center; gap: 18px; }

.orb { position: relative; width: 210px; height: 210px; display: grid; place-items: center; }
.core {
  position: absolute; width: 72px; height: 72px; border-radius: 50%;
  background: radial-gradient(circle at 50% 38%, #fff7e0, var(--gold) 45%, #b8862f 80%);
  box-shadow: 0 0 40px 10px rgba(233,200,120,.7), 0 0 90px 34px rgba(233,200,120,.4);
  animation: corePulse 1.9s ease-in-out forwards;
}
.ring { position: absolute; border-radius: 50%; }
.r1 { width: 112px; height: 112px; border: 2px solid rgba(233,200,120,.6); animation: ringOut 1.9s ease-out forwards; }
.r2 { width: 156px; height: 156px; border: 2px solid rgba(103,232,249,.5); animation: ringOut 1.9s .15s ease-out forwards; }
.r3 { width: 200px; height: 200px; border: 2px solid rgba(139,109,255,.45); animation: ringOut 1.9s .3s ease-out forwards; }

@keyframes corePulse {
  0%   { transform: scale(.4); opacity: .4; }
  40%  { transform: scale(1.05); opacity: 1; }
  70%  { transform: scale(1); opacity: 1; }
  100% { transform: scale(2.3); opacity: 0; }
}
@keyframes ringOut {
  0%   { transform: scale(.3); opacity: .9; }
  100% { transform: scale(1.5); opacity: 0; }
}

.fx-title {
  font-size: 56px; font-weight: 900; letter-spacing: 14px; text-indent: 14px;
  background: linear-gradient(180deg, #fff, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 24px rgba(233,200,120,.6));
  animation: titleIn .8s .2s cubic-bezier(.2,.8,.2,1) both;
}
.fx-realm {
  font-size: 22px; letter-spacing: 4px; color: var(--qi);
  animation: upIn .8s 1s both;
}
@keyframes titleIn { from { opacity: 0; transform: scale(.7); } to { opacity: 1; transform: none; } }
@keyframes upIn { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: none; } }

/* ===== 阶段二：属性提升 ===== */
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
  font-size: 26px; letter-spacing: 10px;
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
  border-radius: 50%; border: 1px solid var(--line); color: var(--qi); font-size: 14px;
}
.fx-name { flex: 1; color: var(--ink); letter-spacing: 1px; font-size: 15px; }
.fx-delta { font-size: 20px; font-weight: 700; color: var(--ink-dim); font-family: "Noto Serif SC", serif; }
.fx-item.up .fx-delta { color: var(--gold-2); text-shadow: 0 0 12px rgba(233,200,120,.4); }
.fx-unit { font-size: 12px; font-style: normal; margin-left: 2px; opacity: .8; }

.fx-btn {
  margin-top: 14px; padding: 11px 34px; border: none; border-radius: 999px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 6px; font-weight: 700; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 10px 30px rgba(233,200,120,.3); transition: .2s;
}
.fx-btn:hover { transform: translateY(-2px); box-shadow: 0 16px 40px rgba(233,200,120,.45); }

/* 移动端 */
@media (max-width: 640px) {
  .fx-burst { gap: 14px; }
  .fx-title { font-size: 44px; }
  .fx-gains { min-width: 0; width: 86vw; padding: 24px 20px; }
}
</style>
