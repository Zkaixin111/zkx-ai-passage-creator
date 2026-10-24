<template>
  <div id="userCenterPage">
    <div class="page-wrap">
      <!-- 问安 + 统计带 -->
      <h1 class="greet">
        {{ loginUserStore.loginUser.userName || '无名墨客' }}，<em>今日也笔耕不辍</em>
      </h1>
      <p class="greet-sub">你落下的每一笔，都替未来的自己省了一次熬夜。</p>

      <!-- 统计带：只展示现有接口真实返回的字段（累计创作篇数 / 剩余额度） -->
      <div class="stats">
        <div class="stat">
          <b>{{ totalArticles }}<i>篇</i></b>
          <span>累计创作</span>
        </div>
        <div class="stat" :class="{ hot: !isAdmin && !isVip }">
          <b>
            <template v-if="isAdmin || isVip">∞<i>不限</i></template>
            <template v-else>{{ quota }}<i>次</i></template>
          </b>
          <span>创作额度余量</span>
        </div>
        <div class="stat">
          <b class="stat-date">{{ joinDate || '--' }}</b>
          <span>执笔至今</span>
        </div>
      </div>

      <!-- 文章架 -->
      <div class="list-head">
        <h2 class="shelf-title">文章架</h2>
        <span class="count">{{ totalArticles }} 篇 · 显示最近 {{ articles.length }} 篇</span>
        <a-button type="primary" @click="router.push('/create')" class="btn-new">＋ 新开一笔</a-button>
      </div>

      <a-spin :spinning="loadingArticles">
        <div v-if="articles.length > 0" class="article-list">
          <div
            v-for="article in articles"
            :key="article.id"
            class="article-row"
            @click="viewArticle(article)"
          >
            <!-- 左侧印章状态标（映射见 constants/fourBrushes.ts 的 STATUS_SEAL_MAP） -->
            <div :class="['a-stamp', `stamp-${getStatusSeal(article.status).variant}`]">
              {{ getStatusSeal(article.status).char }}
            </div>
            <div class="a-body">
              <h3>{{ article.mainTitle || article.topic || '未命名草稿' }}</h3>
              <p>
                {{ formatDate(article.createTime) }} ·
                {{ statusText(article.status) }} ·
                已配图 {{ article.images?.length || 0 }} 张
              </p>
            </div>
            <div class="a-ops" @click.stop>
              <a v-if="article.status === 'FAILED'" @click.stop="viewArticle(article)">查看</a>
              <a v-else @click.stop="viewArticle(article)">继续写</a>
              <a class="danger" @click.stop="confirmDelete(article)">删除</a>
            </div>
          </div>
        </div>
        <div v-else class="empty-shelf">
          架 上 还 空 着 ， 去 写 第 一 笔 吧
        </div>
      </a-spin>

      <!-- 个人资料与安全设置（原有功能保留） -->
      <div class="content-wrapper">
        <a-spin :spinning="loading">
          <div class="content-grid">
            <!-- 左侧：用户信息卡片 -->
            <div class="info-card">
              <div class="avatar-section">
                <div class="avatar-wrapper">
                  <a-avatar
                    :src="loginUserStore.loginUser.userAvatar"
                    :size="96"
                    class="user-avatar"
                  >
                    {{ avatarFallback }}
                  </a-avatar>
                </div>
                <h2 class="user-name">
                  {{ loginUserStore.loginUser.userName || '未命名用户' }}
                </h2>
                <div class="user-meta">
                  <a-tag v-if="isAdmin" color="red" class="role-tag">管理员</a-tag>
                  <a-tag v-else color="gold" class="role-tag">普通用户</a-tag>
                  <span class="account-text">
                    <UserOutlined />
                    {{ loginUserStore.loginUser.userAccount }}
                  </span>
                </div>
                <div v-if="loginUserStore.loginUser.userProfile" class="user-profile">
                  {{ loginUserStore.loginUser.userProfile }}
                </div>
              </div>

              <div class="info-list">
                <div class="info-item">
                  <span class="info-label">账号</span>
                  <span class="info-value">
                    {{ loginUserStore.loginUser.userAccount || '-' }}
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">用户 ID</span>
                  <span class="info-value">
                    {{ loginUserStore.loginUser.id || '-' }}
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">注册时间</span>
                  <span class="info-value">
                    {{ formatTime(loginUserStore.loginUser.createTime) }}
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">最近更新</span>
                  <span class="info-value">
                    {{ formatTime(loginUserStore.loginUser.updateTime) }}
                  </span>
                </div>
              </div>
            </div>

            <!-- 右侧：操作区域 -->
            <div class="action-section">
              <!-- 编辑资料 -->
              <div class="paper-panel">
                <div class="panel-title">
                  <EditOutlined />
                  <span>编辑个人资料</span>
                </div>
                <a-form
                  :model="profileForm"
                  layout="vertical"
                  @finish="handleUpdateProfile"
                >
                  <a-form-item
                    label="用户昵称"
                    name="userName"
                    :rules="[{ required: true, message: '请输入用户昵称' }]"
                  >
                    <a-input
                      v-model:value="profileForm.userName"
                      placeholder="请输入用户昵称"
                      :maxlength="32"
                      show-count
                    />
                  </a-form-item>
                  <a-form-item label="头像 URL" name="userAvatar">
                    <a-input
                      v-model:value="profileForm.userAvatar"
                      placeholder="请输入头像图片链接"
                    />
                  </a-form-item>
                  <a-form-item label="个人简介" name="userProfile">
                    <a-textarea
                      v-model:value="profileForm.userProfile"
                      placeholder="介绍一下自己吧"
                      :rows="3"
                      :maxlength="200"
                      show-count
                    />
                  </a-form-item>
                  <a-form-item>
                    <a-button
                      type="primary"
                      html-type="submit"
                      :loading="profileSubmitting"
                    >
                      保存修改
                    </a-button>
                  </a-form-item>
                </a-form>
              </div>

              <!-- 安全设置 -->
              <div class="paper-panel">
                <div class="panel-title">
                  <SafetyOutlined />
                  <span>账号安全</span>
                </div>
                <div class="security-list">
                  <div class="security-item">
                    <div class="security-info">
                      <LockOutlined class="security-icon" />
                      <div>
                        <div class="security-title">登录密码</div>
                        <div class="security-desc">定期修改密码可以保护账号安全</div>
                      </div>
                    </div>
                    <a-button type="link" @click="router.push('/user/password')">修改</a-button>
                  </div>
                  <div class="security-item">
                    <div class="security-info">
                      <LogoutOutlined class="security-icon" />
                      <div>
                        <div class="security-title">退出登录</div>
                        <div class="security-desc">退出当前账号，需要重新登录</div>
                      </div>
                    </div>
                    <a-popconfirm
                      title="确定要退出登录吗？"
                      ok-text="确定"
                      cancel-text="取消"
                      @confirm="handleLogout"
                    >
                      <a-button type="link" danger>退出</a-button>
                    </a-popconfirm>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </a-spin>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  UserOutlined,
  EditOutlined,
  SafetyOutlined,
  LockOutlined,
  LogoutOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { updateProfile, userLogout } from '@/api/userController.ts'
