# 项目协作规则

## 1. 通用协作要求

### 1.1 语言要求
- 始终使用简体中文沟通、编写说明和记录项目文档。

### 1.2 功能开发闭环
- 所有新功能必须前后端一起实现，不允许只完成后端接口或只完成前端页面。
- 每个功能必须形成完整闭环：后端接口、数据流转、前端页面/交互、权限与异常处理、验证方式都要同步覆盖。
- 功能实现前，需要先明确该功能涉及的角色、入口页面、后端接口、数据状态和异常分支。

### 1.3 代码注释与文档
- 所有方法必须编写详细 Javadoc，说明方法用途、关键参数、返回值和重要异常分支。
- 方法内部必须在关键步骤添加代码注释，说明主要业务决策、数据转换或外部依赖调用。
- 所有业务代码必须添加必要注释，关键逻辑必须写清楚业务意图和处理原因。
- 禁止魔法值，业务常量必须抽取为常量、枚举或配置项。

### 1.4 Git 提交与推送
- 每次完成代码或文档修改并通过必要验证后，必须自动提交到 Git 本地仓库，并推送到远程仓库。
- 提交信息必须简洁说明本次修改目的，推送失败时必须说明失败原因并优先尝试可用的远程推送方式。

## 2. 后端编码规范

### 2.1 技术栈与基础要求
- 技术栈：Java 17、Spring Boot 3.5.x、MySQL 8、Redis、MyBatis-Plus、Spring Security、Maven。
- 构建工具：Maven。
- 开发规范：RESTful API、分层架构、统一返回值、全局异常处理。
- JDK 版本统一为 Java 17，仅使用 LTS 稳定语法，禁止使用废弃 API。
- Spring Boot 版本统一为 3.5.x，严格兼容 `jakarta.*` 包，禁止使用 `javax.*`。
- 文件编码统一为 UTF-8。
- 后端代码缩进统一为 4 个空格。
- 大括号风格统一为行尾大括号，即 K&R 风格。
- 变量命名使用小驼峰，例如 `userName`、`pageSize`。
- 常量命名使用全大写加下划线，例如 `PAGE_SIZE`。
- 类命名使用大驼峰，例如 `UserController`、`UserService`。
- 日志使用 `@Slf4j` 或标准日志门面，禁止使用 `System.out.println`。
- 日期时间统一使用 Java 8 新时间 API，例如 `LocalDateTime`。

### 2.2 项目结构规范
- 项目必须严格使用固定分层结构，禁止乱层。
- `controller`：接口控制层。
- `service`：业务逻辑层。
- `service/impl`：业务实现类。
- `mapper`：DAO 数据访问层，基于 MyBatis-Plus。
- `entity`：数据库实体类（DO）。
- `vo`：前端返回视图对象。
- `dto`：前端传入参数对象。
- `common/base`：基类、通用枚举。
- `common/result`：统一返回值。
- `common/exception`：全局异常。
- `common/config`：配置类。
- `util`：工具类。
- `security`：Spring Security 权限配置。
- `scheduler`：定时任务类，必须放在对应业务模块下。

### 2.3 Controller 层规范
- Controller 必须使用 `@RestController`。
- 接口路径统一使用 `/api/xxx` 前缀。
- 必须遵循 RESTful 风格：查询使用 `GET`，新增使用 `POST`，修改使用 `PUT`，删除使用 `DELETE`。
- 入参按语义使用 `@RequestBody`、`@RequestParam`、`@PathVariable`。
- 接口必须使用统一返回封装，禁止直接返回原始数据。
- 接口必须编写 Swagger/OpenAPI 文档注解，例如 `@Operation`。
- Controller 接口文档必须写清楚接口用途、入参类型、入参字段说明、返回参数说明、鉴权要求和异常情况。
- 禁止在 Controller 中编写业务逻辑，只允许参数接收、鉴权入口和服务编排。

### 2.4 Service 层规范
- Service 接口命名为 `XxxService`。
- Service 实现类命名为 `XxxServiceImpl`，并使用 `@Service` 标注。
- 业务方法命名遵循语义前缀：`getXxx()` 查询单条，`listXxx()` 查询列表，`pageXxx()` 分页查询，`saveXxx()` 新增，`updateXxx()` 修改，`removeXxx()` 删除。
- 所有业务方法必须做参数校验。
- 涉及写操作或多表一致性的业务必须使用 `@Transactional(rollbackFor = Exception.class)`。
- 禁止在 Service 中编写 Controller 层逻辑。
- 业务异常必须抛出自定义异常，并由全局异常处理器统一捕获。

### 2.5 DTO 与 Entity 规范
- 所有 DTO、Entity 等数据实体类必须优先采用 Lombok 注解生成样板代码，避免手写 getter、setter、构造器、toString 等重复代码。
- 实体类必须使用 `@TableName("表名")`。
- 主键必须使用 `@TableId(type = IdType.AUTO)`。
- 字段映射优先使用 `@TableField("字段名")` 明确声明。
- 布尔类型数据库字段使用 `is_xxx` 命名，实体字段使用 `Boolean`。

