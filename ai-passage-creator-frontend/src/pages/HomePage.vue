<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { listArticle } from '@/api/articleController'
import dayjs from 'dayjs'
import {
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

const topic = ref('')

const recentArticles = ref<API.ArticleVO[]>([])
const loadingArticles = ref(false)

const prompts = [
  { label: '职场变化', value: '2026年AI如何改变职场' },
  { label: '工作总结', value: '本季度工作总结：哪些事真正产生了结果' },
  { label: '演讲稿', value: '给团队的一次演讲：把复杂的变化讲清楚' },
  { label: '分析报告', value: '一份能直接拿去汇报的竞品分析' },
]

const features = [
  {
    icon: FileTextOutlined,
    index: '01',
    title: '标题由你定',
    description: '先给出几个候选标题。方向没定下来之前，正文不会往下写。',
  },
  {
    icon: OrderedListOutlined,
    index: '02',
    title: '大纲可改',
    description: '结构一边生成一边能改。满意之后，才进入正文。',
  },
  {
    icon: EditOutlined,
    index: '03',
    title: '正文看得见',
    description: '正文按段落流式出现，进度停在页面上，不用猜它写到哪。',
  },
  {
    icon: PictureOutlined,
    index: '04',
    title: '配图后选',
    description: '读完正文再决定怎么配图：图库、生图、图标或示意图。',
  },
  {
    icon: ThunderboltOutlined,
    index: '05',
    title: '步骤拆开跑',
    description: '标题、大纲、正文、配图分开进行，卡住一步不会整篇重来。',
  },
  {
    icon: ClockCircleOutlined,
    index: '06',
    title: '稿件留得住',
    description: '每次创作都留在历史里，可以回来看，也可以接着改。',
  },
]

const goToCreate = () => {
  if (topic.value.trim()) {
    router.push({ path: '/create', query: { topic: topic.value } })
  } else {
    router.push('/create')
  }
}

const usePrompt = (value: string) => {
  topic.value = value
}

const goToList = () => {
  router.push('/article/list')
}

const viewArticle = (article: API.ArticleVO) => {
  router.push(`/article/${article.taskId}`)
}

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

const formatTime = (time: string | undefined) => {
  if (!time) return '--'
  return dayjs(time).format('MM-DD HH:mm')
}

const statusText = (status: string | undefined) => {
  if (status === 'COMPLETED') return '已完成'
  if (status === 'PROCESSING') return '生成中'
  if (status === 'FAILED') return '失败'
  return '等待中'
}

onMounted(() => {
  loadRecentArticles()
})
</script>

<template>
  <div id="homePage">
    <section class="hero-section">
      <div class="container hero-split">
        <div class="hero-copy">
          <p class="hero-kicker">生成、确认、再生成</p>
          <h1 class="hero-title">每一步，<br />都先由你落笔</h1>
          <p class="hero-lede">
            协笔把一篇文章拆成标题、大纲、正文和配图。AI 负责起草，关键处停下来等你确认。
          </p>

          <form class="composer" @submit.prevent="goToCreate">
            <label class="composer-label" for="home-topic">从一句选题开始</label>
            <div class="composer-row">
              <a-input
                id="home-topic"
                v-model:value="topic"
                placeholder="例如：2026年AI如何改变职场"
                size="large"
                class="topic-input"
              />
              <a-button type="primary" size="large" html-type="submit" class="cta-btn">
                开始创作
              </a-button>
            </div>
            <div class="prompt-row">
              <button
                v-for="item in prompts"
                :key="item.label"
                type="button"
                class="prompt-chip"
                @click="usePrompt(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
          </form>
        </div>

        <aside class="manuscript" aria-label="创作流程示意">
          <div class="manuscript-top">
            <span>草稿</span>
            <span class="manuscript-state">等待确认标题</span>
          </div>
          <p class="manuscript-topic">选题 · 2026 年 AI 如何改变职场</p>
          <ol class="manuscript-list">
            <li class="is-current">
              <span>01</span>
              <div>
                <strong>AI 不会取代判断，它在改写分工</strong>
                <em>当前方案</em>
              </div>
            </li>
            <li>
              <span>02</span>
              <div>
                <strong>把重复劳动交出去之后</strong>
              </div>
            </li>
            <li>
              <span>03</span>
              <div>
                <strong>新的能力是把问题问清楚</strong>
              </div>
            </li>
          </ol>
          <p class="manuscript-note">选定一个标题后，才会继续写大纲。</p>
        </aside>
      </div>
    </section>

    <section class="features-section">
      <div class="container">
        <div class="section-header">
          <p class="section-kicker">写作流程</p>
          <h2 class="section-title">产出交给 AI，决定留在你这里</h2>
        </div>
        <div class="features-grid">
          <article v-for="feature in features" :key="feature.index" class="feature-card">
            <div class="feature-index">{{ feature.index }}</div>
            <component :is="feature.icon" class="feature-icon" />
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-description">{{ feature.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section v-if="loginUserStore.loginUser.id && recentArticles.length > 0" class="articles-section">
      <div class="container">
        <div class="section-header-row">
          <div>
            <h2 class="section-title-sm">最近的稿</h2>
            <p class="section-subtitle-sm">点开即可回到那一次创作</p>
          </div>
          <a-button type="link" class="view-all-btn" @click="goToList">
            全部历史
            <RightOutlined />
          </a-button>
        </div>

        <a-spin :spinning="loadingArticles">
          <div class="articles-grid">
            <button
              v-for="article in recentArticles"
              :key="article.id"
              type="button"
              class="article-card"
              @click="viewArticle(article)"
            >
              <div class="article-cover">
                <img v-if="article.coverImage" :src="article.coverImage" :alt="article.mainTitle" />
                <div v-else class="cover-placeholder">
                  <FileTextOutlined />
                </div>
              </div>
              <div class="article-info">
                <h3 class="article-title">{{ article.mainTitle || article.topic }}</h3>
                <div class="article-meta">
                  <span class="article-time">
                    <ClockCircleOutlined />
                    {{ formatTime(article.createTime) }}
                  </span>
                  <span :class="['article-status', `status-${article.status?.toLowerCase()}`]">
                    {{ statusText(article.status) }}
                  </span>
                </div>
              </div>
            </button>
          </div>
        </a-spin>
      </div>
    </section>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  background: var(--color-background);
}

