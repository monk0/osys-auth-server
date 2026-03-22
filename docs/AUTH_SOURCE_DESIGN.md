# 可插拔认证源架构设计

## 背景

企业部署时可能需要对接自有认证体系：
- 企业微信/钉钉/飞书
- Active Directory / LDAP
- 企业自建 OIDC/OAuth2 服务
- SAML 身份提供商
- CAS 单点登录

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    OSYS Auth Server                          │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Authentication Provider Chain             │  │
│  │  (按优先级依次尝试各认证源)                              │  │
│  └───────────────────────────────────────────────────────┘  │
│                          │                                   │
│           ┌──────────────┼──────────────┐                   │
│           ▼              ▼              ▼                   │
│    ┌──────────┐   ┌──────────┐   ┌──────────┐             │
│    │ 本地认证  │   │ 外部认证  │   │ 第三方   │             │
│    │ (Local)  │   │ (LDAP/AD)│   │ (OIDC)   │             │
│    └──────────┘   └──────────┘   └──────────┘             │
│         │              │              │                     │
│         ▼              ▼              ▼                     │
│    ┌──────────┐   ┌──────────┐   ┌──────────┐             │
│    │ Account  │   │ Account  │   │ Account  │             │
│    │ (LOCAL)  │   │ (LDAP)   │   │ (OIDC)   │             │
│    └──────────┘   └──────────┘   └──────────┘             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 核心设计

### 1. 认证源类型枚举

```java
public enum AuthSourceType {
    LOCAL,           // 本地账号（用户名密码、手机验证码）
    LDAP,            // Active Directory / LDAP
    OIDC,            // OpenID Connect（外部）
    OAUTH2,          // OAuth2（外部）
    SAML,            // SAML 2.0
    CAS,             // CAS 单点登录
    WECHAT_WORK,     // 企业微信
    DINGTALK,        // 钉钉
    FEISHU,          // 飞书
    CUSTOM           // 自定义扩展
}
```

### 2. 认证源配置表

```sql
CREATE TABLE auth_sources (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_code         VARCHAR(50) UNIQUE NOT NULL COMMENT '认证源编码',
    source_name         VARCHAR(100) NOT NULL COMMENT '认证源名称',
    source_type         VARCHAR(20) NOT NULL COMMENT '类型: LDAP/OIDC/OAUTH2/...',
    
    -- 启用状态
    enabled             TINYINT DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    priority            INT DEFAULT 100 COMMENT '优先级（越小越优先）',
    
    -- 配置（JSON 格式，不同类型不同配置）
    config_json         JSON COMMENT '认证源配置',
    
    -- 属性映射（外部属性 -> 本地属性）
    attribute_mapping   JSON COMMENT '属性映射配置',
    
    -- 自动创建账号
    auto_create_user    TINYINT DEFAULT 1 COMMENT '首次登录自动创建用户: 0-否, 1-是',
    default_role        VARCHAR(50) COMMENT '自动创建时默认角色',
    
    -- 状态
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_type (source_type),
    INDEX idx_enabled_priority (enabled, priority)
) COMMENT='认证源配置表';
```

### 3. 支持的认证源配置

#### 3.1 LDAP/AD 配置

```json
{
  "url": "ldap://ad.company.com:389",
  "baseDn": "dc=company,dc=com",
  "bindDn": "cn=admin,dc=company,dc=com",
  "bindPassword": "encrypted_password",
  "userSearchBase": "ou=users,dc=company,dc=com",
  "userSearchFilter": "(sAMAccountName={0})",
  "groupSearchBase": "ou=groups,dc=company,dc=com",
  "groupSearchFilter": "(member={0})",
  "userNameAttribute": "sAMAccountName",
  "userEmailAttribute": "mail",
  "userPhoneAttribute": "telephoneNumber",
  "userDisplayNameAttribute": "displayName",
  "ssl": false,
  "startTls": true
}
```

#### 3.2 OIDC 配置

