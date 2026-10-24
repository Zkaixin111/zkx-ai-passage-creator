<template>
  <div class="article-create-page">
    <!-- 四笔进度条：真实阶段映射进四笔叙事（映射见 constants/fourBrushes.ts） -->
    <div class="stepper">
      <div
        v-for="(brush, index) in FOUR_BRUSHES"
        :key="brush.en"
        :class="['step-node', brushStates[index]]"
      >
        <div class="ball">{{ brushStates[index] === 'done' ? '✓' : brush.char }}</div>
        <div class="step-name">{{ brush.name }}</div>
        <div class="step-en">{{ brush.en }}</div>
      </div>
    </div>

    <!-- 主区：左舞台 + 右日志 -->
    <div class="create-layout">
      <!-- 左：阶段舞台 -->
      <main ref="mainContentRef" class="stage">
        <!-- 任务全部完成时的「成」印章覆盖层（纯展示，不影响数据） -->
        <div v-if="currentPhase === 'COMPLETED'" class="done-stamp">
          <div class="seal-big">成</div>
          <small>成 稿 已 就 · 可 去 署 名</small>
        </div>

        <!-- 阶段切换（带过渡动画） -->
        <Transition name="fade-slide" mode="out-in">
          <!-- 输入状态：第一笔 · 研墨 -->
          <div v-if="currentPhase === 'INPUT'" key="input" class="input-state">
          <div class="stage-kicker">第 一 笔 · 研 墨</div>
          <h2 class="stage-title">研墨 · 你要写什么？</h2>
          <p class="stage-hint">一句话说清选题即可。神笔会先给出几组标题，你点头才往下走。</p>

            <div class="input-area">
              <a-textarea
                v-model:value="topic"
                placeholder="请输入您想创作的文章选题，例如：2026年AI如何改变职场"
                :rows="5"
                :maxlength="500"
                show-count
                class="topic-textarea"
              />

              <!-- 文章风格选择 -->
              <div class="style-section">
                <div class="section-header">
                  <span class="section-title">文章风格</span>
                  <span class="section-tip">（不选择使用默认风格）</span>
                </div>
                <a-radio-group v-model:value="selectedStyle" class="style-group" @change="onStylePresetChange">
                  <a-radio value="">默认</a-radio>
                  <a-radio value="科技风格">科技风格</a-radio>
                  <a-radio value="情感风格">情感风格</a-radio>
                  <a-radio value="教育风格">教育风格</a-radio>
                  <a-radio value="轻松幽默">轻松幽默</a-radio>
                  <a-radio value="__custom">自定义</a-radio>
                </a-radio-group>
                <div v-if="selectedStyle === '__custom'" class="custom-word-count">
                  <a-input
                    v-model:value="customStyle"
                    placeholder="输入自定义风格，如：武侠、散文、科普..."
                    :maxlength="20"
                    class="word-count-input"
                  />
                </div>
              </div>

              <!-- 目标字数选择 -->
              <div class="style-section">
                <div class="section-header">
                  <span class="section-title">目标字数</span>
                  <span class="section-tip">（AI 生成字数可能上下浮动 10%）</span>
                </div>
                <a-radio-group v-model:value="selectedWordCount" class="style-group" @change="onWordCountPresetChange">
                  <a-radio :value="800">短篇 800</a-radio>
                  <a-radio :value="1200">中篇 1200</a-radio>
                  <a-radio :value="2000">长篇 2000</a-radio>
                  <a-radio :value="0">自定义</a-radio>
                </a-radio-group>
                <div v-if="selectedWordCount === 0" class="custom-word-count">
                  <a-input-number
                    v-model:value="customWordCount"
                    :min="300"
                    :max="5000"
                    :step="100"
                    placeholder="输入目标字数"
                    class="word-count-input"
                  />
                  <span class="word-count-unit">字</span>
                </div>
              </div>

              <a-button
                type="primary"
                size="large"
                :loading="isCreating"
                :disabled="!topic.trim() || !hasQuota"
                @click="startCreate"
                class="create-btn"
              >
                <template #icon>
                  <RocketOutlined />
                </template>
                落笔开始 →
              </a-button>
              <div v-if="!hasQuota" class="quota-warning">
                <WarningOutlined />
                <span>配额已用完，无法创建文章</span>
              </div>
            </div>
          </div>

          <!-- 标题生成中 -->
          <div v-else-if="currentPhase === 'TITLE_GENERATING'" key="title-generating" class="loading-stage">
            <a-spin size="large" />
            <h3>研墨正浓 · AI 正在生成标题方案...</h3>
            <p>稍等片刻，即将为您呈现多个精彩标题</p>
          </div>

          <!-- 标题选择阶段 -->
          <TitleSelectingStage
            v-else-if="currentPhase === 'TITLE_SELECTING'"
            key="title-selecting"
            :title-options="titleOptions"
            :loading="confirmLoading"
            @confirm="handleConfirmTitle"
          />

          <!-- 大纲生成中（流式展示） -->
          <div v-else-if="currentPhase === 'OUTLINE_GENERATING'" key="outline-generating" class="outline-generating-state">
            <div class="stage-kicker">第 二 笔 · 起 稿</div>
            <h2 class="stage-title">起稿 · 大纲流式展开</h2>

            <!-- 标题预览 -->
            <div v-if="article.mainTitle" class="preview-header">
              <h1 class="article-title">{{ article.mainTitle }}</h1>
              <p class="article-subtitle">{{ article.subTitle }}</p>
            </div>

            <!-- 大纲流式展示 -->
            <div class="outline-preview">
              <div class="section-label">
                <BulbOutlined />
                <span>AI 正在规划文章大纲</span>
                <span class="typing-cursor">|</span>
              </div>
              <div v-if="parsedOutline.length > 0" class="outline-list">
                <div
                  v-for="item in parsedOutline"
                  :key="item.section"
                  class="outline-item fade-in"
                >
                  <div class="outline-title">{{ item.section }}. {{ item.title }}</div>
                  <ul class="outline-points">
                    <li v-for="(point, idx) in item.points" :key="idx">{{ point }}</li>
                  </ul>
                </div>
              </div>
              <div v-else class="outline-loading">
                <a-spin />
                <span>正在构建文章结构...</span>
              </div>
            </div>
          </div>

          <!-- 大纲编辑阶段 -->
          <OutlineEditingStage
            v-else-if="currentPhase === 'OUTLINE_EDITING'"
            key="outline-editing"
            :outline="outline"
            :loading="confirmLoading"
            :task-id="taskId"
            @confirm="handleConfirmOutline"
          />

          <!-- 正文完成，配图确认阶段 -->
          <div v-else-if="currentPhase === 'CONTENT_CONFIRMING'" key="content-confirming" class="creating-state">
            <div class="stage-kicker">第 三 笔 · 落 笔</div>
            <h2 class="stage-title">落笔 · 正文已成，等你看一眼</h2>
            <div v-if="article.mainTitle" class="preview-header">
              <h1 class="article-title">{{ article.mainTitle }}</h1>
              <p class="article-subtitle">{{ article.subTitle }}</p>
            </div>
            <div v-if="article.content" class="content-preview">
              <div v-html="markdownToHtml(article.content)" class="markdown-body"></div>
            </div>
            <ContentConfirmStage :task-id="taskId" @confirm="handleConfirmContent" />
          </div>

          <!-- 正文生成阶段 -->
          <div v-else-if="currentPhase === 'CONTENT_GENERATING'" key="content-generating" class="creating-state">
          <div class="stage-kicker">第 三 笔 · 落 笔</div>
          <h2 class="stage-title">落笔 · 逐段成文</h2>

          <!-- 标题预览 -->
          <div v-if="article.mainTitle" class="preview-header">
            <h1 class="article-title">{{ article.mainTitle }}</h1>
            <p class="article-subtitle">{{ article.subTitle }}</p>
          </div>

          <!-- 大纲预览（流式解析展示） -->
          <div v-if="outlineRaw" class="outline-preview">
            <div class="section-label">
              <BulbOutlined />
              <span>文章大纲</span>
              <span v-if="isOutlineStreaming" class="typing-cursor">|</span>
            </div>
            <div class="outline-list">
              <div
                v-for="item in parsedOutline"
                :key="item.section"
                class="outline-item"
              >
                <div class="outline-title">{{ item.section }}. {{ item.title }}</div>
                <ul class="outline-points">
                  <li v-for="(point, idx) in item.points" :key="idx">{{ point }}</li>
                </ul>
              </div>
            </div>
          </div>

          <!-- 段落级进度（分段生成模式下展示） -->
          <div v-if="totalSections > 0" class="section-progress-box">
            <div class="progress-header">
              <BulbOutlined />
              <span>正文撰写进度</span>
              <span class="section-count">{{ completedSections }}/{{ totalSections }} 段</span>
            </div>
            <a-progress
              :percent="totalSections > 0 ? Math.round((completedSections / totalSections) * 100) : 0"
              status="active"
              :stroke-color="{ from: '#C08A3E', to: '#C4402F' }"
            />
          </div>

          <!-- 分段展示：已完成段渲染 Markdown / 当前段打字机 / 待生成段灰色占位 -->
          <div v-if="totalSections > 0 && parsedOutline.length > 0" class="content-preview section-list">
            <div
              v-for="(item, idx) in parsedOutline"
              :key="item.section ?? idx"
              :class="['section-block', sectionState(idx)]"
            >
              <template v-if="idx < completedSections">
                <div v-html="markdownToHtml(sectionContents[idx] || '')" class="markdown-body"></div>
              </template>
              <template v-else-if="idx === currentSectionIndex">
                <div class="content-streaming-text">{{ sectionContents[idx] || '' }}<span class="typing-cursor">|</span></div>
              </template>
              <template v-else>
                <div class="section-pending-title">{{ item.section }}. {{ item.title }}</div>
                <div class="section-pending-hint">待生成</div>
              </template>
            </div>
          </div>

          <!-- 正文预览（无段落信息时的整篇流式兜底） -->
          <div v-else-if="article.content" class="content-preview">
            <!-- 流式阶段：纯文本 + 打字光标（零延迟） -->
            <div v-if="isStreaming" class="content-streaming-text">{{ article.content }}<span class="typing-cursor">|</span></div>
            <!-- 流式结束后：完整 Markdown 渲染 -->
            <div v-else v-html="markdownToHtml(article.content)" class="markdown-body"></div>
          </div>

          <!-- 配图进度 -->
          <div v-if="currentStep === 4 && imageProgress > 0" class="image-progress-box">
            <div class="progress-header">
              <PictureOutlined />
              <span>第四笔 · 正在点睛（生成配图）</span>
            </div>
            <a-progress :percent="imageProgress" status="active" :stroke-color="{ from: '#C08A3E', to: '#C4402F' }" />
            <p class="progress-hint">{{ imageCount }}/{{ totalImages }} 张图片已完成</p>
          </div>

          <!-- 加载占位 -->
          <div v-if="currentStep === 0 && !article.mainTitle" class="loading-placeholder">
            <a-spin size="large" />
            <p>AI 正在构思标题...</p>
          </div>
          </div>

          <!-- 创作完成 -->
          <div v-else-if="currentPhase === 'COMPLETED'" key="completed" class="completed-state">
          <div class="stage-kicker">第 四 笔 · 点 睛</div>
          <h2 class="stage-title">点睛 · 四笔已齐</h2>

          <div class="success-header">
            <CheckCircleFilled class="success-icon" />
            <span>文章创作完成！</span>
          </div>

          <div class="preview-header">
            <h1 class="article-title">{{ article.mainTitle }}</h1>
            <p class="article-subtitle">{{ article.subTitle }}</p>
          </div>
          <div class="content-preview">
            <div v-html="markdownToHtml(article.fullContent || article.content || '')" class="markdown-body"></div>
          </div>

          <!-- 导出选项（文章末尾） -->
          <div class="export-banner">
            <div class="export-banner-left">
              <DownloadOutlined class="export-banner-icon" />
              <div class="export-banner-text">
                <span class="export-banner-title">导出文章</span>
                <span class="export-banner-desc">选择格式，一键保存到本地</span>
              </div>
            </div>
            <div class="export-banner-actions">
              <a-button @click="exportMarkdown" class="export-btn" :loading="exportingMd">
                <FileMarkdownOutlined />
                Markdown
              </a-button>
              <a-button type="primary" @click="exportPdf" class="export-btn" :loading="exportingPdf">
                <FilePdfOutlined />
                PDF
              </a-button>
            </div>
          </div>

          <!-- 成稿一览：完成态填充左侧版面（数据全部来自真实结果） -->
          <div class="completion-summary">
            <h4 class="summary-title">
              <BarChartOutlined />
              成稿一览
            </h4>
            <div class="summary-grid">
              <div class="summary-card">
                <div class="summary-value">{{ (article.fullContent || article.content || '').length }}</div>
                <div class="summary-label">字数</div>
              </div>
              <div class="summary-card">
                <div class="summary-value">{{ article.images?.length || 0 }}</div>
                <div class="summary-label">配图</div>
              </div>
              <div class="summary-card">
                <div class="summary-value">{{ Math.max(1, Math.ceil((article.fullContent || article.content || '').length / 400)) }}</div>
                <div class="summary-label">分钟阅读</div>
              </div>
            </div>
          </div>

          <!-- 快捷操作：从右侧迁入，跟随成稿一览 -->
          <div class="completion-actions">
            <a-button size="large" @click="copyContent" class="completion-btn">
              <CopyOutlined />
              复制全文
            </a-button>
            <a-button size="large" @click="viewArticle" class="completion-btn">
              <EyeOutlined />
              查看详情
            </a-button>
            <a-button size="large" type="primary" @click="resetCreate" class="completion-btn primary">
              <RedoOutlined />
              再创作一篇
            </a-button>
          </div>
          </div>
        </Transition>
      </main>

      <!-- 右：辅助面板（生成期间 = SSE 现场直播；其余时候 = 配额/热门选题，两者互斥不遮挡） -->
      <aside class="sidebar-right">
        <!-- 实时执行日志：仅在开始生成（或已有日志）时出现（数据全部来自真实 SSE 流） -->
        <div v-if="showLivePanel" class="log-panel">
          <div class="log-head">
            <span class="log-dot"></span>
            <b>生成现场 · SSE</b>
            <span :class="['live', { 'live-on': isCreating }]">{{ isCreating ? 'LIVE' : '待命' }}</span>
          </div>
          <div class="log-body">
            <div v-if="realtimeLogs.length === 0" class="log-empty">尚无日志 · 落笔之后，生成现场会在这里实时直播</div>
            <div
              v-for="(log, index) in realtimeLogs"
              :key="index"
              class="ln"
            >
              <span class="t">{{ formatLogTime(log.timestamp) }}</span>
              <span :class="['msg', log.level]">{{ log.message }}</span>
              <span v-if="log.level === 'success'" class="ok">✓</span>
            </div>
          </div>
        </div>

        <!-- 配额信息（仅输入阶段且现场面板未出现时展示，避免与 sticky 面板互相遮挡） -->
        <div v-if="currentPhase === 'INPUT' && !showLivePanel" class="panel-section quota-section">
          <h4 class="panel-title">
            <CrownOutlined />
            创作配额
          </h4>
          <div v-if="isAdmin" class="quota-admin">
            <span class="quota-badge admin">管理员</span>
            <span class="quota-text">无限次</span>
          </div>
          <div v-else-if="isVip" class="quota-admin">
            <span class="quota-badge vip">VIP 会员</span>
            <span class="quota-text">无限次</span>
          </div>
          <div v-else class="quota-info">
            <div class="quota-display">
              <span class="quota-number" :class="{ 'low': quota <= 1, 'empty': quota === 0 }">{{ quota }}</span>
              <span class="quota-unit">次</span>
            </div>
            <div class="quota-label">剩余可用</div>
            <a-progress
              :percent="(quota / 5) * 100"
              :show-info="false"
              :stroke-color="quota <= 1 ? '#C4402F' : '#3D6B5C'"
              size="small"
              class="quota-progress"
            />
          </div>
        </div>

        <!-- 热门选题（与配额同条件，与现场面板互斥） -->
        <div v-if="currentPhase === 'INPUT' && !showLivePanel" class="panel-section">
          <h4 class="panel-title">
            <BulbOutlined />
            热门选题
          </h4>
          <div class="hot-tags">
            <span
              v-for="example in exampleTopics"
              :key="example"
              class="hot-tag"
              @click="topic = example"
            >
              {{ example }}
            </span>
          </div>
        </div>

        <!-- 当前选题提示（与现场面板互斥） -->
        <div v-if="currentPhase !== 'INPUT' && currentPhase !== 'COMPLETED' && topic && !showLivePanel" class="panel-section">
          <h4 class="panel-title">
            <BulbOutlined />
            创作选题
          </h4>
          <div class="topic-display">
            <p>{{ topic }}</p>
          </div>
        </div>

        <!-- 完成态的快捷操作与统计已迁入左侧「成稿一览」，右栏仅保留 sticky 日志，避免遮挡 -->

        <!-- 底部帮助链接 -->
        <div v-if="!showLivePanel" class="panel-footer">
          <a class="help-link">
            <QuestionCircleOutlined />
            使用帮助
          </a>
          <a class="help-link">
            <MessageOutlined />
            反馈建议
          </a>
        </div>
      </aside>
    </div>

    <!-- 错误提示 -->
    <a-modal
      v-model:open="errorVisible"
      title="创作失败"
      @ok="errorVisible = false"
    >
      <p>{{ errorMessage }}</p>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, onMounted, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  RocketOutlined,
  CheckCircleFilled,
  CopyOutlined,
  EyeOutlined,
  RedoOutlined,
  BulbOutlined,
  BarChartOutlined,
  QuestionCircleOutlined,
  MessageOutlined,
  PictureOutlined,
  WarningOutlined,
  CrownOutlined,
  DownloadOutlined,
  FileMarkdownOutlined,
  FilePdfOutlined
} from '@ant-design/icons-vue'
import { createArticle, confirmTitle, confirmOutline, confirmContent, getArticle } from '@/api/articleController'
import { connectSSE, closeSSE, type SSEMessage } from '@/utils/sse'
import { isAdmin as checkIsAdmin, isVip as checkIsVip, hasQuota as checkHasQuota } from '@/utils/permission'
import { exportAsMarkdown, exportAsPdf } from '@/utils/article'
import { marked } from 'marked'
import { FOUR_BRUSHES, resolveCurrentBrush, getBrushState, type BrushState } from '@/constants/fourBrushes'
import TitleSelectingStage from './components/TitleSelectingStage.vue'
import OutlineEditingStage from './components/OutlineEditingStage.vue'
import ContentConfirmStage from './components/ContentConfirmStage.vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

