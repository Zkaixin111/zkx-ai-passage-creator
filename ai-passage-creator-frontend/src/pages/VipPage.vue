<template>
  <div class="vip-page">
    <div class="vip-container">
      <!-- 页面头部 -->
      <div class="sec-head">
        <p class="kicker">磨 墨 有 价</p>
        <h1 class="page-title">供起神笔，落笔不愁</h1>
        <p class="page-subtitle">所有档位都包含<b>四笔全流程</b>：研墨 · 起稿 · 落笔 · 点睛。</p>
      </div>

      <!-- 主内容区：左右布局（后端为单一永久会员套餐，保留真实套餐数据套新皮肤） -->
      <div class="main-section">
        <!-- 左侧：价格卡片 -->
        <div class="plan-card rec">
          <div class="plan-badge">最 常 被 挑 走</div>
          <div class="plan-header">
            <div class="plan-icon">
              <CrownOutlined />
            </div>
            <h2 class="plan-name">永久会员</h2>
            <span class="plan-en">LIFETIME</span>
            <div class="price-display">
              <span class="currency">¥</span>
              <span class="price">29.9</span>
              <span class="period">/ 永久</span>
            </div>
            <div class="price-note">
              <span class="original-label">原价</span>
              <span class="original-value">¥99</span>
              <span class="price-tip">一次蘸墨，终身不干</span>
            </div>
          </div>

          <div class="pricing-divider"></div>

          <!-- 权益列表（描金圆点） -->
          <ul class="benefits">
            <li v-for="(item, index) in pricingFeatures" :key="index">
              <span>{{ item }}</span>
            </li>
          </ul>

          <a-button
            type="primary"
            size="large"
            :loading="purchasing"
            :disabled="isVip"
            @click="handlePurchase"
            class="buy-btn"
          >
            <template #icon>
              <ThunderboltOutlined />
            </template>
            {{ isVip ? '您已是永久会员' : '蘸 墨 订 阅' }}
          </a-button>

          <div class="sandbox">
            <SafetyOutlined />
            <span>支付宝安全支付 · 7天无理由退款</span>
          </div>
        </div>

        <!-- 右侧：会员特权 -->
        <div class="features-section">
          <h3 class="features-title">
            <GiftOutlined />
            会员特权
          </h3>
          <div class="features-grid">
            <div v-for="(feature, index) in features" :key="index" class="feature-card">
              <div class="feature-icon-wrapper">
                <component :is="feature.icon" class="feature-icon" />
              </div>
              <div class="feature-content">
                <h4 class="feature-title">{{ feature.title }}</h4>
                <p class="feature-desc">{{ feature.desc }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 常见问题 -->
      <div class="faq-section">
        <h2 class="faq-title">开笔前，四问四答</h2>
        <div class="faq-grid">
          <div v-for="(faq, index) in faqs" :key="index" class="qa">
            <b>{{ faq.question }}</b>
            <p>{{ faq.answer }}</p>
          </div>
        </div>
      </div>

      <!-- 支付弹窗 -->
      <a-modal
        :open="payModalVisible"
        :title="null"
        :footer="null"
        :width="380"
        :closable="payStep !== 'PAYING'"
        @cancel="handlePayModalCancel"
        centered
      >
        <!-- 支付中：支付宝已弹出 -->
        <div v-if="payStep === 'PAYING'" class="pay-paying">
          <a-spin />
          <p class="pay-hint">支付宝支付页面已在新窗口打开，请完成支付</p>
          <p class="pay-order">订单号：{{ payOrderNo }}</p>
          <p class="pay-tip">支付完成后将自动开通会员，请勿关闭此页面</p>
        </div>

        <!-- 支付成功 -->
        <div v-else class="pay-success">
          <CheckCircleFilled class="pay-success-icon" />
          <h3>落印成功！</h3>
          <p>欢迎成为永久会员，畅享全部高级功能</p>
        </div>
      </a-modal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  CheckCircleFilled,
  CrownOutlined,
  SafetyOutlined,
  ThunderboltOutlined,
  RocketOutlined,
  PictureOutlined,
  AppstoreOutlined,
  EditOutlined,
  StarOutlined,
  GiftOutlined
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { createPay } from '@/api/payController'
import { isVip as checkIsVip } from '@/utils/permission'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const purchasing = ref(false)

// 是否是 VIP（管理员也视为 VIP）
const isVip = computed(() => checkIsVip(loginUserStore.loginUser))

// ==================== 支付 ====================
const payModalVisible = ref(false)
const payStep = ref<'PAYING' | 'SUCCESS'>('PAYING')
const payOrderNo = ref('')
let payTimer: ReturnType<typeof setInterval> | null = null

// 购买处理：直接调支付宝下单
const handlePurchase = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }

  if (isVip.value) {
    message.info('您已经是永久会员')
    return
  }

  // 直接调支付宝下单
  purchasing.value = true
  try {
    const res = await createPay({ channel: 'ALIPAY' })
    if (res.data.code !== 0) {
      message.error(res.data.message || '下单失败')
      return
    }
    const data = res.data.data
    if (!data) {
      message.error('下单失败：无返回数据')
      return
    }
    if (data.status === 'PAID') {
      message.success('您已是永久会员')
      await loginUserStore.fetchLoginUser()
      return
    }
    payOrderNo.value = data.orderNo || ''
    if (data.payType === 'ALIPAY_FORM' && data.payUrl) {
      // 支付宝：新窗口提交表单
      openAlipayPage(data.payUrl)
      payStep.value = 'PAYING'
      payModalVisible.value = true
      startPolling()
    } else {
      message.warning('支付方式异常')
    }
  } catch (error) {
    console.error('下单失败:', error)
    message.error('下单失败，请稍后重试')
  } finally {
    purchasing.value = false
  }
}

