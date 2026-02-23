# AI Template - Phase 1 (Backend + Frontend)

## 目标
当前实现已进入 `PROJECT_PLAN.md` 的 **Phase 1 后端主链路**：
- `/api/models`、`/api/chat`、`/api/chat/stream` 可用
- 已接入真实模型插件：OpenAI + DashScope（DashScope 使用原生 SDK）
- 前端最小聊天页可用（模型选择 + 同步/流式对话 + 错误展示）

## 当前能力
- 后端可启动，默认端口 `8080`
- 元数据接口：
  - `GET /api/models`
  - `GET /api/tools`
  - `GET /api/skills`
- 聊天接口：
  - `POST /api/chat`
  - `GET /api/chat/stream`
- 已内置示例插件：
  - 模型：`local-echo`、`local-reverse`
  - 真实模型（启用条件：配置密钥）：
    - `openai-${OPENAI_CHAT_MODEL}`（默认 `openai-gpt-4o`）
    - `dashscope-${DASHSCOPE_CHAT_MODEL}`（默认 `dashscope-qwen-plus`）
  - 工具：`weather.query`（Mock）
  - Skill：`team/default/summarize@1.0.0`

## 快速启动
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local-minimal
```

启用真实模型（推荐）：
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local-full
```

在启动前请先配置 API Key。

Windows PowerShell（临时）：
```powershell
$env:OPENAI_API_KEY="your_openai_key"
$env:DASHSCOPE_API_KEY="your_dashscope_key"
$env:OPENAI_CHAT_MODEL="gpt-4o"
$env:DASHSCOPE_CHAT_MODEL="qwen-plus"
```

Windows PowerShell（长期）：
```powershell
[Environment]::SetEnvironmentVariable("OPENAI_API_KEY","your_openai_key","User")
[Environment]::SetEnvironmentVariable("DASHSCOPE_API_KEY","your_dashscope_key","User")
[Environment]::SetEnvironmentVariable("OPENAI_CHAT_MODEL","gpt-4o","User")
[Environment]::SetEnvironmentVariable("DASHSCOPE_CHAT_MODEL","qwen-plus","User")
```

macOS / Linux：
```bash
export OPENAI_API_KEY="your_openai_key"
export DASHSCOPE_API_KEY="your_dashscope_key"
export OPENAI_CHAT_MODEL="gpt-4o"
export DASHSCOPE_CHAT_MODEL="qwen-plus"
```

验证（PowerShell）：
```powershell
echo $env:OPENAI_API_KEY
echo $env:DASHSCOPE_API_KEY
```

## 示例请求
```bash
curl http://localhost:8080/api/models
```

```bash
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -H "x-request-id: test-1" \
  -d '{"conversationId":"c1","modelId":"local-echo","message":"hello"}'
```

```bash
curl "http://localhost:8080/api/chat/stream?conversationId=c1&model=local-reverse&message=hello"
```

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

打开：`http://localhost:5173`

说明：
- 前端默认通过 Vite 代理访问后端 `/api/*`，无需手动配置跨域。
- 如果后端不是 `8080`，请修改 `frontend/vite.config.js` 中 `proxy.target`。
