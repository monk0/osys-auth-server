# OSYS Auth Server

基于 Spring Authorization Server 的平台级统一认证服务，支持单点登录 (SSO) 和多种登录方式。

## 特性

- ✅ **OIDC 完整支持** - UserInfo 端点、ID Token、Discovery、JWKS
- ✅ **单点登录 (SSO)** - 平台内多子系统统一登录状态
- ✅ **统一登出 (SLO)** - 一处登出，全平台失效
- ✅ **标准 OAuth2/OIDC** - 基于 Spring Authorization Server
- ✅ **多账号体系** - 一个用户可绑定多种认证方式
- ✅ **应用管理** - 子系统注册、授权管理
- ✅ **JWT Token** - Access Token + Refresh Token
- ✅ **安全机制** - 密码加密、验证码限流、登录锁定

## 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      OSYS Auth Server                        │
│                   (中央认证服务器)                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   users     │  │   accounts  │  │  sso_sessions       │ │
│  │  (用户主表)  │  │  (账号表)   │  │  (中央会话管理)      │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ applications│  │client_sessions│ │user_app_authorizations│
│  │ (子系统注册) │  │(子系统会话)  │  │  (用户授权)          │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└──────────────────┬──────────────────────────────────────────┘
                   │ OAuth2/OIDC
       ┌───────────┼───────────┐
       ▼           ▼           ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│  Web门户  │ │  管理后台  │ │  移动APP  │
│ (osys-web)│ │(osys-admin)│ │(osys-mobile)│
└──────────┘ └──────────┘ └──────────┘
```

## 核心能力

### 1. 单点登录 (SSO)

用户在任一子系统登录后，访问其他子系统自动识别登录状态，无需重复输入密码。

**登录流程：**
```
用户访问子系统A → 重定向到 Auth Server → 登录成功
     ↓                                           ↓
  创建 SSO Session                          颁发 Token
     ↓                                           ↓
用户访问子系统B → 重定向到 Auth Server → 发现已有 SSO Session → 直接颁发 Token
```

### 2. 统一登出 (SLO)

用户在一处登出，Auth Server 通知所有已登录子系统清除会话。

### 3. 多账号体系

```
用户 (User)
 └── 账号1 (USERNAME: zhangsan)
 └── 账号2 (MOBILE: 13800138000)
 └── 账号3 (WECHAT: wx_openid_xxx)
 └── 账号4 (EMAIL: xxx@example.com)
```

## 快速开始

### 1. 环境准备

```bash
# 启动 MySQL 和 Redis
docker-compose up -d
```

### 2. 数据库初始化

```bash
# 创建数据库并执行初始化脚本
mysql -u root -p < src/main/resources/schema.sql
```

脚本会自动创建：
- 6 张核心表（users, accounts, sso_sessions 等）
- 4 个示例子系统（Web门户、管理后台、API网关、移动APP）

### 3. 配置应用

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/osys_auth
    username: root
    password: your_password
  
  data:
    redis:
      host: localhost
      port: 6379

auth:
  sso:
    cookie-domain: ".your-domain.com"  # 配置跨域 Cookie 共享
```

### 4. 启动服务

```bash
./mvnw spring-boot:run
```

访问端点：
- 授权端点：`http://localhost:9000/oauth2/authorize`
- Token 端点：`http://localhost:9000/oauth2/token`
- OIDC 配置：`http://localhost:9000/.well-known/openid-configuration`

## 预置子系统

初始化脚本自动创建以下应用：

| 应用编码 | Client ID | 类型 | 说明 |
|---------|-----------|------|------|
| WEB_PORTAL | osys-web | Web | 主站 Web 门户 |
| ADMIN | osys-admin | Web | 管理后台 |
| API_GATEWAY | osys-api | Service | API 网关服务 |
| MOBILE_APP | osys-mobile | App | 移动应用 |

## API 接口

### SSO 登录

```bash
# 1. 用户访问子系统，重定向到
GET http://localhost:9000/oauth2/authorize?
  response_type=code&
  client_id=osys-web&
  redirect_uri=http://localhost:3000/auth/callback&
  scope=openid profile&
  state=xxx

# 2. 用户登录后，重定向回子系统
# 子系统用 code 换 token
POST http://localhost:9000/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code=xxx
&redirect_uri=http://localhost:3000/auth/callback
&client_id=osys-web
&client_secret=secret
```

### 手机号验证码登录

