<template>
  <div v-if="visible" class="battle-mask">
    <div class="battle-box">
      <div class="b-title">论 道 争 锋 · 回 合 战 报</div>
      <div class="b-sub">{{ selfName }} <span class="vs">⚔</span> {{ oppName }}</div>

      <button v-if="!finished" class="b-skip" @click="skip">跳 过</button>

      <div class="b-log" ref="logBox">
        <p
          v-for="(line, i) in shown"
          :key="i"
          class="b-line"
          :class="{ final: line.finalRound }"
        >{{ line.text }}</p>
      </div>

      <div v-if="finished" class="b-result" :class="won ? 'win' : 'lose'">
        <template v-if="won">🏆 你力压群雄，赢下此战！奖励 10 灵石</template>
        <template v-else>💢 你气血耗尽，憾负此战。</template>
      </div>

      <button class="b-close" :disabled="!finished" @click="$emit('close')">
        {{ finished ? '收 剑' : '激战中…' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onUnmounted } from 'vue'

const props = defineProps({
  visible: Boolean,
  selfName: String,
  oppName: String,
  winner: String,
  log: { type: Array, default: () => [] }
})

const shown = ref([])
const finished = ref(false)
const won = ref(false)
const logBox = ref(null)
let timer = null

function stopTimer() {
  if (timer) { clearInterval(timer); timer = null }
}

function startReveal() {
  stopTimer()
  shown.value = []
  finished.value = false
  won.value = false
  const rounds = props.log || []
  let i = 0
  timer = setInterval(async () => {
    if (i >= rounds.length) {
      stopTimer()
      finished.value = true
      won.value = props.winner === props.selfName
      await nextTick()
      scrollBottom()
      return
    }
    shown.value.push(rounds[i])
    i++
    await nextTick()
    scrollBottom()
  }, 750)
}

function scrollBottom() {
  if (logBox.value) logBox.value.scrollTop = logBox.value.scrollHeight
}

// 跳过：立即揭示全部剩余战报并进入结果态
async function skip() {
  stopTimer()
  const rounds = props.log || []
  shown.value = rounds.slice()
  finished.value = true
  won.value = props.winner === props.selfName
  await nextTick()
  scrollBottom()
}

watch(() => props.visible, (v) => {
  if (v) startReveal()
  else stopTimer()
})

onUnmounted(stopTimer)
</script>

<style scoped>
.battle-mask {
  position: fixed; inset: 0; z-index: 9600;
  display: flex; align-items: center; justify-content: center;
  background: rgba(4,7,14,.66); backdrop-filter: blur(3px);
}
.battle-box {
  width: min(560px, 92vw); max-height: 80vh; padding: 24px 26px;
  display: flex; flex-direction: column;
  border-radius: 16px;
  border: 1px solid rgba(233,200,120,.35);
  background: linear-gradient(180deg, rgba(18,26,48,.96), rgba(9,14,28,.98));
  box-shadow: 0 30px 80px rgba(0,0,0,.6);
  animation: bIn .35s cubic-bezier(.2,.8,.2,1);
}
@keyframes bIn { from { opacity: 0; transform: translateY(18px) scale(.97); } to { opacity: 1; transform: none; } }
.b-title {
  text-align: center; font-size: 22px; letter-spacing: 8px; font-weight: 800;
  background: linear-gradient(180deg, #fff7e6, var(--gold) 60%, #b98e34);
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.b-sub { text-align: center; color: var(--ink-dim); letter-spacing: 2px; margin: 8px 0 12px; }
.b-sub .vs { color: var(--qi); margin: 0 6px; font-size: 18px; }
.b-skip {
  align-self: center; margin-bottom: 12px; padding: 6px 20px; border-radius: 999px; cursor: pointer;
  font-family: inherit; font-size: 13px; letter-spacing: 3px; font-weight: 700;
  border: 1px solid rgba(103,232,249,.4); background: transparent; color: var(--qi); transition: .2s;
}
.b-skip:hover { background: rgba(103,232,249,.12); box-shadow: 0 0 14px rgba(103,232,249,.25); }

.b-log {
  flex: 1; min-height: 120px; max-height: 46vh; overflow-y: auto;
  padding: 14px 16px; border-radius: 12px;
  background: rgba(6,10,20,.5); border: 1px solid var(--line);
  font-size: 14px; line-height: 1.9; letter-spacing: .5px;
}
.b-line {
  color: var(--ink); margin: 0; padding: 3px 0;
  border-bottom: 1px dashed rgba(255,255,255,.05);
  animation: lineIn .3s ease;
}
@keyframes lineIn { from { opacity: 0; transform: translateX(-8px); } to { opacity: 1; transform: none; } }
.b-line.final {
  color: var(--gold-2); font-weight: 700; border-bottom: none;
  letter-spacing: 1px;
}

.b-result {
  text-align: center; margin: 14px 0 6px; font-size: 18px; font-weight: 800; letter-spacing: 2px;
}
.b-result.win { color: var(--gold); }
.b-result.lose { color: #ff9a9a; }

.b-close {
  margin-top: 12px; padding: 11px; border-radius: 999px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 4px; font-weight: 700;
  border: none; color: #0c1322;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  transition: .2s;
}
.b-close:disabled { opacity: .5; cursor: default; }
.b-close:not(:disabled):hover { transform: translateY(-1px); box-shadow: 0 12px 28px rgba(233,200,120,.4); }
</style>