// 支付宝表单：新窗口渲染并自动提交
const openAlipayPage = (formHtml: string) => {
  const win = window.open('', '_blank')
  if (win) {
    win.document.write(formHtml)
    win.document.close()
  }
}

// 轮询开通状态（每 3 秒刷新一次用户信息，开通成功后提示）
const startPolling = () => {
  stopPolling()
  payTimer = setInterval(async () => {
    const ok = await loginUserStore.fetchLoginUser()
    if (ok && isVip.value) {
      stopPolling()
      payStep.value = 'SUCCESS'
      message.success('开通成功，欢迎成为永久会员！')
      setTimeout(() => {
        payModalVisible.value = false
      }, 1500)
    }
  }, 3000)
}

const stopPolling = () => {
  if (payTimer) {
    clearInterval(payTimer)
    payTimer = null
  }
}

// 关闭弹窗时停止轮询
const handlePayModalCancel = () => {
  stopPolling()
  payModalVisible.value = false
}

// 会员特权列表（真实会员能力）
const features = [
  {
    icon: RocketOutlined,
    title: '无限创作配额',
    desc: '无限次使用文章创作功能，告别配额限制'
  },
  {
    icon: PictureOutlined,
    title: 'AI 智能生图',
    desc: '使用 DashScope 通义万相文生图，AI 生成独特配图'
  },
  {
    icon: AppstoreOutlined,
    title: 'SVG 图表生成',
    desc: '自动生成精美的概念示意图和思维导图'
  },
  {
    icon: EditOutlined,
    title: 'AI 大纲编辑',
    desc: '使用 AI 助手快速优化文章大纲'
  },
  {
    icon: StarOutlined,
    title: '优先队列',
    desc: '享受更快的生成速度和优先服务'
  },
  {
    icon: GiftOutlined,
    title: '终身有效',
    desc: '一次购买，永久使用，无需续费'
  }
]

// 价格卡片特性（描金圆点权益列表）
const pricingFeatures = [
  '无限创作配额',
  '全部高级配图功能',
  'AI 大纲智能编辑',
  '优先生成队列',
  '终身有效'
]

// FAQ 列表
const faqs = [
  {
    question: '支付后多久生效？',
    answer: '支付成功后立即生效，您将立即获得永久会员权限，刷新页面即可看到变化。'
  },
  {
    question: '如何申请退款？',
    answer: '购买后 7 天内，如不满意可申请退款，退款后会员权限将被取消。'
  },
  {
    question: '会员是否需要续费？',
    answer: '不需要。永久会员一次购买，终身有效，无需任何续费。'
  },
  {
    question: '支付安全吗？',
    answer: '我们使用支付宝官方支付接口，全程加密传输，安全可靠。'
  }
]