// 配额相关计算属性
const isAdmin = computed(() => checkIsAdmin(loginUserStore.loginUser))
const isVip = computed(() => checkIsVip(loginUserStore.loginUser))
const quota = computed(() => loginUserStore.loginUser.quota ?? 0)
const hasQuota = computed(() => checkHasQuota(loginUserStore.loginUser))

// 示例选题
const exampleTopics = [
  '2026年AI如何改变职场',
  '程序员如何提升竞争力',
  '远程办公的利与弊',
  '如何培养深度思考',
  '新能源汽车趋势',
  '健康饮食指南',
]

// 阶段状态
const currentPhase = ref<string>('INPUT')  // INPUT, TITLE_SELECTING, OUTLINE_EDITING, CONTENT_GENERATING, COMPLETED

// 状态
const topic = ref('')
const selectedStyle = ref('')  // 选中的文章风格（空字符串表示默认，__custom 表示自定义）
const customStyle = ref('')  // 自定义风格文本
const selectedWordCount = ref(1200)  // 目标字数
const customWordCount = ref<number | null>(null)  // 自定义字数
const isCustomWordCount = ref(false)
const isCreating = ref(false)
const isCompleted = ref(false)
const isStreaming = ref(false)
const isOutlineStreaming = ref(false)
const currentStep = ref(0)
const taskId = ref('')
const errorVisible = ref(false)
const errorMessage = ref('')
const confirmLoading = ref(false)

