<template>
  <div id="userPasswordPage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-container">
        <div class="header-content">
          <h1 class="page-title">修改密码</h1>
          <p class="page-subtitle">为账号安全，请定期修改登录密码</p>
        </div>
      </div>
    </div>

    <div class="container">
      <a-card :bordered="false" class="password-card">
        <!-- 安全提示 -->
        <a-alert
          class="security-tip"
          type="info"
          show-icon
          message="密码安全建议"
          description="密码长度至少 8 位，建议包含字母、数字和特殊符号，不要使用与账号相同的弱密码。"
        />

        <a-form
          :model="formState"
          layout="vertical"
          class="password-form"
          @finish="handleSubmit"
        >
          <a-form-item
            label="当前密码"
            name="oldPassword"
            :rules="[
              { required: true, message: '请输入当前密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.oldPassword"
              placeholder="请输入当前密码"
              size="large"
            >
              <template #prefix>
                <LockOutlined class="input-icon" />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item
            label="新密码"
            name="newPassword"
            :rules="[
              { required: true, message: '请输入新密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.newPassword"
              placeholder="请输入新密码"
              size="large"
            >
              <template #prefix>
                <KeyOutlined class="input-icon" />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item
            label="确认新密码"
            name="checkNewPassword"
            :rules="[
              { required: true, message: '请再次输入新密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
              { validator: validateCheckPassword },
            ]"
          >
            <a-input-password
              v-model:value="formState.checkNewPassword"
              placeholder="请再次输入新密码"
              size="large"
            >
              <template #prefix>
                <SafetyOutlined class="input-icon" />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                html-type="submit"
                :loading="submitting"
              >
                确认修改
              </a-button>
              <a-button @click="router.back()">返回</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  LockOutlined,
  KeyOutlined,
  SafetyOutlined,
} from '@ant-design/icons-vue'
import { changePassword } from '@/api/userController.ts'

const router = useRouter()
const submitting = ref(false)

const formState = reactive<API.UserPasswordRequest>({
  oldPassword: '',
  newPassword: '',
  checkNewPassword: '',
})

/**
 * 校验两次新密码是否一致
 */
const validateCheckPassword = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void,
) => {
  if (value && value !== formState.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交修改密码
 */
const handleSubmit = async () => {
  submitting.value = true
  try {
    const res = await changePassword({
      oldPassword: formState.oldPassword,
      newPassword: formState.newPassword,
      checkNewPassword: formState.checkNewPassword,
    })
    if (res.data.code === 0) {
      message.success('密码修改成功，请重新登录')
      // 密码修改后通常需要重新登录
      router.push('/user/login')
    } else {
      message.error('修改失败，' + res.data.message)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
#userPasswordPage {
  background: var(--color-background-secondary);
  min-height: calc(100vh - 64px);
  padding-bottom: 60px;

  .page-header {
    background: var(--gradient-hero);
    padding: 32px 20px;
    margin-bottom: 24px;
  }

  .header-container {
    max-width: 700px;
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
    max-width: 700px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .password-card {
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    box-shadow: none;
    background: white;

    :deep(.ant-card-body) {
      padding: 32px;
    }
  }

  .security-tip {
    margin-bottom: 24px;
    border-radius: var(--radius-md);
  }

  .password-form {
    max-width: 100%;
  }

  .input-icon {
    color: var(--color-text-muted);
    font-size: 16px;
  }
}

@media (max-width: 768px) {
  #userPasswordPage {
    .page-title {
      font-size: 22px;
    }

    .password-card :deep(.ant-card-body) {
      padding: 20px;
    }
  }
}
</style>
