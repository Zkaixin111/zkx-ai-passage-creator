<template>
  <div id="userLoginPage">
    <div class="auth-container">
      <AuthBrandSection />
      
      <!-- 右侧表单区域 -->
      <div class="form-section">
        <div class="form-card">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">登录您的账号继续创作</p>
          
          <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit" class="login-form">
            <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input 
                v-model:value="formState.userAccount" 
                placeholder="请输入账号" 
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
                登录
              </a-button>
            </a-form-item>
          </a-form>
          
          <div class="form-footer">
            <span class="footer-text">还没有账号？</span>
            <RouterLink to="/user/register" class="register-link">立即注册</RouterLink>
          </div>
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
  min-height: calc(100vh - 64px);
  background: var(--color-background-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.auth-container {
  display: flex;
  width: 100%;
  max-width: 900px;
  min-height: 520px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

/* 右侧表单区域 */
.form-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: var(--color-surface);
}

.form-card {
  width: 100%;
  max-width: 320px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 32px;
}

.login-form {
  margin-bottom: 24px;
}

.form-input {
  border-radius: var(--radius-lg);
  border-color: var(--color-border);
  transition: all var(--transition-fast);
}

.form-input:hover {
  border-color: var(--color-primary-light);
}

.form-input:focus,
.form-input:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.form-input :deep(.ant-input) {
  padding: 12px 14px;
}

.input-icon {
  color: var(--color-text-muted);
  font-size: 16px;
}

.submit-btn {
  height: 48px;
  font-size: 16px;
  border-radius: var(--radius-lg);
}

.form-footer {
  text-align: center;
}

.footer-text {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.register-link {
  color: var(--color-primary);
  font-weight: 600;
  margin-left: 4px;
  transition: color var(--transition-fast);
}

.register-link:hover {
  color: var(--color-primary-dark);
}

/* 响应式 */
@media (max-width: 768px) {
  .auth-container {
    flex-direction: column;
    min-height: auto;
    border-radius: var(--radius-xl);
  }
  
  .brand-section {
    padding: 32px 24px;
  }
  
  .brand-title {
    font-size: 22px;
  }
  
  .brand-features {
    display: none;
  }
  
  .form-section {
    padding: 32px 24px;
  }

  .form-title {
    font-size: 22px;
  }
}
</style>
