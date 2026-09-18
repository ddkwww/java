<template>
  <div class="stage">
    <div class="panel">
      <LoginVisual />
      <LoginForm />
    </div>
  </div>
</template>

<script setup>
import LoginVisual from '../components/LoginVisual.vue'
import LoginForm from '../components/LoginForm.vue'
</script>

<style scoped>
.stage{
  position:relative;z-index:2;
  height:100%;width:100%;
  display:flex;align-items:center;justify-content:center;
  padding:24px;
}
.panel{
  display:flex;width:min(960px,94vw);height:min(620px,90vh);
  border-radius:22px;overflow:hidden;
  border:1px solid var(--line);
  box-shadow:0 30px 80px rgba(0,0,0,.55),0 0 0 1px rgba(233,200,120,.05) inset;
  backdrop-filter:blur(6px);
}

@media(max-width:760px){
  .panel{flex-direction:column;height:auto}
}
</style>
