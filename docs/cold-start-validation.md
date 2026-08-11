# 冷启动验证报告（docs/cold-start-validation.md）

验证概览
---------
验证智能体类型：冷启动验证智能体（无历史对话，仅能读取 SPEC.md 与 PLAN.md 的新实现智能体）
验证日期：2026-08-11T15:51:26.637+08:00

只读取的文件：
- SPEC.md
- PLAN.md

明确说明：未读取且未依赖以下内容：PROJECT_BRIEF.md、SPEC_PROCESS.md、AGENT_LOG.md、主开发智能体的历史对话或任何未提供的口头说明。


已选任务（Selected Tasks）
-------------------------
- 已选择验证的 task：T1（项目骨架与测试基线）与 T2（后端领域模型与数据库迁移）。
- 选择理由：
  - T1/T2 为主链路早期任务，要求先写失败测试（TDD）并建立测试基线，最能暴露 SPEC/PLAN 在技术选型、项目结构、测试策略与迁移工具方面的缺口。
  - 这两个 task 不依赖业务实现即可写出失败测试，便于检验文档是否足以让陌生 agent 开始实施。
- 评估：这两个 task 非常适合用来验证 SPEC.md / PLAN.md 的清晰度——它们将迫使文档明确包名、迁移工具、测试数据库策略、API 基本契约与构建命令等基础假设。


已生成的失败测试（Generated Failing Tests）
-----------------------------------------
已在仓库创建以下失败测试（用于 TDD 的红灯阶段）：
- backend/src/test/java/com/fan/mixmyfit/auth/AuthApiTest.java
- backend/src/test/java/com/fan/mixmyfit/clothing/ClothingApiTest.java
- backend/src/test/java/com/fan/mixmyfit/outfit/OutfitApiTest.java

每个测试对应验证的 SPEC / PLAN 需求：
- AuthApiTest.registerEndpoint_shouldReturn201_andUserId
  - 验证：注册创建内部 userId，用户名唯一，密码哈希存储且不在响应中泄露明文（来源：SPEC §1.3, §5, PLAN.T3A）。
- AuthApiTest.login_shouldSetHttpOnlyCookie
  - 验证：登录成功时后端通过 Set-Cookie 返回 HttpOnly 登录 Cookie（来源：SPEC §5.2, PLAN.T3A）。
- AuthApiTest.logout_shouldClearCookie
  - 验证：登出时后端清除/重置会话 Cookie（来源：SPEC §5.2, PLAN.T3A）。
- ClothingApiTest.createClothingMultipart_shouldReturn201_andClothingId
  - 验证：图片上传能创建衣物草稿记录，返回 clothingId 和状态（draft/ready），上传文件元数据被保留（来源：SPEC §3.1, PLAN.T6/T7）。
- ClothingApiTest.getClothing_withoutAuth_shouldReturn401
  - 验证：未登录请求被拒绝（401），验证用户隔离与鉴权中间件（来源：SPEC §5, PLAN.T4）。
- OutfitApiTest.createOutfit_emptyPayload_shouldReturn400
  - 验证：保存搭配前提至少有一件衣物或配饰，空载体应返回 400（来源：SPEC §4.3）。
- OutfitApiTest.createOutfit_validPayload_shouldReturn201_andOutfitId
  - 验证：有效搭配创建返回 outfitId（来源：SPEC §4.3, PLAN 中搭配保存相关 task）。


哪些是明确可推导的（What Was Clear）
-----------------------------------
- 项目目标与 MVP 边界、核心模块、主要实体与字段（SPEC 明确列出实体与字段）。
- 必须使用后端管理的 HttpOnly Cookie（SPEC §5.2）与用户隔离的原则（SPEC §5.3）。
- 衣物“待完善”规则、保存搭配必须至少包含一件衣物/配饰等业务约束（SPEC §3.1, §4.3）。
- PLAN 中明确要求 TDD 工作流（先写失败测试再实现），并给出任务依赖关系与文件结构规划（PLAN 全局约束与文件结构）。


歧义与阻塞性问题（Ambiguities and Blocking Questions）
-------------------------------------------------------
下表列出导致必须猜测或暂停的问题（每项包含建议的 SPEC/PLAN 修正）

