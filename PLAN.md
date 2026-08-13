# MixMyFit PLAN

> **给 agentic workers 的要求：** 实施本计划时必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按 task 逐个执行。步骤使用复选框（`- [ ]`）追踪。在 T0 冷启动验证完成并记录之前，禁止开始任何实现 task。

**目标：** 构建 MixMyFit MVP：一个个人衣橱与穿搭规划 Web 应用，用户可以上传衣物图片、管理衣物元数据、组合穿搭、保存搭配方案，并在之后筛选复用。

**架构：** Vue 负责 WebUI，并通过 REST API 与 Spring Boot 后端通信。Spring Boot 负责认证、授权、业务规则、文件上传安全和 MySQL 持久化。上传图片存储在服务端本地文件系统中，并通过 Docker volume 持久化。

**技术栈：** Spring Boot REST API、Java 17、Maven、Vue 前端、MySQL、Flyway、Testcontainers MySQL、本地文件系统上传存储、Docker Compose、GitLab CI。

## 全局约束

- 在 `SPEC.md` 和 `PLAN.md` 完成并通过 T0 冷启动验证之前，不得编写任何实现代码。
- 不得加入 AI / LLM / agent 功能。
- 账号模型为唯一用户名 + 密码 + 可选昵称。
- 认证采用后端设置登录态 Cookie；前端不得直接管理 token。
- 后端根包名固定为 `com.fan.mixmyfit`。
- 后端使用 Java 17 + Maven。
- 数据库迁移固定使用 Flyway，迁移文件放在 `backend/src/main/resources/db/migration/`。
- 后端集成测试使用 Testcontainers MySQL；除非文档显式修订，不得用 H2 替代 MySQL 行为。
- 登录 Cookie 名称固定为 `MMF_SESSION`，必须设置 `HttpOnly`，默认 7 天过期；线上 HTTPS 部署时必须设置 `Secure`；本地默认 `SameSite=Lax`，跨站部署时在 T20 重新确认。
- 后端 API 必须遵循 `SPEC.md` 的“后端 API 最小合约”，包括路径、方法、字段名、状态码和统一错误响应格式。
- 统一错误响应格式为 `{ "code": "ERROR_CODE", "message": "Human readable message" }`。
- 用户只能访问、修改或删除自己的衣物、品类、标签、搭配方案、搭配明细和上传文件。
- 衣物只有同时具备图片 + 品类时，才能进入搭配编辑器。
- 文件上传 multipart 字段名固定为 `file`，单张图片上限 5 MB，仅允许 JPEG、PNG、WebP。
- 上传根目录通过 `UPLOAD_DIR` 配置，本地默认 `backend/uploads`，测试环境必须覆盖为临时目录。
- MVP 不生成服务端缩略图；前端必须使用受控图片展示尺寸。
- 固定搭配主槽位只能是上装、下装、鞋子、帽子。
- 自定义品类默认作为配饰，不得自动变成固定主槽位。
- 前端 / UI 开发必须使用 `Open Design` 进行，所有前端 task 必须沿用 T11 确认的设计系统与交互规则。
- 每个实现 task 必须使用 TDD：先写失败测试，确认红灯，再写最小实现，确认绿灯，再重构。
- `.gitlab-ci.yml` 必须包含名为 `unit-test` 的 job。
- `REFLECTION.md` 由学生本人撰写，不由 AI 编写。

## 依赖关系图

主链路：

`T0 -> T1 -> T2A -> T2B -> T3A -> T3B -> T4 -> (T5 || T6) -> T7A -> T7B -> T8 -> T9 -> T10 -> T17 -> (T18 || T19) -> T20 -> T21`

前端链路：

`T1 -> T11 -> T12 -> T13A -> T13B -> T14 -> T15 -> T16 -> T17`

可并行部分：

- T5 和 T6 可在 T4 后并行。
- T11 可在 T1 后启动，同时后端业务 task 继续推进。
- T12 可在 T3B 和 T11 后启动，同时后端衣物/搭配 task 继续推进。
- T18 和 T19 可在 T17 后并行。

## 文件结构规划

- `backend/`：Spring Boot API 服务。
- `backend/src/main/java/com/fan/mixmyfit/auth/`：注册、登录、退出、当前会话用户处理。
- `backend/src/main/java/com/fan/mixmyfit/user/`：个人资料、昵称修改、密码修改。
- `backend/src/main/java/com/fan/mixmyfit/security/`：Cookie 配置、认证过滤器/拦截器、归属校验辅助。
- `backend/src/main/java/com/fan/mixmyfit/category/`：固定品类与自定义品类 API。
- `backend/src/main/java/com/fan/mixmyfit/tag/`：衣物标签与搭配方案标签。
- `backend/src/main/java/com/fan/mixmyfit/file/`：上传校验、文件存储、图片访问。
- `backend/src/main/java/com/fan/mixmyfit/clothing/`：衣物 CRUD、筛选、批量操作。
- `backend/src/main/java/com/fan/mixmyfit/outfit/`：搭配方案保存、编辑、删除、筛选。
- `backend/src/main/resources/db/migration/`：MySQL schema migration。
- `backend/src/test/`：后端单元测试与集成测试。
- `frontend/`：Vue WebUI。
- `frontend/src/api/`：API client 与请求/响应契约。
- `frontend/src/views/`：页面级视图。
- `frontend/src/components/`：可复用 UI 组件。
- `frontend/src/stores/`：认证状态与应用状态。
- `e2e/`：端到端工作流测试。
- `docker-compose.yml`：本地分发运行环境。
- `.gitlab-ci.yml`：包含 `unit-test` 的 CI pipeline。
- `README.md`：面向用户的安装、运行、安全、分发和部署说明。
- `AGENT_LOG.md`：AI 工作流时间线记录。
- `SPEC_PROCESS.md`：计划阶段与冷启动验证记录。

---

## Tasks

### T0：实现前冷启动验证

**目标：** 在编写任何代码之前，验证一个不同类型的新智能体能否只凭 `SPEC.md` + `PLAN.md` 理解任务，而不依赖隐藏上下文。

**依赖关系：** 已确认的 PLAN 草案。

**涉及文件：**
- 修改：`SPEC_PROCESS.md`
- 如验证暴露计划缺口，修改：`PLAN.md`
- 仅当验证暴露规约缺陷时，修改：`SPEC.md`

**预期实现要点：**
- 启动一个与主开发 Codex 不同类型的新智能体会话。
- 只提供 `SPEC.md` 和 `PLAN.md`。
- 要求它选择 1-2 个 task，并说明会如何开始。
- 明确要求它遇到不确定之处必须暂停提问，而不是猜测继续。
- 记录它的问题、误解和需要修订的文档点。

**TDD 验证步骤：**
- 这是文档验证 task，不是实现 task。
- 验证标准是冷启动智能体能否在没有对话历史的情况下识别 task 输入、依赖、文件路径和第一批失败测试。

**需要先写的失败测试：**
- 不适用。改为先在 `SPEC_PROCESS.md` 记录以下冷启动验证 prompt：

```text
冷启动验证 prompt：
你是一个全新的实现智能体。你只能看到 SPEC.md 和 PLAN.md。
请从 PLAN.md 中选择 1-2 个 task，并说明你会如何用 TDD 开始。
如果任何依赖、文件路径、接口或预期行为不清楚，请暂停并提问。
不要猜测，不要编写实现代码。
```