### 2.6 MyBatis-Plus 与数据库访问规范
- Mapper 层必须继承 `BaseMapper<T>`，禁止手写简单 CRUD。
- Service 层优先继承 `IService<T>`。
- Service 实现类优先继承 `ServiceImpl<M, T>`。
- 分页必须使用 MyBatis-Plus 分页对象 `Page<T>`。
- 查询构造器必须使用 `LambdaQueryWrapper`。
- 禁止使用 `select *`，必须指定查询列。
- 数据库 CRUD 操作优先使用批量操作，不允许默认在循环体内逐条执行数据库操作。
- 如果数据库操作确实无法从循环体内拆解为批量操作，必须在代码中明确注释原因。

### 2.7 MySQL 8 规范
- 表名使用小写加下划线，例如 `sys_user`。
- 字段名使用小写加下划线，例如 `user_name`。
- 表默认必须包含：`id BIGINT` 自增主键、`create_time DATETIME`、`update_time DATETIME`、`is_deleted TINYINT(1)` 逻辑删除字段。
- MyBatis-Plus 必须开启逻辑删除和自动填充。
- 禁止使用数据库外键，关联关系由业务代码控制。
- 时间类型统一使用 `DATETIME`，禁止使用 `TIMESTAMP`。

### 2.8 Redis 规范
- Redis 访问统一使用 Spring Data Redis。
- 序列化方式统一使用 `GenericJackson2JsonRedisSerializer`。
- Redis Key 命名格式为 `业务模块:功能:唯一标识`，例如 `user:info:1001`。
- Redis 常用场景包括缓存查询列表、接口限流、登录 Token 存储和分布式锁。
- Redis 缓存必须设置过期时间，禁止永久缓存。
- 缓存异常不得影响主流程，必须使用 `try/catch` 捕获并记录日志。

### 2.9 Spring Security 权限规范
- 基于 Spring Boot 3.x 最新版 Spring Security。
- 使用 JWT 无状态认证。
- 权限架构至少包含 `JwtAuthenticationFilter`、用户加载服务和 `SecurityConfig`。
- 角色控制使用 `@PreAuthorize("hasRole('ADMIN')")`。
- 接口权限控制使用 `@PreAuthorize("hasAuthority('sys:user:list')")` 这类独立权限码。
- 所有 Controller 接口权限必须按具体接口动作单独拆分权限码，不允许只通过 `read`、`write` 等粗粒度权限简单区分一组接口。
- 新增或调整接口时，必须同步维护独立权限码、权限扫描结果、角色授权入口和接口文档中的鉴权说明。
- 密码加密必须使用 `BCryptPasswordEncoder`。
- 登录、注册、验证码、静态资源和接口文档等公开资源需要明确放行。
- 禁止明文传输或保存密码。
- Token 必须通过请求头传递，格式为 `Authorization: Bearer token`。

### 2.10 统一返回与异常规范
- 统一返回值结构必须包含响应码、提示信息和数据。
- 全局异常处理必须使用 `@RestControllerAdvice`。
- 自定义业务异常统一使用 `BusinessException` 或项目约定的业务异常类。
- 统一响应码枚举应包含成功、未登录、无权限、服务器错误等常见状态。

### 2.11 定时任务规范
- 所有定时任务都必须使用 `@Scheduled` 注解方式实现。
- 定时任务类必须统一放在对应业务模块的 `scheduler` 包下。
- 定时任务内部必须做好异常捕获和日志记录，避免单次失败影响后续调度。

### 2.12 AI 生成后端代码要求
- 必须严格遵循本规范，禁止使用不兼容语法。
- 必须基于 Spring Boot 3.5.x 和 Java 17。
- 必须使用 MyBatis-Plus 官方最佳实践。
- 新增完整业务时必须提供可运行代码：Controller、Service、Mapper、Entity。
- 必须带分页、条件查询、校验和异常处理。
- 必须遵循权限规范，接口必须控制权限。
- 禁止生成过时、废弃或不安全的代码。
- 代码风格必须统一、简洁、企业级、可直接上线。

## 3. 前端编码规范

### 3.1 技术栈与基础要求
- 前端技术栈统一为 Vue 3、Vite、Element Plus、JavaScript/TypeScript。
- 构建工具：Vite。
- UI 组件库：Element Plus。
- 文件编码统一为 UTF-8。
- 前端缩进统一为 2 个空格。
- 字符串引号统一使用单引号。
- 语句结尾不加分号。
- 禁止使用 `var`，统一使用 `const` 或 `let`。
- 代码风格遵循 ESLint 和 Prettier 规范。

