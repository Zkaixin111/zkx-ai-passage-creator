<template>
  <div class="content-confirm-stage">
    <div class="stage-header">
      <div class="stage-kicker">第 三 笔 · 落 笔</div>
      <h2 class="stage-title">落笔 · 正文已就</h2>
      <p class="stage-subtitle">选择是否需要为文章生成配图，画龙最后点睛。</p>
    </div>

    <div class="confirm-card">
      <!-- 是否生成配图 -->
      <a-radio-group v-model:value="generateImages" class="generate-radio">
        <a-radio :value="true">
          <span class="radio-label">生成配图</span>
          <span class="radio-desc">为文章各章节生成插图，图文并茂</span>
        </a-radio>
        <a-radio :value="false">
          <span class="radio-label">不生成配图</span>
          <span class="radio-desc">仅保留纯文字正文，直接完成创作</span>
        </a-radio>
      </a-radio-group>

      <!-- 配图方式选择（生成配图时显示） -->
      <div v-if="generateImages" class="method-section">
        <div class="method-label">
          <PictureOutlined />
          <span>选择配图类型</span>
          <span class="method-hint">可多选，按顺序轮流使用</span>
        </div>
        <a-checkbox-group v-model:value="selectedMethods" class="methods-group">
          <a-checkbox value="PEXELS">Pexels 图库</a-checkbox>
          <a-tooltip :title="isVip ? '' : '仅限 VIP 会员'">
            <a-checkbox value="NANO_BANANA" :disabled="!isVip">
              DashScope 通义万相文生图
              <CrownOutlined v-if="!isVip" class="vip-icon" />
            </a-checkbox>
          </a-tooltip>
          <a-checkbox value="MERMAID">Mermaid 图表</a-checkbox>
          <a-checkbox value="ICONIFY">Iconify 图标</a-checkbox>
          <a-checkbox value="EMOJI_PACK">表情包</a-checkbox>
          <a-tooltip :title="isVip ? '' : '仅限 VIP 会员'">
            <a-checkbox value="SVG_DIAGRAM" :disabled="!isVip">
              SVG 图表
              <CrownOutlined v-if="!isVip" class="vip-icon" />
            </a-checkbox>
          </a-tooltip>
        </a-checkbox-group>
        <p v-if="!isVip" class="vip-notice">
          <CrownOutlined />
          <span>AI 生图和 SVG 图表为 VIP 专属功能</span>
        </p>
      </div>

      <div class="actions">
        <a-button
          type="primary"
          size="large"
          :loading="submitting"
          @click="handleConfirm"
          class="confirm-btn"
        >
          <template #icon>
            <CheckOutlined />
          </template>
          {{ generateImages ? '确认，开始点睛 →' : '完成文章 →' }}
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { CheckOutlined, PictureOutlined, CrownOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { isVip as checkIsVip } from '@/utils/permission'

const props = defineProps<{
  taskId: string
}>()

const emit = defineEmits<{
  (e: 'confirm', data: { generateImages: boolean; enabledImageMethods: string[] }): void
}>()

const loginUserStore = useLoginUserStore()
const isVip = computed(() => checkIsVip(loginUserStore.loginUser))

const generateImages = ref<boolean>(true)
const selectedMethods = ref<string[]>(['PEXELS'])
const submitting = ref(false)

const handleConfirm = async () => {
  if (generateImages.value && selectedMethods.value.length === 0) {
    message.warning('请至少选择一种配图类型')
    return
  }
  submitting.value = true
  try {
    emit('confirm', {
      generateImages: generateImages.value,
      enabledImageMethods: generateImages.value ? selectedMethods.value : [],
    })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.content-confirm-stage {
  padding: 6px 0 10px;

  .stage-header {
    margin-bottom: 20px;
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
    font-size: 24px;
    font-weight: 700;
    color: var(--ink);
    margin: 0 0 6px;
  }

  .stage-subtitle {
    font-size: 14px;
    color: var(--ink-2);
    margin: 0;
  }

  /* 配图确认卡：宣纸底 + 发丝边 */
  .confirm-card {
    background: var(--paper);
    border: 1px solid var(--hairline);
    border-radius: 12px;
    padding: 24px;
  }

  .generate-radio {
    display: flex;
    flex-direction: column;
    gap: 14px;
    width: 100%;

    :deep(.ant-radio-wrapper) {
      display: flex;
      align-items: flex-start;
      margin: 0;
      padding: 14px 16px;
      background: var(--paper-card);
      border: 1px solid var(--hairline);
      border-radius: 10px;
      transition: all var(--transition-fast);

      &:hover {
        border-color: var(--gold);
      }

      &.ant-radio-wrapper-checked {
        border-color: var(--vermilion);
        background: rgba(196, 64, 47, 0.04);
      }
    }

    .radio-label {
      font-family: var(--kai);
      font-size: 15px;
      font-weight: 700;
      color: var(--ink);
    }

    .radio-desc {
      display: block;
      font-size: 13px;
      color: var(--ink-3);
      margin-top: 2px;
      font-weight: 400;
    }
  }

  .method-section {
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px dashed var(--hairline);

    .method-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-family: var(--kai);
      font-size: 15px;
      font-weight: 700;
      color: var(--ink);
      margin-bottom: 12px;

      .anticon {
        color: var(--gold);
      }

      .method-hint {
        font-size: 12px;
        font-weight: 400;
        color: var(--ink-3);
        font-family: var(--font-body);
      }
    }

    .methods-group {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 16px;
    }

    .vip-icon {
      color: var(--gold);
      margin-left: 4px;
    }

    .vip-notice {
      display: flex;
      align-items: center;
      gap: 6px;
      margin: 12px 0 0;
      font-size: 12px;
      color: var(--ink-3);

      .anticon {
        color: var(--gold);
      }
    }
  }

  .actions {
    margin-top: 24px;
    text-align: right;

    .confirm-btn {
      min-width: 180px;
      height: 46px;
      font-family: var(--kai);
      font-size: 15px;
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
    }
  }
}
</style>