**完成标准：**
- `SPEC_PROCESS.md` 记录冷启动 prompt、被选 task、智能体问题、误读点和对应文档修订。
- 任何关键歧义已在 `PLAN.md` 或 `SPEC.md` 中修复。
- 用户确认可以开始实现阶段。

**是否可并行：** 否。必须先于 T1 完成。

**建议分支 / worktree：** `docs/t0-cold-start-validation`

### T1：仓库基线与项目骨架

**状态：** [x] 已完成，2026-08-11。

**完成记录：**
- 已创建最小 Spring Boot 后端骨架、Vue + Vitest 前端骨架和顶层 `Makefile`。
- 已按 TDD 先写 `HealthSmokeTest` 与 `App.test.ts` 并确认红灯，再补充最小实现。
- 后端 smoke test 使用 JetBrains 自带 Maven 可执行文件验证通过；当前 shell 中 `mvn` 不在 PATH。
- 前端 `npm ci`、`npm run build`、`npm test -- --run` 验证通过。
- 当前 shell 中 `make` 不在 PATH，因此 `make test` 入口已创建但未能在本机直接执行。
- 本 task 未实现业务 endpoint、业务 UI flow、数据库迁移、Docker 或 CI。
- Commit hash：`8bd770dcbd595d3befd578b346c7cda1f1b16b8f`。

**目标：** 创建后端、前端、测试入口和共享配置的最小可运行项目骨架，不包含业务行为。

**依赖关系：** T0。

**涉及文件：**
- 创建：`backend/`
- 创建：`frontend/`
- 创建：`Makefile`
- 修改：`.gitignore`
- 修改：`README.md`
- 修改：`AGENT_LOG.md`

**预期实现要点：**
- 后端使用 Java 17、Maven、Spring Boot，根包名固定为 `com.fan.mixmyfit`。
- 后端骨架支持无业务健康检查测试。
- 后端 Maven 命令为 `cd backend && mvn test`。
- 前端骨架支持无业务渲染或构建测试。
- 前端使用 Vue + Vitest；前端依赖安装与验证命令为 `cd frontend && npm ci && npm run build && npm test -- --run`。
- `Makefile` 预留稳定命令：`make test`、`make test-backend`、`make test-frontend`。
- README 必须说明应用仍处于实现阶段，当前只存在骨架命令。

**TDD 验证步骤：**
- 先写后端骨架测试。
- 先写前端骨架测试或构建检查。
- 在搭建骨架前确认两者失败。
- 添加最小框架文件。
- 运行 `make test`。

**需要先写的失败测试：**
- 后端：`backend/src/test/java/com/fan/mixmyfit/HealthSmokeTest.java`，断言 Spring context 能启动且 health endpoint 可用。
- 前端：`frontend/src/App.test.ts`，使用 Vitest + Vue Test Utils 断言默认应用壳能渲染，例如 `expect(screen.getByText("MixMyFit")).toBeTruthy()`。

**完成标准：**
- `make test` 能运行后端和前端检查。
- 不存在业务 endpoint 或业务 UI flow。
- README 与 AGENT_LOG 记录骨架 task 状态。

**是否可并行：** 否。

**建议分支 / worktree：** `task/01-project-skeleton`

### T2A：后端数据库迁移 SQL 与 Flyway 验证

**状态：** [x] 已完成，2026-08-11。

**完成记录：**
- 已添加 Flyway 初始迁移 `backend/src/main/resources/db/migration/V1__initial_schema.sql`。
- 已创建 `users`、`categories`、`clothes`、`clothing_seasons`、`clothing_tags`、`clothing_tag_links`、`outfit_tags`、`outfits`、`outfit_seasons`、`outfit_tag_links`、`outfit_items`。
- 已用 MySQL check constraint、foreign key 和 unique constraint 覆盖用户名唯一、用户内品类/标签唯一、固定/自定义品类归属、枚举值、衣物 draft/ready 与品类关系、outfit item 与 clothing 同用户归属等规则。
- 已添加 Testcontainers MySQL schema 测试，并在测试启动时显式执行 Flyway migration。
- 已将 Testcontainers 升级到 `2.0.5`，以兼容当前 Docker Desktop / Docker API 环境。
- RED：`MigrationScriptTest` 因 `V1__initial_schema.sql` 缺失失败；初次 `SchemaMigrationTest` 在 Docker 未启动或旧 Testcontainers 无法识别 Docker 环境时失败；Docker 可用后暴露 Flyway 未执行导致缺表失败。
- GREEN：`SchemaMigrationTest` 通过，`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`；完整后端 `mvn test` 通过，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- 本 task 未实现 REST 业务行为，未创建 JPA Entity 或 Repository，未写入真实凭据。
- Commit hash：`75334beb4f6725e3824ce9105da911833e3dd593`。

**目标：** 使用 Flyway 实现 SPEC 中所有实体与约束对应的 MySQL schema，并用 Testcontainers MySQL 验证迁移可自动执行。

**依赖关系：** T1。

**涉及文件：**
- 创建：`backend/src/main/resources/db/migration/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/domain/SchemaMigrationTest.java`

**预期实现要点：**
- 使用 Flyway，首个迁移文件命名为 `V1__initial_schema.sql`。
- 测试环境使用 Testcontainers MySQL，并在 Spring Boot 测试启动时自动执行 Flyway migration。
- 表包括：`users`、`categories`、`clothes`、`clothing_seasons`、`clothing_tags`、`clothing_tag_links`、`outfit_tags`、`outfits`、`outfit_seasons`、`outfit_tag_links`、`outfit_items`。
- 使用可区分 ID 字段名，例如 `user_id`、`clothing_id`、`outfit_id`。
- 约束 username 全局唯一。
- 约束同一用户下自定义品类名称、衣物标签名称、搭配方案标签名称不重复。
- 固定品类使用 `user_id = null` 且 `type = fixed` 表达。
- 自定义品类使用 `user_id != null` 且 `type = custom` 表达。

**TDD 验证步骤：**
- 先写 migration/schema 测试。
- 确认缺表时测试失败。
- 添加 Flyway 迁移 SQL。
- 使用 Testcontainers MySQL 确认测试通过。

**需要先写的失败测试：**
- schema 测试断言所有必需表、字段和唯一约束存在。
- 约束测试断言重复 username 被拒绝。
- 约束测试断言同一用户下重复品类名被拒绝，不同用户可使用同名自定义品类。

**完成标准：**
- 数据库迁移可干净执行。
- schema 测试通过。
- 本 task 不实现 REST 业务行为。
- 本 task 不创建 JPA Entity 或 Repository；这些属于 T2B。

**是否可并行：** 否。

**建议分支 / worktree：** `task/02a-backend-migration`

### T2B：后端领域对象、ORM 映射与 Repository 验证

**状态：** [x] 已完成，2026-08-12。

