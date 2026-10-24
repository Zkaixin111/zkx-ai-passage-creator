<script setup lang="ts">
import { provide } from 'vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

// 提供全局中文语言配置
provide('locale', zhCN)

// Ant Design Vue 主题令牌：全站锁定朱砂，避免逐组件内联改色
const themeConfig = {
  token: {
    colorPrimary: '#C4402F',
    colorInfo: '#3D6B5C',
    colorLink: '#C4402F',
    colorSuccess: '#3D6B5C',
    colorError: '#B42318',
    colorWarning: '#C08A3E',
    colorTextBase: '#26221C',
    colorBgBase: '#F7F1E5',
    colorBorder: '#E0D5BE',
    colorBorderSecondary: '#E0D5BE',
    borderRadius: 10,
    fontFamily:
      '-apple-system, "PingFang SC", "Microsoft YaHei", "Segoe UI", sans-serif'
  }
}
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="themeConfig">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
/* 全局CSS变量 - 神笔马良（宣纸 + 墨 + 朱砂 + 描金） */
:root {
  /* ==================== 神笔令牌 ==================== */
  --paper: #F7F1E5;
  --paper-2: #F1E8D6;
  --paper-card: #FFFDF7;
  --ink: #26221C;
  --ink-2: #6B6353;
  --ink-3: #9A9080;
  --vermilion: #C4402F;
  --vermilion-dark: #A83325;
  --gold: #C08A3E;
  --jade: #3D6B5C;
  --hairline: #E0D5BE;
  --kai: "Kaiti SC", "STKaiti", "KaiTi", "Noto Serif SC", "SimSun", serif;
  --shadow-hard: 4px 4px 0 rgba(38, 34, 28, 0.1);
  --shadow-hard-sm: 3px 3px 0 rgba(38, 34, 28, 0.1);
  --shadow-brush-btn: 0 4px 0 var(--vermilion-dark);

  /* ==================== 旧令牌名保留（值改为朱砂系，避免内部页断链） ==================== */
  /* 品牌色 - 朱砂 */
  --color-primary: #C4402F;
  --color-primary-light: #D0563F;
  --color-primary-dark: #A83325;
  --color-secondary: #26221C;
  --color-secondary-light: #6B6353;

  /* 功能色 */
  --color-cta: #C4402F;
  --color-success: #3D6B5C;
  --color-warning: #C08A3E;
  --color-error: #B42318;
  --color-info: #3D6B5C;

  /* 中性色（宣纸底，禁纯白整页、禁纯黑文字） */
  --color-background: #F7F1E5;
  --color-background-secondary: #F1E8D6;
  --color-background-tertiary: #EAE0C9;
  --color-background-dark: #26221C;
  --color-text: #26221C;
  --color-text-secondary: #6B6353;
  --color-text-muted: #9A9080;
  --color-border: #E0D5BE;
  --color-border-light: #EAE0C9;

  /* 渐变（仅保留极淡单向渐变） */
  --gradient-primary: linear-gradient(180deg, #C4402F 0%, #A83325 100%);
  --gradient-hero: linear-gradient(180deg, #F7F1E5 0%, #F1E8D6 100%);
  --gradient-hero-dark: linear-gradient(180deg, #26221C 0%, #332D24 100%);
  --gradient-text: none;
  --gradient-glow: none;

  /* 纯色背景 */
  --bg-primary: #C4402F;
  --bg-primary-hover: #A83325;
  --bg-dark: #26221C;
  --bg-dark-hover: #332D24;
  --bg-success: #3D6B5C;

  /* 阴影（带墨色调） */
  --shadow-sm: 0 1px 2px 0 rgba(38, 34, 28, 0.06);
  --shadow-md: 0 1px 3px rgba(38, 34, 28, 0.08);
  --shadow-lg: 0 2px 8px rgba(38, 34, 28, 0.08);
  --shadow-xl: 0 4px 16px rgba(38, 34, 28, 0.1);
  --shadow-card: 0 2px 10px rgba(38, 34, 28, 0.05);
  --shadow-card-hover: 0 14px 32px rgba(38, 34, 28, 0.1);
  --shadow-green: 0 1px 3px rgba(61, 107, 92, 0.2);
  --shadow-glow: 0 1px 3px rgba(38, 34, 28, 0.06);

  /* 圆角 */
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 14px;
  --radius-2xl: 16px;
  --radius-full: 9999px;

  /* 过渡（统一缓动曲线） */
  --ease-editorial: cubic-bezier(0.16, 1, 0.3, 1);
  --ease: cubic-bezier(0.16, 1, 0.3, 1);
  --transition-fast: 150ms cubic-bezier(0.16, 1, 0.3, 1);
  --transition-normal: 250ms cubic-bezier(0.16, 1, 0.3, 1);
  --transition-slow: 400ms cubic-bezier(0.16, 1, 0.3, 1);
  --transition-bounce: 300ms cubic-bezier(0.16, 1, 0.3, 1);

  /* 毛玻璃（宣纸玻璃底） */
  --glass-bg: rgba(247, 241, 229, 0.88);
  --glass-bg-green: rgba(241, 232, 214, 0.9);
  --glass-blur: blur(10px);
  --glass-border: 1px solid var(--hairline);

  /* 字体栈：显示层楷体 / 正文无衬线 / 数字与代码等宽（纯系统栈，不下载 webfont） */
  --font-display: "Kaiti SC", "STKaiti", "KaiTi", "Noto Serif SC", "SimSun", serif;
  --font-body: -apple-system, "PingFang SC", "Microsoft YaHei", "Segoe UI", sans-serif;
  --font-mono: ui-monospace, "SF Mono", Consolas, monospace;
}

/* 全局基础样式 */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: var(--font-body);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  /* clip 与 hidden 同样防横向溢出，但不创建滚动容器，避免破坏子元素 position: sticky */
  overflow-x: hidden;
  overflow-x: clip;
  background-color: var(--paper);
  color: var(--ink);
  line-height: 1.75;
  /* 极淡单向渐变，避免纯白整页与纯平大色块 */
  background-image: linear-gradient(180deg, #F7F1E5 0%, #F1E8D6 100%);
  background-attachment: fixed;
}

/* 2% 透明度纸纹噪点层，替代光晕装饰 */
body::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.02'/%3E%3C/svg%3E");
}

#app {
  position: relative;
  z-index: 1;
}

html {
  /* 同 body：clip 不创建滚动容器，保证 sticky 生效 */
  overflow-x: hidden;
  overflow-x: clip;
}

/* 显示层标题：楷体，左对齐、字距 0.02em、字重 700 */
h1, h2, h3, h4, h5, h6 {
  font-family: var(--kai);
  font-weight: 700;
  letter-spacing: 0.02em;
  text-align: left;
}

/* 数字与代码统一等宽 */
code, kbd, pre, samp {
  font-family: var(--font-mono);
}

/* 全局链接样式 */
a {
  color: var(--color-text);
  text-decoration: none;
  transition: color var(--transition-fast);
}

a:hover {
  color: var(--vermilion);
}

/* ==================== 印章通用类（朱砂方块 + 内描边，可旋转） ==================== */
.shenbi-seal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: var(--vermilion);
  color: #FBF6EC;
  font-family: var(--kai);
  font-weight: 700;
  border-radius: 7px;
  box-shadow: inset 0 0 0 2px rgba(251, 246, 236, 0.35);
}

