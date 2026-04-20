/* eslint-disable */
// @ts-nocheck
/**
 * 支付模块 API（与后端 PaymentController 对齐，openapi2ts 风格）
 */
import request from '@/request'

/**
 * 创建支付订单 POST /pay/create
 */
export async function createPay(
  body: API.PayCreateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePayCreateResponse>('/pay/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