onMounted(async () => {
  const success = route.query.success
  const cancelled = route.query.cancelled

  if (success === 'true') {
    await loginUserStore.fetchLoginUser()
    Modal.success({
      title: '支付成功！',
      content: '恭喜您成为永久会员，已解锁全部高级功能！',
      okText: '开始创作',
      onOk: () => {
        router.push('/create')
      }
    })
    router.replace('/vip')
  } else if (cancelled === 'true') {
    message.info('支付已取消')
    router.replace('/vip')
  }
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped lang="scss">
.vip-page {
  min-height: calc(100vh - 68px);
  background: var(--paper);
  padding: 48px 24px 80px;
}

.vip-container {
  max-width: 1080px;
  margin: 0 auto;
}

/* 页面头部 */
.sec-head {
  text-align: center;
  margin-bottom: 52px;
}

.kicker {
  font-family: var(--kai);
  color: var(--gold);
  font-size: 14px;
  letter-spacing: 0.4em;
  margin-bottom: 10px;
}

.page-title {
  font-family: var(--kai);
  font-size: 38px;
  font-weight: 700;
  margin: 0 0 10px;
  color: var(--ink);
}

.page-subtitle {
  color: var(--ink-2);
  margin: 0;
  font-size: 15px;

  b {
    color: var(--vermilion);
  }
}

/* 主内容区 */
.main-section {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 28px;
  margin-bottom: 56px;
  align-items: stretch;
}

/* ==================== 价格卡（定价原型 plan.rec 皮肤） ==================== */
.plan-card {
  position: relative;
  background: var(--paper-card);
  border: 1.5px solid var(--hairline);
  border-radius: 18px;
  padding: 36px 30px 30px;
  display: flex;
  flex-direction: column;
  transition: all 0.35s var(--ease);

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 18px 40px rgba(38, 34, 28, 0.1);
    border-color: var(--gold);
  }

  /* 推荐档：朱砂双描边 */
  &.rec {
    border: 2px solid var(--vermilion);
    box-shadow: 0 16px 40px rgba(196, 64, 47, 0.14);
  }
}

/* 推荐档顶部朱砂徽标 */
.plan-badge {
  position: absolute;
  top: -15px;
  left: 50%;
  transform: translateX(-50%) rotate(-2deg);
  background: var(--vermilion);
  color: #fbf6ec;
  font-family: var(--kai);
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.2em;
  padding: 5px 18px;
  border-radius: 999px;
  box-shadow: 0 4px 10px rgba(196, 64, 47, 0.35);
  white-space: nowrap;
}

.plan-header {
  text-align: center;
  padding-bottom: 16px;
}

.plan-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(196, 64, 47, 0.08);
  border-radius: 12px;

  .anticon {
    font-size: 26px;
    color: var(--vermilion);
  }
}

.plan-name {
  font-family: var(--kai);
  font-size: 23px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--ink);
}

.plan-en {
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: var(--ink-3);
  letter-spacing: 0.16em;
  margin-bottom: 20px;
  display: block;
}

/* 楷体价格 */
.price-display {
  display: flex;
  align-items: baseline;
  justify-content: center;
  font-family: var(--kai);
  margin-bottom: 6px;
}

.currency {
  font-size: 18px;
  color: var(--ink-3);
  margin-right: 2px;
  font-weight: 700;
}

.price {
  font-size: 46px;
  font-weight: 700;
  color: var(--vermilion);
  line-height: 1;
}

.period {
  font-size: 15px;
  color: var(--ink-3);
  margin-left: 4px;
}

.price-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--ink-3);
  margin-bottom: 6px;
}

.original-label {
  color: var(--ink-3);
}

.original-value {
  color: var(--ink-3);
  text-decoration: line-through;
}

.price-tip {
  color: var(--gold);
}

.pricing-divider {
  height: 1px;
  background: var(--hairline);
  margin: 16px 0;
}

/* 权益列表：描金圆点 */
.benefits {
  list-style: none;
  display: grid;
  gap: 11px;
  margin: 0 0 30px;
  flex: 1;
  padding: 0;

  li {
    font-size: 14px;
    color: var(--ink-2);
    padding-left: 22px;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      left: 2px;
      top: 9px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--gold);
    }
  }
}