import { listArticle, deleteArticle } from '@/api/articleController.ts'
import { isAdmin as checkIsAdmin, isVip as checkIsVip } from '@/utils/permission.ts'
import { getStatusSeal } from '@/constants/fourBrushes.ts'
import { STATUS_TEXT_MAP } from '@/constants/article.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const loading = ref(false)
const profileSubmitting = ref(false)

const profileForm = reactive<API.UserUpdateRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

// ==================== 文章架（真实数据） ====================
const articles = ref<API.ArticleVO[]>([])
const totalArticles = ref(0)
const loadingArticles = ref(false)

const isAdmin = computed(() => checkIsAdmin(loginUserStore.loginUser))
const isVip = computed(() => checkIsVip(loginUserStore.loginUser))
const quota = computed(() => loginUserStore.loginUser.quota ?? 0)

// 注册时间（取日期部分用于「执笔至今」统计格）
const joinDate = computed(() => {
  const t = loginUserStore.loginUser.createTime
  return t ? dayjs(t).format('YYYY-MM-DD') : ''
})

// 状态文本
const statusText = (status?: string) => (status ? STATUS_TEXT_MAP[status] ?? status : '未知')

// 加载文章架数据（分页第一页，取 total 作为累计创作数）
const loadShelf = async () => {
  if (!loginUserStore.loginUser.id) return
  loadingArticles.value = true
  try {
    const res = await listArticle({ pageNum: 1, pageSize: 5 })
    articles.value = res.data.data?.records || []
    totalArticles.value = res.data.data?.totalRow || articles.value.length
  } catch (error) {
    console.error('加载文章架失败:', error)
  } finally {
    loadingArticles.value = false
  }
}

// 查看文章（生成中/失败态同样进入现场页）
const viewArticle = (article: API.ArticleVO) => {
  router.push(`/article/${article.taskId}`)
}

