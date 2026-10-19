<template>
  <div class="toast" :class="{ show: visible }">{{ message }}</div>
</template>

<script setup>
defineProps({
  message: { type: String, default: '' },
  visible: { type: Boolean, default: false }
})
</script>

<style scoped>
.toast{
  position:fixed;left:50%;bottom:34px;transform:translateX(-50%) translateY(20px);
  background:rgba(13,20,38,.9);border:1px solid var(--line);color:var(--ink);
  padding:12px 22px;border-radius:12px;font-size:14px;letter-spacing:2px;
  opacity:0;pointer-events:none;transition:.35s;z-index:9;backdrop-filter:blur(8px);
  white-space:nowrap;
}
.toast.show{opacity:1;transform:translateX(-50%) translateY(0)}
</style>
