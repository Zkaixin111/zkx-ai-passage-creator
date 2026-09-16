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

// 功能卡片数据（暖主 + 冷点缀，避免全暖腻味）
const features = [
  {
    icon: FileTextOutlined,
    title: '标题由你定',
    description: 'AI 生成多个候选标题，确认后再继续，选题方向牢牢握在自己手里',
    color: '#B45309'
  },
  {
    icon: OrderedListOutlined,
    title: '大纲可编辑',
    description: '流式生成大纲，支持编辑与 AI 二次修改，结构满意才进入正文',
    color: '#0F766E'
  },
  {
    icon: EditOutlined,
    title: '正文实时生成',
    description: '打字机式流式输出，创作过程全程可见，随时掌握生成进度',
    color: '#D97757'
  },
  {
    icon: PictureOutlined,
    title: '配图自己选',
    description: '读完正文后再选择配图方式，Pexels / AI 生图 / 图标 / 表情包等多种来源',
    color: '#0369A1'
  },
  {
    icon: ThunderboltOutlined,
    title: '多智能体协作',
    description: '6 步智能体流水线编排，StateGraph 调度，多图并行获取不卡顿',
    color: '#C2410C'
  },
  {
    icon: ClockCircleOutlined,
    title: '历史管理',
    description: '随时查看和管理所有创作记录，一键回顾每次协同创作过程',
    color: '#6D28D9'
  }
]

onMounted(() => {
  loadRecentArticles()
})
</script>

<template>
  <div id="homePage">
    <!-- Hero Section（非对称分割：左文右可视化） -->
    <div class="hero-section">
      <div class="aurora-bg"></div>
      <div class="hero-grid" aria-hidden="true"></div>
      <div class="container">
        <div class="hero-split">
          <!-- 左栏：文字与输入 -->
          <div class="hero-left">
            <div class="hero-badge">
              <span class="badge-dot"></span>
              <span>AI 人机协同写作平台</span>
            </div>
            <h1 class="hero-title">
              <span class="title-cn">协笔</span>
              <span class="title-en">CoWrite</span>
            </h1>
            <p class="hero-subtitle">生成、确认、再生成 —— 每一步都由你掌控</p>

            <div class="input-wrapper glow-shell">
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

          <!-- 右栏：创作流程可视化（非对称卡片组） -->
          <div class="hero-right">
            <div class="flow-visual" aria-hidden="true">
              <div class="flow-card flow-card-1">
                <span class="flow-step">01</span>
                <div class="flow-body">
                  <span class="flow-label">生成标题</span>
                  <span class="flow-value">AI 产出 3 个方案</span>
                </div>
              </div>
              <div class="flow-card flow-card-2">
                <span class="flow-step">02</span>
                <div class="flow-body">
                  <span class="flow-label">你确认</span>
                  <span class="flow-value">选定一个继续</span>
                </div>
              </div>
              <div class="flow-card flow-card-3">
                <span class="flow-step">03</span>
                <div class="flow-body">
                  <span class="flow-label">生成大纲</span>
                  <span class="flow-value">流式输出可编辑</span>
                </div>
              </div>
              <div class="flow-card flow-card-4">
                <span class="flow-step">04</span>
                <div class="flow-body">
                  <span class="flow-label">配图合成</span>
                  <span class="flow-value">图文一键成稿</span>
                </div>
              </div>
              <!-- 流程连线装饰 -->
              <div class="flow-line line-1"></div>
              <div class="flow-line line-2"></div>
              <div class="flow-line line-3"></div>
            </div>
          </div>
        </div>
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

