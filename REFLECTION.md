# MixMyFit 项目反思

这次期末项目我做的是 MixMyFit，一个个人衣橱和穿搭管理 Web 应用。它不做 AI 推荐，也不做 agent，而是把重点放在真实可用的流程上：注册登录、上传衣物、补全信息、组合搭配、保存方案，再按季节或场景筛选复用。做完以后我最大的感受是，AI 能写很多代码，但如果一开始没有把“做什么”和“做到什么程度算对”说清楚，后面就会变成智能体很努力地写，却不一定写到我想要的地方。

对我帮助最大的 Superpowers 技能是 `brainstorming`、`writing-plans`、`test-driven-development` 和 `verification-before-completion`。`brainstorming` 让我在实现前把范围想清楚。项目最初只是“衣物搭配平台”，但里面有很多选择：要不要 AI 推荐？衣物名称是否必填？编辑器要不要做完整自由画布？经过追问后，我把项目收敛成“不做 AI、不做社交、不做背景去除，只做个人衣橱搭配管理”。

`writing-plans` 把大目标拆成能执行的小任务。比如认证拆成 T3A 注册登录退出和 T3B 个人资料/改密码；数据库拆成 T2A Flyway 迁移和 T2B ORM/Repository；前端衣物管理也拆成列表筛选和批量操作。这样 subagent 每次只处理一个目标，不容易越界。`finishing-a-development-branch` 有时有点形式化，但它也提醒我检查测试、文档和 AGENT_LOG。

TDD 一开始像阻碍，因为 AI 明明可以直接写实现，却必须先写失败测试。但实际做下来，它更像放大器。AI 很擅长补代码，也容易补过头；TDD 给它一个很窄的目标。T3A 认证任务里，测试先暴露缺少 `PasswordEncoder`、`UserRepository.findByUsername` 和 session 失效逻辑。后来我又补了“登出后服务端 session 必须失效”的断言，才发现只清 Cookie 不够，还要删除服务端 session。

TDD 也让我能分清失败原因。T17 E2E 时，第一次失败是 Docker 没启动，不是业务错；后来 `mingw32-make test-e2e` 又暴露 Makefile 缺少测试入口。T18 Docker 分发时，测试发现 healthcheck 依赖 `wget`，于是改成使用 Java runtime 自带能力。

Subagent 最有效的粒度是“一个清楚的业务能力 + 一组可验证测试”。每次我都会要求它先读 `SPEC.md`、`PLAN.md`、`AGENT_LOG.md`，只执行指定 task，不实现其他 task，不写入真实凭据，并在动手前说明任务理解、要改的文件、失败测试和完成标准。这样它通常能在一个 task 内自主推进到测试通过。

SPEC / PLAN 的质量直接影响实现质量。冷启动验证给我的冲击最大：一个新智能体只看原始 `SPEC.md` 和 `PLAN.md`，马上暴露出很多我以为“大家都懂”的问题，比如 API 路径、字段名、状态码、Cookie 名称、上传字段名、测试数据库策略、后端包名和构建命令都不够明确。后来我把 `POST /api/clothes`、字段 `file`、5 MB 限制、JPEG/PNG/WebP、`UPLOAD_DIR`、`MMF_SESSION`、统一错误格式等写进 SPEC，实现明显稳定很多。

一个具体例子是衣物名称是否必填。AI 一开始更像按普通 CRUD 系统理解，倾向于要求名称；但 MixMyFit 是视觉搭配工具，用户主要通过图片识别衣物，名称不应该成为门槛。这个点如果没在 SPEC 里纠正，数据库、API、前端表单都会被带偏。

我最有效的 prompt / context 策略不是给很多材料，而是给关键材料和硬边界：读哪些文件、只做哪个 task、先写什么失败测试、哪些功能不属于本任务、完成后验证什么。要求 AI 在动手前复述计划也很有用，因为很多误解可以提前发现。

凭据和分发要求也让我补上了以前容易忽略的工程意识。MixMyFit 不调用 LLM，所以没有 LLM API Key，但仍然有数据库密码、Cookie、生产环境变量和上传文件路径。README 里必须区分本地开发默认值和生产真实凭据，真实密码只能放在 Railway Variables、Vercel Environment Variables 或本地 `.env`。分发方面，Docker Compose 让前端、后端、MySQL 和上传 volume 能用一套命令启动；线上部署时又发现 Vercel 前端不能访问 Railway private domain，必须使用 Railway Public Domain，并处理 `SameSite=None; Secure` 和 CORS credentials。

如果重做，我会更早把 API 合约写细，尤其是路径、字段名、状态码、错误格式和 Cookie 配置；也会更早统一 Testcontainers 测试配置和前端 API helper，减少重复代码。部署平台也应该更早缩小范围，因为前后端是否同域、Cookie 怎么跨站、上传文件是否需要 volume，都会影响设计。

我对 Superpowers 的总体看法是：它不是让 AI 自动完成项目，而是逼我像项目负责人一样工作。它假设用户愿意审 SPEC、拆 PLAN、看测试结果，并在 AI 偏离时纠正。这个假设在 MixMyFit 里基本成立，因为项目边界可以拆清楚，也能用测试约束。但它也有成本，小改动走完整流程会偏重；审美和体验判断也不能完全交给流程。最后我学到的是，AI 可以承担很多执行工作，但项目最终是否正确，还是取决于我有没有提出正确要求并及时纠正。

*本文经过AI润色