| 区域 | 缺失细节 | 为什么阻塞实现 | 建议的 SPEC/PLAN 修正 |
|---|---|---|---|
| API 路径与 HTTP 方法 | 未对关键 endpoint 给出标准路径与方法（PLAN 中部分示例未具体化） | 测试与 controller 实现需明确路径与方法以进行集成和 MockMvc 断言 | 在 SPEC 或 PLAN 为核心模块列出最小 API contract（路径+方法）示例 |
| 请求 / 响应 JSON 字段 | 未定义请求/响应 JSON 的最小 schema（字段名、类型） | 集成测试需断言响应字段（如 userId, clothingId）与前后端契约，缺失导致测试需猜测 | 在 SPEC 中为每个关键 endpoint 提供最小请求/响应 schema |
| 成功 / 失败 HTTP 状态码 | 未统一错误与成功状态码约定（如注册 201 / 200, logout 204） | 测试断言状态码需要约定，否则不同实现会出现假阳性/假阴性 | 在 SPEC/PLAN 中定义各类操作的期望状态码（注册、创建资源、删除、未授权、验证失败） |
| 统一错误响应格式 | 未指定错误响应 JSON 结构（例如 {error:..} 或 {code:.., message:..}） | 测试需要验证错误响应体内容，否则断言脆弱 | 在 SPEC 中定义错误响应结构与字段 |
| Cookie 名称、过期策略、SameSite 默认值 | 未指定 cookie 名称、过期时间、SameSite 值 | 登录/登出测试需检查 Set-Cookie 的具体属性，安全策略需要固定值以便测试 | 在 SPEC 中给出 Cookie 名称与示例配置（过期、SameSite 推荐值） |
| 上传 multipart 字段名 | 未明确 multipart 字段名（file 或 image 等） | MockMultipartFile 中需使用正确字段名才能让 controller 接收 | 在 PLAN.T6 或 SPEC 中指定上传字段名（例如 "file"） |
| 上传大小限制 | SPEC 有未决项，PLAN.T6 写为 5 MB，但两处不一致 | 测试需校验大小超限被拒绝；必须统一上限 | 在 SPEC 中固定上传上限（建议 5 MB）并在 PLAN 中同步更新 |
| 上传根目录配置键名 | 未指定配置键或默认路径 | 测试与 CI 需要可配置的上传目录（临时目录 vs 持久 volume） | 在 SPEC/PLAN 中给出配置键名（例如 env UPLOAD_DIR）和默认开发值 |
| 是否生成缩略图 | SPEC 未决（是否进入 MVP） | 前端展示与性能策略取决于是否生成缩略图；测试/实现不同 | 在 SPEC 中确定是否生成缩略图，若否则前端需限制展示尺寸；若是则在 PLAN 中新增生成任务 |
| 迁移工具（Flyway） | PLAN 使用 db/migration/ 目录，但未明确 Flyway 是否为标准迁移工具 | 测试中的迁移自动执行、CI 步骤依赖具体工具 | 在 SPEC 或 PLAN 明确迁移工具（建议 Flyway）并说明在测试中如何触发迁移 |
| 测试数据库策略 | SPEC/PLAN 未明确 Testcontainers 或 H2；之前由我询问并获确认为 Testcontainers | 测试配置（依赖、启动时间、CI 兼容性）差异大，影响用例运行 | 在 PLAN.T2 中写明使用 Testcontainers MySQL 以便统一开发/CI 行为 |


需要说明：上述表格中部分项（如迁移工具、测试 DB）在会话中已由实现者确认为 Flyway 与 Testcontainers（你选择了推荐预设）；但在原始 SPEC/PLAN 文档里并未显式写明，仍建议把确认内容写回 SPEC/PLAN 以避免未来实现者假设。 


风险假设（Risky Assumptions）
-------------------------------
下面列出为编写测试不得不做出的假设及其风险：
1. 采用的 API 路径与字段命名（/api/auth/register, userId/clothingId/outfitId 等）
   - 风险：若实现者使用不同路径或字段名，自动化测试会失败；若 API contract 改变，将引发大量测试修改。
2. HTTP 状态码约定（注册 201、登录 200、logout 204、创建资源 201、空载体 400、未授权 401）
   - 风险：不同团队有不同习惯（例如 login 返回 204 或 200），测试容易产生误报。
3. multipart 字段名使用 "file" 与返回 imageUrl 字段
   - 风险：实现可能使用 "image" 或其他字段名称，导致上传接口不兼容测试。
4. 假设使用 Flyway 与 Testcontainers（已由你确认，风险降低）
   - 风险：CI 环境若不支持 Testcontainers，需额外配置；若改用内存 DB，migration 测试需要调整。