// 四笔进度：把真实阶段/智能体步骤映射为四笔索引与各笔状态（映射集中在常量文件）
const currentBrush = computed(() => resolveCurrentBrush(currentPhase.value, currentStep.value))
const brushStates = computed<BrushState[]>(() =>
  FOUR_BRUSHES.map((_, index) => getBrushState(index, currentBrush.value))
)

// 实时日志
interface RealtimeLog {
  timestamp: number
  level: string
  message: string
}
const realtimeLogs = ref<RealtimeLog[]>([])

/* 「生成现场」面板显隐：开始生成（或已有历史日志）时出现，初始与纯输入态隐藏。
   与右侧配额/热门选题互斥，避免 sticky 面板滚动时遮挡它们 */
const showLivePanel = computed(() => isCreating.value || realtimeLogs.value.length > 0)

// 标题方案
const titleOptions = ref<Array<{mainTitle: string, subTitle: string}>>([])

// 大纲项类型
interface OutlineItem {
  title: string
  points: string[]
  section: number
}

// 大纲数据
const outline = ref<OutlineItem[]>([])

// 大纲数据（流式）
const outlineRaw = ref('')

// 规范化大纲条目（流式解析时 points 可能尚未闭合，兜底为空数组，避免模板渲染报错）
const normalizeOutlineItem = (item: any): OutlineItem => ({
  title: item?.title ?? '',
  points: Array.isArray(item?.points) ? item.points : [],
  section: item?.section ?? 0,
})

// 解析大纲 JSON：后端按 prompt 输出纯数组 [{"section":1,"title":"...","points":[...]}]，
// 同时兼容 {"sections":[...]} 包装结构；流式过程中提取累积文本里已完整闭合的章节。
const parsedOutline = computed<OutlineItem[]>(() => {
  if (!outlineRaw.value) return []

  const str = outlineRaw.value.trim()

  // 1. 累积文本恰好是完整 JSON 时直接解析
  try {
    const parsed = JSON.parse(str)
    if (Array.isArray(parsed)) {
      return parsed.map(normalizeOutlineItem)
    }
    if (parsed && Array.isArray(parsed.sections)) {
      return parsed.sections.map(normalizeOutlineItem)
    }
    return []
  } catch {
    // 2. JSON 尚未闭合（流式中）：提取累积文本里最后一个完整的对象数组
    try {
      const start = str.indexOf('[')
      if (start === -1) return []
      const tail = str.substring(start)
      const lastBrace = tail.lastIndexOf('}')
      if (lastBrace <= 0) return []
      const parsed = JSON.parse(tail.substring(0, lastBrace + 1) + ']')
      if (Array.isArray(parsed)) {
        return parsed.map(normalizeOutlineItem)
      }
      return []
    } catch {
      return []
    }
  }
})

// 内容区域引用（用于自动滚动）
const mainContentRef = ref<HTMLElement | null>(null)

