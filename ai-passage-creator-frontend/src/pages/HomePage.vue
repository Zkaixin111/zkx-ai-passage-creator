<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { listArticle } from '@/api/articleController'
import dayjs from 'dayjs'
import {
  FileTextOutlined,
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

// 「四笔成章」步骤文案（对照首页原型：研墨/起稿/落笔/点睛）
const brushSteps = [
  { no: '第一笔', name: '研墨', en: 'TOPIC', desc: '一句话报选题，智能体先和你对齐方向，磨好墨再动笔。' },
  { no: '第二笔', name: '起稿', en: 'OUTLINE', desc: '大纲流式展开，逐级可编辑，你确认之后才进入正文。' },
  { no: '第三笔', name: '落笔', en: 'DRAFT', desc: '正文按段流式输出，写一段你看一段，随时打断重写。' },
  { no: '第四笔', name: '点睛', en: 'IMAGERY', desc: '按段落语义检索真实图源，自动落位，画龙最后点睛。' }
]

// 功能列表数据（宣纸卡 + 印章：保持现有六项真实能力文案）
const features = [
  {
    stamp: '题',
    title: '标题由你定',
    description: 'AI 生成多个候选标题，确认后再继续，选题方向牢牢握在自己手里'
  },
  {
    stamp: '纲',
    title: '大纲可编辑',
    description: '流式生成大纲，支持编辑与 AI 二次修改，结构满意才进入正文'
  },
  {
    stamp: '文',
    title: '正文实时生成',
    description: '打字机式流式输出，创作过程全程可见，随时掌握生成进度'
  },
  {
    stamp: '图',
    title: '配图自己选',
    description: '读完正文后再选择配图方式，Pexels / AI 生图 / 图标 / 表情包等多种来源'
  },
  {
    stamp: '程',
    title: '多智能体协作',
    description: '6 步智能体流水线编排，StateGraph 调度，多图并行获取不卡顿'
  },
  {
    stamp: '返',
    title: '历史管理',
    description: '随时查看和管理所有创作记录，一键回顾每次协同创作过程'
  }
]

// 笔触描边分隔线：滚动进入视口时从 stroke-dashoffset 700 过渡到 0
const brushDividerRef = ref<SVGSVGElement | null>(null)
let brushObserver: IntersectionObserver | null = null

onMounted(() => {
  loadRecentArticles()

  if (brushDividerRef.value && 'IntersectionObserver' in window) {
    brushObserver = new IntersectionObserver(
      (entries, obs) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            brushDividerRef.value?.classList.add('drawn')
            obs.disconnect()
          }
        })
      },
      { threshold: 0.5 }
    )
    brushObserver.observe(brushDividerRef.value)
  } else {
    // 不支持 IntersectionObserver 时直接展示成品墨线
    brushDividerRef.value?.classList.add('drawn')
  }
})

onUnmounted(() => {
  // 清理滚动观察器，避免内存泄漏
  if (brushObserver) {
    brushObserver.disconnect()
    brushObserver = null
  }
})
</script>

