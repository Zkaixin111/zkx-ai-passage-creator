<div align="center">

# 协笔 CoWrite

**AI 人机协同写作平台 —— 生成、确认、再生成，每一步都由你掌控**

基于 Spring AI Alibaba 多智能体编排的 AI 写作工作台：AI 负责产出，用户负责决策。

</div>

---

## 为什么是「协笔」

大多数 AI 写作工具是"输入主题 → 一次生成全文"，结果**不可控、难修改、返工成本高**。

协笔的核心思路是 **Human-in-the-Loop（人机协同）**：把创作拆成 6 个智能体步骤，在**标题、大纲、配图**三个关键决策点暂停，由用户确认或编辑后再继续——AI 永远在用户掌控的轨道上运行。

```
选题 ──▶ 生成标题 ──▶ 用户确认 ──▶ 生成大纲(流式) ──▶ 用户编辑确认
   ──▶ 生成正文(流式) ──▶ 用户选择配图 ──▶ 配图生成 ──▶ 图文合成
```

## 核心特性

- **6 步智能体流水线**：标题 → 大纲 → 正文 → 配图分析 → 配图 → 合成，SSE 实时推送 13 类进度消息，正文/大纲流式逐字输出
- **3 次用户确认打断**：标题确认、大纲编辑、配图选择；阶段状态机 + CAS 防重 + 任务超时清理，保障多轮交互正确性
- **StateGraph 多智能体编排**：基于 Spring AI Alibaba StateGraph 的 DAG 图编排，按人工确认点拆分 5 段子图（标题/大纲/正文/配图/合成旁路）独立编译缓存
- **配图策略模式**：6 种配图方式（Pexels / AI 生图 / Iconify / Bing 表情 / SVG / Mermaid 本地渲染）可插拔，三级降级链保证流程不中断；多图并行获取，总耗时收敛为单图耗时
- **商业闭环**：VIP 会员体系 + 支付宝支付，下单/回调双端幂等、创作配额原子扣减防并发超扣
- **可观测**：`@AgentLog` 注解 + AOP 切面采集智能体执行日志，Redis 队列批量落库，管理端统计分析看板

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3 · Spring AI Alibaba（DashScope qwen3.7-plus）· MyBatis-Flex · Redis |
| 编排 | Spring AI Alibaba StateGraph · SSE（SseEmitter）· CompletableFuture 并行 |
| 前端 | Vue 3 · Vite · TypeScript · Ant Design Vue · Pinia · openapi2ts 契约生成 |
| 存储/外部 | MySQL · Redis · 腾讯云 COS · Pexels / Iconify / Bing / DashScope 通义万相 |

## 项目结构

```
ai-passage-creator-backend/     # Spring Boot 后端
├── agent/                      # 6 个智能体（标题/大纲/正文/配图分析/配图/合成）
├── aspect/                     # @AgentLog AOP 切面
├── config/                     # StateGraph 编排 / SSE / 任务注册表 / 线程池
├── service/image/              # 配图策略（ImageProvider 接口 + 6 种实现）
├── service/impl/               # 任务编排 / 支付 / 统计
└── sql/                        # 建表脚本（幂等可重复执行）

ai-passage-creator-frontend/    # Vue 3 前端
└── src/pages/article/          # 生成流程页（标题确认 → 大纲编辑 → 配图确认）
```

## 快速开始

### 1. 初始化数据库

```bash
mysql -uroot -p < ai-passage-creator-backend/sql/create_table.sql
```

### 2. 配置后端

编辑 `ai-passage-creator-backend/src/main/resources/application.yaml`：

```yaml
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      api-key: ${DASHSCOPE_API_KEY}   # 阿里云百炼 API Key
      chat:
        options:
          model: qwen3.7-plus

article:
  pexels:
    api-key: ${PEXELS_API_KEY}
  cos:
    secret-id: ${COS_SECRET_ID}       # 腾讯云 COS
    secret-key: ${COS_SECRET_KEY}
```

> Mermaid 配图依赖本地渲染工具（首次部署需安装）：
> ```bash
> cd ai-passage-creator-backend/tools/mermaid-cli && npm install
> ```

### 3. 启动

```bash
# 后端（默认 8081）
cd ai-passage-creator-backend && mvn spring-boot:run

# 前端（默认 5173，代理到 8081）
cd ai-passage-creator-frontend && npm install && npm run dev
```

## 配置说明

| 配置项 | 说明 |
|--------|------|
| `spring.ai.openai.*` | 对话模型（DashScope OpenAI 兼容模式，qwen3.7-plus） |
| `article.image.nano-banana.model` | AI 生图模型（默认 wanx2.1-t2i-turbo） |
| `article.image.mermaid.mmdc-path` | mermaid-cli 可执行文件路径 |
| `article.pexels.api-key` | Pexels 图库 API Key |
| `article.cos.*` | 腾讯云 COS（配图存储） |
| `pay.*` | 支付宝支付配置（应用 ID、密钥、回调地址） |

## License

MIT
