<template>
  <!-- 品牌面板：卷轴插画做底 + 墨色渐变遮罩 + 竖排朱砂印章（登录/注册共用） -->
  <div class="brand-panel">
    <!-- 配图层：shenbi-cta.webp 放大 118% 并锚定左侧，右下角"AI生成"水印被裁出可视区 -->
    <div class="brand-bg" aria-hidden="true"></div>
    <!-- 遮罩层：115° 墨色渐变，保证左侧文字对比度 -->
    <div class="brand-mask" aria-hidden="true"></div>
    <div class="brand-content">
      <h1 class="brand-title">
        {{ titleTop }}<br />
        <em>{{ titleEm }}</em>{{ titleBottom }}
      </h1>
      <div class="brand-points">
        <span v-for="(point, index) in points" :key="index">{{ point }}</span>
      </div>
      <div class="brand-note">COWRITE · HUMAN-IN-THE-LOOP WRITING</div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 品牌面板内容全部由父页面传入（标语/亮点用真实产品文案，不造假数据）
interface Props {
  /** 主标题第一行 */
  titleTop: string
  /** 第二行的描金强调字（如「活」「赠」） */
  titleEm: string
  /** 第二行强调字之后的剩余文字 */
  titleBottom: string
  /** 三条「·」亮点文案 */
  points: string[]
}

withDefaults(defineProps<Props>(), {
  titleTop: '一句话，',
  titleEm: '成',
  titleBottom: '一篇文章',
  points: () => [
    'AI 六步流水线：标题、大纲、成稿、配图',
    '每一步都等你确认，不满意随时改',
    '从此没有空白页',
  ],
})
</script>

<style scoped>
.brand-panel {
  position: relative;
  overflow: hidden;
  background: var(--ink);
  min-height: 100%;
}

/* 配图背景层：放大 118% 锚定左侧，裁掉右下水印 */
.brand-bg {
  position: absolute;
  inset: 0;
  background-image: url('@/assets/shenbi-cta.webp');
  background-size: cover;
  background-position: left center;
  background-repeat: no-repeat;
}

/* 墨色遮罩：115° 渐变，左深右浅 */
.brand-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    115deg,
    rgba(38, 34, 28, 0.88) 0%,
    rgba(38, 34, 28, 0.55) 55%,
    rgba(38, 34, 28, 0.25) 100%
  );
}

.brand-content {
  position: relative;
  z-index: 2;
  color: #F7F1E5;
  padding: 56px 52px;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 640px;
}

.brand-title {
  font-family: var(--kai);
  font-size: 42px;
  font-weight: 700;
  line-height: 1.4;
  margin: auto 0 0;
  letter-spacing: 0.02em;
}

.brand-title em {
  font-style: normal;
  color: var(--gold);
}

.brand-points {
  margin-top: 26px;
  display: grid;
  gap: 12px;
}

.brand-points span {
  font-family: var(--kai);
  font-size: 15px;
  color: rgba(247, 241, 229, 0.82);
  letter-spacing: 0.06em;
}

.brand-points span::before {
  content: '·';
  color: var(--gold);
  margin-right: 10px;
  font-weight: 700;
}

/* 底部 mono 底注推到面板最下 */
.brand-note {
  margin-top: auto;
  padding-top: 40px;
  font-family: var(--font-mono);
  font-size: 11.5px;
  color: rgba(247, 241, 229, 0.45);
  letter-spacing: 0.12em;
}

@media (max-width: 860px) {
  .brand-panel {
    display: none;
  }
}
</style>
