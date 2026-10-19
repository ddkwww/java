<template>
  <canvas ref="canvas" class="qi-canvas"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvas = ref(null)
let ctx = null
let W = 0
let H = 0
let parts = []
let rafId = 0

function resize() {
  const cv = canvas.value
  if (!cv) return
  W = cv.width = window.innerWidth
  H = cv.height = window.innerHeight
}

function init() {
  resize()
  const count = Math.min(90, Math.floor((W * H) / 16000))
  parts = Array.from({ length: count }, () => ({
    x: Math.random() * W,
    y: Math.random() * H,
    r: Math.random() * 1.8 + 0.4,
    vy: -(Math.random() * 0.4 + 0.15),
    vx: (Math.random() - 0.5) * 0.25,
    a: Math.random() * 0.5 + 0.2,
    c: Math.random() < 0.5 ? '103,232,249' : '233,200,120'
  }))
}

function tick() {
  if (!ctx) return
  ctx.clearRect(0, 0, W, H)
  for (const p of parts) {
    p.y += p.vy
    p.x += p.vx
    p.a += (Math.random() - 0.5) * 0.02
    if (p.y < -5) { p.y = H + 5; p.x = Math.random() * W }
    if (p.x < -5) p.x = W + 5
    if (p.x > W + 5) p.x = -5
    ctx.beginPath()
    ctx.fillStyle = `rgba(${p.c},${Math.max(0.05, Math.min(0.8, p.a))})`
    ctx.arc(p.x, p.y, p.r, 0, 7)
    ctx.fill()
  }
  rafId = requestAnimationFrame(tick)
}

onMounted(() => {
  ctx = canvas.value.getContext('2d')
  init()
  tick()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
.qi-canvas{
  position:fixed;inset:0;z-index:0;pointer-events:none;
}
</style>