**完成记录：**
- 已添加 Spring Data JPA 依赖，并关闭 `spring.jpa.open-in-view`。
- 已为 `users`、`categories`、`clothes`、`clothing_seasons`、`clothing_tags`、`clothing_tag_links`、`outfit_tags`、`outfits`、`outfit_seasons`、`outfit_tag_links`、`outfit_items` 建立最小 JPA Entity 映射。
- 已添加数据库小写枚举值与 Java enum 的 AttributeConverter，保持 schema 值如 `draft`、`ready`、`main_slot` 与 Java 枚举命名分离。
- 已添加 11 个 Spring Data Repository 接口，位于 `backend/src/main/java/com/fan/mixmyfit/domain/repository/`。
- 已添加 `RepositoryMappingTest`，使用 Testcontainers MySQL 和 Flyway 迁移验证 Repository 映射，不使用 H2。
- RED：新增 Repository 映射测试因缺少 Entity/Repository 和 JPA/DataAccess 依赖编译失败；首次 GREEN 尝试暴露 `outfit_items.user_id` 复合外键映射冲突，后修正为本地 id 列可写、关联对象只读导航。
- GREEN：完整后端 `mvn test` 通过，`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`。
- 本 task 未实现 REST endpoint、认证、业务 service、前端 UI、Docker 或 CI，未写入真实凭据。
- Commit hash：`e7e5a1c55c7757510e4d67dbfd70a4bffc96ebd4`。

**目标：** 在 T2A 的 schema 基础上实现领域对象、JPA/ORM 映射和 Repository 测试，确保 Java 模型与数据库约束一致。

**依赖关系：** T2A。

**涉及文件：**
- 创建：`backend/src/main/java/com/fan/mixmyfit/domain/`
- 创建：`backend/src/main/java/com/fan/mixmyfit/*/repository/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/domain/`

**预期实现要点：**
- 为 `users`、`categories`、`clothes`、`clothing_seasons`、`clothing_tags`、`clothing_tag_links`、`outfit_tags`、`outfits`、`outfit_seasons`、`outfit_tag_links`、`outfit_items` 建立映射。
- Java 字段命名与 API 响应区分：数据库字段保留 `user_id`，Java/JSON 可使用 `userId`。
- Repository 测试必须运行在 Testcontainers MySQL 上，不使用 H2。
- 不实现 REST endpoint。

**TDD 验证步骤：**
- 先写 Repository 映射测试。
- 确认缺少 Entity/Repository 时测试失败。
- 添加最小 Entity、Repository 和映射配置。
- 运行后端测试确认映射测试通过。

**需要先写的失败测试：**
- 可以保存并读取用户、固定品类、用户自定义品类和衣物草稿。
- `category_id = null` 的衣物可保存为 draft。
- 同一用户下重复衣物标签名称被数据库或 Repository 层拒绝。
- `outfit_items` 引用不存在的 clothing 时被拒绝。

**完成标准：**
- Repository 映射测试通过。
- T2A 的迁移测试继续通过。
- 本 task 不实现 REST 业务行为。

**是否可并行：** 否。

**建议分支 / worktree：** `task/02b-backend-domain-mapping`

### T3A：后端注册、登录、退出与 HttpOnly Cookie 会话

**状态：** [x] 已完成，2026-08-12。

