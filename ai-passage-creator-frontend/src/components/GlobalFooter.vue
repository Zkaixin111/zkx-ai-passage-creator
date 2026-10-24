<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <div class="footer-brand">
        <div class="footer-seal shenbi-seal vertical">协笔</div>
        <div class="brand-text">
          <span class="brand-name">协笔 CoWrite</span>
          <span class="brand-slogan">落笔生花 —— AI 人机协同写作</span>
        </div>
      </div>
      <p class="copyright">© 2026 协笔 CoWrite · 落笔生花的 AI 写作平台</p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
/* 墨色底 + 宣纸色文字，与首页页尾原型一致 */
.footer {
  background: var(--ink);
  color: rgba(247, 241, 229, 0.55);
  text-align: center;
  padding: 30px 20px;
  margin-top: 0;
  border-top: none;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 页脚小印章 */
.footer-seal {
  width: 30px;
  height: 30px;
  font-size: 11px;
  border-radius: 5px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.4;
}

.brand-name {
  font-family: var(--kai);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: rgba(247, 241, 229, 0.85);
}

.brand-slogan {
  font-size: 12px;
  color: rgba(247, 241, 229, 0.45);
}

.copyright {
  margin: 0;
  color: rgba(247, 241, 229, 0.45);
  font-size: 13px;
}

@media (max-width: 480px) {
  .footer-brand {
    flex-direction: column;
    gap: 6px;
  }

  .brand-text {
    align-items: center;
  }
}
</style>