### 3.2 Vue 语法与组件结构
- 所有 `.vue` 文件必须使用 Vue 3 `<script setup>` 语法，禁止使用 Options API。
- 必须使用组合式 API，例如 `ref`、`reactive`、`computed`、`watch`、`watchEffect`。
- 单文件组件结构固定为：`<template>`、`<script setup>`、`<style scoped>`。
- 组件文件顶部必须添加用途注释，说明作者、日期和组件用途。
- 标签属性多行展示时，每个属性独占一行。
- 变量和方法使用小驼峰命名，方法名以动词开头，例如 `handleSubmit`、`fetchData`、`resetForm`。
- 常量使用全大写加下划线命名，例如 `PAGE_SIZE`、`BASE_URL`。
- 组件文件使用大驼峰命名，例如 `UserTable.vue`、`LoginForm.vue`。
- 必须给关键业务代码添加注释。

### 3.3 前端模块化规范
- 前端项目必须按功能模块拆分代码，不允许继续把新增业务堆在 `frontend/src/App.vue`。
- 新功能按模块放到 `frontend/src/modules/<feature>/`。
- 每个功能模块至少包含页面/组件、API 调用、状态或工具函数。
- 公共组件放到 `frontend/src/components/`。
- 公共请求封装放到 `frontend/src/api/` 或 `frontend/src/shared/`。
- 后续如果需要修改现有 `App.vue`，应优先拆分相关功能模块，再接入页面入口。
- 所有功能模块必须独立成模块，模块内职责清晰，不允许把多个无关功能堆在同一页面或同一组件中。

### 3.4 Element Plus 使用规范
- 所有功能组件优先使用 Element Plus UI 框架实现。
- 所有 Element Plus 组件直接使用，无需手动注册，例如 `ElButton`、`ElTable`、`ElForm`。
- 弹窗、消息提示必须优先使用 `ElDialog`、`ElMessage`、`ElMessageBox`。
- 表单必须使用 `ElForm`、`ElFormItem` 和对应表单组件，并严格绑定 `model`、`rules`、`ref` 做校验。
- 表格必须使用 `ElTable` 和 `ElTableColumn`，通过 `data`、`prop`、`label` 定义数据和列。
- 分页必须使用 `ElPagination`。
- 弹窗必须使用 `v-model` 控制显示，必要时使用 `append-to-body` 处理遮罩层级。
- 禁用和加载状态统一使用 `:disabled`、`v-loading`。
- 禁止手动封装 Element Plus 基础组件。
- 禁止直接修改 Element Plus 原生样式；如确实需要修改，必须在 `scoped` 样式中使用 `:deep()`。
- 禁止使用 Element Plus 非官方属性。

### 3.5 前端业务代码规范
- 表单提交前必须做表单预校验。
- 操作成功使用 `ElMessage.success`，失败使用 `ElMessage.error`。
- 接口请求统一使用 `async/await` 语法，并必须加 `try/catch` 异常处理。
- 接口返回数据统一赋值给响应式变量。
- 表格页面默认应包含搜索区域、表格区域、分页组件和操作栏，例如查看、编辑、删除。
- 新增和编辑操作应优先共用一个弹窗。
- 弹窗打开时必须初始化数据，关闭时必须重置表单。

### 3.6 AI 生成前端代码要求
- 必须完全符合本规范，禁止使用旧版 Vue 语法。
- 必须生成可运行、可维护、符合企业级中后台页面标准的代码。
- 必须使用 Element Plus 官方推荐写法。
- 代码必须简洁、无冗余、风格统一。
- 禁止生成无法运行、语法错误或风格混乱的代码。

## 4. 数据库迁移脚本规范
- 数据库迁移脚本统一保存到后端迁移目录。
- 数据库迁移脚本采用 `{yyyyMMddHHmmss}__脚本描述.sql` 命名格式。
- 脚本描述使用简短中文或英文语义，能明确表达迁移目的。
- 数据库迁移脚本必须优先采用可重复执行的 SQL 语句，例如使用存在性判断、幂等更新或冲突处理。
- 如果迁移脚本无法做到可重复执行，必须在脚本注释或交付说明中明确说明原因和执行约束。

## 5. 业务流程文档规范
- 每个功能必须提供完整业务流程图，并以 Markdown 文档保存到 `docs/` 目录。
- 流程图统一使用 Mermaid。
- 每个功能一个独立 Markdown 文件，文件名采用 `{yyyyMMddmmss}_文档描述.md` 命名格式。
- 示例：`docs/20260601173000_文档分析流程.md`、`docs/20260601173500_题目审核流程.md`。
- 功能流程文档至少包含：功能目标、参与角色、主流程、异常流程、Mermaid 业务流程图、前后端交互点、相关接口与页面关系。

## 6. 验证要求
- 每个功能完成后必须同时验证后端和前端。
- 后端默认验证命令：`cd backend && mvn test`。
- 前端默认验证命令：`cd frontend && npm run build`。
- 如果某次功能确实不涉及某一端，必须在交付说明中明确原因；但默认要求是前后端一起实现。