<template>
  <div id="homePage">
    <!-- Hero Section：左文右图，宣纸装裱 -->
    <div class="hero">
      <div class="hero-inner">
        <!-- 左栏：文字与输入 -->
        <div class="hero-left">
          <div class="hero-tag">AI 人机协同写作平台</div>
          <h1 class="hero-title">一句话，<em>成</em>一篇文章</h1>
          <p class="hero-subtitle">
            你出选题，AI 生成标题、大纲和成稿；每一步都看得见、可修改，你确认后才进入下一步。
          </p>

          <div class="hero-input">
            <a-input
              v-model:value="topic"
              placeholder="报个选题，例如：AI 时代的产品经理日常"
              size="large"
              class="topic-input"
              @pressEnter="goToCreate"
            />
            <a-button type="primary" size="large" @click="goToCreate" class="btn-brush">
              落笔 →
            </a-button>
          </div>

          <p class="hero-note">NO BLANK PAGE · 从此没有空白页</p>
        </div>

        <!-- 右栏：神笔插画装裱框 -->
        <div class="hero-art">
          <div class="art-frame">
            <img
              src="@/assets/shenbi-hero.webp"
              alt="神笔落墨插画"
              class="art-img"
            />
          </div>
          <div class="art-seal shenbi-seal vertical">协笔</div>
          <!-- 飘浮金墨粒子（纯装饰，reduced-motion 下静止） -->
          <div class="particle p1" aria-hidden="true"></div>
          <div class="particle p2" aria-hidden="true"></div>
          <div class="particle p3" aria-hidden="true"></div>
        </div>
      </div>
    </div>

    <!-- 四笔成章 Section -->
    <div class="steps">
      <div class="container">
        <div class="sec-head">
          <p class="kicker">四 笔 成 章</p>
          <h2 class="sec-title">好文章，就四笔</h2>
          <p class="sec-desc">流程被刻意压到最简，但每一笔落下之前，都等你点头。</p>
        </div>

        <!-- 笔触描边分隔线（滚动入视口时笔画展开） -->
        <svg
          ref="brushDividerRef"
          class="brush-divider"
          viewBox="0 0 560 26"
          aria-hidden="true"
        >
          <path d="M4 18 C 90 4, 150 26, 240 14 S 420 6, 556 16" />
        </svg>

        <div class="step-row">
          <div v-for="step in brushSteps" :key="step.en" class="step">
            <span class="dot" aria-hidden="true"></span>
            <span class="no">{{ step.no }}</span>
            <h3 class="step-name">{{ step.name }}</h3>
            <span class="en">{{ step.en }}</span>
            <p class="step-desc">{{ step.desc }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 笔下有神 Section：宣纸卡 + 印章 -->
    <div class="features">
      <div class="container">
        <div class="sec-head">
          <p class="kicker">笔 下 有 神</p>
          <h2 class="sec-title">不是一键生成，是全程在场</h2>
          <p class="sec-desc">不是丢给你一篇你不敢署名的文章，而是把生成过程摊开。</p>
        </div>
        <div class="feature-grid">
          <div v-for="(feature, index) in features" :key="index" class="paper-card">
            <span class="stamp shenbi-seal">{{ feature.stamp }}</span>
            <span class="num">{{ String(index + 1).padStart(2, '0') }}</span>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-description">{{ feature.description }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 最近创作 Section（真实数据绑定） -->
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

    <!-- 页尾 CTA：卷轴横幅 -->
    <div class="cta">
      <!-- 配图背景层：shenbi-cta.webp 放大 115% 锚定左侧，裁掉右下水印 -->
      <div class="cta-bg" aria-hidden="true"></div>
      <!-- 宣纸渐变遮罩：从左到右由实到透，左侧承载文字 -->
      <div class="cta-mask" aria-hidden="true"></div>
      <div class="container cta-content">
        <h2 class="cta-title">笔已蘸墨，<br />纸已铺好，<em>就差你了</em></h2>
        <p class="cta-subtitle">三步，让初稿自己长出来。</p>
        <a-button type="primary" size="large" @click="goToCreate" class="btn-brush">
          落笔生花 →
        </a-button>
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
  background: transparent;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 32px;
}

/* ==================== Hero：左文右图，宣纸装裱 ==================== */
.hero {
  position: relative;
  overflow: hidden;
  /* 首屏高度撑满视口（扣除顶栏 68px），内容在左半部分垂直居中 */
  min-height: calc(100vh - 68px);
  display: flex;
}

.hero-inner {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: 56px;
  align-items: center;
  width: 100%;
  max-width: 1200px;
  margin: auto;
  padding: 48px 32px;
}

/* 左栏 kicker：朱砂短横线 + 楷体 */
.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-family: var(--kai);
  font-size: 15px;
  color: var(--vermilion);
  margin-bottom: 26px;
  letter-spacing: 0.08em;
}

.hero-tag::before {
  content: '';
  width: 28px;
  height: 2px;
  background: var(--vermilion);
  border-radius: 2px;
}

.hero-title {
  font-family: var(--kai);
  font-weight: 700;
  font-size: 60px;
  line-height: 1.28;
  letter-spacing: 0.02em;
  margin: 0 0 24px;
  color: var(--ink);
}

