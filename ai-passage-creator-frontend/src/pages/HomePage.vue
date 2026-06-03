<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { listArticle } from '@/api/articleController'
import dayjs from 'dayjs'
import {
  RocketOutlined,
  FileTextOutlined,
  OrderedListOutlined,
  EditOutlined,
  PictureOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  RightOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 输入框
const topic = ref('')

// 最近文章
const recentArticles = ref<API.ArticleVO[]>([])
const loadingArticles = ref(false)

const goToCreate = () => {
  if (topic.value.trim()) {
    router.push({ path: '/create', query: { topic: topic.value } })
  } else {
    router.push('/create')
  }
}

const goToList = () => {
  router.push('/article/list')
}

const viewArticle = (article: API.ArticleVO) => {
  router.push(`/article/${article.taskId}`)
}

// 加载最近文章
const loadRecentArticles = async () => {
  if (!loginUserStore.loginUser.id) return

  loadingArticles.value = true
  try {
    const res = await listArticle({ pageNum: 1, pageSize: 6 })
    recentArticles.value = res.data.data?.records || []
  } catch (error) {
    console.error('加载文章失败:', error)
  } finally {
    loadingArticles.value = false
  }
}

// 格式化时间
const formatTime = (time: string | undefined) => {
  if (!time) return '--'
  return dayjs(time).format('MM-DD HH:mm')
}

// 功能卡片数据
const features = [
  {
    icon: FileTextOutlined,
    title: '标题由你定',
    description: 'AI 生成多个候选标题，确认后再继续，选题方向牢牢握在自己手里',
    color: '#22C55E'
  },
  {
    icon: OrderedListOutlined,
    title: '大纲可编辑',
    description: '流式生成大纲，支持编辑与 AI 二次修改，结构满意才进入正文',
    color: '#3B82F6'
  },
  {
    icon: EditOutlined,
    title: '正文实时生成',
    description: '打字机式流式输出，创作过程全程可见，随时掌握生成进度',
    color: '#8B5CF6'
  },
  {
    icon: PictureOutlined,
    title: '配图自己选',
    description: '读完正文后再选择配图方式，Pexels / AI 生图 / 图标 / 表情包等多种来源',
    color: '#F59E0B'
  },
  {
    icon: ThunderboltOutlined,
    title: '多智能体协作',
    description: '6 步智能体流水线编排，StateGraph 调度，多图并行获取不卡顿',
    color: '#EF4444'
  },
  {
    icon: ClockCircleOutlined,
    title: '历史管理',
    description: '随时查看和管理所有创作记录，一键回顾每次协同创作过程',
    color: '#06B6D4'
  }
]

onMounted(() => {
  loadRecentArticles()
})
</script>

<template>
  <div id="homePage">
    <!-- Hero Section -->
    <div class="hero-section">
      <div class="hero-bg"></div>
      <!-- 漂浮光斑装饰 -->
      <div class="orb orb-1" aria-hidden="true"></div>
      <div class="orb orb-2" aria-hidden="true"></div>
      <div class="orb orb-3" aria-hidden="true"></div>
      <div class="container">
        <div class="hero-badge">
          <ThunderboltOutlined />
          <span>AI 人机协同写作平台</span>
        </div>
        <h1 class="hero-title">协笔 <span class="gradient-text">CoWrite</span></h1>
        <p class="hero-subtitle">生成、确认、再生成 —— 每一步都由你掌控</p>

        <!-- 核心输入框 -->
        <div class="input-wrapper">
          <a-input
            v-model:value="topic"
            placeholder="输入您想创作的文章选题，例如：2026年AI如何改变职场"
            size="large"
            class="topic-input"
            @pressEnter="goToCreate"
          >
            <template #prefix>
              <EditOutlined class="input-icon" />
            </template>
          </a-input>
          <a-button type="primary" size="large" @click="goToCreate" class="cta-btn">
            <RocketOutlined />
            开始创作
          </a-button>
        </div>

        <p class="hero-tips">工作总结、心得体会、演讲稿、分析报告... 标题大纲自己拍板，AI 全力辅助</p>
      </div>
    </div>

    <!-- Features Section -->
    <div class="features-section">
      <div class="container">
        <div class="section-header">
          <div class="section-badge">核心能力</div>
          <h2 class="section-title">AI 负责产出，你负责决策</h2>
          <p class="section-subtitle">在关键决策点暂停确认，杜绝不可控的"一键生成"</p>
        </div>
        <div class="features-grid">
          <div
            v-for="(feature, index) in features"
            :key="index"
            class="feature-card"
          >
            <div class="feature-icon-wrapper" :style="{ background: `${feature.color}15` }">
              <component :is="feature.icon" class="feature-icon" :style="{ color: feature.color }" />
            </div>
            <div class="feature-content">
              <h3 class="feature-title">{{ feature.title }}</h3>
              <p class="feature-description">{{ feature.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Articles Section -->
    <div v-if="loginUserStore.loginUser.id && recentArticles.length > 0" class="articles-section">
      <div class="container">
        <div class="section-header-row">
          <div>
            <h2 class="section-title-sm">最近创作</h2>
            <p class="section-subtitle-sm">查看您最近创作的文章</p>
          </div>
          <a-button type="link" @click="goToList" class="view-all-btn">
            查看全部
            <RightOutlined />
          </a-button>
        </div>

        <a-spin :spinning="loadingArticles">
          <div class="articles-grid">
            <div
              v-for="article in recentArticles"
              :key="article.id"
              class="article-card"
              @click="viewArticle(article)"
            >
              <div class="article-cover">
                <img
                  v-if="article.coverImage"
                  :src="article.coverImage"
                  :alt="article.mainTitle"
                />
                <div v-else class="cover-placeholder">
                  <FileTextOutlined />
                </div>
              </div>
              <div class="article-info">
                <h4 class="article-title">{{ article.mainTitle || article.topic }}</h4>
                <div class="article-meta">
                  <span class="article-time">
                    <ClockCircleOutlined />
                    {{ formatTime(article.createTime) }}
                  </span>
                  <span :class="['article-status', `status-${article.status?.toLowerCase()}`]">
                    {{ article.status === 'COMPLETED' ? '已完成' : article.status === 'PROCESSING' ? '生成中' : '等待中' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </a-spin>
      </div>
    </div>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  background: var(--color-background);
}

/* Hero Section */
.hero-section {
  position: relative;
  padding: 80px 20px 100px;
  text-align: center;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--gradient-hero);
  z-index: 0;
}

/* 漂浮光斑 */
.orb {
  position: absolute;
  border-radius: var(--radius-full);
  filter: blur(60px);
  opacity: 0.55;
  z-index: 0;
  pointer-events: none;
  animation: orb-float 12s ease-in-out infinite;
}

.orb-1 {
  width: 320px;
  height: 320px;
  top: -60px;
  left: -80px;
  background: rgba(74, 222, 128, 0.5);
}

.orb-2 {
  width: 240px;
  height: 240px;
  top: 40px;
  right: -60px;
  background: rgba(34, 197, 94, 0.4);
  animation-delay: -4s;
}

.orb-3 {
  width: 180px;
  height: 180px;
  bottom: -40px;
  left: 40%;
  background: rgba(163, 230, 53, 0.35);
  animation-delay: -8s;
}

@keyframes orb-float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-24px) scale(1.06);
  }
}

