import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCurrentUser } from '@/api/userController.ts'
import { DEFAULT_USERNAME } from '@/constants/user'

/**
 * 登录用户信息
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  // 默认值
  const loginUser = ref<API.LoginUserVO>({
    userName: DEFAULT_USERNAME,
  })

  // 获取登录用户信息
  // 任何异常（后端未启动、网络错误、解析错误）都吞掉，返回 false 让调用方感知
  async function fetchLoginUser(): Promise<boolean> {
    try {
      const res = await getCurrentUser()
      if (res.data.code === 0 && res.data.data) {
        loginUser.value = res.data.data
        return true
      }
    } catch {
      // 静默处理，视为未登录
    }
    return false
  }

  // 更新登录用户信息
  function setLoginUser(newLoginUser: API.LoginUserVO) {
    loginUser.value = newLoginUser
  }

  return { loginUser, fetchLoginUser, setLoginUser }
})
