<template>
  <div class="duel-listener">
    <!-- 接收对战请求弹窗（全局，任意页面都会弹出） -->
    <transition name="modal">
      <div v-if="state.incoming" class="modal-mask">
        <div class="modal">
          <div class="m-title">对 战 邀 请</div>
          <p class="m-text">
            <b>{{ state.incoming.from }}</b> 向你发起了切磋
            <span v-if="state.incoming.fromRealm" class="m-realm">（{{ state.incoming.fromRealm }}）</span>
          </p>
          <p class="m-tip">同意则双方进入对战，拒绝则对方将收到提示。</p>
          <div class="m-actions">
            <button class="m-accept" @click="accept">同 意</button>
            <button class="m-reject" @click="reject">拒 绝</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 对战文字战报（回合制，全局） -->
    <BattleLog
      :visible="state.battle.visible"
      :self-name="state.battle.selfName"
      :opp-name="state.battle.oppName"
      :winner="state.battle.winner"
      :log="state.battle.log"
      @close="closeBattle"
    />

    <!-- 轻提示（全局） -->
    <transition name="toast">
      <div v-if="state.toast" class="toast">{{ state.toast }}</div>
    </transition>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import BattleLog from './BattleLog.vue'
import { useDuel } from '../duelBus'

const { state, startPendingPoll, stopPendingPoll, accept, reject, closeBattle } = useDuel()

onMounted(() => {
  startPendingPoll()
})
onUnmounted(() => {
  stopPendingPoll()
})
</script>

<style scoped>
.duel-listener { display: contents; }

/* ===== 对战请求弹窗 ===== */
.modal-mask {
  position: fixed; inset: 0; z-index: 9500;
  display: flex; align-items: center; justify-content: center;
  background: rgba(4,7,14,.6); backdrop-filter: blur(2px);
}
.modal {
  width: min(380px, 90vw); padding: 28px 26px; border-radius: 16px; text-align: center;
  border: 1px solid rgba(233,200,120,.35);
  background: linear-gradient(180deg, rgba(20,29,54,.92), rgba(10,16,32,.95));
  box-shadow: 0 30px 80px rgba(0,0,0,.6);
  animation: gainsIn .4s cubic-bezier(.2,.8,.2,1);
}
@keyframes gainsIn { from { opacity: 0; transform: translateY(20px) scale(.96); } to { opacity: 1; transform: none; } }
.m-title { font-size: 22px; letter-spacing: 8px; color: var(--gold-2); margin-bottom: 14px; }
.m-text { color: var(--ink); font-size: 15px; letter-spacing: 1px; }
.m-text b { color: var(--qi); }
.m-realm { color: var(--gold-2); font-size: 13px; }
.m-tip { color: var(--ink-dim); font-size: 12px; margin: 10px 0 18px; letter-spacing: 1px; }
.m-actions { display: flex; gap: 12px; }
.m-accept, .m-reject {
  flex: 1; padding: 11px; border-radius: 999px; cursor: pointer;
  font-family: inherit; font-size: 15px; letter-spacing: 4px; font-weight: 700; transition: .2s;
}
.m-accept { border: none; color: #0c1322; background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44); }
.m-accept:hover { transform: translateY(-1px); box-shadow: 0 12px 28px rgba(233,200,120,.4); }
.m-reject { border: 1px solid rgba(255,120,120,.4); background: transparent; color: #ff9a9a; }
.m-reject:hover { background: rgba(255,120,120,.12); }

/* ===== 轻提示 ===== */
.toast {
  position: fixed; left: 50%; bottom: 40px; transform: translateX(-50%); z-index: 9999;
  padding: 12px 22px; border-radius: 999px; font-size: 14px; letter-spacing: 2px;
  color: #0c1322; font-weight: 700;
  background: linear-gradient(135deg, var(--gold-2), var(--gold) 50%, #c79a44);
  box-shadow: 0 12px 40px rgba(0,0,0,.4);
}
.toast-enter-active, .toast-leave-active { transition: opacity .3s, transform .3s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(10px); }

.modal-enter-active, .modal-leave-active { transition: opacity .25s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
