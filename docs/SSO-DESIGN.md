# SSO (单点登录) 设计文档

## 架构概述

OSYS Auth Server 作为平台**中央认证中心**，为所有子系统提供统一的身份认证和授权服务。

```
┌─────────────────────────────────────────────────────────────────┐
│                      用户 (User)                                 │
└────────────────────┬────────────────────────────────────────────┘
                     │
       ┌─────────────┼─────────────┐
       │             │             │
       ▼             ▼             ▼
┌──────────┐   ┌──────────┐   ┌──────────┐
│  子系统A  │   │  子系统B  │   │  子系统C  │
│  (商城)   │   │  (管理)   │   │  (支付)   │
└────┬─────┘   └────┬─────┘   └────┬─────┘
     │              │              │
     └──────────────┼──────────────┘
                    │ OAuth2/OIDC
                    ▼
        ┌───────────────────────┐
        │   OSYS Auth Server    │
        │    (中央认证中心)      │
        └───────────────────────┘
```

## 核心机制

### 1. 集中式会话管理

**SSO Session (中央会话)**
```
用户登录成功
    ↓
创建 sso_session (中央会话)
    ↓
颁发各子系统 token
    ↓
子系统通过 token 访问受保护资源
```

**会话层级：**
- **SSO Session**: 用户在中央认证服务器的全局会话
- **Client Session**: 各子系统的独立会话（基于 OAuth2 Token）

### 2. 登录流程 (SSO)

```
用户访问子系统A (未登录)
    ↓
子系统A 重定向到 Auth Server
    ↓
Auth Server 检查 SSO Session
    ↓
无 Session → 显示登录页
    ↓
用户输入账号密码
    ↓
创建 SSO Session
    ↓
生成 Authorization Code
    ↓
重定向回子系统A
    ↓
子系统A 用 Code 换 Token
    ↓
子系统A 登录成功

用户访问子系统B (已登录子系统A)
    ↓
子系统B 重定向到 Auth Server
    ↓
Auth Server 检查 SSO Session (存在!)
    ↓
无需再次登录 → 直接生成 Code
    ↓
重定向回子系统B
    ↓
子系统B 用 Code 换 Token
    ↓
子系统B 登录成功 (SSO 完成)
```

### 3. 登出流程 (SLO - Single Logout)

```
用户在子系统A 点击登出
    ↓
子系统A 调用 Auth Server /oauth2/revoke
    ↓
Auth Server 销毁 SSO Session
    ↓
Auth Server 通知所有已登录子系统 (Back-Channel Logout)
    ↓
各子系统清除本地 Session
    ↓
用户在所有子系统登出
```

## 数据库设计扩展

### 1. 应用/子系统注册表 (applications)

扩展 `oauth2_registered_client`，添加应用元数据：

```sql
CREATE TABLE applications (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id           VARCHAR(100) UNIQUE NOT NULL COMMENT 'OAuth2 Client ID',
    app_name            VARCHAR(100) NOT NULL COMMENT '应用名称',
    app_code            VARCHAR(50) UNIQUE NOT NULL COMMENT '应用编码(系统标识)',
    app_type            VARCHAR(20) NOT NULL COMMENT '类型: WEB, APP, SERVICE',
    description         VARCHAR(500) COMMENT '应用描述',
    
    -- 配置
    homepage_url        VARCHAR(255) COMMENT '首页URL',
    logo_url            VARCHAR(255) COMMENT 'Logo',
    
    -- SSO 配置
    sso_enabled         TINYINT DEFAULT 1 COMMENT '是否启用SSO: 0-否, 1-是',
    sso_session_timeout INT DEFAULT 7200 COMMENT 'SSO会话超时(秒)',
    
    -- 权限配置
    required_scopes     VARCHAR(500) COMMENT '必需权限范围',
    auto_grant_scopes   VARCHAR(500) COMMENT '自动授权的权限',
    
    -- 状态
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_app_code (app_code),
    INDEX idx_client_id (client_id)
) COMMENT='应用/子系统注册表';
```

### 2. SSO 会话表 (sso_sessions)

```sql
CREATE TABLE sso_sessions (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id          VARCHAR(64) UNIQUE NOT NULL COMMENT 'SSO Session ID',
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    
    -- 会话信息
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_accessed_at    DATETIME COMMENT '最后访问时间',
    expires_at          DATETIME NOT NULL COMMENT '过期时间',
    
    -- 设备信息
    ip_address          VARCHAR(45) COMMENT 'IP地址',
    user_agent          VARCHAR(500) COMMENT 'UA',
    device_fingerprint  VARCHAR(64) COMMENT '设备指纹',
    
    -- 状态
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-已失效, 1-有效',
    
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
) COMMENT='SSO 中央会话表';
```