// 配图进度
const imageCount = ref(0)
const totalImages = ref(5)
const imageProgress = ref(0)

// 分段生成状态（ContentAgent 按大纲逐段推送段落级事件）
const sectionContents = ref<string[]>([])
const currentSectionIndex = ref(-1)
const completedSections = ref(0)
const totalSections = ref(0)

// 未完成任务的本地持久化 key（切页/刷新后据此恢复创作现场）
const ACTIVE_TASK_KEY = 'activeTaskId'

// 文章数据
const article = ref<Partial<API.ArticleVO>>({
  mainTitle: '',
  subTitle: '',
  content: '',
  fullContent: '',
  images: [],
})

let eventSource: EventSource | null = null

// Markdown 转 HTML
const markdownToHtml = (markdown: string | undefined) => {
  return marked(markdown || '')
}

// 段落视觉状态：done 已完成 / current 生成中 / pending 待生成
const sectionState = (idx: number): 'done' | 'current' | 'pending' => {
  if (idx < completedSections.value) return 'done'
  if (idx === currentSectionIndex.value) return 'current'
  return 'pending'
}

// 自动滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (mainContentRef.value) {
      mainContentRef.value.scrollTop = mainContentRef.value.scrollHeight
    }
  })
}

// SSE 推送记录的时间戳：只要收到任意一条消息就刷新
const lastSseAt = ref<number>(Date.now())
let watchdogTimer: ReturnType<typeof setInterval> | null = null

// 停止看门狗
const stopWatchdog = () => {
  if (watchdogTimer) {
    clearInterval(watchdogTimer)
    watchdogTimer = null
  }
}

// 用服务端终态补齐前端结果（SSE 丢失时的兜底路径）
const applyCompletedArticle = (vo: API.ArticleVO) => {
  if (vo.mainTitle) article.value.mainTitle = vo.mainTitle
  if (vo.subTitle) article.value.subTitle = vo.subTitle
  if (vo.content) article.value.content = vo.content
  if (vo.fullContent) article.value.fullContent = vo.fullContent
  if (vo.images) article.value.images = vo.images
  if (vo.id) article.value.id = vo.id
  if (vo.coverImage) article.value.coverImage = vo.coverImage
  currentPhase.value = 'COMPLETED'
  currentStep.value = 6
  isCompleted.value = true
  isCreating.value = false
  isStreaming.value = false
  isOutlineStreaming.value = false
  addLog('实时连接静默超时，已通过状态同步补齐最终结果', 'success')
  message.success('文章创作完成!')
}

/**
 * 应用 SSE 快照 —— 断点恢复的核心
 * SSE 建立后后端会立即推一条 SNAPSHOT，携带当前阶段与已生成内容。
 * 按 phase 恢复到对应 UI；实时进度（流式段落）也会被后续消息继续追加。
 */
const applySnapshot = (snap: any) => {
  addLog('已恢复创作现场', 'success')
  if (snap.topic) topic.value = snap.topic
  if (snap.mainTitle) article.value.mainTitle = snap.mainTitle
  if (snap.subTitle) article.value.subTitle = snap.subTitle
  if (snap.titleOptions) titleOptions.value = snap.titleOptions
  if (snap.outline && snap.outline.length > 0) {
    outline.value = snap.outline
    outlineRaw.value = JSON.stringify(snap.outline)
  } else if (snap.outlineRaw) {
    outlineRaw.value = snap.outlineRaw
  }
  if (Array.isArray(snap.sectionContents)) sectionContents.value = snap.sectionContents
  if (snap.content) article.value.content = snap.content
  if (snap.images) article.value.images = snap.images
  if (snap.fullContent) article.value.fullContent = snap.fullContent
  currentSectionIndex.value = snap.currentSectionIndex ?? -1
  completedSections.value = snap.completedSections ?? 0
  totalSections.value = snap.totalSections ?? 0
  if (snap.totalImages) totalImages.value = snap.totalImages
  if (Array.isArray(snap.images)) {
    imageCount.value = snap.images.length
    if (totalImages.value > 0) {
      imageProgress.value = Math.round((imageCount.value / totalImages.value) * 100)
    }
  }

  // 按阶段恢复 UI
  switch (snap.phase) {
    case 'TITLE_GENERATING':
      currentPhase.value = 'TITLE_GENERATING'
      currentStep.value = 0
      isCreating.value = true
      startWatchdog()
      break
    case 'TITLE_SELECTING':
      currentPhase.value = 'TITLE_SELECTING'
      currentStep.value = 1
      break
    case 'OUTLINE_GENERATING':
      currentPhase.value = 'OUTLINE_GENERATING'
      currentStep.value = 1
      isCreating.value = true
      isOutlineStreaming.value = true
      startWatchdog()
      break
    case 'OUTLINE_EDITING':
      currentPhase.value = 'OUTLINE_EDITING'
      currentStep.value = 1
      break
    case 'CONTENT_GENERATING':
      currentPhase.value = 'CONTENT_GENERATING'
      currentStep.value = 2
      isCreating.value = true
      isStreaming.value = currentSectionIndex.value >= 0 && completedSections.value < totalSections.value
      startWatchdog()
      break
    case 'CONTENT_CONFIRMING':
      currentPhase.value = 'CONTENT_CONFIRMING'
      currentStep.value = 3
      break
    case 'IMAGE_GENERATING':
      currentPhase.value = 'CONTENT_GENERATING'
      currentStep.value = 4
      isCreating.value = true
      startWatchdog()
      break
    case 'COMPLETED':
      currentPhase.value = 'COMPLETED'
      currentStep.value = 6
      isCompleted.value = true
      isCreating.value = false
      sessionStorage.removeItem(ACTIVE_TASK_KEY)
      break
    case 'FAILED':
      errorMessage.value = snap.errorMessage || '创作失败'
      errorVisible.value = true
      sessionStorage.removeItem(ACTIVE_TASK_KEY)
      break
  }
}

/**
 * 启动流式看门狗
 * 场景：浏览器休眠、代理断流、服务端已把连接判为失效等情况，
 * 会造成 SSE 链路静默断开而任务其实早已跑完 —— 界面就会永久停在"生成中"。
 * 兜底：生成中若超过 15 秒没收到任何推送，主动查询任务状态；
 * 已完结就直接补齐结果，仍在进行则继续等待。
 */
const startWatchdog = () => {
  stopWatchdog()
  lastSseAt.value = Date.now()
  watchdogTimer = setInterval(async () => {
    // 只在"确实在生成"时巡检，用户在选标题/改大纲/确认配图时不打扰
    if (!isCreating.value || !taskId.value) {
      return
    }
    if (Date.now() - lastSseAt.value < 15000) {
      return
    }
    try {
      const res = await getArticle({ taskId: taskId.value })
      const vo = res.data?.data
      if (!vo) return
      if (vo.status === 'COMPLETED') {
        applyCompletedArticle(vo)
        stopWatchdog()
      } else if (vo.status === 'FAILED') {
        errorMessage.value = vo.errorMessage || '创作失败'
        errorVisible.value = true
        isCreating.value = false
        addLog(`创作失败: ${vo.errorMessage || '未知错误'}`, 'error')
        stopWatchdog()
      } else {
        // 任务仍在进行，重置静默计时，避免重复轮询
        lastSseAt.value = Date.now()
      }
    } catch {
      // 查询失败不打断创作，等下一轮
    }
  }, 5000)
}