```json
{
  "issuer": "https://auth.company.com",
  "clientId": "osys-auth-client",
  "clientSecret": "encrypted_secret",
  "authorizationEndpoint": "https://auth.company.com/oauth2/authorize",
  "tokenEndpoint": "https://auth.company.com/oauth2/token",
  "userInfoEndpoint": "https://auth.company.com/userinfo",
  "jwksUri": "https://auth.company.com/.well-known/jwks.json",
  "scope": "openid profile email",
  "redirectUri": "https://auth.osys.local/auth/callback/oidc",
  "pkce": true
}
```

#### 3.3 企业微信配置

```json
{
  "corpId": "wwxxxxxxxxxxxxxxxx",
  "agentId": "1000002",
  "secret": "encrypted_secret",
  "qrCodeAppId": "wwxxxxxxxxxxxxxxxx",
  "qrCodeAgentId": "1000002",
  "qrCodeRedirectUri": "https://auth.osys.local/auth/callback/wechat-work"
}
```

### 4. 属性映射配置

外部认证源的用户属性需要映射到本地用户字段：

```json
{
  "userCode": "${username}",           // LDAP: sAMAccountName, OIDC: preferred_username
  "nickname": "${displayName}",        // LDAP: displayName, OIDC: name
  "email": "${email}",                 // LDAP: mail, OIDC: email
  "avatar": "${avatarUrl}",            // OIDC: picture
  "department": "${department}",       // 企业微信: department
  "externalId": "${sub}"               // OIDC: sub, LDAP: entryUUID
}
```

### 5. 认证流程

#### 本地认证流程
```
用户输入用户名密码
    ↓
查找 Account (source_type='LOCAL')
    ↓
验证密码（BCrypt）
    ↓
登录成功 → 创建 SSO Session
```

#### 外部 OIDC 认证流程
```
用户点击"企业登录"
    ↓
重定向到外部 OIDC Provider
    ↓
用户在 IdP 登录成功
    ↓
IdP 重定向回 /auth/callback/{sourceCode}
    ↓
用 code 换 token
    ↓
调用 UserInfo 获取用户信息
    ↓
根据 attribute_mapping 映射到本地字段
    ↓
查找或自动创建用户
    ↓
创建 Account (source_type='OIDC')
    ↓
登录成功 → 创建 SSO Session
```

#### LDAP 认证流程
```
用户输入用户名密码
    ↓
尝试 LDAP 认证
    ↓
LDAP bind 成功
    ↓
查询 LDAP 用户信息
    ↓
映射到本地字段
    ↓
查找或自动创建用户
    ↓
创建 Account (source_type='LDAP')
    ↓
登录成功
```

### 6. Java 接口设计

```java
/**
 * 认证源接口
 */
public interface AuthSource {
    
    /**
     * 获取认证源编码
     */
    String getSourceCode();
    
    /**
     * 获取认证源类型
     */
    AuthSourceType getType();
    
    /**
     * 是否启用
     */
    boolean isEnabled();
    
    /**
     * 认证
     * @param credentials 凭证（不同源不同类型）
     * @return 认证结果
     */
    AuthResult authenticate(AuthCredentials credentials);
    
    /**
     * 获取登录 URL（用于重定向到外部 IdP）
     */
    String getLoginUrl(String redirectUri, String state);
    
    /**
     * 处理回调
     */
    AuthResult handleCallback(Map<String, String> params);
    
    /**
     * 同步用户信息（可选）
     */
    void syncUserInfo(String externalId);
}

/**
 * 本地认证源实现
 */
@Component
public class LocalAuthSource implements AuthSource {
    // 实现本地数据库认证
}

/**
 * LDAP 认证源实现
 */
@Component
public class LdapAuthSource implements AuthSource {
    // 实现 LDAP/AD 认证
}

/**
 * OIDC 认证源实现
 */
@Component
public class OidcAuthSource implements AuthSource {
    // 实现外部 OIDC 认证
}

/**
 * 企业微信认证源实现
 */
@Component
public class WechatWorkAuthSource implements AuthSource {
    // 实现企业微信扫码登录
}
```

### 7. 配置示例

#### application.yml

