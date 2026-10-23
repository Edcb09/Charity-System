<template>
  <router-view />
</template>

<script setup>
</script>

<style scoped>
html, body, #app {
  margin: 0;
  padding: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden !important; /* 👈 强制隐藏外层所有滚动条 */
  box-sizing: border-box;
}

/* 确保所有元素计算宽高时包含 padding */
*, *::before, *::after {
  box-sizing: inherit;
}
</style>
