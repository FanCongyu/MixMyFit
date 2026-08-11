# MixMyFit

## 项目简介

MixMyFit 是一个个人衣橱搭配管理 Web 应用，面向希望数字化管理衣物并进行穿搭组合的普通用户。

用户可以上传自己的衣物图片，按品类、颜色、季节和标签管理衣物，并在搭配编辑器中组合、预览、保存和复用穿搭方案。项目目标是减少用户反复试穿或手工拼图的成本，让用户能基于自己的真实衣物图片完成搭配规划。

本项目是 AI4SE 期末项目 B（非 harness 应用类项目）。当前已完成 SPEC、PLAN 与冷启动验证，尚未进入业务实现。

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
- [ ] 实现核心功能
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

当前仓库仍处于规格设计阶段，主要文件包括：

```text
.
├── README.md
├── SPEC.md
├── SPEC_PROCESS.md
├── PROJECT_BRIEF.md
├── PLAN.md
├── AGENT_LOG.md
├── REFLECTION.md
├── docs/
│   ├── cold-start-validation.md
│   └── assignment/
│       ├── 通用要求.md
│       └── AI4SE_Final_Project_B_应用类项目.md
└── src/
```

实现阶段计划创建 `backend/`、`frontend/`、`e2e/`、`docker-compose.yml` 和 `.gitlab-ci.yml` 等文件。目前这些实现目录和分发文件尚未生成。

## 安装与运行

当前没有可安装或可运行的应用。

计划技术栈为 Spring Boot REST API（Java 17 + Maven）、Vue、MySQL、Flyway、Testcontainers MySQL、Docker Compose 和 GitLab CI。安装与运行命令将在进入实现阶段后补充。

## 测试

当前尚未进入实现阶段，因此还没有可运行的测试套件。

计划中的测试策略包括后端测试、前端测试、少量 E2E 测试，以及 GitLab CI 中名为 `unit-test` 的 job。具体命令将在实现阶段补充。

## 分发与部署

当前尚未产生分发产物，也没有线上部署 URL。

SPEC 中暂定分发方式为 Docker Compose，最终部署平台将在部署阶段确认。

## 安全边界

MixMyFit MVP 不调用 LLM、agent 或外部 AI API，因此不需要配置 LLM API key。

后续实现仍必须保护：

- 用户密码。
- 会话 Cookie。
- 数据库连接配置。
- 生产环境配置。
- 上传文件访问路径。

真实数据库密码、Token 或其他生产凭据不得提交到 Git。后续如需要配置文件模板，应只提交示例配置，不提交真实凭据。

## 已知限制

- 当前仅完成 SPEC、PLAN 和冷启动验证文档，没有可运行应用。
- 后端、前端、数据库迁移、Docker 和 CI 文件尚未创建。
- Docker、CI、部署和线上 URL 尚未完成。
