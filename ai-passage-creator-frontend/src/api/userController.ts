/* eslint-disable */
import request from '@/request'

// ==================== 公开接口 ====================

/**
 * 用户注册
 * POST /user/register
 */
export async function userRegister(
  body: API.UserRegisterRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong>('/user/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/**
 * 用户登录
 * POST /user/login
 */
export async function userLogin(
  body: API.UserLoginRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLoginUserVO>('/user/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

// ==================== 需要登录的接口 ====================

/**
 * 获取当前登录用户信息
 * GET /user/current
 */
export async function getCurrentUser(options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO>('/user/current', {
    method: 'GET',
    ...(options || {}),
  })
}

/**
 * 用户注销
 * POST /user/logout
 */
export async function userLogout(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/user/logout', {
    method: 'POST',
    ...(options || {}),
  })
}

/**
 * 修改密码
 * PUT /user/password
 */
export async function changePassword(
  body: API.UserPasswordRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>('/user/password', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/**
 * 修改个人信息
 * PUT /user/profile
 */
export async function updateProfile(
  body: API.UserUpdateRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>('/user/profile', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

// ==================== 管理员接口 ====================

/**
 * 用户列表查询（管理员）
 * GET /user/list
 */
export async function listUsers(
  params: API.UserQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageUserVO>('/user/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/**
 * 删除用户（管理员）
 * DELETE /user/{id}
 */
export async function deleteUser(id: string, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>(`/user/${id}`, {
    method: 'DELETE',
    ...(options || {}),
  })
}

/**
 * 修改用户信息（管理员）
 * PUT /user/{id}
 */
export async function updateUserByAdmin(
  id: string,
  body: API.UserUpdateByAdminRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>(`/user/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/**
 * 添加用户（管理员）
 * POST /user/add
 */
export async function addUser(
  body: API.UserAddRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong>('/user/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
