<template>
  <div id="userRegisterPage">
    <div class="auth-container">
      <!-- 左侧品牌面板（登录注册共用组件，传入注册标语与真实权益亮点） -->
      <AuthBrandSection
        title-top="新笔开锋，"
        title-em="赠"
        title-bottom="你三篇"
        :points="brandPoints"
      />

      <!-- 右侧表单区域 -->
      <div class="form-section">
        <div class="form-box">
          <h1 class="form-title">开笔</h1>
          <p class="form-subtitle">取个笔名，神笔即刻开锋。</p>

          <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit" class="register-form">
            <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input
                v-model:value="formState.userAccount"
                placeholder="登录用账号"
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
                { min: 8, message: '密码不能小于 8 位' },
              ]"
            >
              <a-input-password
                v-model:value="formState.userPassword"
                placeholder="至少 8 位，含字母与数字"
                size="large"
                class="form-input"
              >
                <template #prefix>
                  <LockOutlined class="input-icon" />
                </template>
              </a-input-password>
            </a-form-item>
            <a-form-item
              name="checkPassword"
              :rules="[
                { required: true, message: '请确认密码' },
                { min: 8, message: '密码不能小于 8 位' },
                { validator: validateCheckPassword },
              ]"
            >
              <a-input-password
                v-model:value="formState.checkPassword"
                placeholder="再输一遍确认密码"
                size="large"
                class="form-input"
              >
                <template #prefix>
                  <SafetyOutlined class="input-icon" />
                </template>
              </a-input-password>
            </a-form-item>

            <!-- 协议勾选 + AI 辅助提示（本次改造新增的展示项） -->
            <a-form-item name="agree" :rules="[{ validator: validateAgree }]">
              <a-checkbox v-model:checked="agreed" class="agree-checkbox">
                我已阅读并同意
                <span class="agree-link">《用户协议》</span>
                与
                <span class="agree-link">《隐私政策》</span>
                ，知悉生成内容由 AI 辅助完成。
              </a-checkbox>
            </a-form-item>

            <a-form-item>
              <a-button type="primary" html-type="submit" size="large" block class="submit-btn">
                开 笔
              </a-button>
            </a-form-item>
          </a-form>

          <p class="switch">已有神笔？<RouterLink to="/user/login" class="switch-link">去登录 →</RouterLink></p>
          <p class="decor">新 笔 开 锋 · 落 笔 生 花</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { reactive, ref } from 'vue'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons-vue'
import { DEFAULT_QUOTA } from '@/constants/user'
import AuthBrandSection from '@/components/AuthBrandSection.vue'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

// 协议勾选（未勾选不可提交，仅前端校验，不影响接口字段）
const agreed = ref(false)

const validateAgree = (_rule: unknown, value: boolean, callback: (error?: Error) => void) => {
  if (!agreed.value) {
    callback(new Error('请先阅读并同意用户协议与隐私政策'))
  } else {
    callback()
  }
}

// 品牌面板亮点：以真实注册赠送额度（DEFAULT_QUOTA）为准生成文案
const brandPoints = [
  `注册即得 ${DEFAULT_QUOTA} 篇免费成稿额度`,
  '全程可干预，写完能署名',
  '三十秒开笔，不用学就会',
]

/**
 * 验证确认密码
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
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
  min-height: 680px;
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
  margin: 0 0 32px;
}

.register-form {
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

/* 协议勾选：朱砂 accent */
.agree-checkbox {
  font-size: 13px;
  color: var(--ink-2);
  white-space: normal;
  line-height: 1.6;
}

.agree-checkbox :deep(.ant-checkbox-inner) {
  border-color: var(--ink-2);
}

.agree-checkbox :deep(.ant-checkbox-checked .ant-checkbox-inner) {
  background: var(--vermilion);
  border-color: var(--vermilion);
}

.agree-link {
  color: var(--vermilion);
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
  margin-top: 40px;
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