// 开始创作
const startCreate = async () => {
  if (!topic.value.trim()) {
    message.warning('请输入选题')
    return
  }

  if (!hasQuota.value) {
    message.error('配额不足，无法创建文章')
    return
  }

  isCreating.value = true
  currentStep.value = 0
  realtimeLogs.value = []
  addLog('开始创建文章任务...', 'info')

  try {
    // 创建任务（配图方式在正文生成后确认，此处不传）
    const finalWordCount = selectedWordCount.value === 0
      ? (customWordCount.value || 1200)
      : selectedWordCount.value
    const finalStyle = selectedStyle.value === '__custom'
      ? (customStyle.value.trim() || undefined)
      : (selectedStyle.value || undefined)
    const res = await createArticle({
      topic: topic.value,
      style: finalStyle,
      wordCount: finalWordCount,
    })
    if (res.data.code !== 0) {
      message.error(res.data.message || '创建任务失败')
      isCreating.value = false
      return
    }
    const newTaskId = res.data.data
    if (!newTaskId) {
      throw new Error('创建任务失败：未返回任务ID')
    }
    taskId.value = newTaskId
    addLog(`任务创建成功，ID: ${newTaskId}`, 'success')
    // 持久化 taskId：切换页面/刷新后 onMounted 据此恢复创作现场
    sessionStorage.setItem(ACTIVE_TASK_KEY, newTaskId)

    // 刷新用户信息（更新配额）
    await loginUserStore.fetchLoginUser()

    // 建立 SSE 连接
    addLog('已建立实时连接，开始生成...', 'info')
    eventSource = connectSSE(taskId.value, {
      onMessage: handleSSEMessage,
      onError: handleSSEError,
      onComplete: handleSSEComplete,
    })
    startWatchdog()
  } catch (error) {
    const err = error as Error
    message.error(err.message || '创建任务失败')
    isCreating.value = false
  }
}

// 添加日志
const addLog = (message: string, level: string = 'info') => {
  realtimeLogs.value.push({
    timestamp: Date.now(),
    level,
    message
  })
  // 限制日志数量，最多保留 50 条
  if (realtimeLogs.value.length > 50) {
    realtimeLogs.value.shift()
  }
}

// 格式化日志时间
const formatLogTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour12: false })
}

// 处理 SSE 消息
const handleSSEMessage = (msg: SSEMessage) => {
  // 收到任意推送都刷新看门狗计时：真正的危险信号是"静默"
  lastSseAt.value = Date.now()
  console.log('SSE消息:', msg)

  switch (msg.type) {
    case 'SNAPSHOT':
      // SSE 建立后的第一条消息：恢复创作现场（切页/刷新/断线重连场景）
      applySnapshot(msg)
      break

    case 'AGENT1_COMPLETE':
      // 智能体1完成，进入标题生成阶段（显示加载）
      currentPhase.value = 'TITLE_GENERATING'
      currentStep.value = 1
      addLog('智能体1：标题方案生成完成', 'success')
      break

    case 'TITLES_GENERATED':
      // 标题方案生成完成，切换到选择标题阶段
      currentPhase.value = 'TITLE_SELECTING'
      titleOptions.value = msg.titleOptions || []
      isCreating.value = false
      addLog(`生成了 ${msg.titleOptions?.length || 0} 个标题方案`, 'success')
      break

    case 'AGENT2_STREAMING':
      // 大纲流式输出（显示生成中状态）
      currentPhase.value = 'OUTLINE_GENERATING'
      isOutlineStreaming.value = true
      outlineRaw.value += msg.content || ''
      scrollToBottom()
      break

    case 'OUTLINE_GENERATED':
      // 大纲生成完成，切换到编辑大纲阶段
      currentPhase.value = 'OUTLINE_EDITING'
      outline.value = msg.outline || []
      // 同步流式区为完整大纲，保证编辑/正文阶段展示一致
      outlineRaw.value = JSON.stringify(msg.outline || [])
      isCreating.value = false
      isOutlineStreaming.value = false
      addLog('大纲生成完成，等待确认', 'success')
      // 保持在步骤1（规划大纲），用户编辑大纲时仍处于此阶段
      break

    case 'AGENT2_COMPLETE':
      // 大纲完成（内部处理，已在 OUTLINE_GENERATED 中切换阶段）
      // 不改变 currentStep，保持在步骤1，等用户确认大纲后才进入步骤2
      break

    case 'CONTENT_SECTION_START':
      // 分段模式：某段开始生成
      currentPhase.value = 'CONTENT_GENERATING'
      currentStep.value = 2
      isCreating.value = true
      isStreaming.value = true
      currentSectionIndex.value = msg.sectionIndex ?? 0
      totalSections.value = msg.totalSections ?? totalSections.value
      addLog(`开始撰写第 ${(msg.sectionIndex ?? 0) + 1}/${msg.totalSections} 段：${msg.sectionTitle || ''}`, 'info')
      break

    case 'AGENT3_STREAMING':
      // 正文流式输出，进入步骤2（撰写正文）
      currentPhase.value = 'CONTENT_GENERATING'
      currentStep.value = 2
      isStreaming.value = true
      if (typeof msg.sectionIndex === 'number') {
        // 分段模式：追加到对应段落，并同步整篇内容（供 CONTENT_CONFIRMING 阶段展示）
        const idx = msg.sectionIndex
        while (sectionContents.value.length <= idx) sectionContents.value.push('')
        sectionContents.value[idx] += msg.content || ''
        article.value.content = sectionContents.value.join('\n\n')
      } else {
        // 兼容：无段落信息时走整篇累积
        article.value.content += msg.content || ''
      }
      scrollToBottom()
      break

    case 'CONTENT_SECTION_COMPLETE':
      // 分段模式：某段生成完成
      completedSections.value = (msg.sectionIndex ?? 0) + 1
      addLog(`第 ${(msg.sectionIndex ?? 0) + 1}/${msg.totalSections} 段已完成`, 'success')
      break

    case 'AGENT3_COMPLETE':
      // 正文完成，进入配图确认阶段（用户选择是否生成图片）
      isStreaming.value = false
      currentStep.value = 3
      currentPhase.value = 'CONTENT_CONFIRMING'
      addLog('正文生成完成，等待确认配图方案', 'success')
      break

    case 'AGENT4_COMPLETE':
      // 配图分析完成，进入配图生成步骤
      currentStep.value = 4
      totalImages.value = msg.imageRequirements?.length || 5
      addLog(`配图需求分析完成，共 ${totalImages.value} 张`, 'success')
      break

    case 'IMAGE_COMPLETE':
      // 单张配图完成
      imageCount.value++
      imageProgress.value = Math.round((imageCount.value / totalImages.value) * 100)
      addLog(`配图生成中 ${imageCount.value}/${totalImages.value}`, 'info')
      break

    case 'AGENT5_COMPLETE':
      // 所有配图完成，进入图文合成步骤
      currentStep.value = 5
      article.value.images = msg.images
      addLog('所有配图生成完成', 'success')
      break

    case 'MERGE_COMPLETE':
      // 图文合成完成
      article.value.fullContent = msg.fullContent
      scrollToBottom()
      addLog('图文合成完成', 'success')
      break

    case 'ALL_COMPLETE':
      // 全部完成
      stopWatchdog()
      currentPhase.value = 'COMPLETED'
      currentStep.value = 6
      isCompleted.value = true
      sessionStorage.removeItem(ACTIVE_TASK_KEY)
      message.success('文章创作完成!')
      addLog('文章创作完成！', 'success')
      break

    case 'ERROR':
      stopWatchdog()
      errorMessage.value = msg.message || '创作失败'
      errorVisible.value = true
      isCreating.value = false
      currentPhase.value = 'INPUT'
      sessionStorage.removeItem(ACTIVE_TASK_KEY)
      addLog(`创作失败: ${msg.message || '未知错误'}`, 'error')
      break
  }
}

// 判断错误是否属于任务过期/不存在（后端 getContext 抛 NOT_FOUND 时提示）
const isTaskExpired = (msg?: string) => /过期|不存在/.test(msg || '')

