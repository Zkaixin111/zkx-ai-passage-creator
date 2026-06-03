<template>
  <a-layout class="basic-layout">
    <!-- 全局背景光晕氛围 -->
    <div class="ambient-glow" aria-hidden="true"></div>
    <!-- 顶部导航栏 -->
    <GlobalHeader />
    <!-- 主要内容区域 -->
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>
    <!-- 底部版权信息 -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.basic-layout {
  background: none;
  position: relative;
  min-height: 100vh;
}

/* 全局背景光晕（固定层，不干扰滚动） */
.ambient-glow {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  pointer-events: none;
  background: var(--gradient-glow);
  opacity: 0.6;
}

.main-content {
  position: relative;
  z-index: 1;
  width: 100%;
  padding: 0;
  background: none;
  margin: 0;
}
</style>