.container {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 0 auto;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.2);
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 24px;
  color: var(--color-primary-dark);
}

.hero-title {
  font-size: 52px;
  font-weight: 700;
  margin: 0 0 16px;
  letter-spacing: -1.5px;
  line-height: 1.1;
  color: var(--color-text);
  background: linear-gradient(135deg, var(--color-primary-dark) 0%, var(--color-primary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 20px;
  margin: 0 0 40px;
  color: var(--color-text-secondary);
  font-weight: 400;
}

/* 核心输入框 */
.input-wrapper {
  display: flex;
  gap: 12px;
  max-width: 700px;
  margin: 0 auto 20px;
  padding: 8px;
  background: white;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
}

.topic-input {
  flex: 1;
  border: none !important;
  box-shadow: none !important;
  font-size: 16px;
  padding: 8px 16px;
  background: transparent !important;
}

.topic-input:focus {
  box-shadow: none !important;
}

.input-icon {
  color: var(--color-text-muted);
  font-size: 18px;
}

.cta-btn {
  height: 52px !important;
  padding: 0 32px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  border-radius: var(--radius-lg) !important;
  background: var(--gradient-primary) !important;
  border: none !important;
  color: white !important;
  box-shadow: var(--shadow-green) !important;
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  transition: opacity var(--transition-normal) !important;
}

.cta-btn:hover,
.cta-btn:focus,
.cta-btn:active {
  background: var(--gradient-primary) !important;
  border: none !important;
  color: white !important;
  box-shadow: var(--shadow-green) !important;
  opacity: 0.92;
}

.cta-btn :deep(.ant-wave) {
  display: none;
}

.hero-tips {
  font-size: 14px;
  color: var(--color-text-muted);
  margin: 0;
}

/* Features Section */
.features-section {
  padding: 80px 20px;
  background: var(--color-background-secondary);
}

.features-section .container {
  max-width: 1100px;
}

.section-header {
  text-align: center;
  margin-bottom: 48px;
}

.section-badge {
  display: inline-block;
  padding: 6px 14px;
  background: rgba(34, 197, 94, 0.1);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-dark);
  margin-bottom: 16px;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--color-text);
  letter-spacing: -0.5px;
}