```yaml
auth:
  sources:
    # 本地认证（默认启用）
    local:
      enabled: true
      priority: 100
    
    # 企业 LDAP
    corporate-ldap:
      enabled: ${LDAP_ENABLED:false}
      type: LDAP
      priority: 10
      config:
        url: ${LDAP_URL:ldap://ad.company.com:389}
        base-dn: ${LDAP_BASE_DN:dc=company,dc=com}
        bind-dn: ${LDAP_BIND_DN:cn=admin,dc=company,dc=com}
        bind-password: ${LDAP_BIND_PASSWORD:}
        user-search-base: ${LDAP_USER_SEARCH_BASE:ou=users}
        user-search-filter: (sAMAccountName={0})
      attribute-mapping:
        userCode: "${sAMAccountName}"
        nickname: "${displayName}"
        email: "${mail}"
      auto-create-user: true
      default-role: USER
    
    # 企业自建 OIDC
    corporate-oidc:
      enabled: ${OIDC_ENABLED:false}
      type: OIDC
      priority: 20
      config:
        issuer: ${OIDC_ISSUER:https://auth.company.com}
        client-id: ${OIDC_CLIENT_ID:osys-auth}
        client-secret: ${OIDC_CLIENT_SECRET:}
        scope: "openid profile email"
      attribute-mapping:
        userCode: "${preferred_username}"
        nickname: "${name}"
        email: "${email}"
        avatar: "${picture}"
      auto-create-user: true
    
    # 企业微信
    wechat-work:
      enabled: ${WECHAT_WORK_ENABLED:false}
      type: WECHAT_WORK
      priority: 30
      config:
        corp-id: ${WECHAT_CORP_ID:}
        agent-id: ${WECHAT_AGENT_ID:}
        secret: ${WECHAT_SECRET:}
```

### 8. 登录页面适配

登录页根据启用的认证源动态显示：

```html
<!-- 本地登录 -->
<form id="local-login">
  <input type="text" name="username" placeholder="用户名/手机号">
  <input type="password" name="password" placeholder="密码">
  <button>登录</button>
</form>

<!-- 外部认证源按钮（动态生成） -->
<div class="external-logins">
  <button class="ldap-login" data-source="corporate-ldap">
    企业 AD 登录
  </button>
  <button class="oidc-login" data-source="corporate-oidc">
    企业门户登录
  </button>
  <button class="wechat-login" data-source="wechat-work">
    企业微信扫码
  </button>
</div>
```

### 9. 数据库更新脚本

```sql
-- 添加认证源字段到 accounts 表
ALTER TABLE accounts 
ADD COLUMN source_code VARCHAR(50) DEFAULT 'LOCAL' COMMENT '认证源编码',
ADD COLUMN external_id VARCHAR(255) COMMENT '外部系统用户ID',
ADD INDEX idx_source_code (source_code),
ADD INDEX idx_external_id (source_code, external_id);

-- 创建认证源配置表
CREATE TABLE auth_sources (...);

-- 初始化本地认证源
INSERT INTO auth_sources (source_code, source_name, source_type, enabled, priority, config_json) 
VALUES ('LOCAL', '本地账号', 'LOCAL', 1, 100, '{}');
```

### 10. 企业部署建议

#### 场景1：纯本地部署
```yaml
auth:
  sources:
    local:
      enabled: true
```

#### 场景2：对接企业 AD
```yaml
auth:
  sources:
    local:
      enabled: true  # 保留本地账号用于管理员
      priority: 100
    corporate-ldap:
      enabled: true
      priority: 10   # AD 优先
```

#### 场景3：对接企业自建 IAM
```yaml
auth:
  sources:
    corporate-oidc:
      enabled: true
      type: OIDC
    local:
      enabled: false  # 关闭本地登录
```

#### 场景4：多认证源共存
```yaml
auth:
  sources:
    wechat-work:
      enabled: true  # 员工用企业微信
      priority: 10
    corporate-ldap:
      enabled: true  # 管理员用 AD
      priority: 20
    local:
      enabled: true  # 系统账号用本地
      priority: 100
```