/* 「活」字朱砂 + 描金底衬条 */
.hero-title em {
  font-style: normal;
  color: var(--vermilion);
  position: relative;
}

.hero-title em::after {
  content: '';
  position: absolute;
  left: -2%;
  right: -2%;
  bottom: 6px;
  height: 10px;
  background: rgba(192, 138, 62, 0.35);
  z-index: -1;
  border-radius: 5px;
}

.hero-subtitle {
  font-size: 17px;
  color: var(--ink-2);
  max-width: 480px;
  margin: 0 0 40px;
}

/* 输入壳：卡片底 + 1.5px 墨边 + 硬阴影，focus 时朱砂阴影 */
.hero-input {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 600px;
  background: var(--paper-card);
  border: 1.5px solid var(--ink);
  border-radius: 14px;
  padding: 8px 8px 8px 22px;
  box-shadow: var(--shadow-hard);
  transition: box-shadow 0.3s var(--ease), transform 0.3s var(--ease);
}

.hero-input:focus-within {
  box-shadow: 4px 4px 0 rgba(196, 64, 47, 0.28);
  transform: translate(-1px, -1px);
}

.topic-input {
  flex: 1;
  border: none !important;
  box-shadow: none !important;
  font-size: 15px;
  background: transparent !important;
  color: var(--ink);
}

.topic-input :deep(.ant-input) {
  background: transparent;
}

.topic-input :deep(.ant-input::placeholder) {
  color: var(--ink-3);
}

.topic-input:focus {
  box-shadow: none !important;
  border: none !important;
}

/* 朱砂「落笔」按钮：楷体 + 描金按压感 */
.btn-brush {
  background: var(--vermilion) !important;
  color: #FBF6EC !important;
  border: none !important;
  font-family: var(--kai);
  font-size: 16.5px !important;
  font-weight: 700 !important;
  letter-spacing: 0.1em;
  border-radius: 10px !important;
  height: 48px !important;
  padding: 0 28px !important;
  white-space: nowrap;
  box-shadow: 0 4px 0 var(--vermilion-dark) !important;
  transition: all 0.3s var(--ease) !important;
}

.btn-brush:hover,
.btn-brush:focus {
  background: var(--vermilion-dark) !important;
  color: #FBF6EC !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 0 var(--vermilion-dark) !important;
}

.btn-brush:active {
  transform: translateY(2px);
  box-shadow: 0 2px 0 var(--vermilion-dark) !important;
}

.hero-note {
  margin-top: 18px;
  font-family: var(--font-mono);
  font-size: 12.5px;
  color: var(--ink-3);
  letter-spacing: 0.05em;
}

/* 右侧插画：宣纸装裱框 + 内 8px 衬边 + 外投影 */
.hero-art {
  position: relative;
}

.art-frame {
  border-radius: 16px;
  overflow: hidden;
  aspect-ratio: 4 / 3.4;
  border: 1px solid var(--hairline);
  box-shadow:
    0 24px 60px rgba(38, 34, 28, 0.16),
    inset 0 0 0 8px rgba(255, 253, 247, 0.9);
  background: var(--paper-card);
}

/* object-position:left center 锚定左侧；容器宽高比 4/3.4(<1.5)
   配合 cover 放大裁切，右下角"AI生成"水印（左缘约在图宽 91.5% 处）不可见 */
.art-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: left center;
  display: block;
}

/* 框外右上角朱砂印章 */
.art-seal {
  position: absolute;
  right: -14px;
  top: -14px;
  width: 64px;
  height: 64px;
  font-size: 17px;
  line-height: 1.2;
  border-radius: 8px;
  transform: rotate(6deg);
  box-shadow:
    inset 0 0 0 2.5px rgba(255, 253, 247, 0.4),
    0 8px 20px rgba(196, 64, 47, 0.3);
}

/* 飘浮金墨粒子（自动播放动画包在 reduced-motion 内） */
.particle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(192, 138, 62, 0.55), rgba(192, 138, 62, 0));
  pointer-events: none;
}

.particle.p1 {
  width: 54px;
  height: 54px;
  left: -18px;
  top: 8%;
}

