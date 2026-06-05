<script setup lang="ts">
import { provide } from 'vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

// 提供全局中文语言配置
provide('locale', zhCN)
</script>

<template>
  <a-config-provider :locale="zhCN">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
/* 引入 Google Fonts - Geometric Modern */
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Work+Sans:wght@300;400;500;600;700&display=swap');

/* 全局CSS变量 - 清新绿色配色方案 */
:root {
  /* 品牌色 - 绿色系 */
  --color-primary: #22C55E;
  --color-primary-light: #4ADE80;
  --color-primary-dark: #16A34A;
  --color-secondary: #0F172A;
  --color-secondary-light: #334155;

  /* 功能色 */
  --color-cta: #22C55E;
  --color-success: #22C55E;
  --color-warning: #EAB308;
  --color-error: #EF4444;
  --color-info: #3B82F6;

  /* 中性色 */
  --color-background: #FFFFFF;
  --color-background-secondary: #F8FAFC;
  --color-background-tertiary: #F1F5F9;
  --color-background-dark: #0F172A;
  --color-text: #0F172A;
  --color-text-secondary: #475569;
  --color-text-muted: #94A3B8;
  --color-border: #E2E8F0;
  --color-border-light: #F1F5F9;

  /* 渐变 */
  --gradient-primary: linear-gradient(135deg, #22C55E 0%, #16A34A 100%);
  --gradient-hero: linear-gradient(180deg, #DCFCE7 0%, #FFFFFF 100%);
  --gradient-hero-dark: linear-gradient(180deg, #166534 0%, #14532D 100%);
  --gradient-text: linear-gradient(135deg, #16A34A 0%, #22C55E 50%, #4ADE80 100%);
  --gradient-glow: radial-gradient(circle at 30% 20%, rgba(34, 197, 94, 0.18) 0%, transparent 45%),
                   radial-gradient(circle at 70% 80%, rgba(74, 222, 128, 0.14) 0%, transparent 40%);

  /* 纯色背景 */
  --bg-primary: #22C55E;
  --bg-primary-hover: #16A34A;
  --bg-dark: #0F172A;
  --bg-dark-hover: #1E293B;
  --bg-success: #22C55E;

  /* 阴影 */
  --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  --shadow-card: 0 1px 3px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-card-hover: 0 8px 24px rgba(34, 197, 94, 0.15);
  --shadow-green: 0 4px 14px rgba(34, 197, 94, 0.25);
  --shadow-glow: 0 0 0 1px rgba(34, 197, 94, 0.12), 0 12px 40px -8px rgba(34, 197, 94, 0.28);

  /* 圆角 */
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-2xl: 24px;
  --radius-full: 9999px;

  /* 过渡 */
  --transition-fast: 150ms ease-out;
  --transition-normal: 200ms ease-out;
  --transition-slow: 300ms ease-out;
  --transition-bounce: 400ms cubic-bezier(0.34, 1.56, 0.64, 1);

  /* 毛玻璃 */
  --glass-bg: rgba(255, 255, 255, 0.9);
  --glass-bg-green: rgba(220, 252, 231, 0.8);
  --glass-blur: blur(12px);
  --glass-border: 1px solid rgba(255, 255, 255, 0.6);

  /* Aurora 光晕动效色（大模型产品风） */
  --aurora-1: rgba(34, 197, 94, 0.35);
  --aurora-2: rgba(56, 189, 248, 0.22);
  --aurora-3: rgba(163, 230, 53, 0.25);
  --aurora-4: rgba(129, 140, 248, 0.18);
}

/* 全局样式优化 */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Work Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
  background-color: var(--color-background);
  color: var(--color-text);
  line-height: 1.6;
  /* 微噪点纹理：大模型产品质感（灰阶噪点叠加） */
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.028'/%3E%3C/svg%3E");
  background-attachment: fixed;
}

html {
  overflow-x: hidden;
}

h1, h2, h3, h4, h5, h6 {
  font-family: 'Outfit', sans-serif;
}

/* 全局链接样式 */
a {
  color: var(--color-text);
  text-decoration: none;
  transition: color var(--transition-fast);
}

a:hover {
  color: var(--color-primary);
}

/* 全局按钮增强 */
.ant-btn {
  transition: all var(--transition-normal);
  font-weight: 500;
  border-radius: var(--radius-md);
}

.ant-btn-primary {
  background: var(--bg-primary);
  border-color: var(--bg-primary);
  box-shadow: var(--shadow-green);
}

.ant-btn-primary:not(:disabled):hover {
  background: var(--bg-primary-hover);
  border-color: var(--bg-primary-hover);
  box-shadow: 0 6px 20px rgba(34, 197, 94, 0.35);
}

/* 全局卡片增强 */
.ant-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
}

.ant-card.hoverable:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
}

/* 玻璃卡片通用类 */
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: var(--glass-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
}

/* 渐变描边卡片（大模型产品风：双背景实现彩色描边） */
.gradient-border-card {
  position: relative;
  background: linear-gradient(#fff, #fff) padding-box,
              linear-gradient(135deg, rgba(34, 197, 94, 0.5), rgba(56, 189, 248, 0.25), rgba(163, 230, 53, 0.4)) border-box;
  border: 1.5px solid transparent;
  border-radius: var(--radius-xl);
  transition: transform var(--transition-bounce), box-shadow var(--transition-normal);
}

.gradient-border-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-glow);
}

