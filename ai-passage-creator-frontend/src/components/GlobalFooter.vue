<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <div class="footer-brand">
        <span class="brand-name gradient-text">协笔 CoWrite</span>
        <span class="brand-slogan">生成、确认、再生成 —— AI 人机协同写作</span>
      </div>
      <p class="copyright">© 2026 协笔 CoWrite · AI 人机协同写作平台</p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
.footer {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  text-align: center;
  padding: 28px 20px;
  margin-top: 0;
  border-top: 1px solid var(--color-border);
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.brand-slogan {
  font-size: 13px;
  color: var(--color-text-muted);
}

.copyright {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 13px;
}

@media (max-width: 480px) {
  .footer-brand {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
