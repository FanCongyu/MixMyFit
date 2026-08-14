package com.fan.mixmyfit.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DocumentationFinalizationTest {
    private final Path projectRoot = Path.of("").toAbsolutePath().normalize().getParent();

    @Test
    void readmeCoversFinalUserFacingDocumentationSections() throws IOException {
        String readme = Files.readString(projectRoot.resolve("README.md"));

        assertThat(readme)
                .contains("## 项目简介")
                .contains("## 安装与运行")
                .contains("## 测试")
                .contains("## 分发与部署")
                .contains("## 目录结构")
                .contains("## 安全边界")
                .contains("## 已知限制")
                .contains("docker compose up --build")
                .contains("cd backend && mvn test")
                .contains("cd frontend && npm ci && npm run build && npm test -- --run")
                .contains("mingw32-make test")
                .contains("powershell.exe -NoProfile -ExecutionPolicy Bypass -File e2e/railway-smoke.ps1")
                .doesNotContain("当前仓库处于实现早期阶段");
    }

    @Test
    void agentLogRecordsTask21DocumentationFinalizationEvidence() throws IOException {
        String agentLog = Files.readString(projectRoot.resolve("AGENT_LOG.md"));

        assertThat(agentLog)
                .contains("T21 README、AGENT_LOG、PLAN 状态与最终文档检查")
                .contains("superpowers:executing-plans")
                .contains("DocumentationFinalizationTest")
                .contains("人工干预")
                .contains("Commit hash");
    }

    @Test
    void planFinalChecklistIsCompletedForDocumentedAcceptanceEvidence() throws IOException {
        String plan = Files.readString(projectRoot.resolve("PLAN.md"));

        assertThat(plan)
                .contains("- [x] T0 冷启动验证已在任何实现 task 前完成。")
                .contains("- [x] 没有加入 AI / LLM / agent 功能。")
                .contains("- [x] 后端测试通过。")
                .contains("- [x] 前端测试/构建通过。")
                .contains("- [x] E2E 核心工作流通过。")
                .contains("- [x] `docker compose up` 能启动完整本地系统。")
                .contains("- [x] README 记录安装、运行、测试、分发、部署、目录结构、安全边界和限制。")
                .contains("T21 完成说明")
                .contains("Commit hash");
    }
}
