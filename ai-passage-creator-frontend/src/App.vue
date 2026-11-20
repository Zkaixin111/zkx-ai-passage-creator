<script setup lang="ts">
import { provide } from 'vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

provide('locale', zhCN)

const theme = {
  token: {
    colorPrimary: '#c2510a',
    colorSuccess: '#2f6b4f',
    colorWarning: '#b7791f',
    colorError: '#b42318',
    colorInfo: '#1d4e89',
    colorText: '#1c1915',
    colorTextSecondary: '#5c564c',
    colorBorder: '#e4dcd0',
    colorBgLayout: '#f7f4ee',
    colorBgContainer: '#fffcf7',
    borderRadius: 10,
    fontFamily:
      "'Noto Sans SC', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif",
    wireframe: false,
  },
  components: {
    Button: {
      primaryShadow: 'none',
      defaultShadow: 'none',
    },
    Layout: {
      headerBg: 'transparent',
      bodyBg: 'transparent',
      footerBg: 'transparent',
    },
  },
}
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="theme">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
* {
  box-sizing: border-box;
}

html {
  overflow-x: hidden;
}

body {
  margin: 0;
  padding: 0;
  font-family: var(--font-sans);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
  background-color: var(--color-background);
  color: var(--color-text);
  line-height: 1.65;
}

h1,
h2,
h3,
h4,
h5,
h6 {
  font-family: var(--font-serif);
  font-weight: 600;
  letter-spacing: 0;
}

a {
  color: var(--color-text);
  text-decoration: none;
  transition: color var(--transition-fast);
}

a:hover {
  color: var(--color-primary);
}

.ant-btn {
  font-weight: 600;
  box-shadow: none;
}

.ant-btn-primary {
  background: var(--bg-primary);
  border-color: var(--bg-primary);
  box-shadow: none;
}

.ant-btn-primary:not(:disabled):hover {
  background: var(--bg-primary-hover);
  border-color: var(--bg-primary-hover);
}

.ant-message {
  top: 76px !important;
}

.ant-btn-primary:disabled,
.ant-btn-primary.ant-btn-disabled {
  background: var(--color-background-tertiary) !important;
  color: var(--color-text-muted) !important;
  border-color: transparent !important;
  box-shadow: none !important;
}

.ant-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
}

.page-header {
  border-bottom: 1px solid var(--color-border);
}

.ant-input:focus,
.ant-input-focused,
.ant-input-affix-wrapper:focus,
.ant-input-affix-wrapper-focused,
.ant-input-affix-wrapper:focus-within {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary) 16%, transparent) !important;
}

.ant-input:hover,
.ant-input-affix-wrapper:hover {
  border-color: var(--color-primary-light);
}

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

.ant-progress-bg {
  background: var(--color-primary) !important;
}

.ant-spin-dot-item {
  background-color: var(--color-primary);
}

.ant-menu-horizontal > .ant-menu-item:hover::after,
.ant-menu-horizontal > .ant-menu-item-active::after,
.ant-menu-horizontal > .ant-menu-item-selected::after {
  border-bottom-color: var(--color-text) !important;
}

::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(36, 28, 22, 0.22);
  border-radius: var(--radius-full);
  border: 2px solid transparent;
  background-clip: padding-box;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(36, 28, 22, 0.38);
  border: 2px solid transparent;
  background-clip: padding-box;
}

::selection {
  background: color-mix(in srgb, var(--color-primary) 24%, transparent);
  color: var(--color-text);
}
</style>