/* 购买按钮：朱砂 + 按压感 */
.buy-btn {
  width: 100%;
  height: 48px;
  font-family: var(--kai);
  font-size: 15.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  background: var(--vermilion) !important;
  border: none !important;
  border-radius: 999px !important;
  color: #fbf6ec !important;
  box-shadow: 0 4px 0 var(--vermilion-dark) !important;
  transition: all 0.3s var(--ease) !important;

  &:hover:not(:disabled) {
    background: var(--vermilion-dark) !important;
    transform: translateY(-2px);
    box-shadow: 0 6px 0 var(--vermilion-dark) !important;
  }

  &:disabled {
    background: var(--color-border) !important;
    color: var(--ink-3) !important;
    box-shadow: none !important;
  }
}

/* 沙箱演示提示 */
.sandbox {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 14px;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--ink-3);
  letter-spacing: 0.1em;

  .anticon {
    font-size: 13px;
  }
}

/* ==================== 会员特权（宣纸卡） ==================== */
.features-section {
  background: var(--paper-card);
  border-radius: 18px;
  padding: 32px;
  border: 1.5px solid var(--hairline);
  box-shadow: var(--shadow-hard-sm);
}

.features-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--kai);
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 24px;
  color: var(--ink);

  .anticon {
    color: var(--gold);
    font-size: 20px;
  }
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.feature-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  background: var(--paper);
  border: 1px dashed var(--hairline);
  border-radius: 14px;
  transition:
    transform var(--transition-bounce),
    box-shadow var(--transition-normal),
    border-color var(--transition-normal);

  &:hover {
    transform: translateY(-3px);
    border-color: var(--gold);
    border-style: solid;
    box-shadow: var(--shadow-card-hover);
  }
}

.feature-icon-wrapper {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(196, 64, 47, 0.08);
  border-radius: 10px;
}

.feature-icon {
  font-size: 18px;
  color: var(--vermilion);
}

.feature-content {
  flex: 1;
  min-width: 0;
}

.feature-title {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--ink);
}

.feature-desc {
  font-size: 13px;
  color: var(--ink-2);
  margin: 0;
  line-height: 1.5;
}

/* ==================== FAQ（问 · 描金前缀） ==================== */
.faq-section {
  max-width: 640px;
  margin: 0 auto 20px;
  border-top: 1px solid var(--hairline);
  padding-top: 40px;
}

.faq-title {
  font-family: var(--kai);
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 20px;
  text-align: center;
  color: var(--ink);
}

.qa {
  margin-bottom: 18px;

  b {
    font-family: var(--kai);
    font-size: 15.5px;
    display: block;
    margin-bottom: 6px;
    color: var(--ink);

    &::before {
      content: '问 · ';
      color: var(--gold);
    }
  }

  p {
    font-size: 13.5px;
    color: var(--ink-2);
    padding-left: 2px;
    margin: 0;
    line-height: 1.7;
  }
}

/* 响应式 */
@media (max-width: 992px) {
  .main-section {
    grid-template-columns: 1fr;
  }

  .plan-card {
    max-width: 420px;
    margin: 0 auto;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .faq-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .vip-page {
    padding: 32px 16px 60px;
  }

  .page-title {
    font-size: 28px;
  }
}

.faq-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 32px;
}

/* 支付弹窗 */
.pay-paying {
  text-align: center;
  padding: 8px 0;

  .pay-hint {
    font-size: 14px;
    font-weight: 600;
    color: var(--ink);
    margin: 0 0 8px;
  }

  .pay-order {
    font-family: var(--font-mono);
    font-size: 12px;
    color: var(--ink-3);
    margin: 0 0 4px;
    word-break: break-all;
  }

  .pay-tip {
    font-size: 12px;
    color: var(--gold);
    margin: 0;
  }
}

.pay-success {
  text-align: center;
  padding: 16px 0;

  .pay-success-icon {
    font-size: 56px;
    color: var(--vermilion);
    margin-bottom: 12px;
  }

  h3 {
    font-family: var(--kai);
    font-size: 20px;
    font-weight: 700;
    color: var(--ink);
    margin: 0 0 8px;
  }

  p {
    font-size: 14px;
    color: var(--ink-2);
    margin: 0;
  }
}
</style>
