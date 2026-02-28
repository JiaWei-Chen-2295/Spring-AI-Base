# Troubleshooting

## App fails to start

- Check Java version (`17+`)
- Check port `8080` is free

## API returns MODEL_NOT_FOUND

- Verify `modelId` from `/api/models`

## 所有接口返回 401

- 检查 `application.yml` 中 `app.features.auth-enabled` 是否为 `true`
- 若处于开发阶段，将其改为 `false` 后重启，所有接口即可无需 token 访问
- 若需要保持 `true`，请先调用 `POST /api/auth/login` 获取 token，再在请求头中携带 `Authorization: Bearer <token>`

## auth-enabled=true 但 admin 账户不存在

- `AdminInitializer` 仅在 `auth-enabled=true` 时启动
- 检查数据库 `users` 表，确认 Flyway migration 已执行（`roles` 表需有 `ADMIN` 记录）
- 重启应用，`AdminInitializer` 会在启动时自动创建 `admin / admin123`

## SSE 流式聊天在 auth-enabled=true 时返回 401

- `EventSource` 无法设置请求头，token 需通过 query param 传递
- 前端 `buildStreamUrl()` 会自动追加 `?token=<jwt>`；若自行构造 SSE URL，请手动添加该参数
- 确认 `localStorage` 中的 `auth-storage` 包含有效的 `state.token`

## 登录后仍被重定向到 /login

- 打开浏览器 DevTools → Application → LocalStorage，查看 `auth-storage` 是否存在且 `state.isAuthenticated` 为 `true`
- 检查 `GET /api/config` 响应：若 `authEnabled: false`，前端会跳过登录页直接进入 Dashboard

## @PublicApi 注解的接口仍然需要登录

- 确认 `auth-enabled: true`（`false` 时所有接口本就公开）
- 确认注解已加在 Controller 类或方法上，且 `@Retention(RetentionPolicy.RUNTIME)` 存在
- 查看启动日志中 `Auth enabled — public endpoints: [...]`，确认该 URL 出现在列表中