**完成记录：**
- 已实现 `POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/logout`。
- 注册成功返回 `201` 和不含密码哈希的用户响应；登录成功返回 `200`、用户响应和 `MMF_SESSION` Cookie；登出成功返回 `204` 并清除 Cookie。
- 已使用 `spring-security-crypto` 的 BCrypt 保存密码哈希，未保存明文密码。
- 已拒绝重复用户名；登录失败统一返回 `INVALID_CREDENTIALS`，不区分用户名是否存在。
- `MMF_SESSION` 使用服务端生成的随机不透明 session id；Cookie 设置 `HttpOnly`、`SameSite=Lax`、默认 `Max-Age=604800` 秒；登出时同时删除服务端内存 session 映射。
- 为保持原有无数据库 health smoke test 范围，`HealthSmokeTest` 使用 test-only `UserRepository` mock。
- RED：新增 `AuthEndpointTest` 后，因缺少 `spring-security-crypto`、`PasswordEncoder` 和 `UserRepository.findByUsername` 编译失败；随后新增登出服务端 session 失效断言，因 session 未删除失败。
- GREEN：`AuthEndpointTest` 通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`；完整后端 `mvn test` 通过，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- 分支 / worktree：`task/03a-auth-session`，工作区 `D:\My Work\Homework\智能化软件工程师训练营\MixMyFit-task-03a-auth-session`，已确认是 linked worktree。
- 测试结果：使用本机 `.m2\wrapper` 中 Maven 可执行文件运行 `mvn test`，最终通过，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- Spec 合规检查结论：通过；Critical issues：无；处理结果：无需阻塞修复。
- 代码质量检查结论：通过；Critical issues：无；处理结果：非阻塞建议留待后续 task 或全局错误处理整理。
- 完成分支决定：保留当前分支，暂不开 PR / 暂不合并；原因是 README 和过程记录需要收尾，且提交后还需回填 commit hash。
- 本 task 未实现 `GET /api/profile`、`PATCH /api/profile` 或 `POST /api/profile/password`；这些仍属于 T3B。未实现 T4 用户隔离 helper，未写入真实凭据。
- Commit hash：`f328fda4aacf38aff10e4ec2d717f90fd0857fa2`。

**目标：** 实现注册、登录、退出、密码哈希和后端管理的 HttpOnly Cookie 会话。

**依赖关系：** T2B。

**涉及文件：**
- 创建：`backend/src/main/java/com/fan/mixmyfit/auth/`
- 创建/修改：`backend/src/main/java/com/fan/mixmyfit/security/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/auth/`

**预期实现要点：**
- 必须实现 `SPEC.md` 中认证 API 合约：`POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/logout`。
- 注册成功返回 `201` 和 `userId`；登录成功返回 `200` 和 `Set-Cookie`；登出成功返回 `204`。
- 注册字段包括用户名、密码、确认密码、可选昵称。
- 只保存密码哈希，不保存明文。
- 拒绝重复用户名。
- 用户名或密码错误时，登录失败信息不泄露用户名是否存在。
- 登录成功设置名为 `MMF_SESSION`、默认 7 天过期、带 `HttpOnly` 的 Cookie。
- 本地默认 `SameSite=Lax`；线上 HTTPS 时必须设置 `Secure`。
- 退出时通过同名 `MMF_SESSION` Cookie 的 `Max-Age=0` 或等效过期时间清除 Cookie。
- 前端可见 API 不返回 password hash。

**TDD 验证步骤：**
- 先写认证 endpoint 测试。
- 确认注册、登录、退出测试失败。
- 实现最小 auth service 与 controller。
- 确认测试通过。

**需要先写的失败测试：**
- `POST /api/auth/register` 返回 `201` 和 `userId`，且密码以哈希而非明文保存。
- 重复用户名返回安全错误。
- `POST /api/auth/login` 返回 `200`，响应包含名为 `MMF_SESSION` 且带 `HttpOnly` 的 `Set-Cookie`。
- `POST /api/auth/logout` 返回 `204` 并清除 `MMF_SESSION`。

**完成标准：**
- 认证测试通过。
- Cookie 安全标志有测试覆盖。
- 不包含个人资料更新或修改密码；这些属于 T3B。

**是否可并行：** 否。

**建议分支 / worktree：** `task/03a-auth-session`

### T3B：后端个人资料查看、昵称修改与密码修改

**状态：** [x] 已完成，2026-08-12。

**完成记录：**
- 已实现 `GET /api/profile`、`PATCH /api/profile`、`POST /api/profile/password`。
- 已登录用户可读取 `userId`、`username`、`nickname`；响应不包含 password 或 password hash。
- 已登录用户可修改自己的昵称；测试覆盖用户 A 修改昵称不影响用户 B。
- 修改密码必须提供旧密码并验证；旧密码错误返回安全错误；修改成功后旧密码登录失败，新密码可登录。
- 未登录 profile 请求返回 `401 Unauthorized`，符合 `SPEC.md` 需要认证接口的状态码约定。
- RED：新增 `ProfileEndpointTest` 后，profile 相关 endpoint 缺失导致 `404`；修正未登录状态码 contract 时先观察到 `expected: 401 UNAUTHORIZED but was: 400 BAD_REQUEST`。
- GREEN：`ProfileEndpointTest` 通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- 分支 / worktree：`task/03b-profile-password`，工作区 `D:\My Work\Homework\智能化软件工程师训练营\MixMyFit-task-03b-profile-password`，已确认是 linked worktree。
- 测试结果：`mvn -Dtest=ProfileEndpointTest test` 通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`；`mvn -Dtest=AuthEndpointTest,ProfileEndpointTest test` 通过，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`；完整后端 `mvn test` 通过，`Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`。
- SPEC 合规检查：通过；Critical issues：无。
- 代码质量检查：通过；Critical issues：无；非阻塞建议留待 T4 或测试 support 整理。
- 完成分支决定：保留，暂不开 PR / 不合并 / 不丢弃。
- Commit：`78e94e3`。

**目标：** 实现个人资料读取、昵称修改和需要验证旧密码的密码修改。

**依赖关系：** T3A。

**涉及文件：**
- 创建：`backend/src/main/java/com/fan/mixmyfit/user/`
- 修改：`backend/src/main/java/com/fan/mixmyfit/auth/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/user/`

**预期实现要点：**
- 已登录用户可以查看用户名和昵称。
- 已登录用户可以修改昵称。
- 修改密码必须提供并验证旧密码。
- 修改密码后，旧密码不能继续登录。
- 未登录请求被拒绝。

**TDD 验证步骤：**
- 先写个人资料和修改密码测试。
- 确认测试失败。
- 实现最小 profile service 与 endpoint。
- 确认测试通过。

**需要先写的失败测试：**
- `GET /api/profile` 返回当前用户的 username 和 nickname。
- `PATCH /api/profile` 只修改当前用户昵称。
- `POST /api/profile/password` 在旧密码错误时拒绝。
- 成功修改密码后，旧密码登录失败。

**完成标准：**
- 个人资料与修改密码测试通过。
- T3A 的会话契约保持不变。

**是否可并行：** 否。

**建议分支 / worktree：** `task/03b-profile-password`

### T4：后端安全边界与用户隔离测试基线

**状态：** 已完成。

**执行分支 / worktree：** `task/04-user-isolation`

**测试结果：**
- RED：`mvn -Dtest=UserIsolationSupportTest test` 因缺少 `CurrentUserResolver`、`OwnershipGuard`、`AccessDeniedException` 编译失败，确认失败测试有效。
- GREEN：`mvn -Dtest=UserIsolationSupportTest test` 通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- 相关测试：`mvn -Dtest=UserIsolationSupportTest,ProfileEndpointTest,AuthEndpointTest test` 通过，`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- 完整后端测试：`mvn test` 通过，`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。

**SPEC 合规检查：** 通过。

**代码质量检查：** 通过。

**完成分支决定：** 开 PR。

**Commit hash：** `505f4e28d2015e4587b959d554a513cdef8be793`。

**目标：** 提供可复用的当前用户与资源归属校验工具，以及用户隔离测试 fixture。

**依赖关系：** T3A。

**涉及文件：**
- 修改：`backend/src/main/java/com/fan/mixmyfit/security/`
- 创建：`backend/src/test/java/com/fan/mixmyfit/support/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/security/`

**预期实现要点：**
- 为 controller/service 提供一致的当前 `user_id` 获取方式。
- 提供可复用的用户 A / 用户 B 测试辅助。
- 添加未登录访问被拒绝的负向测试。
- 后续涉及数据访问的 task 必须使用这些 fixture 证明跨用户访问被拒绝。

**TDD 验证步骤：**
- 先写失败的安全 fixture 测试。
- 实现 current-user resolver 和 ownership helper。
- 确认测试通过。

**需要先写的失败测试：**
- 不带 session Cookie 的请求返回未授权。
- 测试辅助能创建两个带独立 Cookie 的登录用户。
- 资源归属 helper 拒绝访问其他用户资源。

**完成标准：**
- 安全辅助工具可供后续后端测试使用。
- 除测试 helper 所需内容外，不新增业务资源 endpoint。

**是否可并行：** 否。

**建议分支 / worktree：** `task/04-user-isolation`


### T5：后端品类与标签 API

**状态：** 已完成。

**分支 / worktree：** `task/05-category-tags`；`D:\My Work\Homework\智能化软件工程师训练营\MixMyFit-task-05-category-tags`。

**测试结果：**
- TDD 红灯：`mvn test -Dtest=CategoryEndpointTest,TagEndpointTest` 初次运行 4 个测试因 endpoint 缺失返回 `404 NOT_FOUND`。
- Task 相关测试：`mvn test -Dtest=CategoryEndpointTest,TagEndpointTest` 通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- 回归测试：`mvn test -Dtest=HealthSmokeTest` 通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- 完整后端测试：`mvn test` 通过，`Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`。

**SPEC 合规检查：** 通过。

**代码质量检查：** 通过。

**完成分支决定：** 开 PR。

**Commit hash：** 8aabd8c31de8f4be2a077f2cca39a12267c4ab69。

**目标：** 实现固定/自定义品类 API，以及分离建模的衣物标签和搭配方案标签 API。

**依赖关系：** T4。

**涉及文件：**
- 创建：`backend/src/main/java/com/fan/mixmyfit/category/`
- 创建：`backend/src/main/java/com/fan/mixmyfit/tag/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/category/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/tag/`

**预期实现要点：**
- 初始化或暴露固定品类：上装、下装、鞋子、帽子。
- 允许用户创建自定义一级品类。
- 阻止同一用户下自定义品类重名。
- 衣物标签和搭配方案标签分开管理。
- 所有自定义品类和标签都必须绑定 `user_id`。

**TDD 验证步骤：**
- 先写品类/标签 API 测试。
- 确认 endpoint 缺失时测试失败。
- 实现 endpoint 和 service。
- 确认测试通过。

**需要先写的失败测试：**
- 已登录用户可以看到固定品类。
- 用户可以创建自定义品类。
- 用户 A 不能查看或修改用户 B 的自定义品类。
- 文本相同的衣物标签和搭配标签被分开存储。

**完成标准：**
- 品类和标签测试通过。
- API 可供衣物和搭配方案 task 使用。

**是否可并行：** 是。T4 后可与 T6 并行。

**建议分支 / worktree：** `task/05-category-tags`

### T6：后端安全文件上传与图片访问

**状态：** 已完成。

**分支 / worktree：** `task/06-secure-upload`；`D:\My Work\Homework\智能化软件工程师训练营\MixMyFit-task-06-secure-upload`。

**测试结果：**
- TDD 红灯：`mvn test -Dtest=SecureUploadEndpointTest` 因缺少 `clothing/file` 实现类编译失败；`mvn test -Dtest=StoredFileServiceConfigurationTest` 证明 `UPLOAD_DIR` 未生效。
- Task 相关测试：`mvn test -Dtest=SecureUploadEndpointTest,StoredFileServiceConfigurationTest` 通过，`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- 回归测试：`mvn test -Dtest=HealthSmokeTest` 通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- 编译验证：`mvn -DskipTests test` 通过。
- 完整后端测试：`mvn test` 未通过，原因是当前环境找不到可用 Docker，Testcontainers 测试无法启动；Task 6 新增测试已通过。