// 任务过期处理：提示并回到输入阶段
const handleTaskExpired = () => {
  stopWatchdog()
  message.error('任务已过期或已失效，请重新创建')
  closeSSE(eventSource)
  eventSource = null
  resetCreate()
}

// 确认标题
const handleConfirmTitle = async (data: {mainTitle: string, subTitle: string, userDescription: string}) => {
  confirmLoading.value = true
  try {
    const res = await confirmTitle({
      taskId: taskId.value,
      selectedMainTitle: data.mainTitle,
      selectedSubTitle: data.subTitle,
      userDescription: data.userDescription
    })
    if (res.data.code !== 0) {
      if (isTaskExpired(res.data.message)) {
        handleTaskExpired()
        return
      }
      message.warning(res.data.message || '确认标题失败')
      return
    }
    // 保存标题信息，用于大纲生成阶段展示
    article.value.mainTitle = data.mainTitle
    article.value.subTitle = data.subTitle
    // 乐观切换到大纲生成阶段：立即隐藏确认按钮，避免等待 SSE 期间重复点击
    outlineRaw.value = ''
    currentPhase.value = 'OUTLINE_GENERATING'
    isCreating.value = true
    addLog('标题已确认，开始生成大纲...', 'info')
    startWatchdog()
  } catch (error) {
    const err = error as Error
    if (isTaskExpired(err.message)) {
      handleTaskExpired()
      return
    }
    message.error(err.message || '确认标题失败')
  } finally {
    confirmLoading.value = false
  }
}

// 确认大纲
const handleConfirmOutline = async (outlineData: Array<{section: number, title: string, points: string[]}>) => {
  confirmLoading.value = true
  try {
    const res = await confirmOutline({
      taskId: taskId.value,
      outline: outlineData
    })
    if (res.data.code !== 0) {
      if (isTaskExpired(res.data.message)) {
        handleTaskExpired()
        return
      }
      message.warning(res.data.message || '确认大纲失败')
      return
    }
    // 更新 outlineRaw 为用户修改后的大纲，确保 CONTENT_GENERATING 阶段展示正确的大纲
    outlineRaw.value = JSON.stringify({ sections: outlineData })
    // 初始化分段生成状态（段落事件随后端生成进度逐步填充）
    sectionContents.value = []
    currentSectionIndex.value = -1
    completedSections.value = 0
    totalSections.value = outlineData.length
    // 乐观切换到正文生成阶段，避免等待 SSE 期间重复点击
    currentPhase.value = 'CONTENT_GENERATING'
    currentStep.value = 2
    isCreating.value = true
    addLog('大纲已确认，开始生成正文...', 'info')
    startWatchdog()
  } catch (error) {
    const err = error as Error
    if (isTaskExpired(err.message)) {
      handleTaskExpired()
      return
    }
    message.error(err.message || '确认大纲失败')
  } finally {
    confirmLoading.value = false
  }
}

// 确认配图方案（正文完成后用户选择是否生成图片及类型）
const handleConfirmContent = async (data: {generateImages: boolean, enabledImageMethods: string[]}) => {
  confirmLoading.value = true
  try {
    const res = await confirmContent({
      taskId: taskId.value,
      generateImages: data.generateImages,
      enabledImageMethods: data.enabledImageMethods
    })
    if (res.data.code !== 0) {
      if (isTaskExpired(res.data.message)) {
        handleTaskExpired()
        return
      }
      message.warning(res.data.message || '操作失败')
      return
    }
    if (data.generateImages) {
      message.success('开始生成配图...')
      addLog(`已确认生成配图（${data.enabledImageMethods.join('、') || '默认'}）`, 'info')
    } else {
      message.success('正在合成纯文字文章...')
      addLog('已确认不生成配图，开始合成', 'info')
    }
    // 回到正文展示阶段，等待 SSE 推送配图/合成进度
    currentPhase.value = 'CONTENT_GENERATING'
    currentStep.value = 3
    isCreating.value = true
    startWatchdog()
  } catch (error) {
    const err = error as Error
    if (isTaskExpired(err.message)) {
      handleTaskExpired()
      return
    }
    message.error(err.message || '操作失败')
  } finally {
    confirmLoading.value = false
  }
}

// 处理 SSE 错误
const handleSSEError = (error: Event) => {
  console.error('SSE错误:', error)
  // 任务已完成时忽略断开事件
  if (currentPhase.value === 'COMPLETED') {
    return
  }
  // 服务端关闭连接（任务过期清理）或网络异常
  message.warning('实时连接已断开，刷新页面可自动恢复创作进度')
  isCreating.value = false
}

// 处理 SSE 完成
const handleSSEComplete = () => {
  console.log('SSE连接关闭')
}

// 复制全文
const copyContent = async () => {
  const content = article.value.fullContent || article.value.content || ''
  try {
    await navigator.clipboard.writeText(content)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}

// 导出为 Markdown
const exportingMd = ref(false)
const exportMarkdown = () => {
  exportingMd.value = true
  try {
    exportAsMarkdown({
      title: article.value.mainTitle || '文章',
      subTitle: article.value.subTitle,
      content: article.value.content,
      fullContent: article.value.fullContent,
    })
    message.success('Markdown 文件已下载')
  } finally {
    exportingMd.value = false
  }
}

// 导出为 PDF（直接生成 PDF 文件）
const exportingPdf = ref(false)
const exportPdf = async () => {
  exportingPdf.value = true
  try {
    await exportAsPdf({
      title: article.value.mainTitle || '文章',
      subTitle: article.value.subTitle,
      content: article.value.content,
      fullContent: article.value.fullContent,
    })
    message.success('PDF 文件已下载')
  } catch {
    message.error('PDF 生成失败，请重试')
  } finally {
    exportingPdf.value = false
  }
}

// 切换字数预设时清空自定义输入
const onWordCountPresetChange = () => {
  if (selectedWordCount.value !== 0) {
    customWordCount.value = null
  }
}

// 切换风格预设时清空自定义输入
const onStylePresetChange = () => {
  if (selectedStyle.value !== '__custom') {
    customStyle.value = ''
  }
}

// 查看文章详情
const viewArticle = () => {
  router.push(`/article/${taskId.value}`)
}

// 重新创作
const resetCreate = () => {
  stopWatchdog()
  sessionStorage.removeItem(ACTIVE_TASK_KEY)
  currentPhase.value = 'INPUT'
  topic.value = ''
  selectedStyle.value = ''
  customStyle.value = ''
  selectedWordCount.value = 1200
  customWordCount.value = null
  titleOptions.value = []
  outline.value = []
  isCreating.value = false
  isCompleted.value = false
  isStreaming.value = false
  isOutlineStreaming.value = false
  currentStep.value = 0
  imageCount.value = 0
  imageProgress.value = 0
  outlineRaw.value = ''
  confirmLoading.value = false
  realtimeLogs.value = []
  sectionContents.value = []
  currentSectionIndex.value = -1
  completedSections.value = 0
  totalSections.value = 0
  article.value = {
    mainTitle: '',
    subTitle: '',
    content: '',
    fullContent: '',
    images: [],
  }
}

// 组件挂载时检查路由参数 + 恢复未完成的创作现场
onMounted(() => {
  if (route.query.topic) {
    topic.value = route.query.topic as string
  }
  // 恢复入口：优先路由 taskId（历史列表"继续创作"跳转），其次 sessionStorage（切页/刷新）
  const resumeTaskId = (route.query.taskId as string) || sessionStorage.getItem(ACTIVE_TASK_KEY)
  if (resumeTaskId) {
    taskId.value = resumeTaskId
    sessionStorage.setItem(ACTIVE_TASK_KEY, resumeTaskId)
    addLog('检测到未完成的创作任务，正在恢复现场...', 'info')
    // 建立 SSE：后端会先推一条 SNAPSHOT 快照，applySnapshot 据此恢复到当前阶段
    eventSource = connectSSE(taskId.value, {
      onMessage: handleSSEMessage,
      onError: handleSSEError,
      onComplete: handleSSEComplete,
    })
  }
})

// 组件卸载前关闭 SSE
onBeforeUnmount(() => {
  stopWatchdog()
  closeSSE(eventSource)
})
</script>

<style scoped lang="scss">
.article-create-page {
  min-height: calc(100vh - 68px);
  background: var(--paper);
  padding: 0 20px 40px;
}

/* ==================== 四笔进度条 ==================== */
.stepper {
  display: flex;
  align-items: flex-start;
  max-width: 860px;
  margin: 36px auto 8px;
  padding: 0 12px;
}

.step-node {
  flex: 1;
  text-align: center;
  position: relative;
}

/* 连接虚线：随状态变色 */
.step-node:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 19px;
  left: calc(50% + 30px);
  right: calc(-50% + 30px);
  border-top: 2px dashed var(--hairline);
}