.container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
}

.hero-section {
  padding: 72px 0 80px;
  border-bottom: 1px solid var(--color-border);
}

.hero-split {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 56px;
  align-items: center;
}

.hero-kicker,
.section-kicker {
  margin: 0 0 14px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--color-primary-dark);
}

.hero-title {
  margin: 0 0 16px;
  font-size: clamp(40px, 5vw, 60px);
  line-height: 1.15;
  color: var(--color-text);
}

.hero-lede {
  max-width: 38em;
  margin: 0 0 32px;
  font-size: 17px;
  line-height: 1.75;
  color: var(--color-text-secondary);
}

.composer {
  max-width: 560px;
  padding: 18px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
}

.composer-label {
  display: block;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.composer-row {
  display: flex;
  gap: 10px;
}

.topic-input {
  flex: 1;
}

.cta-btn {
  height: 40px;
  padding: 0 20px;
  white-space: nowrap;
}

.prompt-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.prompt-chip {
  border: 1px solid var(--color-border);
  background: var(--color-background);
  color: var(--color-text-secondary);
  border-radius: var(--radius-full);
  padding: 4px 12px;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.prompt-chip:hover {
  border-color: var(--color-text);
  color: var(--color-text);
}

.manuscript {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: 22px 22px 18px;
  box-shadow: var(--shadow-md);
}

.manuscript-top,
.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.manuscript-top {
  margin-bottom: 18px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--color-text-muted);
}

.manuscript-state {
  color: var(--color-primary-dark);
  letter-spacing: 0;
  font-weight: 600;
}

.manuscript-topic {
  margin: 0 0 16px;
  font-family: var(--font-serif);
  font-size: 18px;
}

.manuscript-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.manuscript-list li {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
}

.manuscript-list li span {
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--color-text-muted);
}

.manuscript-list strong {
  display: block;
  font-weight: 600;
  color: var(--color-text);
}

.manuscript-list em {
  font-style: normal;
  font-size: 12px;
  color: var(--color-primary-dark);
}

.manuscript-list .is-current {
  background: color-mix(in srgb, var(--color-primary) 8%, var(--color-surface));
  outline: 1px solid color-mix(in srgb, var(--color-primary) 28%, transparent);
}

.manuscript-note {
  margin: 14px 0 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.features-section {
  padding: 80px 0;
}

.section-header {
  max-width: 640px;
  margin-bottom: 28px;
}

.section-title {
  margin: 0;
  font-size: clamp(28px, 3vw, 36px);
  line-height: 1.3;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.feature-card {
  padding: 22px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.feature-index {
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--color-primary-dark);
}

.feature-icon {
  display: block;
  margin: 14px 0 12px;
  font-size: 20px;
  color: var(--color-text);
}

.feature-title {
  margin: 0 0 8px;
  font-size: 18px;
}

.feature-description {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.articles-section {
  padding: 0 0 88px;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
}

.section-title-sm {
  margin: 0 0 4px;
  font-size: 28px;
}

.section-subtitle-sm {
  margin: 0;
  color: var(--color-text-secondary);
}

.view-all-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  color: var(--color-text);
  font-weight: 600;
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.article-card {
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 0;
  text-align: left;
  font: inherit;
  color: inherit;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
}

.article-card:hover {
  border-color: var(--color-text);
}

.article-cover {
  height: 148px;
  background: var(--color-background-tertiary);
}

.article-cover img,
.cover-placeholder {
  width: 100%;
  height: 100%;
}

.article-cover img {
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--color-text-muted);
}

.article-info {
  padding: 16px;
}

.article-title {
  margin: 0 0 12px;
  font-size: 17px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.article-status {
  font-size: 12px;
  font-weight: 600;
}

.article-status.status-completed {
  color: var(--color-success);
}

.article-status.status-processing {
  color: var(--color-info);
}

.article-status.status-failed {
  color: var(--color-error);
}

.article-status.status-pending {
  color: var(--color-text-muted);
}

@media (max-width: 992px) {
  .hero-split,
  .features-grid,
  .articles-grid {
    grid-template-columns: 1fr 1fr;
  }

  .hero-split {
    gap: 28px;
  }

  .manuscript {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 40px 0 48px;
  }

  .hero-title {
    font-size: 34px;
  }

  .composer-row,
  .features-grid,
  .articles-grid,
  .section-header-row {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }

  .composer-row {
    gap: 10px;
  }

  .features-grid,
  .articles-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .cta-btn {
    width: 100%;
  }

  .section-header-row {
    gap: 8px;
    align-items: flex-start;
  }
}
</style>