**SPEC 合规检查：** 通过。

**代码质量检查：** 通过。

**完成分支决定：** 保留。

**Commit hash：** 7915d108a5576c27881c940833580a71572a6875。

**目标：** 实现安全图片上传存储和带归属校验的图片访问。

**依赖关系：** T4。

**涉及文件：**
- 创建：`backend/src/main/java/com/fan/mixmyfit/file/`
- 修改：`backend/src/main/java/com/fan/mixmyfit/clothing/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/file/`

**预期实现要点：**
- 衣物上传 endpoint 使用 `POST /api/clothes`。
- multipart 文件字段名固定为 `file`。
- 上传成功返回 `201`，响应至少包含 `clothingId`、`status`、`imageUrl`、`originalFilename`、`contentType`、`fileSize`。
- MVP 文件大小上限确定为每张图片 5 MB。
- 只允许 JPEG、PNG、WebP MIME 类型。
- 服务端生成文件名，不信任原始文件名。
- 文件存储在 `UPLOAD_DIR` 配置的上传根目录下，本地默认 `backend/uploads`，测试环境覆盖为临时目录。
- 保存原始文件名、content type、大小和存储路径元数据。
- 图片访问 endpoint 使用 `GET /api/clothes/{clothingId}/image`，在返回文件前必须校验归属。

**TDD 验证步骤：**
- 先写上传校验测试。
- 确认非法类型、超大文件、跨用户访问测试失败。
- 实现存储和访问逻辑。
- 确认测试通过。

**需要先写的失败测试：**
- `POST /api/clothes` 使用字段名 `file` 上传图片时返回 `201` 和 `clothingId`。
- 上传非图片 MIME 类型被拒绝。
- 上传大于 5 MB 的文件被拒绝。
- 存储文件名不同于原始文件名。
- 用户 A 不能获取用户 B 上传的文件。

**完成标准：**
- 上传安全测试通过。
- 不包含完整衣物元数据编辑；这些属于 T7A/T7B。

**是否可并行：** 是。T4 后可与 T5 并行。

**建议分支 / worktree：** `task/06-secure-upload`

### T7A：后端衣物基础 CRUD 与归属校验

**状态：** 已完成

**分支 / worktree：** `task/07a-clothing-crud`; `D:\My Work\Homework\智能化软件工程师训练营\MixMyFit-task-07a-clothing-crud`

**测试结果：**
- `mvn test -Dtest=ClothingCrudEndpointTest` 通过；Tests run: 6, Failures: 0, Errors: 0, Skipped: 0。
- `mvn test -Dtest=ClothingCrudEndpointTest,SecureUploadEndpointTest,StoredFileServiceConfigurationTest` 通过；Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- `mvn test-compile` 通过。
- 完整 `mvn test` 未通过，阻塞原因是当前 Docker/Testcontainers 环境不可用；新增 T7A 与非 Docker 相关测试通过。

**SPEC 合规检查：** 通过

**代码质量检查：** 通过

**完成分支决定：** 开 PR

**Commit hash：** 9da4aa3b4c9756af1e402e7ee2f27aeae3277cff

**目标：** 实现衣物记录的创建、读取、更新、删除，并严格执行归属校验。

**依赖关系：** T5、T6。

**涉及文件：**
- 创建/修改：`backend/src/main/java/com/fan/mixmyfit/clothing/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/clothing/`

**预期实现要点：**
- 必须遵循 `SPEC.md` 衣物 API 合约：`GET /api/clothes/{clothingId}`、`GET /api/clothes`、`PATCH /api/clothes/{clothingId}`、`DELETE /api/clothes/{clothingId}`。
- 上传创建的衣物记录归属于当前用户。
- 支持编辑名称、品类、颜色、季节和衣物标签。
- 支持删除自己的衣物。
- 读取、更新、删除都必须执行归属校验。
- 不允许衣物引用其他用户的自定义品类或标签。

**TDD 验证步骤：**
- 先写 CRUD 和归属测试。
- 确认衣物 endpoint 缺失时测试失败。
- 实现最小 endpoint/service。
- 确认测试通过。

**需要先写的失败测试：**
- 用户可以读取自己的衣物。
- 用户可以更新衣物元数据。
- 用户 A 不能读取、更新或删除用户 B 的衣物。
- 用户不能给自己的衣物设置用户 B 的自定义品类。

**完成标准：**
- 基础衣物 CRUD 测试通过。
- 不包含筛选、分页、待完善计数和颜色复用；这些属于 T7B。

**是否可并行：** 否。

**建议分支 / worktree：** `task/07a-clothing-crud`

### T7B：后端待完善状态、分页、筛选与颜色复用

**状态：** 已完成

**分支 / worktree：** `task/07b-clothing-filtering`; `D:\My Work\Homework\智能化软件工程师训练营\MixMyFit-task-07b-clothing-filtering`

**测试结果：**
- TDD 红灯：`mvn test -Dtest=ClothingCrudEndpointTest` 初始失败 6 个，原因是筛选、分页、颜色复用和待完善计数 endpoint 缺失；新增默认排序测试修正后先失败于返回 `clothingId` 升序。
- Task 相关测试：`mvn test -Dtest=ClothingCrudEndpointTest` 通过；Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- 完整后端测试：Docker Desktop 启动后，`mvn test` 通过；Tests run: 54, Failures: 0, Errors: 0, Skipped: 0。

**SPEC 合规检查：** 通过

**代码质量检查：** 通过

**完成分支决定：** 开 PR

**Commit hash：** 待填写

**目标：** 实现衣物 draft/ready 状态、分页列表、筛选、待完善数量和已有颜色复用 API。

**依赖关系：** T7A。

**涉及文件：**
- 修改：`backend/src/main/java/com/fan/mixmyfit/clothing/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/clothing/`

**预期实现要点：**
- `category_id = null` 表示 draft / 待完善。
- 有品类的衣物为 ready。
- 衣物列表支持分页。
- 支持按品类、待完善状态、颜色、季节、标签筛选。
- 待完善数量只统计当前用户的衣物。
- 已有颜色 API 返回当前用户使用过的去重颜色。

**TDD 验证步骤：**
- 先写列表和筛选测试。
- 确认测试失败。
- 实现查询层和 endpoint。
- 确认测试通过。

**需要先写的失败测试：**
- 缺少品类的衣物出现在待完善筛选结果中。
- ready 筛选排除 draft 衣物。
- 季节 + 标签 + 颜色组合筛选只返回当前用户匹配衣物。
- 颜色复用 endpoint 排除其他用户的颜色。

