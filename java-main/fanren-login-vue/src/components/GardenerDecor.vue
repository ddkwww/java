<template>
  <!-- 第五人格·园丁（艾玛·伍兹）立体手办：置于登录卡片外的左侧空白背景 -->
  <div class="gardener-decor" aria-hidden="true">
    <div class="glow"></div>
    <div class="ring"></div>
    <img class="gardener" src="/gardener.png" alt="园丁 艾玛·伍兹">
  </div>
</template>

<script setup>
// 纯装饰组件，无逻辑
</script>

<style scoped>
.gardener-decor{
  /* 卡片宽 min(960px,94vw) 居中 => 左空白区宽 = 50% - min(480px,47vw)，取其中心 */
  position:absolute;
  left:calc((50% - min(480px,47vw)) / 2);
  top:47%;
  transform:translate(-50%,-44%);
  width:clamp(240px,25vw,380px);
  z-index:0;
  display:flex;flex-direction:column;align-items:center;justify-content:flex-end;
  pointer-events:none;
}
.gardener{
  position:relative;z-index:3;
  width:100%;height:auto;display:block;
  filter:drop-shadow(0 26px 30px rgba(0,0,0,.6))
         drop-shadow(0 0 34px rgba(120,180,230,.3));
  animation:float 4.5s ease-in-out infinite;
  transform-origin:50% 100%;
}
@keyframes float{
  0%,100%{transform:translateY(0) rotate(-.7deg)}
  50%{transform:translateY(-14px) rotate(.7deg)}
}
/* 脚下发光光晕 */
.glow{
  position:absolute;z-index:1;left:50%;bottom:1%;
  width:170%;height:90px;transform:translateX(-50%);
  background:radial-gradient(ellipse at center,rgba(233,200,120,.5),rgba(103,232,249,.2) 45%,transparent 72%);
  filter:blur(11px);border-radius:50%;
  animation:pulse 4.5s ease-in-out infinite;
}
@keyframes pulse{
  0%,100%{opacity:.7;transform:translateX(-50%) scaleX(1)}
  50%{opacity:1;transform:translateX(-50%) scaleX(.9)}
}
/* 符文底座 */
.ring{
  position:absolute;z-index:2;left:50%;bottom:-3%;
  width:125%;height:40px;transform:translateX(-50%) perspective(180px) rotateX(60deg);
  border:1px solid rgba(233,200,120,.5);border-radius:50%;
  background:radial-gradient(ellipse at center,rgba(233,200,120,.26),transparent 70%);
  box-shadow:0 0 26px rgba(233,200,120,.32),inset 0 0 18px rgba(103,232,249,.22);
}

/* 窄屏左侧空白不足时隐藏，避免遮挡卡片 */
@media(max-width:1400px){
  .gardener-decor{display:none}
}
</style>
