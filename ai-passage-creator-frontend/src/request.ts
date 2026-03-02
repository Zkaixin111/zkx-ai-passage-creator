import axios, { type AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'
import { API_BASE_URL } from '@/config/env'
import { REQUEST_TIMEOUT, UNAUTHORIZED_CODE } from '@/constants'

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

/**
 * 把网络错误 / 超时转换为统一业务响应
 * 让调用方按业务错误走流程（res.data.code !== 0），
 * 避免未 catch 导致 [Vue warn] Unhandled error during execution of component event handler
 */
const buildFakeResponse = (
  messageText: string,
  config: AxiosResponse['config'] | undefined,
): AxiosResponse => {
  return {
    data: { code: -1, data: null, message: messageText },
    status: 200,
    statusText: 'OK',
    headers: {},
    config: config ?? ({} as AxiosResponse['config']),
  } as unknown as AxiosResponse
}

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    // 未登录
    if (data.code === UNAUTHORIZED_CODE) {
      // 不是获取当前用户信息的请求，并且用户目前不是已经在用户登录页面，则跳转到登录页面
      if (
        !response.request.responseURL.includes('user/current') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
    }
    return response
  },
  function (error) {
    // 网络错误 / 后端未启动：没有 response 说明请求根本没到达服务器
    if (!error.response) {
      const msg = '后端服务连接失败，请确认服务已启动'
      message.error(msg)
      return Promise.resolve(buildFakeResponse(msg, error.config))
    }
    // 请求超时
    if (error.code === 'ECONNABORTED') {
      const msg = '请求超时，请稍后重试'
      message.error(msg)
      return Promise.resolve(buildFakeResponse(msg, error.config))
    }
    // 其它错误（4xx/5xx 等业务异常）→ 仍 reject，由业务页面 catch
    return Promise.reject(error)
  },
)

export default myAxios