// 删除文章（真实删除接口 + 二次确认）
const confirmDelete = (article: API.ArticleVO) => {
  Modal.confirm({
    title: '确定要删除这篇文章吗？',
    content: article.mainTitle || article.topic || '',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deleteArticle({ id: article.id ?? '' })
        if (res.data.code === 0) {
          message.success('删除成功')
          await loadShelf()
        } else {
          message.error('删除失败，' + res.data.message)
        }
      } catch {
        message.error('删除失败，请稍后重试')
      }
    },
  })
}

// 日期格式化（mono 元信息）
const formatDate = (time?: string) => (time ? dayjs(time).format('YYYY-MM-DD') : '--')

// ==================== 个人资料（原有逻辑保留） ====================
// 头像文字回退（取昵称首字）
const avatarFallback = computed(() => {
  const name = loginUserStore.loginUser.userName
  if (!name) return '用'
  return name.charAt(0).toUpperCase()
})

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 初始化表单
const initForm = () => {
  const u = loginUserStore.loginUser
  profileForm.userName = u.userName ?? ''
  profileForm.userAvatar = u.userAvatar ?? ''
  profileForm.userProfile = u.userProfile ?? ''
}

// 保存个人信息
const handleUpdateProfile = async () => {
  profileSubmitting.value = true
  try {
    const res = await updateProfile({
      userName: profileForm.userName,
      userAvatar: profileForm.userAvatar,
      userProfile: profileForm.userProfile,
    })
    if (res.data.code === 0) {
      message.success('保存成功')
      // 刷新登录用户信息
      await loginUserStore.fetchLoginUser()
      initForm()
    } else {
      message.error('保存失败，' + res.data.message)
    }
  } finally {
    profileSubmitting.value = false
  }
}

// 退出登录
const handleLogout = async () => {
  try {
    const res = await userLogout()
    if (res.data.code === 0) {
      loginUserStore.setLoginUser({ userName: '未登录' })
      message.success('退出登录成功')
      router.push('/user/login')
    } else {
      message.error('退出登录失败，' + res.data.message)
    }
  } catch {
    message.error('退出登录失败')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    // 若 store 中没有当前用户信息，先拉取一次
    if (!loginUserStore.loginUser.id) {
      await loginUserStore.fetchLoginUser()
    }
    initForm()
  } finally {
    loading.value = false
  }
  loadShelf()
})
</script>

<style scoped lang="scss">
#userCenterPage {
  background: var(--paper);
  min-height: calc(100vh - 68px);
  padding-bottom: 60px;
}

.page-wrap {
  max-width: 1100px;
  margin: 0 auto;
  padding: 44px 32px 0;
}

/* ==================== 问安 + 统计带 ==================== */
.greet {
  font-family: var(--kai);
  font-size: 30px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--ink);
}

.greet em {
  font-style: normal;
  color: var(--vermilion);
}

.greet-sub {
  color: var(--ink-2);
  font-size: 14px;
  margin: 0 0 30px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-top: 1px solid var(--hairline);
  border-bottom: 1px solid var(--hairline);
  margin-bottom: 44px;
}

.stat {
  padding: 24px 8px;
  text-align: center;
  border-right: 1px dashed var(--hairline);
}

.stat:last-child {
  border-right: none;
}

.stat b {
  font-family: var(--kai);
  font-size: 32px;
  font-weight: 700;
  display: block;
  color: var(--ink);
  line-height: 1.2;
}

.stat b i {
  font-style: normal;
  font-size: 15px;
  color: var(--gold);
  margin-left: 4px;
}

.stat span {
  font-size: 12.5px;
  color: var(--ink-3);
  letter-spacing: 0.1em;
}

.stat.hot b {
  color: var(--vermilion);
}

.stat-date {
  font-family: var(--font-mono) !important;
  font-size: 22px !important;
  padding-top: 8px;
}

/* ==================== 文章架 ==================== */
.list-head {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
}

.shelf-title {
  font-family: var(--kai);
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  color: var(--ink);
}

.count {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--ink-3);
  margin-left: 12px;
}

.btn-new {
  margin-left: auto;
  border-radius: 999px !important;
  font-family: var(--kai);
  font-weight: 700;
  letter-spacing: 0.1em;
  padding: 0 26px !important;
  height: 40px !important;
  box-shadow: 0 4px 0 var(--vermilion-dark) !important;
}

.article-list {
  display: grid;
  gap: 14px;
}

.article-row {
  display: flex;
  align-items: center;
  gap: 22px;
  background: var(--paper-card);
  border: 1px solid var(--hairline);
  border-radius: 14px;
  padding: 20px 26px;
  transition: all 0.3s var(--ease);
  cursor: pointer;
}

