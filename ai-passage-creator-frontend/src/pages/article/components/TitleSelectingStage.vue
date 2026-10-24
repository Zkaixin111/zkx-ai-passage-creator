<template>
  <div class="title-selecting-stage">
    <div class="stage-header">
      <div class="stage-kicker">第 一 笔 · 研 墨</div>
      <h2 class="stage-title">研墨 · 挑一个标题</h2>
      <p class="stage-subtitle">AI 为您生成了以下标题，选择一个或自己写，点头才往下走。</p>
    </div>

    <a-radio-group v-model:value="selectedIndex" class="title-options">
      <div v-for="(option, index) in titleOptions" :key="index" class="title-option">
        <a-radio :value="index">
          <div class="title-content">
            <div class="title-main">{{ option.mainTitle }}</div>
            <div class="title-sub">{{ option.subTitle }}</div>
          </div>
        </a-radio>
      </div>
      <div class="title-option custom">
        <a-radio :value="-1">
          <div class="title-content">
            <div class="title-main">自定义标题</div>
          </div>
        </a-radio>
        <div v-if="selectedIndex === -1" class="custom-inputs">
          <a-input
            v-model:value="customMainTitle"
            placeholder="输入主标题"
            class="custom-input"
          />
          <a-input
            v-model:value="customSubTitle"
            placeholder="输入副标题"
            class="custom-input"
          />
        </div>
      </div>
    </a-radio-group>

    <div class="description-section">
      <label class="section-label">补充描述（可选）</label>
      <p class="section-tip">补充您对文章的期望、重点强调的内容等</p>
      <a-textarea
        v-model:value="userDescription"
        placeholder="例如：请重点强调技术原理，用通俗的语言讲解..."
        :rows="4"
        :maxlength="500"
        show-count
        class="description-textarea"
      />
    </div>

    <div class="actions">
      <a-button
        type="primary"
        size="large"
        :loading="loading"
        :disabled="!canConfirm"
        @click="handleConfirm"
        class="confirm-btn"
      >
        <template #icon>
          <CheckOutlined />
        </template>
        确认，进入起稿 →
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { CheckOutlined } from '@ant-design/icons-vue'

interface TitleOption {
  mainTitle: string
  subTitle: string
}

interface Props {
  titleOptions: TitleOption[]
  loading?: boolean
}

interface Emits {
  (e: 'confirm', data: {
    mainTitle: string
    subTitle: string
    userDescription: string
  }): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<Emits>()

const selectedIndex = ref<number>(0)
const customMainTitle = ref('')
const customSubTitle = ref('')
const userDescription = ref('')

const canConfirm = computed(() => {
  if (selectedIndex.value === -1) {
    return customMainTitle.value.trim() && customSubTitle.value.trim()
  }
  return selectedIndex.value >= 0 && selectedIndex.value < props.titleOptions.length
})

const handleConfirm = () => {
  let mainTitle = ''
  let subTitle = ''

  if (selectedIndex.value === -1) {
    mainTitle = customMainTitle.value
    subTitle = customSubTitle.value
  } else {
    const selected = props.titleOptions[selectedIndex.value]
    mainTitle = selected.mainTitle
    subTitle = selected.subTitle
  }

  emit('confirm', {
    mainTitle,
    subTitle,
    userDescription: userDescription.value
  })
}
</script>

<style scoped lang="scss">
.title-selecting-stage {
  max-width: 900px;
  margin: 0 auto;
  padding: 6px 0 10px;
}

.stage-header {
  margin-bottom: 32px;
}

.stage-kicker {
  font-family: var(--kai);
  color: var(--gold);
  font-size: 14px;
  letter-spacing: 0.24em;
  margin-bottom: 6px;
}

.stage-title {
  font-family: var(--kai);
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--ink);
}

.stage-subtitle {
  font-size: 14px;
  color: var(--ink-2);
  margin: 0;
}

.title-options {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 28px;
  width: 100%;
}

/* 标题选项：宣纸底 + 发丝边，hover 描金，选中朱砂 */
.title-option {
  padding: 18px 20px;
  background: var(--paper);
  border: 1.5px solid var(--hairline);
  border-radius: 12px;
  transition: all 0.3s var(--ease);

  &:hover {
    border-color: var(--gold);
  }

  :deep(.ant-radio-wrapper) {
    width: 100%;
    align-items: flex-start;
  }

  :deep(.ant-radio) {
    margin-top: 4px;
  }

  :deep(.ant-radio-wrapper-checked) {
    color: var(--vermilion);
  }
}

.title-content {
  flex: 1;
  margin-left: 12px;
}

.title-main {
  font-family: var(--kai);
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 6px;
  line-height: 1.4;
}

.title-sub {
  font-size: 13.5px;
  color: var(--ink-2);
  line-height: 1.6;
}

.custom-inputs {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-left: 32px;
}

.custom-input {
  font-size: 15px;
  padding: 10px 14px;
}

.description-section {
  margin-bottom: 28px;
}

.section-label {
  display: block;
  font-family: var(--kai);
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 6px;
}

.section-tip {
  font-size: 13px;
  color: var(--ink-3);
  margin: 0 0 12px;
}

.description-textarea {
  font-size: 15px;
  line-height: 1.6;
  border-radius: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

/* 确认按钮：朱砂 + 按压感 */
.confirm-btn {
  height: 48px;
  padding: 0 38px;
  font-family: var(--kai);
  font-size: 15.5px;
  font-weight: 700;
  letter-spacing: 0.1em;
  border-radius: 999px !important;
  background: var(--vermilion) !important;
  border: none !important;
  color: #FBF6EC !important;
  box-shadow: 0 4px 0 var(--vermilion-dark) !important;
  transition: all 0.3s var(--ease) !important;

  &:hover:not(:disabled) {
    background: var(--vermilion-dark) !important;
    transform: translateY(-2px);
    box-shadow: 0 6px 0 var(--vermilion-dark) !important;
  }

  &:disabled {
    background: var(--color-border) !important;
    box-shadow: none !important;
    color: var(--ink-3) !important;
  }
}
</style>