.step-node.done:not(:last-child)::after {
  border-top-color: var(--jade);
}

.step-node.current:not(:last-child)::after {
  border-top-color: var(--gold);
}

.ball {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin: 0 auto 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--kai);
  font-size: 16px;
  font-weight: 700;
  background: var(--paper-card);
  border: 2.5px solid var(--hairline);
  color: var(--ink-3);
  transition: all 0.4s var(--ease);
}

/* current 态：朱砂描边放大 + 光圈 */
.step-node.current .ball {
  border-color: var(--vermilion);
  color: var(--vermilion);
  box-shadow: 0 0 0 5px rgba(196, 64, 47, 0.12);
  transform: scale(1.08);
}

/* done 态：黛绿实心打勾 */
.step-node.done .ball {
  border-color: var(--jade);
  background: var(--jade);
  color: var(--paper);
}

.step-name {
  font-family: var(--kai);
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}

.step-node.current .step-name {
  color: var(--vermilion);
}

.step-en {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--ink-3);
  letter-spacing: 0.12em;
}

/* ==================== 主区布局：左舞台 + 右日志 ==================== */
.create-layout {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 26px;
  max-width: 1200px;
  margin: 26px auto 0;
}

/* 阶段舞台卡：白底 + 1.5px 墨边 + 硬阴影 */
.stage {
  position: relative;
  background: var(--paper-card);
  border: 1.5px solid var(--ink);
  border-radius: 16px;
  padding: 34px 38px;
  box-shadow: 5px 5px 0 rgba(38, 34, 28, 0.1);
  min-height: 430px;
  overflow: hidden;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

/* 每笔 kicker：描金楷体 */
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
  margin: 0 0 18px;
  color: var(--ink);
}

.stage-hint {
  font-size: 13.5px;
  color: var(--ink-2);
  margin: 0 0 22px;
}

/* ==================== 完成印章覆盖层 ==================== */
.done-stamp {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  background: rgba(247, 241, 229, 0.92);
  z-index: 5;
  pointer-events: none;
}

.done-stamp .seal-big {
  width: 150px;
  height: 150px;
  border: 5px solid var(--vermilion);
  border-radius: 14px;
  color: var(--vermilion);
  font-family: var(--kai);
  font-size: 64px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(-8deg);
  box-shadow: inset 0 0 0 3px rgba(196, 64, 47, 0.3);
  background: rgba(255, 253, 247, 0.6);
}

.done-stamp small {
  margin-top: 18px;
  font-family: var(--kai);
  font-size: 17px;
  color: var(--ink-2);
  letter-spacing: 0.3em;
}

/* ==================== 输入状态 ==================== */
.topic-textarea {
  border-radius: 12px !important;
  font-size: 15px;
}

.style-section {
  margin-top: 22px;
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.section-title {
  font-family: var(--kai);
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}

.section-tip {
  font-size: 12px;
  color: var(--ink-3);
}

.style-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
}

.custom-word-count {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 280px;
}

.word-count-unit {
  font-size: 13px;
  color: var(--ink-2);
}

/* 朱砂「落笔开始」按钮：按压感 */
.create-btn {
  margin-top: 26px;
  height: 50px;
  padding: 0 34px;
  font-family: var(--kai);
  font-size: 16px;
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
    color: var(--ink-3) !important;
    box-shadow: none !important;
  }
}

.quota-warning {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 14px;
  font-size: 13px;
  color: var(--vermilion);
}

/* ==================== 标题/大纲/正文展示 ==================== */
.loading-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 80px 0;
  text-align: center;

  h3 {
    font-family: var(--kai);
    font-size: 18px;
    font-weight: 700;
    margin: 0;
    color: var(--ink);
  }

  p {
    margin: 0;
    font-size: 13.5px;
    color: var(--ink-3);
  }
}

.preview-header {
  padding: 16px 0 14px;
  margin-bottom: 16px;
  border-bottom: 1px dashed var(--hairline);
}

.article-title {
  font-family: var(--kai);
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--ink);
}

.article-subtitle {
  font-size: 14px;
  color: var(--ink-2);
  margin: 0;
}

.outline-preview {
  margin-bottom: 22px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--kai);
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 12px;

  .anticon {
    color: var(--gold);
  }
}

.typing-cursor {
  color: var(--vermilion);
  animation: blink 0.9s step-end infinite;
}

.outline-list {
  display: grid;
  gap: 10px;
}

.outline-item {
  background: var(--paper);
  border: 1px solid var(--hairline);
  border-radius: 10px;
  padding: 12px 16px;
  transition: border-color 0.3s var(--ease);

  &:hover {
    border-color: var(--gold);
  }
}

.outline-title {
  font-family: var(--kai);
  font-size: 15.5px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 6px;
}

.outline-points {
  margin: 0;
  padding-left: 22px;

  li {
    font-size: 13px;
    color: var(--ink-2);
    margin-bottom: 4px;
  }
}

.outline-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13.5px;
  color: var(--ink-3);
  padding: 20px 0;
}

.content-preview {
  margin-bottom: 18px;
}

.content-streaming-text {
  font-size: 15.5px;
  line-height: 2;
  white-space: pre-wrap;
  color: var(--ink);
}

.markdown-body {
  font-size: 15.5px;
  line-height: 2;
  color: var(--ink);

  :deep(h1), :deep(h2), :deep(h3) {
    font-family: var(--kai);
    color: var(--ink);
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 10px;
  }
}

.image-progress-box {
  margin-top: 20px;
  padding: 16px 18px;
  background: var(--paper);
  border: 1px dashed var(--hairline);
  border-radius: 12px;
}

/* ==================== 分段生成展示 ==================== */
.section-progress-box {
  margin-bottom: 18px;
  padding: 14px 18px;
  background: var(--paper);
  border: 1px dashed var(--hairline);
  border-radius: 12px;

  .section-count {
    margin-left: auto;
    font-family: var(--font-mono);
    font-size: 12px;
    color: var(--ink-3);
  }
}

.section-list {
  display: grid;
  gap: 14px;
}

.section-block {
  border-radius: 12px;
  transition: all 0.3s var(--ease);

  &.done {
    padding: 4px 2px;
  }

  &.current {
    background: var(--paper);
    border: 1.5px solid var(--gold);
    padding: 16px 18px;
  }

  &.pending {
    background: var(--paper);
    border: 1.5px dashed var(--hairline);
    padding: 16px 18px;
    opacity: 0.75;
  }
}

.section-pending-title {
  font-family: var(--kai);
  font-size: 15px;
  font-weight: 700;
  color: var(--ink-2);
}

.section-pending-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-3);
  letter-spacing: 0.1em;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--kai);
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 10px;

  .anticon {
    color: var(--gold);
  }
}