建议补充到 SPEC.md（按优先级）
----------------------------------
1. （高）为核心后端 API（认证、衣物、搭配）在 SPEC 中给出最小 API contract：路径、方法、请求字段与响应字段、成功/失败状态码与错误响应结构示例。 
2. （高）统一并明确上传大小上限（建议 5 MB）、multipart 字段名（例如 "file"）与返回文件元字段（imageUrl、storagePath、contentType、fileSize）。
3. （高）指定 Cookie 名称与基本属性（HttpOnly、Secure 在 HTTPS、SameSite 推荐值、过期时间示例）。
4. （中）明确迁移工具（推荐 Flyway）以及在测试时如何触发迁移（测试启动自动执行）。
5. （中）明确测试数据库策略（推荐 Testcontainers MySQL），并在 CI 中说明如何运行。 
6. （中）为错误响应定义统一格式（{ "error": "message" } 或带 code 的对象），便于断言。 
7. （低）定义分页/列表接口约定（page、size、默认排序）。


建议补充或调整到 PLAN.md（按优先级）
-----------------------------------------
1. （高）在 T2 中把“迁移 SQL”与“领域对象/ORM 映射”拆成两个子 task（T2a：迁移 SQL + migration 测试；T2b：实体/Repository 映射 + 映射测试）。
2. （高）在 T1 明确后端 root Java 包名字段（已由你提供 com.fan.mixmyfit，应写入 PLAN），并把构建工具/Java 版本写入 PLAN（Maven + Java17）。
3. （高）为 T1 明确前端构建/测试命令（例如 npm ci && npm run build; npm test-runner），并给出 Vitest 的基本断言样例。 
4. （中）在 T6 中将上传上限（5 MB）与允许的 MIME 类型（JPEG/PNG/WebP）写回 SPEC/PLAN 并尽量同步两处表述。 
5. （中）在 PLAN 中为关键 API 提供一页“API 合约草案”作为后续任务的输入文件（可以是 yaml/json schema）。
6. （低）在 PLAN 中说明 CI 对 Testcontainers 的支持需求或替代方案（如 H2）以便学生在 CI 环境中能通过测试。


最终结论（Final Verdict）
--------------------------
- 问：仅凭当前 SPEC.md + PLAN.md，陌生 agent 是否可以可靠执行 T1/T2？
  - 答：不够可靠。尽管总体目标、实体与 TDD 流程明确，但若要实际实现并让测试稳定通过，文档必须补充关键 API 合约、错误格式、Cookie 细节、上传配置与迁移/测试工具的明确说明，否则陌生 agent 会在实现细节上不得不猜测，从而导致不一致或错误实现。

- 哪些问题必须在实现前修订？（优先修订项）
  1. 明确核心 API contract（路径/方法/请求/响应/状态码/错误格式）。
  2. 指定上传相关配置（字段名、上限 5 MB、允许 MIME 类型、上传目录配置键）。
  3. 明确 Cookie 名称与属性（HttpOnly 已列出，但需要名称与过期策略）。
  4. 明确迁移工具（Flyway）与测试 DB 策略（Testcontainers MySQL）。
  5. 在 PLAN 中补充 Java 根包名与构建工具说明（Maven + Java17）。

- 是否建议继续进入实现阶段？
  - 建议在上述高优先级修订完成并写回 SPEC/PLAN 之后再进入实现阶段。当前可进行文档修订和分支规划；实现阶段（创建 pom.xml、Application.java、构建脚手架并跑测试）应在这些修订完成后启动。


当前文件变更（Current File Changes）
-----------------------------------
- 已创建（未修改其他实现文件）：
  1. backend/src/test/java/com/fan/mixmyfit/auth/AuthApiTest.java
  2. backend/src/test/java/com/fan/mixmyfit/clothing/ClothingApiTest.java
  3. backend/src/test/java/com/fan/mixmyfit/outfit/OutfitApiTest.java
  4. docs/cold-start-validation.md（本报告文件）

- 未创建或修改：pom.xml、Application.java、任何 frontend 文件或迁移文件。

Git 状态与差异说明：
- 仓库当前不是一个 Git 仓库（项目根信息显示非 Git 仓库）。因此无法提供真实的 `git status` 或 `git diff --stat` 输出。
- 作为替代，列出已创建文件的相对路径（见上）。如果你希望我初始化一个临时 Git 仓库或运行 git 命令，我需要你的明确授权。


结束语
-----
已按你的指示停止代码实现，仅完成文档性收尾并将冷启动验证报告写入 docs/cold-start-validation.md。请检阅该文件与刚才创建的失败测试，确认是否需要我在文档中加入更具体的 API contract 草案（我可以基于现有测试草案把每个 endpoint 的 JSON schema 写成示例）。

如需我继续，请明确允许下一步操作（例如创建 pom.xml 与 Application.java 并运行测试），或先提供进一步的 SPEC/PLAN 补充。