# PROJECT_BRIEF

## 课程与项目类型

这是 AI4SE 期末项目，选择 B：非 harness 应用类项目。
完整要求 = 通用要求 + B 类项目要求。

## 核心约束

1. 必须使用 Superpowers 全流程。
2. 在 SPEC.md 和 PLAN.md 完成并通过冷启动验证前，禁止编写实现代码。
3. 必须遵守 TDD：先写失败测试，再写最小实现，再重构。
4. 必须有 SPEC.md、PLAN.md、SPEC_PROCESS.md、AGENT_LOG.md、README.md、REFLECTION.md。
5. 凡涉及前端 / UI，使用 **[Open Design](https://github.com/nexu-io/open-design)** 进行界面开发，并在 SPEC 中说明所选设计系统与 skill。
6. 必须有可一键运行测试。
7. 必须有 CI，.gitlab-ci.yml 中包含 unit-test job。
8. 必须考虑凭据安全和分发。
9. 最终需要线上可访问 WebUI。

## 我的初步项目想法

项目名称暂定：
MixMyFit

目标用户：
普通用户

要解决的问题：
用户在对自己的衣服（上装、下装、鞋子、帽子）进行搭配时，通常只能通过实际试穿或图片拼接等方式完成，不仅过程繁琐，也不便于操作。本应用旨在为用户提供一个便捷的衣物搭配与套装保存平台。


核心功能初步设想：
1. 用户注册与登录
2. 服装图片上传与分类管理
3. 服装搭配组合功能
4. 搭配方案保存与查看
5. 搭配方案管理
6. 【可选模块】收藏或点赞搭配、图片背景去除

技术栈倾向：
后端：【Spring Boot / FastAPI / Node.js 等】
前端：【Vue / React / Thymeleaf 等】
数据库：【SQLite / PostgreSQL / MySQL / H2 等】
部署：【Docker + Render/Railway/服务器/NJU GitLab CI 等】

AI 使用方式：
不使用
如果使用 LLM，必须设计 API key 安全存储，不得硬编码。

## 对 Codex 的要求

请先使用 Superpowers 的 brainstorming 技能帮助我澄清项目方向。
不要写任何实现代码。
先通过提问帮助我确定需求、边界、功能模块、数据模型、凭据安全、分发方式、测试策略和验收标准。
每一部分设计完成后，请分块展示给我确认。
只有在我明确确认设计后，才能生成 SPEC.md。
SPEC.md 确认后，再进入 writing-plans 生成 PLAN.md。