.progress-hint {
  margin: 8px 0 0;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--ink-3);
}

.loading-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 0;

  p {
    margin: 0;
    color: var(--ink-3);
    font-size: 13.5px;
  }
}

.success-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--kai);
  font-size: 16px;
  font-weight: 700;
  color: var(--jade);
  margin-bottom: 14px;

  .success-icon {
    color: var(--jade);
    font-size: 20px;
  }
}

.export-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 22px;
  padding: 16px 20px;
  background: var(--paper-2);
  border: 1px dashed var(--hairline);
  border-radius: 12px;
}

/* ==================== 成稿一览（完成态左侧填充） ==================== */
.completion-summary {
  margin-top: 22px;
  padding: 20px 22px;
  background: var(--paper-card);
  border: 1px solid var(--hairline);
  border-radius: 14px;
  box-shadow: 3px 3px 0 rgba(38, 34, 28, 0.06);
}

.summary-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--kai);
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.summary-card {
  text-align: center;
  padding: 16px 8px;
  background: var(--paper);
  border: 1px solid var(--hairline);
  border-radius: 10px;
  transition: all 0.3s var(--ease);
}

.summary-card:hover {
  border-color: var(--gold);
  transform: translateY(-2px);
}

.summary-value {
  font-family: var(--kai);
  font-size: 26px;
  font-weight: 700;
  color: var(--vermilion);
  line-height: 1.2;
}

.summary-label {
  margin-top: 4px;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-3);
  letter-spacing: 0.12em;
}

/* 快捷操作：三键横排 */
.completion-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 18px;
}

.completion-btn {
  height: 46px;
  font-family: var(--kai);
  font-size: 14.5px;
  font-weight: 700;
  letter-spacing: 0.08em;
  border-radius: 12px;
  border: 1.5px solid var(--ink);
  background: var(--paper-card);
  color: var(--ink);
  box-shadow: 3px 3px 0 rgba(38, 34, 28, 0.1);
  transition: all 0.3s var(--ease);
}

.completion-btn:hover {
  transform: translate(-1px, -1px);
  border-color: var(--vermilion);
  box-shadow: 4px 4px 0 rgba(196, 64, 47, 0.2);
}

.completion-btn.primary {
  background: var(--vermilion);
  border-color: var(--vermilion);
  color: #fbf6ec;
  box-shadow: 0 4px 0 var(--vermilion-dark);
}

.completion-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 0 var(--vermilion-dark);
}

.export-banner-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.export-banner-icon {
  font-size: 22px;
  color: var(--gold);
}

.export-banner-text {
  display: flex;
  flex-direction: column;
}

.export-banner-title {
  font-family: var(--kai);
  font-size: 14.5px;
  font-weight: 700;
  color: var(--ink);
}

.export-banner-desc {
  font-size: 12px;
  color: var(--ink-3);
}

.export-banner-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

/* ==================== 右侧：SSE 日志面板（羊皮纸浅色，固定不随滚动） ==================== */
.log-panel {
  background: var(--paper-card);
  border: 1.5px solid var(--hairline);
  border-radius: 16px;
  padding: 22px 22px 18px;
  color: var(--ink-2);
  display: flex;
  flex-direction: column;
  min-height: 430px;
  /* grid 子项默认 stretch 会令 sticky 失效，必须收回 stretch */
  align-self: start;
  position: sticky;
  top: 88px;
  /* sticky 面板浮于同栏其他内容之上（互斥后正常不会重叠，此为保险） */
  z-index: 5;
  box-shadow: 4px 4px 0 rgba(38, 34, 28, 0.06);
}

.log-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--hairline);
  margin-bottom: 14px;

  b {
    font-family: var(--font-mono);
    font-size: 12.5px;
    letter-spacing: 0.14em;
    font-weight: 600;
    color: var(--ink);
  }
}

.log-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--jade);
  box-shadow: 0 0 8px var(--jade);
  flex-shrink: 0;
}

.live {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.1em;
  color: var(--ink-3);
}

.live.live-on {
  color: var(--vermilion);
}

.log-body {
  flex: 1;
  overflow: auto;
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.9;
  color: var(--ink-2);
}

.log-empty {
  color: var(--ink-3);
  padding: 12px 0;
}

.ln {
  display: block;
}

.ln .t {
  color: var(--ink-3);
  margin-right: 8px;
}

.ln .msg {
  &.info {
    color: var(--ink);
  }

  &.success {
    color: var(--jade);
  }

  &.error {
    color: var(--vermilion-dark);
  }
}

.ln .ok {
  color: var(--jade);
  margin-left: 6px;
}

@media (prefers-reduced-motion: no-preference) {
  .ln {
    animation: log-fadein 0.4s var(--ease) both;
  }

  @keyframes log-fadein {
    from {
      opacity: 0;
    }
    to {
      opacity: 1;
    }
  }
}

/* ==================== 右侧其他面板 ==================== */
.sidebar-right {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-section {
  background: var(--paper-card);
  border: 1px solid var(--hairline);
  border-radius: 14px;
  padding: 18px 20px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--kai);
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 14px;

  .anticon {
    color: var(--gold);
  }
}

.quota-admin {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quota-badge {
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;

  &.admin {
    background: rgba(196, 64, 47, 0.1);
    color: var(--vermilion);
  }

  &.vip {
    background: rgba(192, 138, 62, 0.14);
    color: var(--gold);
  }
}

.quota-text {
  font-family: var(--kai);
  font-size: 14px;
  color: var(--ink-2);
}

.quota-display {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.quota-number {
  font-family: var(--kai);
  font-size: 34px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.2;

  &.low {
    color: var(--vermilion);
  }

  &.empty {
    color: var(--vermilion-dark);
  }
}

.quota-unit {
  font-size: 13px;
  color: var(--ink-3);
}

.quota-label {
  font-size: 12px;
  color: var(--ink-3);
  letter-spacing: 0.1em;
  margin: 4px 0 10px;
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* 热门选题 chip：虚线胶囊，hover 朱砂 */
.hot-tag {
  font-family: var(--kai);
  font-size: 13px;
  color: var(--ink-2);
  background: var(--paper);
  border: 1.5px dashed var(--hairline);
  border-radius: 999px;
  padding: 6px 14px;
  cursor: pointer;
  transition: all 0.3s var(--ease);

  &:hover {
    border-color: var(--vermilion);
    color: var(--vermilion);
  }
}

.topic-display {
  background: var(--paper);
  border-radius: 10px;
  padding: 12px 16px;

  p {
    margin: 0;
    font-size: 14px;
    color: var(--ink);
    line-height: 1.7;
  }
}

.action-list {
  display: grid;
  gap: 10px;
}

.action-btn {
  border-radius: 10px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
  background: var(--paper);
  border-radius: 10px;
  padding: 14px 8px;
}

.stat-value {
  font-family: var(--kai);
  font-size: 24px;
  font-weight: 700;
  color: var(--ink);
}

.stat-label {
  font-size: 12px;
  color: var(--ink-3);
  letter-spacing: 0.1em;
}

.panel-footer {
  display: flex;
  justify-content: space-between;
  padding: 4px 8px;

  .help-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12.5px;
    color: var(--ink-3);
    cursor: pointer;

    &:hover {
      color: var(--vermilion);
    }
  }
}

/* ==================== 阶段切换过渡 ==================== */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.25s var(--ease), transform 0.25s var(--ease);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ==================== Responsive ==================== */
@media (max-width: 992px) {
  .create-layout {
    grid-template-columns: 1fr;
  }

  .stepper {
    flex-wrap: wrap;
    gap: 12px;
  }

  .log-panel {
    position: static;
    min-height: 280px;
    order: -1;
  }
}
</style>
