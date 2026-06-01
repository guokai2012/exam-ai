# Exam AI

智能出题系统单仓库，当前拆分为前端和后端两个独立目录。

根目录提供 Maven 聚合 `pom.xml`，只聚合 `backend` 模块。使用 IDEA 时建议直接打开
`exam-ai-backend` 根目录，等待 Maven 自动导入后，后端会作为 Spring Boot Maven 模块识别。
如果 IDEA 曾经把根目录识别成普通 Java 项目，可先执行 Maven Reload；仍不生效时，关闭项目后删除本地
`.idea` 目录再重新打开根目录。

## 目录结构

```text
.
├── backend/   # Spring Boot 后端服务
└── frontend/  # Vue 3 + Vite + Element Plus 前端工程
```

## 后端

后端技术栈：Java 17、Spring Boot 3.5.x、MySQL 8、Redis、MyBatis-Plus、Spring Security。

启动前需要准备：

- MySQL：默认库名 `exam_ai`
- Redis：默认 `localhost:6379`
- 配置文件：`backend/src/main/resources/application.yml`

常用命令：

```bash
cd backend
mvn test
mvn spring-boot:run
```

也可以在仓库根目录执行：

```bash
mvn -pl backend test
mvn -pl backend spring-boot:run
```

Spring Boot 启动类：`com.exam.ai.ExamAiBackendApplication`。

后端默认端口：`8080`。

### AI 文档分析配置

文档上传默认保存在 `backend/storage/documents`，不会提交到 Git。

AI 分析使用 OpenAI 兼容接口，启动前按需配置环境变量：

```bash
OPENAI_BASE_URL=https://api.openai.com
OPENAI_API_KEY=你的密钥
OPENAI_MODEL=gpt-4o-mini
```

未配置真实 `OPENAI_API_KEY` 时，文档上传和文本提取仍可用，点击分析会返回明确错误。

文档 AI 解析失败时会按系统配置 `ai.document-analysis.max-retries` 自动重试，默认首次失败后再重试 1 次。该配置建议不超过 3 次；保存超过 3 的值时，后端会自动按 3 次保存并返回提示。

文档解析按题目边界分块执行，避免将一道题拆到两个 AI 请求中。解析失败时会保留每个分块的进度，`PARSE_PARTIAL_FAILED` 或 `PARSE_FAILED` 状态下可继续解析失败或未完成的分块，已成功分块不会重复解析。

文档上传、文档分析和题目审核由教师角色操作。每位教师只能查看、分析、审核自己上传文档导入的题目。

AI 分析成功后，每一道题都会进入正式题库：

- AI 返回 `categoryName` 作为题库分类。
- 分类已存在时复用，不存在时自动创建。
- 同一教师、同一分类下按“规范化题干 SHA-256”去重。
- 参数不同的数学变式题会作为不同题目保存。
- 新入库题目默认状态为 `PARSE_PENDING_CONFIRM`。

题目生命周期由 Spring Statemachine 控制：

- `PARSE_PENDING_CONFIRM`：文档解析后待教师确认。
- `PARSE_REJECTED`：教师从文档解析结果中驳回。
- `TAG_PENDING`：教师确认后，等待 AI 标签分析。
- `TAG_PROCESSING`：AI 标签分析中。
- `TAG_FAILED`：AI 标签失败，后续定时任务可重试。
- `AVAILABLE`：标签分析完成，题目可用于检索、组卷和对外使用。

AI 标签定时扫描间隔可通过 `app.ai.tagging-delay` 配置，默认 30 秒。

AI 标签失败会先进入 `TAG_FAILED`，之后按系统配置 `ai.tagging.max-retries` 重试，默认首次失败后再重试 3 次。达到上限仍失败时，系统只给该题所属教师发送站内通知。

## 前端

前端技术栈：Vue 3、Vite、Element Plus。

常用命令：

```bash
cd frontend
npm install
npm run dev
npm run build
```

前端开发服务默认端口：`5173`。开发环境下 `/api` 会代理到 `http://localhost:8080`。