/* 竖排印章（writing-mode 实现竖排「协笔」等二字） */
.shenbi-seal.vertical {
  writing-mode: vertical-rl;
  letter-spacing: 2px;
  line-height: 1.15;
}

/* ==================== 全局按钮增强 ==================== */
.ant-btn {
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), background var(--transition-fast);
  font-weight: 500;
  border-radius: 10px;
  box-shadow: none;
}

.ant-btn-primary {
  background: var(--bg-primary);
  border-color: var(--bg-primary);
  box-shadow: var(--shadow-brush-btn);
}

.ant-btn-primary:not(:disabled):hover {
  background: var(--bg-primary-hover);
  border-color: var(--bg-primary-hover);
  box-shadow: 0 6px 0 var(--vermilion-dark);
  transform: translateY(-2px);
}

/* 按钮按压微反馈（与硬阴影配套的"按下去"手感） */
.ant-btn:not(:disabled):active {
  transform: translateY(2px);
  box-shadow: 0 2px 0 var(--vermilion-dark);
}

/* 全局卡片增强：宣纸卡 + 发丝边框 */
.ant-card {
  border-radius: 14px;
  border: 1px solid var(--hairline);
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
}

.ant-card.hoverable:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
}

/* 玻璃卡片通用类（宣纸面板 + 发丝边） */
.glass-card {
  background: var(--paper-card);
  border: 1px solid var(--hairline);
  border-radius: 14px;
  box-shadow: var(--shadow-card);
}

