<template>
  <div id="userLoginPage">
    <div class="auth-container">
      <!-- 左侧品牌面板（登录注册共用组件，传入登录标语与亮点） -->
      <AuthBrandSection
        title-top="写什么，"
        title-em="活"
        title-bottom="什么"
        :points="brandPoints"
      />

      <!-- 右侧表单区域 -->
      <div class="form-section">
        <div class="form-box">
          <h1 class="form-title">欢迎回来</h1>
          <p class="form-subtitle">笔已蘸好墨，就等你签到了。</p>

          <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit" class="login-form">
            <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input
                v-model:value="formState.userAccount"
                placeholder="你的账号"
                size="large"
                class="form-input"
              >
                <template #prefix>
                  <UserOutlined class="input-icon" />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item
              name="userPassword"
              :rules="[
                { required: true, message: '请输入密码' },
                { min: 8, message: '密码长度不能小于 8 位' },
              ]"
            >
              <a-input-password
                v-model:value="formState.userPassword"
                placeholder="请输入密码"
                size="large"
                class="form-input"
              >
                <template #prefix>
                  <LockOutlined class="input-icon" />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item>
              <a-button type="primary" html-type="submit" size="large" block class="submit-btn">
                执 笔 登 录
              </a-button>
            </a-form-item>

            <!-- 注册入口：次级按钮，显眼 -->
            <a-form-item class="register-item">
              <RouterLink to="/user/register" class="register-link">
                <a-button size="large" block class="register-btn">还 没 有 神 笔 ？ 免 费 开 笔</a-button>
              </RouterLink>
            </a-form-item>
          </a-form>

          <p class="decor">研 墨 · 起 稿 · 落 笔 · 点 睛</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { userLogin } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import AuthBrandSection from '@/components/AuthBrandSection.vue'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

// 左侧品牌面板三条亮点（真实产品能力）
const brandPoints = [
  'AI 六步流水线：标题、大纲、成稿、配图',
  '每一步都等你确认，不满意随时改',
  '从此没有空白页',
]

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserLoginRequest) => {
  const res = await userLogin(values)
  // 登录成功，把登录态保存到全局状态中
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    // 支持登录后跳转到来源页面
    const redirect = route.query.redirect as string
    router.push({
      path: redirect || '/',
      replace: true,
    })
  } else {
    message.error('登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userLoginPage {
  min-height: calc(100vh - 68px);
  background: var(--paper);
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: 0;
}

/* 左右分栏：左 46% 品牌面板 + 右 54% 表单 */
.auth-container {
  display: grid;
  grid-template-columns: 46% 54%;
  width: 100%;
  min-height: 640px;
}

.form-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  background: var(--paper);
}

.form-box {
  width: 100%;
  max-width: 420px;
}

.form-title {
  font-family: var(--kai);
  font-size: 34px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--ink);
}

.form-subtitle {
  color: var(--ink-2);
  font-size: 14.5px;
  margin: 0 0 36px;
}

.login-form {
  margin-bottom: 8px;
}

/* 输入框：卡片底 + 1.5px 墨边 + 硬阴影 */
.form-input {
  border-radius: 12px;
}

.form-input :deep(.ant-input),
.form-input :deep(.ant-input-affix-wrapper) {
  background: var(--paper-card);
  border: 1.5px solid var(--ink);
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 15px;
  color: var(--ink);
  box-shadow: var(--shadow-hard-sm);
  transition: all 0.3s var(--ease);
}

.form-input :deep(.ant-input-affix-wrapper) {
  padding: 8px 16px;
}

.form-input :deep(.ant-input::placeholder) {
  color: var(--ink-3);
}

.form-input :deep(.ant-input-affix-wrapper:hover) {
  border-color: var(--ink);
}

.form-input :deep(.ant-input-affix-wrapper-focused),
.form-input :deep(.ant-input-affix-wrapper:focus-within) {
  border-color: var(--vermilion);
  box-shadow: 3px 3px 0 rgba(196, 64, 47, 0.3);
  transform: translate(-1px, -1px);
}

.input-icon {
  color: var(--ink-3);
  font-size: 16px;
}

/* 浏览器自动填充覆盖：Chrome 会把 autofill 输入框染成淡蓝色，强制回到宣纸卡底色 */
.form-input :deep(.ant-input:-webkit-autofill),
.form-input :deep(.ant-input:-webkit-autofill:hover),
.form-input :deep(.ant-input:-webkit-autofill:focus) {
  -webkit-box-shadow: 0 0 0 1000px #fffdf7 inset;
  -webkit-text-fill-color: var(--ink);
  caret-color: var(--ink);
  border-radius: 12px;
  transition: background-color 99999s ease-in-out 0s;
}

/* 注册次级按钮：墨色描边胶囊，与朱砂主按钮形成层级 */
.register-item {
  margin-bottom: 6px !important;
}

.register-btn {
  height: 46px;
  font-family: var(--kai);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.14em;
  border-radius: 12px !important;
  background: transparent !important;
  border: 1.5px solid var(--ink) !important;
  color: var(--ink) !important;
  box-shadow: none !important;
  transition: all 0.3s var(--ease) !important;
}

.register-btn:hover {
  background: var(--ink) !important;
  color: var(--paper) !important;
  transform: translateY(-1px);
}

/* 朱砂提交按钮：楷体 + 按压感 */
.submit-btn {
  height: 50px;
  font-family: var(--kai);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.2em;
  border-radius: 12px !important;
  background: var(--vermilion) !important;
  border: none !important;
  color: #FBF6EC !important;
  box-shadow: 0 4px 0 var(--vermilion-dark) !important;
  transition: all 0.3s var(--ease) !important;
}

.submit-btn:hover,
.submit-btn:focus {
  background: var(--vermilion-dark) !important;
  color: #FBF6EC !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 0 var(--vermilion-dark) !important;
}

.submit-btn:active {
  transform: translateY(2px);
  box-shadow: 0 2px 0 var(--vermilion-dark) !important;
}

.submit-btn :deep(.ant-wave) {
  display: none;
}

/* 切换链接与底部装饰 */
.switch {
  margin-top: 26px;
  text-align: center;
  font-size: 14px;
  color: var(--ink-2);
}

.switch-link {
  color: var(--vermilion);
  text-decoration: none;
  font-weight: 600;
}

.switch-link:hover {
  color: var(--vermilion-dark);
}

.decor {
  margin-top: 44px;
  padding-top: 24px;
  border-top: 1px dashed var(--hairline);
  text-align: center;
  font-family: var(--font-mono);
  font-size: 11.5px;
  color: var(--ink-3);
  letter-spacing: 0.14em;
}

/* 响应式：窄屏隐藏品牌面板 */
@media (max-width: 860px) {
  .auth-container {
    grid-template-columns: 1fr;
  }
}
</style>
