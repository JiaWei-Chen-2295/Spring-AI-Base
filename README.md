<h1 align="center">Spring AI Reference Project</h1>

<p align="center">
  <b>一个面向 Spring 生态的 AI 应用参考项目，提供多模型接入、工具调用和可扩展工程结构。</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/Spring%20AI-1.1.2-6DB33F?logo=spring&logoColor=white" alt="Spring AI 1.1.2"/>
  <img src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=white" alt="React 19"/>
  <img src="https://img.shields.io/badge/Java-17+-ED8B00?logo=openjdk&logoColor=white" alt="Java 17+"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue" alt="License"/>
</p>

---

## Why This Template?

> 不是 Demo，是**真正能二开的模板**。

| 痛点 | 本模板的解法 |
|---|---|
| 接入新模型要改一堆核心代码 | **SPI 插件架构** — 新增模型/工具/技能只加文件，不改 core |
| 聊天记录重启就没了 | **JDBC 持久化记忆** — 默认 H2 零配置，一行改 MySQL |
| 从零搭项目要几天 | **克隆即跑** — 30 分钟跑通模型+工具+流式输出 |
| Function Calling 接入复杂 | **内置工具链** — SAA Agent + Skills + MCP 开箱可用 |

---

## Features

- **多模型路由** — OpenAI / DashScope / DeepSeek / Ollama，按需切换，统一接口
- **模型管理后台** — 管理员动态添加、编辑、删除、启用/禁用模型，支持任意 OpenAI 兼容 API
- **聊天记忆** — JDBC 持久化，H2 零配置启动，配置切 MySQL/PostgreSQL
- **数据库管理** — MyBatis-Plus ORM + Flyway 版本迁移，切换数据库零 SQL
- **Function Calling** — SAA ReactAgent 驱动，工具自动注入，支持 Tracing
- **Skills 系统** — 命名空间 + 版本管理 + Python 脚本执行 + `skills.sh` 批量导入
- **MCP 能力** — 按开关启用外部 MCP 工具（搜索、文件、数据库）
- **流式输出** — SSE 实时推送 token + 工具调用元数据
- **React 控制台** — Ant Design 5 + Zustand，模型/工具/技能/对话/设置全可控
- **系统设置** — API Key 通过页面配置，DB 优先、环境变量兜底
- **插件化扩展** — `ModelAdapter` / `ToolAdapter` / `SkillProvider` 三大 SPI
- **多端客户端** — Kotlin Multiplatform (KMP) 构建的 Compose Desktop/Android 原生体验客户端

---

## Screenshots

### AI 对话

多模型切换、流式输出（SSE）、工具调用追踪、会话历史管理。

![AI Chat](docs/photos/chat.png)

### 模型管理

管理员后台配置和管理 AI 模型，支持内置模型启用/禁用，动态添加 OpenAI 兼容模型。

![Model Management](docs/photos/model_manager.png)

### 添加模型

通过管理界面动态添加新模型，配置 API 地址、密钥、模型名称和能力声明，无需修改代码。

![Add Model](docs/photos/add_model.png)

### Skills 技能 & MCP 工具

Skills 技能系统按需加载增强模型能力；MCP 协议接入外部工具，实时展示执行过程。

<p>
  <img src="docs/photos/skills.png" width="49%" alt="Skills"/>
  <img src="docs/photos/mcp.png" width="49%" alt="MCP"/>
</p>

---

## Quick Start

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

> 零配置即可启动（H2 内存库，无需数据库和 API Key）。API Key 启动后在前端 **Settings** 页面配置即可。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 3. 打开浏览器

访问 **http://localhost:5173**，选择模型即可对话。

### 4. (可选) 启动原生客户端 (KMP)

本模板包含一个基于 Compose Multiplatform 的精美桌面/移动跨平台客户端：

```bash
cd kmp-client
./gradlew :desktopApp:run
```

---

## Chat Memory — 聊天记忆

本模板内置 **JDBC 持久化聊天记忆**，基于 Spring AI `ChatMemory` + `JdbcChatMemoryRepository`。

### 默认：H2 内存数据库（零配置）

启动即用，无需任何额外配置。聊天记录自动持久化，支持对话历史回溯。

### 切换 MySQL（自动建库建表）

创建 `backend/src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_template?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

启动时指定 profile：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

> **无需手动建库建表** — `createDatabaseIfNotExist=true` 自动创建数据库，Flyway 自动执行建表迁移。

### 配置参考

所有可配置项见 `application-example.yml`，包含完整注释和 local/test/prod 三种 profile 示例。

### 对话管理 API

| 接口 | 说明 |
|---|---|
| `GET /api/conversations` | 列出所有对话 |
| `GET /api/conversations/{id}/messages` | 获取对话历史消息 |
| `DELETE /api/conversations/{id}` | 清除对话记录 |

---

## Architecture

```
Clients
  ├─ Web Frontend (React 19 + Ant Design + Zustand)
  └─ KMP Client (Compose Desktop / Android + Voyager + Koin)
            |
            v
Backend (Spring Boot 3.5 + Spring AI 1.1)
  ├─ ChatController (/api/chat, /api/chat/stream)
  ├─ ConversationController (/api/conversations)
  ├─ ModelAdminController (/api/admin/models) — CRUD + toggle
  ├─ SkillAdminController (/api/admin/skills) — CRUD + import
  ├─ ChatService (ReactAgent + ChatMemory integration)
  ├─ ModelRegistry → builtin adapters + dynamic adapters + enable/disable
  ├─ ToolRegistry  → ToolAdapter SPI
  ├─ SkillRegistry → builtin + dynamic skills
  ├─ ChatMemory → JdbcChatMemoryRepository → H2 / MySQL
  └─ MyBatis-Plus + Flyway → app_settings / model_config tables
            |
            v
