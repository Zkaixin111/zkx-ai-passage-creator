<template>
  <a-layout-header class="header">
    <div class="header-container">
      <div class="header-left">
        <RouterLink to="/" class="logo-link">
          <div class="logo-wrapper">
            <!-- 朱砂印章 logo：竖排「协笔」，纯 CSS 实现 -->
            <div class="seal-logo shenbi-seal vertical">协笔</div>
            <div class="site-title-block">
              <h1 class="site-title">协笔</h1>
              <span class="site-sub">COWRITE</span>
            </div>
          </div>
        </RouterLink>
      </div>

      <!-- 中间：导航菜单 -->
      <nav class="nav-center">
        <RouterLink
          v-for="item in menuItems"
          :key="item.key"
          :to="item.key"
          :class="['nav-item', { active: selectedKeys.includes(item.key) }]"
        >
          <component :is="item.icon" class="nav-icon" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <!-- 右侧：用户操作区域 -->
      <div class="header-right">
        <div v-if="loginUserStore.loginUser.id" class="user-dropdown">
          <!-- VIP 标识 -->
          <RouterLink v-if="!isVip" to="/vip" class="upgrade-vip-btn">
            <CrownOutlined />
            <span>升级 VIP</span>
          </RouterLink>
          <RouterLink v-else to="/vip" class="vip-badge">
            <CrownOutlined />
            <span>VIP</span>
          </RouterLink>

          <a-dropdown>
            <a-space class="user-info">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="36" class="user-avatar" />
              <span class="user-name">
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </span>
            </a-space>
            <template #overlay>
              <a-menu class="dropdown-menu">
                <a-menu-item key="user-center" class="dropdown-item" @click="router.push('/user/center')">
                  <UserOutlined />
                  <span>个人中心</span>
                </a-menu-item>
                <a-menu-item v-if="isVip" key="vip-info" class="vip-info-item" @click="router.push('/vip')">
                  <CrownOutlined />
                  <span>永久会员权益</span>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item @click="doLogout" class="dropdown-item">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else>
          <RouterLink to="/user/login" class="login-btn">登录</RouterLink>
        </div>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import {
  LogoutOutlined,
  HomeOutlined,
  EditOutlined,
  UnorderedListOutlined,
  SettingOutlined,
  CrownOutlined,
  BarChartOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { isVip as checkIsVip } from '@/utils/permission'

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 判断是否为 VIP（管理员也视为 VIP）
const isVip = computed(() => checkIsVip(loginUserStore.loginUser))

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: HomeOutlined,
    label: '首页',
  },
  {
    key: '/create',
    icon: EditOutlined,
    label: '创作',
  },
  {
    key: '/article/list',
    icon: UnorderedListOutlined,
    label: '历史',
  },
  {
    key: '/admin/userManage',
    icon: SettingOutlined,
    label: '管理',
    admin: true,
  },
  {
    key: '/admin/statistics',
    icon: BarChartOutlined,
    label: '数据',
    admin: true,
  },
]

// 过滤菜单项
const menuItems = computed(() => {
  return originItems.filter((item) => {
    if (item.admin) {
      const loginUser = loginUserStore.loginUser
      return loginUser && loginUser.userRole === 'admin'
    }
    return true
  })
})

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
/* 全站浅色宣纸玻璃头（含首页：首页 Hero 也是宣纸风，无需暗色切换） */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(247, 241, 229, 0.88);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  padding: 0;
  height: 68px;
  line-height: 68px;
  border-bottom: 1px solid var(--hairline);
  transition: background var(--transition-fast);
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-link {
  display: block;
  transition: opacity var(--transition-fast);
}

.logo-link:hover {
  opacity: 0.85;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 朱砂印章 logo：40px 方块 + 内描边 + 竖排二字 */
.seal-logo {
  width: 40px;
  height: 40px;
  font-size: 15px;
  line-height: 1.15;
}

.site-title-block {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1.2;
}

.site-title {
  margin: 0;
  font-family: var(--kai);
  font-size: 20px;
  font-weight: 700;
  color: var(--ink);
  white-space: nowrap;
  letter-spacing: 0.04em;
}

.site-sub {
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: var(--ink-3);
  letter-spacing: 0.14em;
}

/* 导航菜单 */
.nav-center {
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: normal;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  line-height: 1.4;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 14.5px;
  font-weight: 500;
  color: var(--ink-2);
  transition: all var(--transition-fast);
  text-decoration: none;
}

.nav-item:hover {
  color: var(--vermilion);
  background: rgba(255, 253, 247, 0.9);
}

.nav-item.active {
  color: var(--vermilion);
  background: rgba(196, 64, 47, 0.08);
  font-weight: 600;
}

.nav-icon {
  font-size: 16px;
}

/* 用户区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-dropdown {
  cursor: pointer;
  height: 68px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.upgrade-vip-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  background: transparent;
  color: var(--vermilion);
  text-decoration: none;
  transition: all var(--transition-fast);

  &:hover {
    background: rgba(196, 64, 47, 0.08);
    color: var(--vermilion-dark);
  }

  .anticon {
    font-size: 13px;
  }
}

.vip-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--gold);
  text-decoration: none;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--vermilion);
  }

  .anticon {
    font-size: 13px;
  }
}

.user-info {
  padding: 6px 12px;
  border-radius: 10px;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
}

.user-info:hover {
  background: rgba(255, 253, 247, 0.9);
}

.user-avatar {
  border: 2px solid var(--hairline);
}

.user-name {
  font-weight: 500;
  color: var(--ink);
  font-size: 14px;
}

.login-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 22px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
  color: var(--ink);
  background: transparent;
  border: 1.5px solid var(--ink);
  box-shadow: none;
  transition: all var(--transition-fast);
  text-decoration: none;
}

.login-btn:hover {
  color: var(--paper);
  background: var(--ink);
}

.dropdown-menu {
  border-radius: 10px;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--hairline);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  transition: all var(--transition-fast);
}

.dropdown-item:hover {
  background: var(--paper-2);
}

.vip-info-item {
  color: var(--vermilion);
  background: rgba(196, 64, 47, 0.08);
  font-weight: 600;
  cursor: default;

  &:hover {
    background: rgba(196, 64, 47, 0.14);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .header-container {
    padding: 0 16px;
  }

  .site-title-block {
    display: none;
  }

  .nav-item span {
    display: none;
  }

  .nav-item {
    padding: 8px 12px;
  }

  .user-name {
    display: none;
  }
}
</style>