/* Hero Section（非对称分割） */
.hero-section {
  position: relative;
  padding: 88px 20px 110px;
  overflow: hidden;
  background: linear-gradient(180deg, #FBF7F2 0%, var(--color-background) 100%);
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

/* 网格背景层（极淡，营造大模型产品氛围） */
.hero-grid {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(180, 83, 9, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(180, 83, 9, 0.05) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(ellipse 80% 60% at 50% 30%, black 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse 80% 60% at 50% 30%, black 30%, transparent 75%);
}

/* 非对称分割容器 */
.hero-split {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 64px;
  align-items: center;
  text-align: left;
}

.hero-left {
  position: relative;
  z-index: 1;
}

.container {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 0 auto;
}

/* 右栏流程可视化 */
.hero-right {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
}

.flow-visual {
  position: relative;
  width: 100%;
  max-width: 420px;
  padding: 24px 0;
}

.flow-card {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  background: linear-gradient(#fff, #fff) padding-box,
              linear-gradient(135deg, rgba(180, 83, 9, 0.35), rgba(217, 119, 87, 0.15)) border-box;
  border: 1.5px solid transparent;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  margin-bottom: 16px;
  transition: transform var(--transition-bounce), box-shadow var(--transition-normal);
  animation: card-slide-in 0.6s ease-out both;
}

.flow-card:hover {
  transform: translateX(6px);
  box-shadow: var(--shadow-glow);
}

.flow-card-1 { animation-delay: 0.1s; }
.flow-card-2 { animation-delay: 0.25s; }
.flow-card-3 { animation-delay: 0.4s; }
.flow-card-4 { animation-delay: 0.55s; }

@keyframes card-slide-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.flow-step {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--gradient-primary);
  color: white;
  font-size: 13px;
  font-weight: 700;
  font-family: 'Outfit', sans-serif;
  box-shadow: 0 4px 12px rgba(180, 83, 9, 0.3);
}

.flow-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.flow-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.flow-value {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 流程连线装饰 */
.flow-line {
  position: absolute;
  left: 39px;
  width: 2px;
  background: linear-gradient(180deg, rgba(180, 83, 9, 0.35), rgba(180, 83, 9, 0.1));
  z-index: 1;
}

.line-1 { top: 56px; height: 20px; }
.line-2 { top: 132px; height: 20px; }
.line-3 { top: 208px; height: 20px; }

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: var(--glass-border);
  box-shadow: var(--shadow-sm);
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 28px;
  color: var(--color-primary-dark);
}

/* badge 呼吸圆点 */
.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  box-shadow: 0 0 0 0 rgba(180, 83, 9, 0.5);
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(180, 83, 9, 0.45);
  }
  70% {
    box-shadow: 0 0 0 8px rgba(180, 83, 9, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(180, 83, 9, 0);
  }
}

.hero-title {
  font-size: clamp(44px, 5.5vw, 68px);
  font-weight: 800;
  margin: 0 0 18px;
  letter-spacing: -2px;
  line-height: 1.05;
  display: flex;
  align-items: baseline;
  justify-content: flex-start;
  gap: 18px;
}

.title-cn {
  color: var(--color-text);
}

.title-en {
  background: var(--gradient-text);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-family: 'Outfit', sans-serif;
  letter-spacing: -3px;
}

.hero-subtitle {
  font-size: 20px;
  margin: 0 0 44px;
  color: var(--color-text-secondary);
  font-weight: 400;
}

/* 核心输入框（发光外壳） */
.input-wrapper {
  display: flex;
  gap: 12px;
  max-width: 100%;
  margin: 0 0 24px;
  padding: 8px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border-radius: var(--radius-2xl);
  border: var(--glass-border);
  transition: box-shadow var(--transition-normal), border-color var(--transition-normal);
}

/* 聚焦时绿色光晕扩散 */
.glow-shell:focus-within {
  border-color: rgba(180, 83, 9, 0.35);
  box-shadow: 0 0 0 4px rgba(180, 83, 9, 0.08), 0 16px 48px -12px rgba(180, 83, 9, 0.35);
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
  box-shadow: 0 0 0 1px rgba(180, 83, 9, 0.2), 0 8px 32px -6px rgba(180, 83, 9, 0.55) !important;
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal), opacity var(--transition-normal) !important;
}

.cta-btn:hover,
.cta-btn:focus,
.cta-btn:active {
  background: var(--gradient-primary) !important;
  border: none !important;
  color: white !important;
  transform: translateY(-2px);
  box-shadow: 0 0 0 1px rgba(180, 83, 9, 0.3), 0 14px 44px -8px rgba(180, 83, 9, 0.7) !important;
  opacity: 0.95;
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
  position: relative;
  padding: 96px 20px;
  background: var(--color-background-secondary);
  overflow: hidden;
}

/* section 顶部细光带分隔 */
.features-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(180, 83, 9, 0.35), transparent);
}

.features-section .container {
  max-width: 1100px;
}

.section-header {
  text-align: center;
  margin-bottom: 52px;
}

.section-badge {
  display: inline-block;
  padding: 6px 16px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: var(--glass-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-dark);
  box-shadow: var(--shadow-sm);
  margin-bottom: 18px;
}

.section-title {
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 800;
  margin: 0 0 14px;
  color: var(--color-text);
  letter-spacing: -1px;
}

.section-subtitle {
  font-size: 17px;
  color: var(--color-text-secondary);
  margin: 0;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-auto-rows: auto;
  gap: 20px;
}

/* 非对称错落：第 1/4 张卡下沉，制造节奏感 */
.feature-card:nth-child(1),
.feature-card:nth-child(4) {
  transform: translateY(12px);
}

.feature-card:nth-child(1):hover,
.feature-card:nth-child(4):hover {
  transform: translateY(6px);
}

.feature-card {
  position: relative;
  overflow: hidden;
  /* 渐变描边卡片 */
  background: linear-gradient(#fff, #fff) padding-box,
              linear-gradient(135deg, rgba(180, 83, 9, 0.4), rgba(217, 119, 87, 0.18)) border-box;
  border: 1.5px solid transparent;
  border-radius: var(--radius-xl);
  padding: 26px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
  transition: transform var(--transition-bounce), box-shadow var(--transition-normal);
  cursor: pointer;
}

/* 卡片右上角光晕（hover 浮现） */
.feature-card::after {
  content: '';
  position: absolute;
  top: -40px;
  right: -40px;
  width: 120px;
  height: 120px;
  border-radius: var(--radius-full);
  background: radial-gradient(circle, rgba(180, 83, 9, 0.16) 0%, transparent 70%);
  opacity: 0;
  transition: opacity var(--transition-normal);
  pointer-events: none;
}

.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-glow);
}

.feature-card:hover::after {
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
  background: rgba(180, 83, 9, 0.1);
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
  background: rgba(180, 83, 9, 0.1);
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
  .hero-split {
    grid-template-columns: 1fr;
    gap: 48px;
    text-align: center;
  }

  .hero-title {
    justify-content: center;
  }

  .hero-left .input-wrapper {
    max-width: 560px;
    margin-left: auto;
    margin-right: auto;
  }

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