### 3. 客户端登录状态表 (client_sessions)

记录用户在各个子系统的登录状态：

```sql
CREATE TABLE client_sessions (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    sso_session_id      VARCHAR(64) NOT NULL COMMENT '关联SSO Session',
    client_id           VARCHAR(100) NOT NULL COMMENT 'OAuth2 Client ID',
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    
    -- Token 信息
    access_token_hash   VARCHAR(64) COMMENT 'Access Token 哈希(用于查找)',
    refresh_token_hash  VARCHAR(64) COMMENT 'Refresh Token 哈希',
    
    -- 会话时间
    login_at            DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    expires_at          DATETIME NOT NULL COMMENT 'Token过期时间',
    
    -- 状态
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-已登出, 1-有效',
    
    INDEX idx_sso_session (sso_session_id),
    INDEX idx_client_user (client_id, user_id),
    INDEX idx_expires_at (expires_at)
) COMMENT='客户端登录状态表';
```

### 4. 用户应用授权表 (user_app_authorizations)

记录用户对各应用的授权情况（类似"记住授权"）：

```sql
CREATE TABLE user_app_authorizations (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    client_id           VARCHAR(100) NOT NULL COMMENT '应用Client ID',
    
    -- 授权信息
    scopes              VARCHAR(500) NOT NULL COMMENT '已授权的权限范围',
    authorized_at       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    
    -- 是否自动授权（跳过确认页）
    auto_authorized     TINYINT DEFAULT 0 COMMENT '0-每次确认, 1-自动授权',
    
    UNIQUE KEY uk_user_client (user_id, client_id),
    INDEX idx_client_id (client_id)
) COMMENT='用户应用授权表';
```

## 接口设计

### 1. 应用注册接口

```java
@PostMapping("/api/admin/applications")
public Result<ApplicationVO> registerApplication(@RequestBody @Valid ApplicationDTO dto) {
    // 注册新应用/子系统
    // 自动生成 client_id 和 client_secret
    // 返回应用凭证
}
```

### 2. SSO 状态查询

```java
@GetMapping("/api/sso/status")
public Result<SsoStatusVO> checkSsoStatus(@CookieValue(name = "SSO_SESSION", required = false) String sessionId) {
    // 检查用户是否已登录（用于子系统前端判断）
    // 返回: 是否登录、登录用户信息、已授权的应用列表
}
```

### 3. 强制登出 (SLO)

```java
@PostMapping("/api/sso/logout")
public Result<Void> globalLogout(@CookieValue("SSO_SESSION") String sessionId) {
    // 1. 销毁 SSO Session
    // 2. 通知所有已登录子系统 (Back-Channel Logout)
    // 3. 清除所有 Client Sessions
}
```

### 4. 获取已登录应用列表

```java
@GetMapping("/api/user/sessions")
public Result<List<ClientSessionVO>> listActiveSessions(@AuthenticationPrincipal Jwt jwt) {
    // 返回用户当前在哪些子系统登录
}
```

## 配置扩展

```yaml
auth:
  sso:
    # SSO Session 配置
    session-timeout: 7200  # 2小时
    session-refresh-threshold: 1800  # 30分钟内访问自动续期
    
    # 登出配置
    back-channel-logout-enabled: true
    back-channel-logout-uri-template: "{clientBaseUrl}/auth/logout"
    
    # Cookie 配置
    cookie-name: "OSYS_SSO_SESSION"
    cookie-domain: ".osys.local"  # 跨子系统共享
    cookie-secure: true
    cookie-http-only: true
    cookie-same-site: "Lax"
    
    # 授权确认页配置
    auto-approve-scopes: "openid,profile"  # 自动同意的权限
    require-user-consent: true  # 是否显示授权确认页
```

## 安全考虑

### 1. 会话安全
- SSO Session ID 使用加密随机生成
- Cookie 设置 HttpOnly、Secure、SameSite
- 定期清理过期会话

### 2. 跨域安全
- 使用 CORS 限制允许的子系统域名
- PKCE 流程防止授权码拦截攻击

### 3. 登出安全
- Back-Channel Logout 使用签名验证
- 防止会话固定攻击

## 后续扩展

- [ ] MFA (多因素认证) - SSO 级别统一启用
- [ ] 设备管理 - 查看/管理已登录设备
- [ ] 审计日志 - 记录所有子系统的登录/操作
- [ ] 应用市场 - 动态注册第三方应用
- [ ] 权限中心 - 细粒度 RBAC 权限控制
