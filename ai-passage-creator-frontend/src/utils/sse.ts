/**
 * SSE 工具函数
 */
import { API_BASE_URL } from '@/config/env'

export interface SSEMessage {
  type: string
  data?: any
  [key: string]: any
}

export interface SSEOptions {
  onMessage: (message: SSEMessage) => void
  /** 仅在所有重连尝试都失败后触发（真正的断链） */
  onError?: (error: Event) => void
  /** 每次连接断开、等待浏览器自动重连时触发 */
  onReconnecting?: (attempt: number) => void
  /** 连接建立或重连成功时触发 */
  onOpen?: () => void
  onComplete?: () => void
}

/** 最大连续重连次数，超过后判定为断链并交给状态轮询兜底 */
const MAX_RECONNECT_ATTEMPTS = 6

/**
 * 建立 SSE 连接
 * <p>
 * 断线策略：onerror 时**不主动 close**，交由 EventSource 原生机制自动重连
 * （服务端每次重连都会推一条 SNAPSHOT，前端据此恢复到当前阶段）。
 * 只有连续重连超过上限才关闭连接并回调 onError，交给看门狗轮询服务端终态。
 * <p>
 * 注意：客户端主动调 closeSSE 后 readyState 变为 CLOSED，此处不会再重连。
 */
export const connectSSE = (taskId: string, options: SSEOptions): EventSource => {
  const { onMessage, onError, onReconnecting, onOpen, onComplete } = options

  let attempts = 0
  const eventSource = new EventSource(`${API_BASE_URL}/article/progress/${taskId}`)

  eventSource.onopen = () => {
    attempts = 0
    onOpen?.()
  }

  eventSource.onmessage = (event) => {
    try {
      const message: SSEMessage = JSON.parse(event.data)
      onMessage(message)

      // 检查是否完成
      if (message.type === 'ALL_COMPLETE' || message.type === 'ERROR') {
        eventSource.close()
        onComplete?.()
      }
    } catch (error) {
      console.error('SSE 消息解析失败:', error)
    }
  }

  eventSource.onerror = (error) => {
    // 客户端已主动关闭（组件卸载/任务完成）：不重连、不回调
    if (eventSource.readyState === EventSource.CLOSED) {
      return
    }
    attempts += 1
    if (attempts > MAX_RECONNECT_ATTEMPTS) {
      console.error('SSE 重连次数超限，判定为断链:', error)
      eventSource.close()
      onError?.(error)
      return
    }
    // 浏览器会按默认间隔（约 3 秒）自动重连，这里只做提示
    console.warn(`SSE 连接中断，等待第 ${attempts} 次自动重连`)
    onReconnecting?.(attempts)
  }

  return eventSource
}

/**
 * 关闭 SSE 连接
 */
export const closeSSE = (eventSource: EventSource | null) => {
  if (eventSource) {
    eventSource.close()
  }
}
