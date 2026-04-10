declare namespace API {
  type AgentExecutionStats = {
    taskId?: string
    totalDurationMs?: number
    agentCount?: number
    agentDurations?: Record<string, any>
    overallStatus?: string
    logs?: AgentLog[]
  }

  type AgentLog = {
    id?: string
    taskId?: string
    agentName?: string
    startTime?: string
    endTime?: string
    durationMs?: number
    status?: string
    errorMessage?: string
    prompt?: string
    inputData?: string
    outputData?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ArticleAiModifyOutlineRequest = {
    taskId?: string
    modifySuggestion?: string
  }

  type ArticleConfirmOutlineRequest = {
    taskId?: string
    outline?: OutlineSection[]
  }

  type ArticleConfirmContentRequest = {
    taskId?: string
    generateImages?: boolean
    enabledImageMethods?: string[]
  }

  type ArticleConfirmTitleRequest = {
    taskId?: string
    selectedMainTitle?: string
    selectedSubTitle?: string
    userDescription?: string
  }

  type ArticleCreateRequest = {
    topic?: string
    style?: string
    enabledImageMethods?: string[]
  }

  type ArticleQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    userId?: number
    status?: string
  }

  type ArticleVO = {
    id?: string
    taskId?: string
    userId?: number
    topic?: string
    userDescription?: string
    mainTitle?: string
    subTitle?: string
    titleOptions?: TitleOption[]
    outline?: OutlineItem[]
    content?: string
    fullContent?: string
    coverImage?: string
    images?: ImageItem[]
    status?: string
    phase?: string
    errorMessage?: string
    createTime?: string
    completedTime?: string
  }

  type BaseResponseAgentExecutionStats = {
    code?: number
    data?: AgentExecutionStats
    message?: string
  }

  type BaseResponseArticleVO = {
    code?: number
    data?: ArticleVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseListOutlineSection = {
    code?: number
    data?: OutlineSection[]
    message?: string
  }

  type BaseResponseListPaymentRecord = {
    code?: number
    data?: PaymentRecord[]
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageArticleVO = {
    code?: number
    data?: PageArticleVO
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseStatisticsVO = {
    code?: number
    data?: StatisticsVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type BaseResponseVoid = {
    code?: number
    data?: Record<string, any>
    message?: string
  }

  type DeleteRequest = {
    id?: string
  }

  type getArticleParams = {
    taskId: string
  }

  type getExecutionLogsParams = {
    taskId: string
  }

  type getProgressParams = {
    taskId: string
  }

  type ImageItem = {
    position?: number
    url?: string
    method?: string
    keywords?: string
    sectionTitle?: string
    description?: string
  }

  type LoginUserVO = {
    id?: string
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    quota?: number
    vipTime?: string
    createTime?: string
    updateTime?: string
  }

  type OutlineItem = {
    section?: number
    title?: string
    points?: string[]
  }

  type OutlineSection = {
    section?: number
    title?: string
    points?: string[]
  }

  type PageArticleVO = {
    records?: ArticleVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PaymentRecord = {
    id?: string
    userId?: number
    stripeSessionId?: string
    stripePaymentIntentId?: string
    amount?: number
    currency?: string
    status?: string
    productType?: string
    description?: string
    refundTime?: string
    refundReason?: string
    createTime?: string
    updateTime?: string
  }

  type refundParams = {
    reason?: string
  }

  type SseEmitter = {
    timeout?: number
  }

  type StatisticsVO = {
    todayCount?: number
    weekCount?: number
    monthCount?: number
    totalCount?: number
    successRate?: number
    avgDurationMs?: number
    activeUserCount?: number
    totalUserCount?: number
    vipUserCount?: number
    quotaUsed?: number
  }

  type TitleOption = {
    mainTitle?: string
    subTitle?: string
  }

  type User = {
    id?: string
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    quota?: number
    vipTime?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  /**
   * 管理员添加用户请求（与后端 UserAddRequest 对齐）
   */
  type UserAddRequest = {
    userAccount?: string
    userPassword?: string
    userName?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  /**
   * 修改密码请求（与后端 UserPasswordRequest 对齐）
   */
  type UserPasswordRequest = {
    oldPassword?: string
    newPassword?: string
    checkNewPassword?: string
  }

  /**
   * 用户列表查询请求（管理员，与后端 UserQueryRequest 对齐）
   */
  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    userName?: string
    userAccount?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  /**
   * 用户修改个人信息请求（与后端 UserUpdateRequest 对齐）
   * 仅用于普通用户修改自己的昵称、头像、简介
   */
  type UserUpdateRequest = {
    userName?: string
    userAvatar?: string
    userProfile?: string
  }

  /**
   * 管理员修改用户请求（与后端 UserUpdateByAdminRequest 对齐）
   */
  type UserUpdateByAdminRequest = {
    id?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  /**
   * 用户视图对象（管理端用，与后端 UserVO 对齐）
   */
  type UserVO = {
    id?: string
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
  }
}