.section-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  position: relative;
  overflow: hidden;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: var(--glass-border);
  border-radius: var(--radius-xl);
  padding: 24px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
  transition: transform var(--transition-bounce), box-shadow var(--transition-normal), border-color var(--transition-normal);
  cursor: pointer;
}

/* 卡片顶部渐变光条 */
.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--gradient-primary);
  opacity: 0;
  transition: opacity var(--transition-normal);
}

.feature-card:hover {
  transform: translateY(-5px);
  border-color: rgba(34, 197, 94, 0.35);
  box-shadow: var(--shadow-glow);
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-icon-wrapper {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  transition: background var(--transition-normal);
}

.feature-card:hover .feature-icon-wrapper {
  background: rgba(34, 197, 94, 0.1);
}

.feature-icon {
  font-size: 22px;
  transition: transform var(--transition-bounce);
}

.feature-card:hover .feature-icon {
  transform: scale(1.15) rotate(-5deg);
}

.feature-content {
  flex: 1;
  min-width: 0;
}

.feature-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 6px;
  color: var(--color-text);
}

.feature-description {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.5;
}

/* Articles Section */
.articles-section {
  padding: 60px 20px 80px;
  background: var(--color-background);
}

.articles-section .container {
  max-width: 1100px;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.section-title-sm {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--color-text);
}

.section-subtitle-sm {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.view-all-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-primary);
  font-weight: 500;
  padding: 0;
}

.view-all-btn:hover {
  color: var(--color-primary-dark);
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.article-card {
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  overflow: hidden;
  transition: all var(--transition-normal);
  cursor: pointer;
}

.article-card:hover {
  border-color: var(--color-primary-light);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.article-cover {
  height: 140px;
  background: var(--color-background-tertiary);
  overflow: hidden;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: var(--color-text-muted);
}

.article-info {
  padding: 16px;
}

.article-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 12px;
  color: var(--color-text);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.article-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.article-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.article-status.status-completed {
  background: rgba(34, 197, 94, 0.1);
  color: var(--color-primary-dark);
}

.article-status.status-processing {
  background: rgba(59, 130, 246, 0.1);
  color: #2563EB;
}

.article-status.status-pending {
  background: var(--color-background-tertiary);
  color: var(--color-text-muted);
}

/* Responsive */
@media (max-width: 992px) {
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .articles-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 60px 20px 80px;
  }

  .hero-title {
    font-size: 36px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .input-wrapper {
    flex-direction: column;
    padding: 12px;
  }

  .cta-btn {
    width: 100%;
    justify-content: center;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .articles-grid {
    grid-template-columns: 1fr;
  }

  .section-title {
    font-size: 24px;
  }

  .section-header-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
