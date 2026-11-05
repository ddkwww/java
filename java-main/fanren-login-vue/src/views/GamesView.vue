<template>
  <div class="games-wrap">
    <header class="games-head">
      <h1>小游戏 · 修仙闲趣</h1>
      <p class="sub">以灵石为筹码，与同道切磋消遣。当前开放如下玩法：</p>
    </header>

    <section class="game-grid">
      <article class="game-card" @click="openMonopoly">
        <div class="gc-badge">热</div>
        <div class="gc-icon">🎲</div>
        <h2>大富翁 · 灵石夺宝</h2>
        <p class="gc-desc">
          四人同台，掷骰前行。灵矿、坊市、天劫、秘境……落格皆有灵石盈亏。
          报名缴纳 500 灵石入奖池，终局按局内灵石排名瓜分奖池。
        </p>
        <ul class="gc-meta">
          <li><span>人数</span><b>4 人</b></li>
          <li><span>筹码</span><b>灵石</b></li>
          <li><span>报名费</span><b>500</b></li>
        </ul>
        <button class="gc-btn">进入牌桌 ›</button>
      </article>

      <article class="game-card locked">
        <div class="gc-icon">🀄</div>
        <h2>更多玩法</h2>
        <p class="gc-desc">斗法棋、御兽竞速、探灵夺宝……后续陆续开放，敬请期待。</p>
        <ul class="gc-meta">
          <li><span>状态</span><b>筹备中</b></li>
        </ul>
        <button class="gc-btn" disabled>敬请期待</button>
      </article>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function openMonopoly() { router.push('/games/monopoly') }
</script>

<style scoped>
.games-wrap{max-width:1080px;margin:0 auto;padding:32px 24px 48px;overflow-y:auto;height:100%;}
.games-head h1{
  font-size:30px;letter-spacing:4px;font-weight:700;
  background:linear-gradient(180deg,#fff7e6,var(--gold) 60%,#b98e34);
  -webkit-background-clip:text;background-clip:text;color:transparent;
}
.games-head .sub{margin-top:10px;color:var(--ink-dim);font-size:15px;letter-spacing:1px;}
.game-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:22px;margin-top:28px;}
.game-card{
  position:relative;padding:24px 22px 22px;border-radius:18px;cursor:pointer;
  background:linear-gradient(160deg,rgba(20,29,54,.7),rgba(13,20,38,.7));
  border:1px solid var(--line);transition:.25s;overflow:hidden;
}
.game-card:hover{transform:translateY(-4px);border-color:rgba(233,200,120,.45);box-shadow:0 14px 40px rgba(0,0,0,.4),0 0 24px rgba(233,200,120,.18);}
.game-card.locked{opacity:.7;cursor:default;}
.game-card.locked:hover{transform:none;border-color:var(--line);box-shadow:none;}
.gc-badge{
  position:absolute;top:14px;right:14px;padding:3px 10px;border-radius:999px;font-size:12px;font-weight:700;
  background:linear-gradient(135deg,#ff6b6b,#e04040);color:#fff;box-shadow:0 0 10px rgba(255,80,80,.5);
}
.gc-icon{font-size:46px;margin-bottom:10px;}
.gc-badge + .gc-icon{}
.game-card h2{font-size:21px;letter-spacing:2px;color:var(--gold-2);margin-bottom:10px;}
.gc-desc{font-size:14px;line-height:1.8;color:var(--ink-dim);min-height:78px;}
.gc-meta{display:flex;gap:18px;margin:14px 0 18px;flex-wrap:wrap;}
.gc-meta li{display:flex;flex-direction:column;gap:2px;}
.gc-meta span{font-size:12px;color:var(--ink-dim);letter-spacing:1px;}
.gc-meta b{font-size:16px;color:var(--qi);}
.gc-btn{
  width:100%;padding:11px 0;border-radius:12px;cursor:pointer;font-family:inherit;font-size:15px;letter-spacing:2px;
  color:#0c1322;font-weight:700;border:none;
  background:linear-gradient(135deg,var(--gold-2),var(--gold) 50%,#c79a44);
  box-shadow:0 4px 16px rgba(233,200,120,.25);transition:.2s;
}
.gc-btn:hover{transform:translateY(-1px);box-shadow:0 8px 24px rgba(233,200,120,.45);}
.gc-btn:disabled{background:rgba(140,170,220,.18);color:var(--ink-dim);cursor:not-allowed;box-shadow:none;}
</style>