.particle.p2 {
  width: 34px;
  height: 34px;
  right: 6%;
  bottom: 14%;
}

.particle.p3 {
  width: 20px;
  height: 20px;
  left: 38%;
  top: -10px;
}

@media (prefers-reduced-motion: no-preference) {
  .particle {
    animation: floaty 7s ease-in-out infinite;
  }

  .particle.p2 {
    animation-duration: 9s;
    animation-delay: -3s;
  }

  .particle.p3 {
    animation-duration: 11s;
    animation-delay: -5s;
  }

  @keyframes floaty {
    0%,
    100% {
      transform: translateY(0);
    }
    50% {
      transform: translateY(-26px);
    }
  }
}

/* ==================== 四笔成章：卷轴笔触分隔 ==================== */
.steps {
  padding: 96px 0 40px;
}

.sec-head {
  text-align: left;
  max-width: 560px;
  margin-bottom: 24px;
}

.kicker {
  font-family: var(--kai);
  color: var(--vermilion);
  font-size: 15px;
  letter-spacing: 0.2em;
  margin-bottom: 12px;
}

.sec-title {
  font-family: var(--kai);
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--ink);
  margin: 0;
}

.sec-desc {
  color: var(--ink-2);
  margin-top: 12px;
  font-size: 15.5px;
}

/* 笔触 SVG：默认 dashoffset 700 藏起，.drawn 后 1.6s 展开为成品墨线 */
.brush-divider {
  display: block;
  width: min(560px, 100%);
  height: 26px;
  margin: 8px 0 40px;
}

.brush-divider path {
  stroke: var(--ink);
  stroke-width: 2.5;
  fill: none;
  stroke-linecap: round;
  stroke-dasharray: 700;
  stroke-dashoffset: 700;
}

.brush-divider.drawn path {
  transition: stroke-dashoffset 1.6s var(--ease);
  stroke-dashoffset: 0;
}

/* 四步行：顶部 hairline 实线 + 虚线分隔 + 朱砂圆点压线 */
.step-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  border-top: 1px solid var(--hairline);
  border-bottom: 1px solid var(--hairline);
}

.step {
  padding: 34px 26px 30px;
  border-right: 1px dashed var(--hairline);
  position: relative;
  transition: background 0.3s var(--ease);
}

.step:last-child {
  border-right: none;
}

.step:hover {
  background: rgba(255, 253, 247, 0.9);
}

.step .dot {
  position: absolute;
  top: -7px;
  left: 26px;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: var(--paper);
  border: 3px solid var(--vermilion);
}

.step .no {
  font-family: var(--kai);
  font-size: 14px;
  color: var(--gold);
  letter-spacing: 0.18em;
}

.step-name {
  font-family: var(--kai);
  font-size: 24px;
  font-weight: 700;
  margin: 10px 0 4px;
  color: var(--ink);
}

.step .en {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-3);
  letter-spacing: 0.1em;
  display: block;
  margin-bottom: 12px;
}

.step-desc {
  font-size: 13.5px;
  color: var(--ink-2);
  margin: 0;
}

/* ==================== 笔下有神：宣纸卡 + 印章 ==================== */
.features {
  padding: 72px 0 104px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 22px;
  margin-top: 48px;
}

.paper-card {
  position: relative;
  background: var(--paper-card);
  border: 1px solid var(--hairline);
  border-radius: 14px;
  padding: 30px 26px 26px;
  box-shadow: 0 2px 10px rgba(38, 34, 28, 0.05);
  transition: transform 0.35s var(--ease), box-shadow 0.35s var(--ease);
}

.paper-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 32px rgba(38, 34, 28, 0.1);
}

/* 右上角朱砂印章：hover 反向旋转 + 放大 */
.paper-card .stamp {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 44px;
  height: 44px;
  font-size: 21px;
  border-radius: 7px;
  opacity: 0.92;
  transform: rotate(8deg);
}

.paper-card .num {
  font-family: var(--font-mono);
  font-size: 12.5px;
  color: var(--gold);
  letter-spacing: 0.12em;
}

.feature-title {
  font-family: var(--kai);
  font-size: 19px;
  font-weight: 700;
  margin: 10px 44px 8px 0;
  color: var(--ink);
}

