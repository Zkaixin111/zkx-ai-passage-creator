/**
 * 四笔叙事映射常量 —— 「神笔马良」视觉体系专用
 *
 * 后端真实的阶段/状态枚举不直接出现在组件里做视觉判断，
 * 统一在这里归组为「四笔」：研墨(TOPIC) / 起稿(OUTLINE) / 落笔(DRAFT) / 点睛(IMAGERY)。
 * 组件只消费本文件的映射表，不散落魔法判断。
 */

/** 笔序定义：每笔的球内单字、中文名、英文副标 */
export interface BrushStep {
  /** 球内展示的单字 */
  char: string
  /** 楷体笔名 */
  name: string
  /** 等宽英文副标 */
  en: string
}

export const FOUR_BRUSHES: BrushStep[] = [
  { char: '墨', name: '研墨', en: 'TOPIC' },
  { char: '纲', name: '起稿', en: 'OUTLINE' },
  { char: '笔', name: '落笔', en: 'DRAFT' },
  { char: '睛', name: '点睛', en: 'IMAGERY' },
]

/**
 * 创作页阶段枚举 → 四笔索引
 * ArticleCreatePage 的 currentPhase 取值：INPUT / TITLE_GENERATING / TITLE_SELECTING /
 * OUTLINE_GENERATING / OUTLINE_EDITING / CONTENT_GENERATING / CONTENT_CONFIRMING / COMPLETED
 * 归组规则：选题与标题=研墨；大纲=起稿；正文=落笔；配图与合成、完成=点睛
 */
export const PHASE_TO_BRUSH: Record<string, number> = {
  INPUT: 0,
  TITLE_GENERATING: 0,
  TITLE_SELECTING: 0,
  OUTLINE_GENERATING: 1,
  OUTLINE_EDITING: 1,
  CONTENT_GENERATING: 2,
  CONTENT_CONFIRMING: 2,
  COMPLETED: 3,
}

/**
 * 后端智能体步骤序号（0~6）→ 四笔索引
 * 0 生成标题=研墨；1 规划大纲=起稿；2 撰写正文=落笔；
 * 3 配图分析 / 4 生成配图 / 5 图文合成 / 6 完成=点睛
 */
export const AGENT_STEP_TO_BRUSH: Record<number, number> = {
  0: 0,
  1: 1,
  2: 2,
  3: 3,
  4: 3,
  5: 3,
  6: 3,
}

/** 笔的视觉状态：done 已完成 / current 进行中 / pending 未开始 */
export type BrushState = 'done' | 'current' | 'pending'

/**
 * 文章状态枚举 → 文章架印章视觉
 * COMPLETED=成（朱砂实心）/ PROCESSING=生（黛绿空心圆）/ PENDING=稿（描金空心方块）/ FAILED=败（墨色空心方块）
 */
export interface StatusSeal {
  /** 印章单字 */
  char: string
  /** 印章视觉变体类名 */
  variant: 'done' | 'run' | 'draft' | 'fail'
}

export const STATUS_SEAL_MAP: Record<string, StatusSeal> = {
  COMPLETED: { char: '成', variant: 'done' },
  PROCESSING: { char: '生', variant: 'run' },
  PENDING: { char: '稿', variant: 'draft' },
  FAILED: { char: '败', variant: 'fail' },
}

/** 兜底印章（未知状态按「稿」处理） */
export const DEFAULT_STATUS_SEAL: StatusSeal = { char: '稿', variant: 'draft' }

/** 根据状态枚举取印章配置 */
export const getStatusSeal = (status?: string): StatusSeal => {
  if (!status) return DEFAULT_STATUS_SEAL
  return STATUS_SEAL_MAP[status] ?? DEFAULT_STATUS_SEAL
}

/**
 * 由阶段枚举与智能体步骤序号计算当前笔索引。
 * 任务全部完成（phase === 'COMPLETED' 或 step 到达末位）时所有笔都视为 done。
 */
export const resolveCurrentBrush = (phase: string, agentStep: number): number => {
  if (phase === 'COMPLETED') return FOUR_BRUSHES.length
  if (phase === 'INPUT') return 0
  const byPhase = PHASE_TO_BRUSH[phase]
  if (byPhase !== undefined) return byPhase
  return AGENT_STEP_TO_BRUSH[agentStep] ?? 0
}

/** 由当前笔索引计算每一笔的状态 */
export const getBrushState = (brushIndex: number, currentBrush: number): BrushState => {
  if (currentBrush >= FOUR_BRUSHES.length) return 'done'
  if (brushIndex < currentBrush) return 'done'
  if (brushIndex === currentBrush) return 'current'
  return 'pending'
}