**完成标准：**
- 筛选和待完善状态测试通过。
- API 可供衣物 UI 和搭配编辑器候选衣物选择使用。

**是否可并行：** 否。

**建议分支 / worktree：** `task/07b-clothing-filtering`

### T8：后端衣物批量操作

**目标：** 实现对选中衣物的批量品类、颜色、季节和标签操作。

**依赖关系：** T7B。

**涉及文件：**
- 修改：`backend/src/main/java/com/fan/mixmyfit/clothing/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/clothing/`

**预期实现要点：**
- 批量设置品类。
- 批量设置颜色。
- 批量设置季节。
- 批量添加衣物标签。
- 批量移除衣物标签。
- 如果请求中包含不属于当前用户的 clothing ID，必须拒绝操作。

**TDD 验证步骤：**
- 先写批量操作测试。
- 确认测试失败。
- 实现 batch service。
- 确认测试通过。

**需要先写的失败测试：**
- 批量设置品类能使选中的 draft 衣物变为 ready。
- 批量设置季节会替换选中衣物的季节。
- 批量添加/移除标签只影响当前用户选中的衣物。
- 混入其他用户 clothing ID 的请求被拒绝，且不得修改其他用户数据。

**完成标准：**
- 批量操作测试通过。
- 每一种批量操作都有用户隔离覆盖。

**是否可并行：** 否。

**建议分支 / worktree：** `task/08-clothing-batch`

### T9：后端搭配方案保存与校验

**目标：** 实现搭配方案保存，包括固定主槽位、配饰、元数据和保存校验。

**依赖关系：** T7B。

**涉及文件：**
- 创建：`backend/src/main/java/com/fan/mixmyfit/outfit/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/outfit/`

**预期实现要点：**
- 必须遵循 `SPEC.md` 搭配创建 API 合约：`POST /api/outfits`。
- 创建成功返回 `201` 和 `outfitId`。
- 空 payload 或 `items` 为空时返回 `400` 和统一错误响应。
- 保存上装、下装、鞋子、帽子主槽位，且四个槽位都可为空。
- 保存 0 到多个配饰层 item。
- 至少包含一件已选择衣物或配饰才能保存。
- draft 衣物不能保存进搭配方案。
- 标题为空时生成默认标题。
- 保存配饰位置、尺寸档位和 z-index。
- 所有引用的 clothing 必须属于当前用户。

**TDD 验证步骤：**
- 先写搭配保存测试。
- 确认测试失败。
- 实现保存 endpoint/service。
- 确认测试通过。

**需要先写的失败测试：**
- `POST /api/outfits` 完全空搭配保存返回 `400`。
- 只选择部分主槽位的搭配可以保存。
- 有效 payload 返回 `201` 和 `outfitId`。
- 空标题生成默认标题。
- draft 衣物不能被保存进搭配方案。
- 用户 A 不能用用户 B 的衣物保存搭配方案。

**完成标准：**
- 搭配保存测试通过。
- 搭配方案管理筛选、编辑、删除不在本 task 内；它们属于 T10。

**是否可并行：** 可与 T11/T12 前端基线工作重叠。

**建议分支 / worktree：** `task/09-outfit-save`

### T10：后端搭配方案筛选、编辑与删除

**目标：** 实现搭配方案详情、列表筛选、内容与信息编辑、删除。

**依赖关系：** T9。

**涉及文件：**
- 修改：`backend/src/main/java/com/fan/mixmyfit/outfit/`
- 测试：`backend/src/test/java/com/fan/mixmyfit/outfit/`

**预期实现要点：**
- 必须遵循 `SPEC.md` 搭配管理 API 合约：`GET /api/outfits/{outfitId}`、`GET /api/outfits`、`PATCH /api/outfits/{outfitId}`、`DELETE /api/outfits/{outfitId}`。
- 查看搭配方案详情，包括 items、季节和搭配标签。
- 编辑搭配内容和元数据。
- 删除自己的搭配方案。
- 支持按搭配标签、季节、标签 + 季节组合筛选。
- 所有查询和修改都必须受当前 `user_id` 约束。

**TDD 验证步骤：**
- 先写搭配方案管理测试。
- 确认测试失败。
- 实现 endpoint 和 service。
- 确认测试通过。

**需要先写的失败测试：**
- 用户可以按季节筛选自己的搭配方案。
- 用户可以按搭配标签筛选自己的搭配方案。
- 季节 + 标签组合筛选生效。
- 用户 A 不能查看、编辑或删除用户 B 的搭配方案。

**完成标准：**
- 搭配方案管理测试通过。
- 后端 API 可支撑完整前端工作流。

**是否可并行：** 否。

**建议分支 / worktree：** `task/10-outfit-management`

### T11：前端基线、Open Design 方向与 API Client

**目标：** 建立 Vue 应用结构、API client、路由壳和 Open Design 实现方向。

**依赖关系：** T1。

**涉及文件：**
- 创建/修改：`frontend/src/api/`
- 创建/修改：`frontend/src/router/`
- 创建/修改：`frontend/src/stores/`
- 创建/修改：`frontend/src/styles/`
- 修改：`README.md`

**预期实现要点：**
- 创建应用壳，不做营销式 landing page。
- API client 使用 Cookie credentials。
- 确认并记录 `Open Design` 的具体设计系统 / skill 使用方式。
- 在 README 中记录所选 Open Design 方向，但不宣称最终 UI 已完成。
- 建立前端测试工具。

**TDD 验证步骤：**
- 先写前端渲染/API client 测试。
- 确认测试失败。
- 实现 app shell 和 client。
- 确认测试通过。

**需要先写的失败测试：**
- API client 发请求时启用 credentials。
- Router 能渲染已登录应用壳的 placeholder route。

**完成标准：**
- 前端测试/构建通过。
- 除 placeholder 外不包含业务 UI。

**是否可并行：** 是。T1 后即可与后端 task 并行。

**建议分支 / worktree：** `task/11-frontend-baseline`

### T12：前端认证与个人资料页面

**目标：** 实现注册、登录、退出、个人资料、昵称修改和密码修改页面。

**依赖关系：** T3B、T11。

**涉及文件：**
- 创建：`frontend/src/views/auth/`
- 创建：`frontend/src/views/profile/`
- 修改：`frontend/src/stores/auth*`
- 测试：`frontend/src/**/__tests__/`

**预期实现要点：**
- UI 组件、布局和交互状态必须沿用 T11 确认的 `Open Design` 规则。
- 注册和登录表单使用后端 Cookie 会话。
- 退出调用后端 logout，并清理本地认证状态。
- 个人资料显示 username 和 nickname。
- 昵称修改和密码修改显示成功/错误状态。
- 密码字段不得进入日志。

**TDD 验证步骤：**
- 先写组件/API 交互测试。
- 确认测试失败。
- 实现 view 和 store。
- 确认测试通过。

**需要先写的失败测试：**
- 登录表单调用 API，成功后跳转到应用视图。
- 个人资料页面从 API 渲染 username。
- 修改密码表单要求旧密码和新密码。

**完成标准：**
- 认证和个人资料 UI 测试通过。
- 手动运行时能通过认证流程进入个人资料页。

**是否可并行：** 是。T3B 和 T11 后即可与 T5-T10 后端 task 并行。

