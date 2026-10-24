<template>
  <div class="outline-editing-stage">
    <div class="stage-header">
      <div class="stage-kicker">第 二 笔 · 起 稿</div>
      <h2 class="stage-title">起稿 · 大纲逐级可改</h2>
      <p class="stage-subtitle">每一行都可以直接修改、排序或增删，不满意整段可退回重跑。</p>
    </div>

    <div class="outline-list" ref="outlineListRef">
      <div
        v-for="(section, index) in outlineSections"
        :key="section.section"
        class="outline-section"
        :data-section-id="section.section"
      >
        <div class="section-header">
          <span class="drag-handle" title="拖动排序">⋮⋮</span>
          <span class="section-number">{{ index + 1 }}</span>
          <a-input
            v-model:value="section.title"
            placeholder="章节标题"
            class="section-title-input"
          />
          <a-button
            type="text"
            danger
            @click="deleteSection(index)"
            class="delete-btn"
          >
            <template #icon>
              <DeleteOutlined />
            </template>
          </a-button>
        </div>

        <div class="section-points">
          <div v-for="(point, pointIdx) in section.points" :key="pointIdx" class="point-item">
            <span class="point-bullet">•</span>
            <a-input
              v-model:value="section.points[pointIdx]"
              placeholder="要点内容"
              class="point-input"
            />
            <a-button
              type="text"
              size="small"
              @click="deletePoint(index, pointIdx)"
              class="delete-point-btn"
            >
              ×
            </a-button>
          </div>
          <a-button
            type="dashed"
            @click="addPoint(index)"
            class="add-point-btn"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            添加要点
          </a-button>
        </div>
      </div>
    </div>

    <div class="ai-chat-section" :class="{ 'vip-only': !isVip }">
      <div class="chat-header">
        <RobotOutlined />
        <span>AI 助手修改大纲</span>
        <span v-if="!isVip" class="vip-badge-small">
          <CrownOutlined />
          VIP
        </span>
      </div>
      <div v-if="isVip" class="chat-input-wrapper">
        <a-textarea
          v-model:value="modifySuggestion"
          placeholder="告诉 AI 如何修改大纲，例如：请在第二章节后增加一个关于实践案例的章节"
          :rows="3"
          :maxlength="500"
          show-count
          class="chat-textarea"
        />
        <a-button
          type="primary"
          :loading="aiModifying"
          :disabled="!modifySuggestion.trim()"
          @click="handleAiModify"
          class="ai-modify-btn"
        >
          <template #icon>
            <RobotOutlined />
          </template>
          AI 修改大纲
        </a-button>
      </div>
      <div v-else class="vip-upgrade-notice">
        <CrownOutlined class="vip-icon" />
        <p>AI 修改大纲功能仅限 VIP 会员使用</p>
        <RouterLink to="/vip" class="upgrade-btn">
          立即升级 VIP
        </RouterLink>
      </div>
    </div>

    <div class="actions">
      <a-button
        size="large"
        @click="addSection"
        class="add-section-btn"
      >
        <template #icon>
          <PlusOutlined />
        </template>
        添加章节
      </a-button>

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
        确认，进入落笔 →
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { CheckOutlined, DeleteOutlined, PlusOutlined, RobotOutlined, CrownOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import Sortable from 'sortablejs'
import { aiModifyOutline } from '@/api/articleController'
import { useLoginUserStore } from '@/stores/loginUser'
import { isVip as checkIsVip } from '@/utils/permission'

interface OutlineSection {
  section: number
  title: string
  points: string[]
}

interface Props {
  outline: API.OutlineSection[]
  taskId: string
  loading?: boolean
}

interface Emits {
  (e: 'confirm', outline: OutlineSection[]): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<Emits>()

const loginUserStore = useLoginUserStore()

// 判断是否为 VIP（管理员也视为 VIP）
const isVip = computed(() => checkIsVip(loginUserStore.loginUser))

// 转换 API 类型为内部类型
const outlineSections = ref<OutlineSection[]>(
  props.outline.map((item, index) => ({
    section: item.section ?? index + 1,
    title: item.title ?? '',
    points: item.points ?? []
  }))
)
const outlineListRef = ref<HTMLElement | null>(null)
const modifySuggestion = ref('')
const aiModifying = ref(false)

const canConfirm = computed(() => {
  return outlineSections.value.length > 0 &&
         outlineSections.value.every(section =>
           section.title.trim() &&
           section.points.length > 0 &&
           section.points.every(point => point.trim())
         )
})

onMounted(() => {
  nextTick(() => {
    if (outlineListRef.value) {
      Sortable.create(outlineListRef.value, {
        animation: 150,
        handle: '.drag-handle',
        onEnd: (evt) => {
          const { oldIndex, newIndex } = evt
          if (oldIndex !== undefined && newIndex !== undefined) {
            const item = outlineSections.value.splice(oldIndex, 1)[0]
            outlineSections.value.splice(newIndex, 0, item)
            // 更新 section 序号
            outlineSections.value.forEach((sec, idx) => {
              sec.section = idx + 1
            })
          }
        }
      })
    }
  })
})

const addSection = () => {
  const newSection: OutlineSection = {
    section: outlineSections.value.length + 1,
    title: '',
    points: ['']
  }
  outlineSections.value.push(newSection)
}

const deleteSection = (index: number) => {
  outlineSections.value.splice(index, 1)
  // 更新 section 序号
  outlineSections.value.forEach((sec, idx) => {
    sec.section = idx + 1
  })
}

const addPoint = (sectionIndex: number) => {
  outlineSections.value[sectionIndex].points.push('')
}

const deletePoint = (sectionIndex: number, pointIndex: number) => {
  const section = outlineSections.value[sectionIndex]
  if (section.points.length > 1) {
    section.points.splice(pointIndex, 1)
  }
}

const handleConfirm = () => {
  emit('confirm', outlineSections.value)
}

const handleAiModify = async () => {
  if (!modifySuggestion.value.trim()) {
    message.warning('请输入修改建议')
    return
  }

  aiModifying.value = true
  try {
    const res = await aiModifyOutline({
      taskId: props.taskId,
      modifySuggestion: modifySuggestion.value
    })
    if (res.data.code !== 0) {
      message.error(res.data.message || 'AI 修改失败')
      return
    }
    if (res.data.data) {
      outlineSections.value = res.data.data.map((item, index) => ({
        section: item.section ?? index + 1,
        title: item.title ?? '',
        points: item.points ?? []
      }))
      modifySuggestion.value = ''
      message.success('AI 已根据您的建议修改大纲')
    }
  } catch (error) {
    const err = error as Error
    message.error(err.message || 'AI 修改失败')
  } finally {
    aiModifying.value = false
  }
}
</script>

<style scoped lang="scss">
.outline-editing-stage {
  max-width: 1000px;
  margin: 0 auto;
  padding: 6px 0 10px;
}

.stage-header {
  margin-bottom: 28px;
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

.outline-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 28px;
}

/* 章节卡：宣纸底 + 发丝边，hover 描金 */
.outline-section {
  background: var(--paper);
  border: 1.5px solid var(--hairline);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s var(--ease);

  &:hover {
    border-color: var(--gold);
    box-shadow: 0 4px 12px rgba(38, 34, 28, 0.06);
  }
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.drag-handle {
  cursor: grab;
  font-size: 20px;
  color: var(--ink-3);
  user-select: none;
  line-height: 1;

  &:active {
    cursor: grabbing;
  }

  &:hover {
    color: var(--vermilion);
  }
}

/* 章节序号：朱砂印章式圆标 */
.section-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--vermilion);
  color: #FBF6EC;
  font-family: var(--kai);
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
  transform: rotate(4deg);
  box-shadow: inset 0 0 0 2px rgba(251, 246, 236, 0.3);
}

.section-title-input {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
}

.delete-btn {
  flex-shrink: 0;
}

.section-points {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-left: 44px;
}

.point-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.point-bullet {
  color: var(--gold);
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
}

.point-input {
  flex: 1;
  font-size: 14px;
}

.delete-point-btn {
  font-size: 18px;
  color: var(--ink-3);
  padding: 0;
  width: 24px;
  height: 24px;
  flex-shrink: 0;

  &:hover {
    color: var(--vermilion);
  }
}

.add-point-btn {
  align-self: flex-start;
}

/* AI 助手区：虚线描金框 */
.ai-chat-section {
  background: var(--paper);
  border: 1.5px dashed var(--gold);
  border-radius: 12px;
  padding: 22px;
  margin-bottom: 28px;

  &.vip-only {
    border-color: var(--hairline);
  }
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--kai);
  font-size: 16px;
  font-weight: 700;
  color: var(--gold);
  margin-bottom: 16px;

  .anticon {
    font-size: 18px;
  }

  .vip-badge-small {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 3px 10px;
    border-radius: var(--radius-full);
    font-size: 11px;
    font-weight: 600;
    background: var(--vermilion);
    color: #FBF6EC;
    margin-left: auto;
    font-family: var(--font-body);
  }
}

.vip-upgrade-notice {
  text-align: center;
  padding: 16px;

  .vip-icon {
    font-size: 44px;
    color: var(--gold);
    margin-bottom: 12px;
  }

  p {
    margin: 0 0 16px;
    color: var(--ink-2);
    font-size: 14px;
  }

  .upgrade-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 10px 24px;
    border-radius: 999px;
    font-size: 14px;
    font-weight: 700;
    background: var(--vermilion);
    color: #FBF6EC;
    text-decoration: none;
    transition: all 0.3s var(--ease);
    box-shadow: 0 4px 0 var(--vermilion-dark);

    &:hover {
      background: var(--vermilion-dark);
      transform: translateY(-2px);
      box-shadow: 0 6px 0 var(--vermilion-dark);
      color: #FBF6EC;
    }
  }
}

.chat-input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.chat-textarea {
  flex: 1;
  font-size: 14px;
  line-height: 1.6;
}

.ai-modify-btn {
  height: 40px;
  padding: 0 20px;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
}

.add-section-btn,
.confirm-btn {
  height: 48px;
  padding: 0 32px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 999px;
}

/* 确认按钮：朱砂 + 按压感 */
.confirm-btn {
  font-family: var(--kai);
  letter-spacing: 0.1em;
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
