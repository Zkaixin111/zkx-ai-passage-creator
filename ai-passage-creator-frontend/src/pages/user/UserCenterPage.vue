<template>
  <div id="userCenterPage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-container">
        <div class="header-content">
          <h1 class="page-title">个人中心</h1>
          <p class="page-subtitle">管理您的账号信息与个人资料</p>
        </div>
      </div>
    </div>

    <div class="container">
      <a-spin :spinning="loading">
        <div class="content-wrapper">
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
                <a-tag v-if="isAdmin" color="purple" class="role-tag">
                  管理员
                </a-tag>
                <a-tag v-else color="blue" class="role-tag">
                  普通用户
                </a-tag>
                <span class="account-text">
                  <UserOutlined />
                  {{ loginUserStore.loginUser.userAccount }}
                </span>
              </div>
              <div v-if="loginUserStore.loginUser.userProfile" class="user-profile">
                {{ loginUserStore.loginUser.userProfile }}
              </div>
            </div>

            <a-divider />

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
            <a-card :bordered="false" class="action-card">
              <template #title>
                <div class="card-title">
                  <EditOutlined />
                  <span>编辑个人资料</span>
                </div>
              </template>
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
            </a-card>

            <!-- 安全设置 -->
            <a-card :bordered="false" class="action-card">
              <template #title>
                <div class="card-title">
                  <SafetyOutlined />
                  <span>账号安全</span>
                </div>
              </template>
              <div class="security-list">
                <div class="security-item">
                  <div class="security-info">
                    <LockOutlined class="security-icon" />
                    <div>
                      <div class="security-title">登录密码</div>
                      <div class="security-desc">
                        定期修改密码可以保护账号安全
                      </div>
                    </div>
                  </div>
                  <a-button type="link" @click="router.push('/user/password')">
                    修改
                  </a-button>
                </div>
                <a-divider style="margin: 12px 0" />
                <div class="security-item">
                  <div class="security-info">
                    <LogoutOutlined class="security-icon" />
                    <div>
                      <div class="security-title">退出登录</div>
                      <div class="security-desc">
                        退出当前账号，需要重新登录
                      </div>
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
            </a-card>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
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
import { isAdmin as checkIsAdmin } from '@/utils/permission.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const loading = ref(false)
const profileSubmitting = ref(false)

const profileForm = reactive<API.UserUpdateRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

// 头像文字回退（取昵称首字）
const avatarFallback = computed(() => {
  const name = loginUserStore.loginUser.userName
  if (!name) return '用'
  return name.charAt(0).toUpperCase()
})

const isAdmin = computed(() => checkIsAdmin(loginUserStore.loginUser))

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
})
</script>

<style scoped lang="scss">
#userCenterPage {
  background: var(--color-background-secondary);
  min-height: calc(100vh - 64px);
  padding-bottom: 60px;

  .page-header {
    background: var(--gradient-hero);
    padding: 32px 20px;
    margin-bottom: 24px;
  }

  .header-container {
    max-width: 1100px;
    margin: 0 auto;
  }

  .page-title {
    font-size: 28px;
    font-weight: 700;
    margin: 0 0 6px;
    letter-spacing: -0.5px;
    color: var(--color-text);
  }

  .page-subtitle {
    font-size: 14px;
    color: var(--color-text-secondary);
    margin: 0;
  }

  .container {
    max-width: 1100px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .content-wrapper {
    display: grid;
    grid-template-columns: 320px 1fr;
    gap: 24px;
    align-items: start;
  }

  /* 左侧信息卡片 */
  .info-card {
    background: white;
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    padding: 28px 24px;
    position: sticky;
    top: 80px;
  }

  .avatar-section {
    text-align: center;
  }

  .avatar-wrapper {
    display: inline-block;
    margin-bottom: 16px;
  }

  .user-avatar {
    border: 3px solid var(--color-border);
    background: var(--gradient-primary) !important;
    color: white !important;
    font-size: 36px;
    font-weight: 600;
  }

  .user-name {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-text);
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
    color: var(--color-text-secondary);
  }

  .user-profile {
    margin-top: 16px;
    font-size: 13px;
    color: var(--color-text-secondary);
    line-height: 1.6;
    background: var(--color-background-secondary);
    border-radius: var(--radius-md);
    padding: 12px 16px;
    text-align: left;
  }

  .info-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .info-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
  }

  .info-label {
    color: var(--color-text-muted);
  }

  .info-value {
    color: var(--color-text);
    font-weight: 500;
  }

  /* 右侧操作区 */
  .action-section {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .action-card {
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    box-shadow: none;
    background: white;

    :deep(.ant-card-body) {
      padding: 24px;
    }

    :deep(.ant-card-head) {
      border-bottom: 1px solid var(--color-border-light);
      min-height: auto;
      padding: 16px 24px;
    }
  }

  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text);
  }

  .security-list {
    display: flex;
    flex-direction: column;
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
    color: var(--color-text-muted);
  }

  .security-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text);
  }

  .security-desc {
    font-size: 12px;
    color: var(--color-text-muted);
    margin-top: 2px;
  }
}

/* 响应式 */
@media (max-width: 768px) {
  #userCenterPage {
    .content-wrapper {
      grid-template-columns: 1fr;
    }

    .info-card {
      position: static;
    }

    .page-title {
      font-size: 22px;
    }
  }
}
</style>
