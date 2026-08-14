# MixMyFit

## 项目简介

MixMyFit 是一个个人衣橱搭配管理 Web 应用，面向希望数字化管理衣物并进行穿搭组合的普通用户。

用户可以上传自己的衣物图片，按品类、颜色、季节和标签管理衣物，并在搭配编辑器中组合、预览、保存和复用穿搭方案。项目目标是减少用户反复试穿或手工拼图的成本，让用户能基于自己的真实衣物图片完成搭配规划。

本项目是 AI4SE 期末项目 B（非 harness 应用类项目）。当前已完成 SPEC、PLAN、冷启动验证、后端认证与个人资料 API、衣物与搭配核心 API、前端核心页面、核心 E2E，以及 Docker Compose 本地分发；尚未完成 CI 或线上部署。

## 目标用户

目标用户是拥有一定数量衣物、希望用数字化方式管理衣物并进行穿搭组合的用户。

典型使用场景：

- 日常通勤或上学前快速挑选穿搭。
- 穿搭爱好者整理衣柜和风格方案。
- 旅行或出差前规划要带的衣物组合。
- 复用以前保存过的搭配方案。

## 核心功能

SPEC 阶段确定的 MVP 功能包括：

- 账号注册、登录、退出、修改昵称、修改密码。
- 批量上传衣物图片。
- 固定品类：上装、下装、鞋子、帽子。
- 用户自定义一级品类，例如项链、耳环、包、围巾等。
- 衣物颜色、季节、多选标签管理。
- 待完善衣物提醒与筛选。
- 批量设置品类、颜色、季节，批量添加或移除标签。
- 搭配编辑器：四个固定主槽位 + 自定义品类配饰层。
- 搭配编辑器中按颜色、季节、标签筛选候选衣物。
- 配饰添加、移除、拖动、层级调整、小/中/大尺寸档位。
- 搭配方案保存、查看、编辑、删除。
- 搭配方案按场景标签和季节筛选。

暂不进入 MVP 的内容：

- AI / LLM / agent。
- 手机号注册、邮箱验证、找回密码。
- 社交、点赞、公开收藏。
- 电商购买链接。
- 图片背景去除。
- 正式多级品类 / 子分类系统。
- 完整自由画布式穿搭拼贴编辑器。

## 当前状态

- [x] 初始化项目仓库
- [x] 完成 brainstorming
- [x] 完成 SPEC.md
- [x] 更新 SPEC_PROCESS.md
- [x] 更新 PROJECT_BRIEF.md
- [x] 按 SPEC 阶段更新 README.md
- [x] 完成 PLAN.md
- [x] 完成冷启动验证
- [x] 创建后端、前端和测试入口骨架
- [x] 创建后端 MySQL schema migration 并用 Testcontainers 验证
- [x] 创建后端 JPA 领域对象、枚举转换和 Repository 映射验证
- [x] 实现后端注册、登录、退出和 HttpOnly Cookie 会话
- [x] 实现后端个人资料查看、昵称修改和密码修改 API
- [x] 建立前端应用壳、placeholder route 和 Cookie credentials API client
- [x] 实现前端注册、登录、退出、个人资料、昵称修改和密码修改页面
- [x] 实现核心功能
- [x] 提供 Docker Compose 本地分发
- [ ] 配置测试与 CI
- [ ] 完成部署与分发

## 项目文档

- `SPEC.md`：项目设计文档
- `PLAN.md`：实现计划
- `SPEC_PROCESS.md`：规约生成过程记录
- `docs/cold-start-validation.md`：冷启动验证报告
- `PROJECT_BRIEF.md`：项目上下文简报，供新的 AI 会话理解当前状态
- `AGENT_LOG.md`：智能体协作开发日志
- `REFLECTION.md`：最终反思报告

## 目录结构

当前仓库处于实现早期阶段，已包含项目骨架、数据库迁移、JPA/Repository 映射、后端认证 / 个人资料 API 和前端认证 / 个人资料页面，主要文件包括：

```text
.
├── README.md
├── SPEC.md
├── SPEC_PROCESS.md
├── PROJECT_BRIEF.md
├── PLAN.md
├── AGENT_LOG.md
├── REFLECTION.md
├── Makefile
├── docker-compose.yml
├── e2e/
│   ├── README.md
│   └── run-backend-e2e.ps1
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fan/mixmyfit/MixMyFitApplication.java
│       ├── main/java/com/fan/mixmyfit/domain/
│       ├── main/java/com/fan/mixmyfit/domain/repository/
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/V1__initial_schema.sql
│       └── test/java/com/fan/mixmyfit/
│           ├── HealthSmokeTest.java
│           └── domain/
│               ├── MigrationScriptTest.java
│               ├── RepositoryMappingTest.java
│               └── SchemaMigrationTest.java
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   ├── package-lock.json
│   ├── index.html
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── api/
│       ├── router/
│       ├── stores/
│       ├── styles/
│       ├── views/
│       ├── App.vue
│       ├── App.test.ts
│       ├── main.ts
│       └── vue-shims.d.ts
├── docs/
│   ├── cold-start-validation.md
│   └── assignment/
│       ├── 通用要求.md
│       └── AI4SE_Final_Project_B_应用类项目.md
└── src/
```

后续实现阶段仍计划创建 `.gitlab-ci.yml` 等文件。

## 安装与运行

当前已有后端 API、前端页面、核心 E2E 和 Docker Compose 本地分发。CI 和线上部署仍未完成。

### Docker Compose 本地启动

首次启动完整本地系统：

