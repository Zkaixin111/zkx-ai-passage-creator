<template>
  <div class="brand-section">
    <div class="brand-content">
      <img src="@/assets/logo.png" alt="" class="logo-img" />
      <p class="brand-kicker">人机协同写作</p>
      <h1 class="brand-title">协笔</h1>
      <p class="brand-subtitle">生成、确认、再生成。关键的地方，停下来等你。</p>
      <ul class="brand-features">
        <li>标题和大纲先给你看</li>
        <li>正文一边写，进度一边留在页面上</li>
        <li>配图等到读完正文再决定</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.brand-section {
  flex: 1.05;
  display: flex;
  align-items: center;
  padding: 48px 40px;
  background: var(--color-background-dark);
  color: #f6f1e8;
}

.brand-content {
  max-width: 360px;
}

.logo-img {
  width: 56px;
  height: 56px;
  object-fit: contain;
  border-radius: 14px;
  margin-bottom: 28px;
}

.brand-kicker {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #e7c3a4;
}

.brand-title {
  margin: 0 0 12px;
  font-size: 48px;
  line-height: 1;
  color: #f6f1e8;
}

.brand-subtitle {
  margin: 0 0 28px;
  font-size: 15px;
  line-height: 1.7;
  color: rgba(246, 241, 232, 0.78);
}

.brand-features {
  margin: 0;
  padding: 0;
  list-style: none;
}

.brand-features li {
  position: relative;
  padding-left: 16px;
  margin-bottom: 12px;
  font-size: 14px;
  color: rgba(246, 241, 232, 0.9);
}

.brand-features li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.65em;
  width: 6px;
  height: 1px;
  background: #e7c3a4;
}

@media (max-width: 768px) {
  .brand-section {
    padding: 28px 24px;
  }

  .brand-title {
    font-size: 36px;
  }

  .brand-features {
    display: none;
  }
}
</style>
