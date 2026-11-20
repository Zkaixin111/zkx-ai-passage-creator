<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <div class="footer-brand">
        <span class="brand-name">协笔</span>
        <span class="brand-slogan">生成、确认、再生成</span>
      </div>
      <p class="copyright">
        <a
          href="https://www.codefather.cn"
          target="_blank"
          rel="noopener noreferrer"
          class="author-link"
        >
          编程导航原创项目
        </a>
      </p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
.footer {
  background: var(--color-background);
  text-align: center;
  padding: 28px 20px 36px;
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
  font-family: var(--font-serif);
  font-size: 16px;
  color: var(--color-text);
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

.author-link {
  color: var(--color-text-muted);
  transition: color var(--transition-fast);
}

.author-link:hover {
  color: var(--color-text);
}

@media (max-width: 480px) {
  .footer-brand {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
