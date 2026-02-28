# 鉴权与权限管理

## 概览

本项目采用 **JWT + RBAC** 鉴权体系，并通过 `app.features.auth-enabled` 开关实现"开发免登录 / 生产强鉴权"一键切换。

权限分三层，二次开发者只需在 Controller 上加对应注解，**无需修改 SecurityConfig**：

| 场景 | 做法 | 典型接口 |
|------|------|---------|
| 公开接口（无需登录） | `@PublicApi` | 登录、`/api/config` |
| 普通接口（需登录） | 不加注解（默认） | 聊天、模型列表 |
| 管理员接口（需 ADMIN 角色） | `@PreAuthorize("hasRole('ADMIN')")` | 用户管理、角色管理 |

---

## 开关配置

`backend/src/main/resources/application.yml`（或对应 profile 文件）：

```yaml
app:
  features:
    auth-enabled: false   # true = 生产鉴权模式；false = 开发免登录模式（默认）
```

### `auth-enabled: false`（默认 — 开发模式）

- 后端：所有接口 `permitAll()`，无需携带 token
- 前端：自动跳过登录页，直接进入 Dashboard
- `AdminInitializer` Bean **不加载**（无需初始化 admin 账户）

### `auth-enabled: true`（生产模式）

- 后端：仅 `@PublicApi` 标注的接口公开；`/api/admin/**` 要求 ADMIN 角色；其余接口要求已登录
- 前端：未登录时跳转 `/login`；登录后正常使用
- 启动时自动创建 `admin / admin123` 管理员账户（如不存在）
- SSE 流式接口自动携带 token（通过 query param `?token=...`）

---

## `@PublicApi` 注解

### 用法

```java
// 整个 Controller 公开
@PublicApi
@RestController
@RequestMapping("/api/open")
public class OpenController { ... }

// 单个方法公开（同一 Controller 内其他方法仍需登录）
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @PublicApi
    @GetMapping("/featured")        // 公开
    public List<Product> featured() { ... }

    @GetMapping("/all")             // 需登录
    public List<Product> all() { ... }
}
```

### 工作原理

`SecurityConfig` 在应用启动时通过 `RequestMappingHandlerMapping` 扫描所有 Handler Method，收集带有 `@PublicApi`（类级或方法级）的 URL pattern，与 Swagger / Actuator 固定白名单合并后统一 `permitAll()`。

二次开发者**不需要**编辑 SecurityConfig 的任何代码。

---

## 内置公开接口

| 接口 | 说明 |
|------|------|
| `POST /api/auth/login` | 用户登录，返回 JWT token |
| `POST /api/auth/logout` | 登出（客户端清除 token） |
| `POST /api/auth/refresh` | 刷新 token |
| `GET /api/config` | 查询 `authEnabled` 等前端配置 |
| `/swagger-ui/**` | Swagger UI |
| `/v3/api-docs/**` | OpenAPI 文档 |
| `/actuator/**` | Spring Actuator |

---

## 管理员接口（ADMIN 角色）

`/api/admin/**` 在 URL 层面要求 `ROLE_ADMIN`（SecurityConfig 规则），Controller 上的 `@PreAuthorize("hasRole('ADMIN')")` 提供方法级双重保护。

```java
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/audit")
public class AuditController { ... }
```

---

## SSE 流式接口鉴权

浏览器 `EventSource` 不支持自定义请求头，因此 `JwtTokenFilter` 在检查 `Authorization: Bearer <token>` 请求头之后，会继续检查 `?token=<jwt>` 查询参数。

前端 `buildStreamUrl()` 会自动从 `localStorage` 读取 token 并追加到 URL，无需手动处理。

```
GET /api/chat/stream?conversationId=...&model=...&message=...&token=<jwt>
```

---

## 默认账户（auth-enabled=true 时）

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | ADMIN |

**生产环境请在首次登录后立即修改密码。**

---

## 验证步骤

### 开发模式（auth-enabled=false）

```bash
# 1. 启动后端
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local-minimal

# 2. 无 token 直接访问业务接口 → 200
curl http://localhost:8080/api/models

# 3. 查询配置
curl http://localhost:8080/api/config
# → {"authEnabled":false}
```

### 生产模式（auth-enabled=true）

```bash
# 1. 修改 application.yml: auth-enabled: true，重启

# 2. 公开接口无需 token → 200
curl http://localhost:8080/api/config
# → {"authEnabled":true}

# 3. 业务接口无 token → 401
curl http://localhost:8080/api/models

# 4. 登录取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# 5. 携带 token 访问 → 200
curl http://localhost:8080/api/models -H "Authorization: Bearer $TOKEN"

# 6. SSE 流式（token 通过 query param）
curl "http://localhost:8080/api/chat/stream?model=...&message=hello&token=$TOKEN"
```

---

## 扩展：新增用户/角色

`auth-enabled=true` 时，可通过管理后台（`/users`、`/roles` 页面）或直接调用管理接口创建用户并分配角色。Role 的 `roleCode` 对应 Spring Security 权限中的 `ROLE_<roleCode>`。

---

## 相关源文件

| 文件 | 说明 |
|------|------|
| `core/PublicApi.java` | `@PublicApi` 注解定义 |
| `infra/http/SecurityConfig.java` | 安全配置 + `@PublicApi` 扫描逻辑 |
| `infra/security/JwtTokenFilter.java` | JWT 解析（Header + query param） |
| `infra/security/JwtTokenProvider.java` | Token 生成与验证 |
| `infra/db/AdminInitializer.java` | 自动创建 admin 账户（auth-enabled=true 时） |
| `api/controller/AuthController.java` | 登录 / 登出 / 刷新 / 个人信息 |
| `api/controller/MetadataController.java` | `GET /api/config` 公开配置接口 |
| `frontend/src/core/state/authStore.js` | 前端鉴权状态 + `checkAuthRequired()` |
| `frontend/src/App.jsx` | `AuthGuard` / `PublicRoute` 路由守卫 |
