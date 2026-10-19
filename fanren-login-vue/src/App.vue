<template>
  <QiParticles />
  <div class="fog"></div>

  <router-view v-slot="{ Component }">
    <component :is="Component" />
  </router-view>
</template>

<script setup>
import QiParticles from './components/QiParticles.vue'
</script>

<style scoped>
/* 流光雾气 */
.fog{
  position:fixed;inset:-20%;z-index:0;pointer-events:none;
  background:
    radial-gradient(40% 30% at 20% 80%,rgba(103,232,249,.10),transparent 70%),
    radial-gradient(45% 35% at 85% 20%,rgba(139,109,255,.12),transparent 70%);
  filter:blur(8px);
  animation:drift 18s ease-in-out infinite alternate;
}
@keyframes drift{from{transform:translate3d(-2%,1%,0) scale(1)}to{transform:translate3d(2%,-1%,0) scale(1.06)}}
</style>