**建议分支 / worktree：** `task/12-frontend-auth`

### T13A：前端衣物库列表、筛选与待完善状态 UI

**目标：** 实现衣物库列表、筛选控件、待完善标识和待完善数量 UI。

**依赖关系：** T7B、T11。

**涉及文件：**
- 创建：`frontend/src/views/clothing/`
- 创建：`frontend/src/components/clothing/`
- 修改：`frontend/src/api/clothing*`
- 测试：`frontend/src/**/__tests__/`

**预期实现要点：**
- UI 组件、布局和交互状态必须沿用 T11 确认的 `Open Design` 规则。
- 以图片卡片展示衣物，支持可选名称和待完善状态。
- 提供品类、待完善状态、颜色、季节、标签筛选。
- 显示当前用户待完善衣物数量。
- 图片展示尺寸受控，避免布局不稳定。

**TDD 验证步骤：**
- 先写衣物列表/筛选 UI 测试。
- 确认测试失败。
- 实现列表和筛选。
- 确认测试通过。

**需要先写的失败测试：**
- API 返回后渲染待完善数量。
- 应用待完善筛选时发送正确 query 参数。
- 名称为空的衣物显示 fallback 文案或 image-only 状态。

**完成标准：**
- 衣物列表/筛选 UI 测试通过。
- 不包含批量上传和批量操作；它们属于 T13B。

**是否可并行：** 否。

**建议分支 / worktree：** `task/13a-frontend-clothing-list`

### T13B：前端批量上传反馈与批量操作 UI

**目标：** 实现批量图片上传反馈，以及多选后的批量元数据/标签操作。

**依赖关系：** T8、T13A。

**涉及文件：**
- 修改：`frontend/src/views/clothing/`
- 修改：`frontend/src/components/clothing/`
- 修改：`frontend/src/api/clothing*`
- 测试：`frontend/src/**/__tests__/`

**预期实现要点：**
- UI 组件、布局和交互状态必须沿用 T11 确认的 `Open Design` 规则。
- 衣物卡片支持多选。
- 批量上传多张图片，并显示进度或结果反馈。
- 批量设置品类、颜色、季节。
- 批量添加/移除衣物标签。
- 上传和批量设置品类后刷新待完善数量。

**TDD 验证步骤：**
- 先写批量 UI 测试。
- 确认测试失败。
- 实现上传和批量操作 UI。
- 确认测试通过。

**需要先写的失败测试：**
- 选择多件衣物后启用批量工具栏。
- 批量设置品类操作携带选中 ID 调用 API。
- 上传成功后显示创建数量并更新待完善数量。

**完成标准：**
- 批量上传和批量操作 UI 测试通过。
- UI 覆盖衣物管理 MVP 闭环。

**是否可并行：** 否。

**建议分支 / worktree：** `task/13b-frontend-clothing-batch`

### T14：前端搭配编辑器固定主槽位

**目标：** 实现搭配编辑器中的四个固定主槽位：上装、下装、鞋子、帽子。

**依赖关系：** T7B、T11。

**涉及文件：**
- 创建：`frontend/src/views/outfit-editor/`
- 创建：`frontend/src/components/outfit/`
- 修改：`frontend/src/api/clothing*`
- 测试：`frontend/src/**/__tests__/`

**预期实现要点：**
- UI 组件、布局和交互状态必须沿用 T11 确认的 `Open Design` 规则。
- 只渲染四个固定槽位。
- 每个槽位可以在该品类 ready 衣物中切换上一件/下一件。
- 每个槽位可以打开候选衣物选择器。
- 候选选择器支持颜色、季节、多选标签筛选。
- 每个槽位可以清空。
- draft 衣物不出现在候选列表。

**TDD 验证步骤：**
- 先写编辑器槽位测试。
- 确认测试失败。
- 实现 editor state 和槽位 UI。
- 确认测试通过。

**需要先写的失败测试：**
- 编辑器恰好渲染四个槽位。
- 下一件按钮会为对应品类选择下一件 ready 衣物。
- 候选筛选发送 category + color + season + tags。
- 清空按钮只清空当前槽位。

**完成标准：**
- 主槽位编辑器测试通过。
- 配饰和保存功能要到 T15/T16 才完整。

**是否可并行：** 可在 T10 前基于衣物 API 开始。

**建议分支 / worktree：** `task/14-frontend-editor-slots`

### T15：前端配饰层

**目标：** 实现自定义品类配饰层交互。

**依赖关系：** T14。

**涉及文件：**
- 修改：`frontend/src/views/outfit-editor/`
- 修改：`frontend/src/components/outfit/`
- 测试：`frontend/src/**/__tests__/`

**预期实现要点：**
- UI 组件、布局和交互状态必须沿用 T11 确认的 `Open Design` 规则。
- 配饰只能来自自定义品类。
- 支持添加配饰。
- 支持移除配饰。
- 支持拖动位置。
- 支持调整 z-index。
- 支持 small、medium、large 三个尺寸档位。
- 不实现旋转、自由缩放、裁剪或完整自由画布编辑器。

**TDD 验证步骤：**
- 先写配饰交互测试。
- 确认测试失败。
- 实现配饰状态和 UI 控件。
- 确认测试通过。

**需要先写的失败测试：**
- 固定品类不会作为配饰候选项。
- 添加配饰后保存 position、size、z-index。
- 尺寸控件只允许 small、medium、large。

**完成标准：**
- 配饰 UI 测试通过。
- 编辑器状态可为 T16 生成保存 payload。

**是否可并行：** 否。

**建议分支 / worktree：** `task/15-frontend-accessories`

### T16：前端搭配方案列表、详情、编辑、删除与筛选

**目标：** 实现从编辑器保存搭配方案，以及搭配方案列表、详情、编辑、删除和筛选 UI。

**依赖关系：** T10、T15。

**涉及文件：**
- 创建：`frontend/src/views/outfits/`
- 修改：`frontend/src/views/outfit-editor/`
- 修改：`frontend/src/api/outfit*`
- 测试：`frontend/src/**/__tests__/`

**预期实现要点：**
- UI 组件、布局和交互状态必须沿用 T11 确认的 `Open Design` 规则。
- 保存 editor payload，包括主槽位、配饰、标题、备注、搭配标签和季节。
- 客户端在调用 API 前拦截完全空的搭配方案。
- 显示搭配方案列表。
- 支持按搭配标签、季节和组合条件筛选。
- 显示详情。
- 编辑内容和元数据。
- 删除前必须确认。

**TDD 验证步骤：**
- 先写搭配方案 UI 测试。
- 确认测试失败。
- 实现保存、列表、详情、编辑、删除 UI。
- 确认测试通过。

**需要先写的失败测试：**
- 空编辑器保存时显示错误，且不调用 API。
- 保存部分搭配时发送预期 payload。
- 搭配方案列表筛选发送 tag + season query。
- 删除操作必须确认后才调用 API。

**完成标准：**
- 搭配方案 UI 测试通过。
- 完整前端 MVP 工作流可进入 E2E。

**是否可并行：** 否。

**建议分支 / worktree：** `task/16-frontend-outfits`

### T17：端到端核心工作流测试

**目标：** 通过 E2E 测试验证完整 MVP 闭环和跨用户隔离。