/* 描边卡片（类名保留以兼容内部页） */
.gradient-border-card {
  position: relative;
  background: var(--paper-card);
  border: 1px solid var(--hairline);
  border-radius: 14px;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
}

.gradient-border-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
}

/* 背景氛围层（类名保留以兼容内部页，改为宣纸渐变） */
.aurora-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
  background: var(--gradient-hero);
  opacity: 0.6;
}

/* 主按钮（类名保留以兼容内部页） */
.glow-btn {
  position: relative;
  background: var(--bg-primary) !important;
  border: none !important;
  color: #FBF6EC !important;
  box-shadow: var(--shadow-brush-btn) !important;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast) !important;
}

.glow-btn:hover {
  background: var(--bg-primary-hover) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 0 var(--vermilion-dark) !important;
}

/* 可点击卡片悬浮动效 */
.hoverable-card {
  transition: transform var(--transition-normal), box-shadow var(--transition-normal), border-color var(--transition-normal);
}

.hoverable-card:hover {
  transform: translateY(-2px);
  border-color: var(--gold);
  box-shadow: var(--shadow-card-hover);
}

/* 强调文字（实色朱砂强调，类名保留以兼容内部页） */
.gradient-text {
  background: none;
  -webkit-background-clip: initial;
  background-clip: initial;
  -webkit-text-fill-color: currentColor;
  color: var(--vermilion);
}

/* 自定义滚动条（墨色系） */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(38, 34, 28, 0.22);
  border-radius: var(--radius-full);
  transition: background var(--transition-fast);
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(38, 34, 28, 0.4);
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
  background: rgba(196, 64, 47, 0.18);
  color: var(--ink);
}

/* Ant Design 菜单覆盖 */
.ant-menu-horizontal > .ant-menu-item:hover::after,
.ant-menu-horizontal > .ant-menu-item-active::after,
.ant-menu-horizontal > .ant-menu-item-selected::after {
  border-bottom-color: var(--vermilion) !important;
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
  transform: none !important;
}

.ant-btn-primary:disabled {
  background: var(--color-border) !important;
  color: var(--color-text-muted) !important;
}

/* Steps 组件样式覆盖 */
.ant-steps .ant-steps-item-process .ant-steps-item-icon {
  background: var(--vermilion);
  border-color: var(--vermilion);
}

.ant-steps .ant-steps-item-finish .ant-steps-item-icon {
  border-color: var(--jade);
}

.ant-steps .ant-steps-item-finish .ant-steps-item-icon > .ant-steps-icon {
  color: var(--jade);
}

.ant-steps .ant-steps-item-finish > .ant-steps-item-container > .ant-steps-item-tail::after {
  background-color: var(--jade);
}

/* 输入框聚焦状态（朱砂 focus ring，无发光） */
.ant-input:focus,
.ant-input-focused,
.ant-input-affix-wrapper:focus,
.ant-input-affix-wrapper-focused {
  border-color: var(--vermilion) !important;
  box-shadow: 3px 3px 0 rgba(196, 64, 47, 0.2) !important;
}

.ant-input:hover {
  border-color: var(--gold);
}

/* 文本域 */
.ant-input-textarea-show-count::after {
  color: var(--ink-3);
}

/* Progress 组件 */
.ant-progress-bg {
  background: var(--vermilion);
}

/* Spin 组件 */
.ant-spin-dot-item {
  background-color: var(--vermilion);
}

/* 自动播放动画统一包裹（尊重系统减弱动效偏好） */
@media (prefers-reduced-motion: no-preference) {
  .anim-fade-in {
    animation: shenbi-fade-in 0.5s var(--ease) both;
  }

  @keyframes shenbi-fade-in {
    from {
      opacity: 0;
      transform: translateY(8px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
}
</style>
