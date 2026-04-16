import { USER_ROLE_ADMIN } from '@/constants/user'

/**
 * 权限判断工具
 */

/** VIP 类型：永久会员 */
export const VIP_TYPE_PERMANENT = 'PERMANENT'

/**
 * 判断用户是否为管理员
 */
export const isAdmin = (user?: API.LoginUserVO): boolean => {
  return user?.userRole === USER_ROLE_ADMIN
}

/**
 * 判断用户是否为 VIP（基于 vipType，管理员视为 VIP）
 */
export const isVip = (user?: API.LoginUserVO): boolean => {
  if (isAdmin(user)) {
    return true
  }
  return user?.vipType === VIP_TYPE_PERMANENT
}

/**
 * 判断用户是否有配额
 */
export const hasQuota = (user?: API.LoginUserVO): boolean => {
  if (isAdmin(user) || isVip(user)) {
    return true
  }
  return (user?.quota ?? 0) > 0
}