```bash
# 发送验证码
curl -X POST http://localhost:9000/api/auth/sms/send \
  -H "Content-Type: application/json" \
  -d '{"mobile": "13800138000", "type": "LOGIN"}'

# 验证码登录
curl -X POST http://localhost:9000/api/auth/sms/login \
  -H "Content-Type: application/json" \
  -d '{"mobile": "13800138000", "code": "123456"}'
```

### 统一登出 (SLO)

```bash
# 调用全局登出
POST http://localhost:9000/api/sso/logout
Authorization: Bearer your_access_token

# 或 OAuth2 标准登出
GET http://localhost:9000/logout?id_token_hint=xxx&post_logout_redirect_uri=xxx
```

### OIDC UserInfo

```bash
# 使用 Access Token 获取用户信息
GET http://localhost:9000/userinfo
Authorization: Bearer your_access_token

# 返回示例
{
  "sub": "U123456",
  "name": "张三",
  "nickname": "张三",
  "picture": "https://example.com/avatar.jpg",
  "email": "zhangsan@example.com",
  "email_verified": true,
  "phone_number": "13800138000",
  "phone_number_verified": true,
  "user_id": 1,
  "user_code": "U123456",
  "account_types": ["USERNAME", "MOBILE", "EMAIL"]
}
```

### OIDC Discovery

```bash
# 获取 OIDC 配置信息
GET http://localhost:9000/.well-known/openid-configuration

# 返回配置包括：
# - authorization_endpoint
# - token_endpoint
# - userinfo_endpoint
# - jwks_uri
# - scopes_supported
# - claims_supported
```

### JWKS (公钥)

```bash
# 获取验证 JWT 签名的公钥
GET http://localhost:9000/oauth2/jwks

# 返回 JWK Set，客户端用此验证 ID Token 和 Access Token
```

## 数据库设计

### 核心表

| 表名 | 说明 |
|------|------|
| `users` | 用户主表 |
| `accounts` | 账号表（多种认证方式） |
| `sso_sessions` | SSO 中央会话表 |
| `client_sessions` | 子系统登录状态表 |
| `applications` | 子系统注册表 |
| `user_app_authorizations` | 用户应用授权表 |
| `sms_codes` | 短信验证码表 |
| `login_logs` | 登录日志表 |

详细设计见 `docs/SSO-DESIGN.md`

## 项目结构

```
osys-auth-server/
├── docs/
│   └── SSO-DESIGN.md           # SSO 架构设计文档
├── src/
│   ├── main/java/com/osys/auth/
│   │   ├── config/             # 配置类
│   │   ├── controller/         # 控制器
│   │   ├── service/            # 业务层
│   │   ├── repository/         # 数据访问
│   │   ├── entity/             # 实体类
│   │   │   ├── User.java
│   │   │   ├── Account.java
│   │   │   ├── SsoSession.java       # SSO 会话
│   │   │   ├── ClientSession.java    # 客户端会话
│   │   │   ├── Application.java      # 应用注册
│   │   │   └── ...
│   │   └── security/           # 安全配置
│   └── resources/
│       ├── application.yml
│       └── schema.sql          # 数据库脚本
├── pom.xml
├── Dockerfile
└── docker-compose.yml
```

## 配置说明

### SSO 配置

```yaml
auth:
  sso:
    session-timeout: 7200              # SSO 会话超时（秒）
    cookie-name: "OSYS_SSO_SESSION"    # Cookie 名称
    cookie-domain: ".your-domain.com"  # 跨子系统共享 Cookie
    cookie-secure: true
    cookie-http-only: true
    auto-approve-scopes: "openid,profile"  # 自动同意的权限
    back-channel-logout-enabled: true  # 启用统一登出
```

### 子系统注册

```sql
-- 注册新应用
INSERT INTO applications 
  (client_id, app_name, app_code, app_type, homepage_url, sso_enabled)
VALUES 
  ('my-app', 'My Application', 'MY_APP', 'WEB', 'https://app.example.com', 1);
```

## Docker 部署

```bash
# 构建镜像
docker build -t osys-auth-server .

# 运行
docker run -p 9000:9000 \
  -e MYSQL_HOST=mysql \
  -e REDIS_HOST=redis \
  -e SSO_COOKIE_DOMAIN=.your-domain.com \
  osys-auth-server

# 或一键启动
docker-compose up -d
```

## 开发计划

- [x] 基础 OAuth2 认证
- [x] SSO 单点登录
- [x] SLO 统一登出
- [x] 多账号体系
- [x] 应用/子系统管理
- [ ] 设备管理（查看已登录设备）
- [ ] MFA 多因素认证
- [ ] 审计日志
- [ ] 权限中心 (RBAC)

## 许可证

MIT