```bash
docker compose up --build
```

启动后：

- 前端：`http://localhost:5173`
- 后端健康检查：`http://localhost:8080/actuator/health`
- MySQL：仅在 compose 网络内暴露给后端服务。

停止容器：

```bash
docker compose down
```

停止并删除持久化数据卷（会清空 MySQL 数据和上传文件）：

```bash
docker compose down -v
```

重新构建镜像：

```bash
docker compose build --no-cache
docker compose up
```

配置方式：

- 可通过 shell 环境变量或本地 `.env` 文件覆盖 `MYSQL_DATABASE`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD`。
- 默认值仅用于本地开发，不是生产凭据。
- 不要提交真实 API Key、Token、数据库密码或其他生产凭据。

数据目录：

- MySQL 数据保存在 Docker named volume `mysql-data`。
- 上传文件保存在 Docker named volume `upload-data`，挂载到后端容器 `/app/uploads`。

已知限制：

- 首次 `docker compose up --build` 会下载 Maven、Node、MySQL、Nginx 和 JRE 基础镜像，耗时取决于网络。
- 当前 Compose 配置面向本地开发分发，不包含 HTTPS、生产域名、生产 Secret 管理或线上备份策略。
- 如端口 `5173` 或 `8080` 被占用，需要先停止占用进程，或在本地 `docker-compose.override.yml` 中调整端口映射。

后端测试命令：

```bash
cd backend && mvn test
```

前端依赖安装、构建和测试命令：

```bash
cd frontend && npm ci && npm run build && npm test -- --run
```

顶层测试入口：

```bash
make test
```

核心 E2E 测试入口：

```bash
make test-e2e
```

Windows 环境如果没有 GNU Make，但安装了 MinGW Make，可运行：

```bash
mingw32-make test-e2e
```

本地环境需要自行安装 Java 17+、Maven、Node.js、npm 和 make。后端 schema 测试和 E2E 测试使用 Testcontainers MySQL，因此运行完整后端测试或 E2E 时还需要 Docker daemon 可用。当前开发机 shell 中 `mvn` 和 `make` 不在 PATH；E2E 验证使用 `mingw32-make test-e2e`，并由 `e2e/run-backend-e2e.ps1` 自动查找本机 Maven 或 Maven wrapper 缓存。

## 测试

当前测试套件覆盖：

- 后端：Spring Boot context 启动并暴露 `/actuator/health`。
- 后端数据库：Flyway 可在 Testcontainers MySQL 上干净执行 `V1__initial_schema.sql`，并验证必需表、关键字段、唯一约束、枚举约束和跨用户归属约束。
- 后端 Repository：JPA Entity 与 Spring Data Repository 可在 Testcontainers MySQL 上保存和读取核心对象，并验证 draft 衣物、标签唯一约束和 `outfit_items` 外键约束。
- 后端认证：`POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/logout`，覆盖 BCrypt 密码哈希、重复用户名、统一登录失败信息、`MMF_SESSION` HttpOnly Cookie、7 天过期、`SameSite=Lax` 和登出清理。
- 前端：Vue 应用壳渲染 `MixMyFit`，`/app` 渲染已登录应用壳占位 route，API client 默认使用 Cookie credentials；认证 UI 覆盖登录、注册、退出本地状态清理；个人资料 UI 覆盖 username/nickname 渲染、昵称修改成功/错误状态、密码修改必填校验和密码值不写入日志。
- E2E：`CoreFlowE2eTest` 覆盖注册、登录、上传两张衣物图片、批量补全品类/颜色/季节/标签、创建搭配、保存搭配、按标签筛选，以及用户 A 不能直接访问用户 B 的衣物或搭配方案。

## 前端 Open Design 方向

T11 前端基线采用面向衣橱管理工具的 Open Design 方向：第一屏直接进入应用壳，不做营销式 landing page；整体以安静、实用、可扫描的工作台体验为目标。后续 UI 应延续克制的中性色底、清晰层级、稳定尺寸控件和受控图片容器，并通过 `object-fit`、固定展示区域和懒加载约束衣物图片展示。当前仅记录设计方向和基础结构，不表示最终业务 UI 已完成。

后续任务会逐步加入 CI 测试。

## 分发与部署

当前已提供 Docker Compose 本地分发方式，可启动前端、后端和 MySQL。线上部署 URL 尚未提供，最终部署平台将在部署阶段确认。

## 安全边界

MixMyFit MVP 不调用 LLM、agent 或外部 AI API，因此不需要配置 LLM API key。

后续实现仍必须保护：

- 用户密码。
- 会话 Cookie。
- 数据库连接配置。
- 生产环境配置。
- 上传文件访问路径。

真实数据库密码、Token 或其他生产凭据不得提交到 Git。后续如需要配置文件模板，应只提交示例配置，不提交真实凭据。

T3A 认证会话实现使用后端设置的 `MMF_SESSION` Cookie。Cookie 本地默认 `HttpOnly`、`SameSite=Lax`、7 天过期；生产 HTTPS 部署时应通过配置启用 Secure Cookie。密码使用 BCrypt 哈希保存，不保存明文密码。

## 已知限制

- 当前 Compose 配置使用本地开发默认密码；生产环境必须改用部署平台 Secret 或受控环境变量。
- CI、部署和线上 URL 尚未完成。
- 当前开发机 shell 中 `mvn` 和 `make` 不在 PATH；需要配置本地工具链后才能直接运行 `cd backend && mvn test`、`make test` 和 `make test-e2e`。当前可用替代命令是 `mingw32-make test-e2e`。