**依赖关系：** T13B、T16。

**涉及文件：**
- 创建：`e2e/`
- 修改：`Makefile`
- 修改：`README.md`

**预期实现要点：**
- E2E 覆盖注册登录、批量上传、批量补全元数据、创建搭配、保存搭配、筛选搭配。
- E2E 覆盖用户 A 不能访问用户 B 的衣物或搭配方案。
- 测试必须有文档化命令可运行。

**TDD 验证步骤：**
- 先基于当前应用写 E2E 测试。
- 确认它们因流程缺失或不完整而失败。
- 只修复相关应用代码中的流程衔接问题。
- 确认 E2E 通过。

**需要先写的失败测试：**
- `注册 -> 登录 -> 上传两张图片 -> 批量设置品类/颜色/季节/标签 -> 创建搭配 -> 保存 -> 按标签筛选`。
- `用户 A 直接访问用户 B 的衣物/搭配方案时返回未授权或不存在`。

**完成标准：**
- E2E suite 本地通过。
- README 如实记录 E2E 命令。

**是否可并行：** 否。

**建议分支 / worktree：** `task/17-e2e-core-flow`

### T18：Docker Compose 分发

**目标：** 提供前端、后端、MySQL 和持久化上传目录的容器化本地分发方式。

**依赖关系：** T17。

**涉及文件：**
- 创建：`docker-compose.yml`
- 创建：`backend/Dockerfile`
- 创建：`frontend/Dockerfile`
- 修改：`README.md`
- 修改：`.gitignore`

**预期实现要点：**
- `docker compose up` 启动前端、后端和 MySQL。
- MySQL 数据使用 volume。
- 上传目录使用 volume。
- 配置通过环境变量或示例 env 文件注入。
- 不提交真实凭据。

**TDD 验证步骤：**
- 先添加 Docker 校验命令或检查。
- 确认 Docker 文件缺失时校验失败。
- 添加 Dockerfile 和 compose 配置。
- 确认 stack 启动且 health check 通过。

**需要先写的失败测试：**
- `docker-compose.yml` 不存在时 compose 配置校验失败。
- 后端容器缺少必需数据库配置时不能启动。

**完成标准：**
- Docker Compose 可在本地启动完整应用。
- README 包含首次启动、停止、重建、数据目录和已知限制说明。

**是否可并行：** 是。T17 后可与 T19 并行。

**建议分支 / worktree：** `task/18-docker-compose`

### T19：GitLab CI 与一键测试入口

**目标：** 在核心 E2E 已存在后，添加运行完整项目测试入口的 CI。

**依赖关系：** T17。

**涉及文件：**
- 创建：`.gitlab-ci.yml`
- 修改：`Makefile`
- 修改：`README.md`

**预期实现要点：**
- `.gitlab-ci.yml` 包含名为 `unit-test` 的 job。
- `unit-test` 至少运行后端核心测试。
- CI 同时校验前端构建/测试。
- 如果实际可行，在 T18 合并后 CI 也校验 Docker build。
- `make test` 保持为本地一键测试入口。

**TDD 验证步骤：**
- 如工具可用，先添加 CI lint/check。
- 确认 pipeline 配置缺失或失败。
- 添加 CI 配置。
- 运行本地等价命令。

**需要先写的失败测试：**
- 仓库检查断言 `.gitlab-ci.yml` 包含 `unit-test`。
- CI wiring 前，本地 `make test` 应因命令或配置缺失而失败。

**完成标准：**
- 本地 `make test` 通过。
- `.gitlab-ci.yml` 包含 `unit-test`。
- README 记录 CI 预期。

**是否可并行：** 是。T17 后可与 T18 并行。

**建议分支 / worktree：** `task/19-gitlab-ci`

### T20：部署准备与线上 WebUI 记录

**目标：** 准备部署配置，并记录最终线上 WebUI URL。

**依赖关系：** T18、T19，以及用户确认部署平台。

**涉及文件：**
- 按需要修改/创建：具体平台部署配置
- 修改：`README.md`
- 修改：`AGENT_LOG.md`

**预期实现要点：**
- 在编写平台特定配置前，先询问并确认 Render、Railway、Fly.io 或其他平台。
- 记录所选部署拓扑下 HTTPS/Cookie `Secure` 和 `SameSite` 行为。
- 记录线上部署与本地 Docker Compose 的差异。
- 部署成功后，在 README 记录公网 WebUI URL。

**TDD 验证步骤：**
- 部署使用 smoke check，不使用普通单元 TDD。
- 部署前确认本地 production build 通过。
- 验证线上 health endpoint 和 WebUI 可访问。

**需要先写的失败测试：**
- 部署 smoke checklist 初始为失败：后端 health 不可访问、前端不可访问、登录 Cookie flags 未验证。

**完成标准：**
- 线上 WebUI 可访问。
- README 包含部署架构、URL 和已知限制。
- AGENT_LOG 记录平台、命令和人工干预。

**是否可并行：** 否。

**建议分支 / worktree：** `task/20-deployment`

### T21：README、AGENT_LOG、PLAN 状态与最终文档检查

**目标：** 完成非反思报告类文档和过程证据整理。

**依赖关系：** T20。

**涉及文件：**
- 修改：`README.md`
- 修改：`AGENT_LOG.md`
- 修改：`PLAN.md`
- 如需要，修改：`SPEC_PROCESS.md`
- 不编写：`REFLECTION.md`

**预期实现要点：**
- README 覆盖项目简介、安装、运行、测试、分发、目录结构、安全边界、部署和已知限制。
- AGENT_LOG 包含关键 Superpowers 技能、task、subagent、commit、测试和人工干预记录。
- PLAN task 状态在实现后包含完成说明和 commit hash。
- `REFLECTION.md` 保留给学生本人撰写。

**TDD 验证步骤：**
- 文档检查清单作为本 task 的验证。
- 如工具可用，运行链接/路径检查。
- 确认 README 中的命令与真实命令一致。

**需要先写的失败测试：**
- 文档检查清单初始标记缺失章节和缺失 AGENT_LOG 条目为失败。

**完成标准：**
- README 和 AGENT_LOG 满足作业要求。
- PLAN 准确反映已完成 task。
- `REFLECTION.md` 未由 AI 生成。

**是否可并行：** 否。

**建议分支 / worktree：** `task/21-docs-finalization`

## 最终验证清单

- [ ] T0 冷启动验证已在任何实现 task 前完成。
- [ ] 没有加入 AI / LLM / agent 功能。
- [ ] 所有后端数据访问都受当前 `user_id` 约束。
- [ ] 上传安全覆盖类型、大小、服务端文件名和带归属校验的访问。
- [ ] 衣物 draft/ready 规则符合图片 + 品类。
- [ ] 搭配编辑器只有四个固定主槽位。
- [ ] 自定义品类作为配饰，不作为固定槽位。
- [ ] 空搭配保存被拒绝。
- [ ] 后端测试通过。
- [ ] 前端测试/构建通过。
- [ ] E2E 核心工作流通过。
- [ ] `docker compose up` 能启动完整本地系统。
- [ ] `.gitlab-ci.yml` 包含 `unit-test`。
- [ ] README 记录安装、运行、测试、分发、部署、目录结构、安全边界和限制。
- [ ] AGENT_LOG 已更新。