.feature-description {
  font-size: 14px;
  color: var(--ink-2);
  margin: 0;
  line-height: 1.7;
}

@media (prefers-reduced-motion: no-preference) {
  .paper-card .stamp {
    transition: transform 0.45s var(--ease), opacity 0.45s var(--ease);
  }

  .paper-card:hover .stamp {
    transform: rotate(-4deg) scale(1.12);
    opacity: 1;
  }
}

/* ==================== 最近创作 Section ==================== */
.articles-section {
  padding: 64px 0 96px;
  background: var(--paper-2);
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.section-title-sm {
  font-family: var(--kai);
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--ink);
}

.section-subtitle-sm {
  font-size: 14px;
  color: var(--ink-2);
  margin: 0;
}

.view-all-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--vermilion);
  font-weight: 500;
  padding: 0;
}

.view-all-btn:hover {
  color: var(--vermilion-dark);
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.article-card {
  background: var(--paper-card);
  border-radius: 14px;
  border: 1px solid var(--hairline);
  overflow: hidden;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
  cursor: pointer;
}

.article-card:hover {
  border-color: var(--gold);
  box-shadow: 0 8px 22px rgba(38, 34, 28, 0.07);
  transform: translateY(-2px);
}

.article-cover {
  height: 140px;
  background: var(--paper-2);
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
  color: var(--ink-3);
}

.article-info {
  padding: 16px;
}

.article-title {
  font-family: var(--kai);
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--ink);
  line-height: 1.5;
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
  color: var(--ink-3);
  font-family: var(--font-mono);
}

.article-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.article-status.status-completed {
  background: rgba(61, 107, 92, 0.12);
  color: var(--jade);
}

.article-status.status-processing {
  background: rgba(192, 138, 62, 0.12);
  color: var(--gold);
}

.article-status.status-pending,
.article-status.status-failed {
  background: var(--paper-2);
  color: var(--ink-3);
}

/* ==================== 页尾 CTA：卷轴横幅 ==================== */
.cta {
  position: relative;
  overflow: hidden;
  background: var(--paper-2);
}

/* shenbi-cta.webp：115% 放大 + left center 锚定，任何视口下右下水印不可见 */
.cta-bg {
  position: absolute;
  inset: 0;
  background-image: url('@/assets/shenbi-cta.webp');
  background-size: 115% auto;
  background-position: left center;
  background-repeat: no-repeat;
}

/* 从左到右宣纸渐变遮罩：左侧文字区实底，右侧透出卷轴插画 */
.cta-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    rgba(247, 241, 229, 0.96) 0%,
    rgba(247, 241, 229, 0.82) 38%,
    rgba(247, 241, 229, 0.1) 72%
  );
}

.cta-content {
  position: relative;
  z-index: 2;
  padding: 110px 32px;
  max-width: 520px;
}

.cta-title {
  font-family: var(--kai);
  font-size: 42px;
  font-weight: 700;
  line-height: 1.35;
  margin: 0 0 14px;
  color: var(--ink);
}

.cta-title em {
  font-style: normal;
  color: var(--vermilion);
}

.cta-subtitle {
  color: var(--ink-2);
  font-size: 16px;
  margin: 0 0 34px;
}

/* ==================== Responsive ==================== */
@media (max-width: 992px) {
  .hero-inner {
    grid-template-columns: 1fr;
    padding: 56px 0 72px;
  }

  .hero-title {
    font-size: 42px;
  }

  .step-row,
  .feature-grid {
    grid-template-columns: 1fr;
  }

  .step {
    border-right: none;
    border-bottom: 1px dashed var(--hairline);
  }

  .articles-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-input {
    flex-direction: column;
    align-items: stretch;
    padding: 12px;
  }

  .btn-brush {
    width: 100%;
    justify-content: center;
  }

  .articles-grid {
    grid-template-columns: 1fr;
  }

  .section-header-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .cta-bg {
    background-size: 160% auto;
  }

  .cta-content {
    padding: 80px 32px;
  }

  .cta-title {
    font-size: 32px;
  }
}
</style>