/* Aurora 背景层（慢速漂移光晕） */
.aurora-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.aurora-bg::before,
.aurora-bg::after {
  content: '';
  position: absolute;
  border-radius: var(--radius-full);
  filter: blur(90px);
  opacity: 0.6;
  animation: aurora-drift 18s ease-in-out infinite;
}

.aurora-bg::before {
  width: 55%;
  height: 55%;
  top: -10%;
  left: -5%;
  background: var(--aurora-1);
}

.aurora-bg::after {
  width: 45%;
  height: 45%;
  bottom: -8%;
  right: -5%;
  background: var(--aurora-2);
  animation-delay: -9s;
}

@keyframes aurora-drift {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(6%, 4%) scale(1.08);
  }
  66% {
    transform: translate(-4%, -3%) scale(0.95);
  }
}

/* 发光主按钮 */
.glow-btn {
  position: relative;
  background: var(--gradient-primary) !important;
  border: none !important;
  color: white !important;
  box-shadow: 0 0 0 1px rgba(34, 197, 94, 0.2), 0 8px 32px -6px rgba(34, 197, 94, 0.55) !important;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal) !important;
}

.glow-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 0 1px rgba(34, 197, 94, 0.3), 0 14px 44px -8px rgba(34, 197, 94, 0.7) !important;
}

/* 可点击卡片悬浮动效 */
.hoverable-card {
  transition: transform var(--transition-bounce), box-shadow var(--transition-normal), border-color var(--transition-normal);
}

.hoverable-card:hover {
  transform: translateY(-4px);
  border-color: var(--color-primary-light);
  box-shadow: var(--shadow-card-hover);
}

/* 渐变文字通用类 */
.gradient-text {
  background: var(--gradient-text);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 按钮按压微反馈 */
.ant-btn:not(:disabled):active {
  transform: scale(0.97);
}

/* 自定义滚动条（绿色系） */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(34, 197, 94, 0.25);
  border-radius: var(--radius-full);
  transition: background var(--transition-fast);
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(34, 197, 94, 0.45);
}

/* 可点击元素统一cursor */
.clickable,
[role="button"],
button,
.ant-btn,
.ant-card.hoverable {
  cursor: pointer;
}

/* 选中状态 */
::selection {
  background: rgba(34, 197, 94, 0.2);
  color: var(--color-text);
}

/* Ant Design 菜单覆盖 */
.ant-menu-horizontal > .ant-menu-item:hover::after,
.ant-menu-horizontal > .ant-menu-item-active::after,
.ant-menu-horizontal > .ant-menu-item-selected::after {
  border-bottom-color: var(--color-primary) !important;
}

.ant-menu-horizontal > .ant-menu-item:hover,
.ant-menu-horizontal > .ant-menu-item-active {
  color: var(--color-text) !important;
}

/* Ant Design 按钮 disabled 状态 */
.ant-btn:disabled {
  background: var(--color-border) !important;
  color: var(--color-text-muted) !important;
  border-color: var(--color-border) !important;
  box-shadow: none !important;
}

.ant-btn-primary:disabled {
  background: var(--color-border) !important;
  color: var(--color-text-muted) !important;
}

/* Steps 组件样式覆盖 */
.ant-steps .ant-steps-item-process .ant-steps-item-icon {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.ant-steps .ant-steps-item-finish .ant-steps-item-icon {
  border-color: var(--color-primary);
}

.ant-steps .ant-steps-item-finish .ant-steps-item-icon > .ant-steps-icon {
  color: var(--color-primary);
}

.ant-steps .ant-steps-item-finish > .ant-steps-item-container > .ant-steps-item-tail::after {
  background-color: var(--color-primary);
}

/* 输入框聚焦状态 */
.ant-input:focus,
.ant-input-focused,
.ant-input-affix-wrapper:focus,
.ant-input-affix-wrapper-focused {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.1) !important;
}

.ant-input:hover {
  border-color: var(--color-primary-light);
}

/* 文本域 */
.ant-input-textarea-show-count::after {
  color: var(--color-text-muted);
}

/* Progress 组件 */
.ant-progress-bg {
  background: var(--gradient-primary);
}

/* Spin 组件 */
.ant-spin-dot-item {
  background-color: var(--color-primary);
}
</style>