.article-row:hover {
  border-color: var(--gold);
  transform: translateX(4px);
  box-shadow: 0 8px 22px rgba(38, 34, 28, 0.07);
}

/* 印章状态标：成/生/稿/败，视觉变体由 STATUS_SEAL_MAP 的 variant 决定 */
.a-stamp {
  width: 52px;
  height: 52px;
  border-radius: 9px;
  flex-shrink: 0;
  font-family: var(--kai);
  font-size: 23px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(6deg);
}

/* 成 = 已成稿：朱砂实心 */
.a-stamp.stamp-done {
  background: var(--vermilion);
  color: #fbf6ec;
  box-shadow: inset 0 0 0 2px rgba(251, 246, 236, 0.4);
}

/* 生 = 生成中：黛绿空心圆 */
.a-stamp.stamp-run {
  background: transparent;
  color: var(--jade);
  box-shadow: inset 0 0 0 2.5px var(--jade);
  border-radius: 50%;
}

/* 稿 = 等待中/草稿：描金空心方块 */
.a-stamp.stamp-draft {
  background: transparent;
  color: var(--gold);
  box-shadow: inset 0 0 0 2.5px var(--gold);
}

/* 败 = 失败：墨色空心方块 */
.a-stamp.stamp-fail {
  background: transparent;
  color: var(--ink-2);
  box-shadow: inset 0 0 0 2.5px var(--ink-2);
}

.a-body {
  flex: 1;
  min-width: 0;
}

.a-body h3 {
  font-family: var(--kai);
  font-size: 18.5px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.a-body p {
  font-size: 12.5px;
  color: var(--ink-3);
  font-family: var(--font-mono);
  letter-spacing: 0.04em;
  margin: 0;
}

.a-ops {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}

.a-ops a {
  font-size: 13.5px;
  color: var(--ink-2);
  text-decoration: none;
  transition: color 0.25s var(--ease);
  cursor: pointer;
}

.a-ops a:hover {
  color: var(--vermilion);
}

.a-ops a.danger:hover {
  color: var(--vermilion-dark);
}

.empty-shelf {
  margin-top: 34px;
  text-align: center;
  font-family: var(--kai);
  font-size: 15px;
  color: var(--ink-3);
  letter-spacing: 0.2em;
  padding: 24px 0 48px;
}

/* ==================== 资料与安全（宣纸面板） ==================== */
.content-wrapper {
  margin-top: 48px;
  padding-bottom: 20px;
}

.content-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
  align-items: start;
}

.info-card {
  background: var(--paper-card);
  border-radius: 14px;
  border: 1px solid var(--hairline);
  box-shadow: var(--shadow-hard-sm);
  padding: 28px 24px;
  position: sticky;
  top: 88px;
}

.avatar-section {
  text-align: center;
}

.avatar-wrapper {
  display: inline-block;
  margin-bottom: 16px;
}

.user-avatar {
  border: 3px solid var(--hairline);
  background: var(--vermilion) !important;
  color: #fbf6ec !important;
  font-size: 36px;
  font-weight: 600;
}

.user-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--ink);
  margin: 0 0 12px;
}

.user-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.role-tag {
  border-radius: var(--radius-full);
  font-weight: 500;
  font-size: 12px;
}

.account-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--ink-2);
  font-family: var(--font-mono);
}

.user-profile {
  margin-top: 16px;
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.6;
  background: var(--paper-2);
  border-radius: 10px;
  padding: 12px 16px;
  text-align: left;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px dashed var(--hairline);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.info-label {
  color: var(--ink-3);
}

.info-value {
  color: var(--ink);
  font-weight: 500;
}

.action-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.paper-panel {
  background: var(--paper-card);
  border: 1px solid var(--hairline);
  border-radius: 14px;
  box-shadow: var(--shadow-hard-sm);
  padding: 24px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--kai);
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--hairline);

  .anticon {
    color: var(--vermilion);
  }
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.security-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.security-icon {
  font-size: 20px;
  color: var(--ink-3);
}

.security-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--ink);
}

.security-desc {
  font-size: 12px;
  color: var(--ink-3);
  margin-top: 2px;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-wrap {
    padding: 32px 20px 0;
  }

  .stats {
    grid-template-columns: 1fr;
  }

  .stat {
    border-right: none;
    border-bottom: 1px dashed var(--hairline);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .info-card {
    position: static;
  }

  .greet {
    font-size: 24px;
  }

  .article-row {
    flex-wrap: wrap;
  }
}
</style>