Providers: DashScope / OpenAI / DeepSeek / Ollama / Any OpenAI-compatible
MCP Servers: brave-search / filesystem (optional)
```

### 插件化 SPI

```java
// 新增模型 — 实现 ModelAdapter 即可
public interface ModelAdapter {
    String provider();
    String modelId();
    CapabilitySet capabilities();
    HealthStatus health();
    ChatResult invoke(ChatCommand cmd);
    Flux<String> stream(ChatCommand cmd);
}

// 新增工具 — 实现 ToolAdapter 即可
public interface ToolAdapter {
    String toolName();
    ToolRiskLevel riskLevel();
    ToolResult invoke(ToolCommand cmd);
}

// 新增技能 — 实现 SkillProvider 即可
public interface SkillProvider {
    String skillName();
    String version();
    String content();
}
```

> **扩展原则：新增能力只加 `plugins/` 下的文件，永远不改 `core/`。**

---

## Project Structure

```
backend/
  core/          # SPI 接口定义（ModelAdapter / ToolAdapter / SkillProvider）+ 领域模型
  app/           # 服务编排（ChatService / ModelRegistry / ToolRegistry / SkillRegistry / SettingsService）
  plugins/       # 插件实现（model/ tool/ skill/）
  api/           # REST 接口 + DTO + Admin Controller
  infra/         # 基础设施（安全、HTTP、记忆、数据库）
    db/entity/   # MyBatis-Plus 实体
    db/mapper/   # MyBatis-Plus Mapper 接口
    db/typehandler/ # 自定义类型处理器

frontend/
  src/pages/     # 页面（Dashboard / Chat / Models / Tools / Skills / Settings）
  src/layouts/   # 布局（MainLayout — 暗色侧边栏 + 面包屑）
  src/core/      # API client + Zustand state
  src/shared/    # 共享组件（MessageBubble / ToolCallCard）
  src/utils/     # 工具函数（SSE streaming / format）

kmp-client/
  composeApp/    # 共享跨平台 UI (Compose Multiplatform)
  desktopApp/    # 桌面端打包与平台入口
  androidApp/    # 安卓端打包与平台入口

docs/
  photos/        # 截图
  quickstart/    # 快速启动文档
  extension-guides/  # 扩展指南
  troubleshooting/   # 故障排查
```

---

## API Key Configuration

API Key 支持三种配置方式（优先级从高到低）：

1. **页面设置** — 前端 Settings 页面配置，存入数据库
2. **环境变量** — `DASHSCOPE_API_KEY`、`OPENAI_API_KEY`
3. **yml 配置** — `application.yml` 或自定义 profile

| 环境变量 | 用途 | 必填 |
|---|---|---|
| `DASHSCOPE_API_KEY` | DashScope / 通义千问 | 可通过页面配置替代 |
| `OPENAI_API_KEY` | OpenAI 兼容接口 | 可通过页面配置替代 |

<details>
<summary><b>Windows PowerShell 配置</b></summary>

```powershell
# 当前终端临时生效
$env:DASHSCOPE_API_KEY="your_key"
$env:OPENAI_API_KEY="your_key"

# 写入用户环境变量（长期生效）
[Environment]::SetEnvironmentVariable("DASHSCOPE_API_KEY","your_key","User")
```

</details>

---

## Function Calling & Skills

### 验证 Function Calling

1. 启动后端，在 Settings 页面配置 API Key（或设置环境变量）
2. 前端选择真实模型（如 `dashscope-qwen3.5-plus`）
3. 勾选工具：`weather.query` 或 `mcp.time.now`
4. 提问：`帮我查一下北京天气并总结`

### Skills 管理

- 前端 **Skills Config** 面板支持新增/删除/导入
- 支持 `skills.sh` 批量导入格式
- 支持 URL/GitHub slug 远程导入
- Python 脚本技能自动发现执行

<details>
<summary><b>skills.sh 示例</b></summary>

```bash
add_skill "team/custom/research" "1.0.0" <<'EOF'
You are a research copilot.
Always provide 3 key findings first.
EOF
```

</details>

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.5.9, Spring AI 1.1.2, Spring AI Alibaba 1.1.2.0 |
| ORM | MyBatis-Plus 3.5.16 |
| Migration | Flyway |
| Web Frontend | React 19, Vite 7, Ant Design 5, Zustand |
| Desktop/Mobile Client | Kotlin Multiplatform (KMP), Compose Multiplatform, Voyager, Koin, Ktor |
| Database | H2 (default) / MySQL / PostgreSQL |
| AI Models | OpenAI, DashScope, Local Mock |
| Agent | SAA ReactAgent + SkillsAgentHook |

---

## Roadmap

- [x] Phase 1 — 模型路由 + 工具调用 + 流式输出 + Web 控制台
- [x] Phase 1.5 — JDBC 聊天记忆 + 对话管理 API
- [x] Phase 2 — 模型管理后台 + Skills 管理 + 动态模型配置
- [x] Phase 2.5 — MyBatis-Plus + Flyway + 系统设置页面 + yml 精简
- [ ] Phase 3 — MCP 插件生态 + 插件脚手架
- [ ] Phase 4 — 鉴权 + 限流 + 审计 + 可观测 + 成本治理

---

## Contributing

欢迎提交 Issue 和 PR！

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/your-feature`
3. 提交变更：`git commit -m 'Add your feature'`
4. 推送分支：`git push origin feature/your-feature`
5. 提交 Pull Request

---

## License

本项目采用 [Apache License 2.0](./LICENSE) 开源协议。


默认管理员
用户名: admin
密码: admin123
角色: ADMIN