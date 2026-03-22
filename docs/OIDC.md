# OIDC (OpenID Connect) 配置说明

本文档说明 OSYS Auth Server 的 OIDC 支持。

## 什么是 OIDC

OpenID Connect (OIDC) 是基于 OAuth2 的身份层，提供：
- **身份验证**：验证用户身份
- **用户信息**：通过 UserInfo 端点获取用户资料
- **ID Token**：包含用户身份的 JWT

## 支持的 OIDC 端点

| 端点 | 路径 | 说明 |
|------|------|------|
| Discovery | `/.well-known/openid-configuration` | OIDC 配置信息 |
| Authorization | `/oauth2/authorize` | 授权端点 |
| Token | `/oauth2/token` | Token 端点 |
| UserInfo | `/userinfo` | 用户信息端点 |
| JWKS | `/oauth2/jwks` | 公钥端点 |
| Logout | `/logout` | 登出端点 |

## 支持的 Scopes

| Scope | 说明 |
|-------|------|
| `openid` | 必需，表示 OIDC 请求 |
| `profile` | 获取用户基本信息（昵称、头像等）|
| `email` | 获取邮箱地址 |
| `phone` | 获取手机号 |
| `read` | 读取权限 |
| `write` | 写入权限 |

## 支持的 Claims

### 标准 Claims

| Claim | 说明 | 示例 |
|-------|------|------|
| `sub` | 用户唯一标识 | `U123456` |
| `name` | 全名 | `张三` |
| `nickname` | 昵称 | `张三` |
| `picture` | 头像 URL | `https://...` |
| `email` | 邮箱 | `xxx@example.com` |
| `email_verified` | 邮箱是否验证 | `true` |
| `phone_number` | 手机号 | `13800138000` |
| `phone_number_verified` | 手机号是否验证 | `true` |

### 自定义 Claims

| Claim | 说明 |
|-------|------|
| `user_id` | 内部用户 ID |
| `user_code` | 用户编码 |
| `account_types` | 用户绑定的账号类型列表 |

## ID Token 结构

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "key-id"
  },
  "payload": {
    "iss": "http://localhost:9000",
    "sub": "U123456",
    "aud": "osys-web",
    "exp": 1640995200,
    "iat": 1640991600,
    "auth_time": 1640991600,
    "nonce": "random-nonce",
    "name": "张三",
    "email": "xxx@example.com",
    "email_verified": true,
    "user_id": 1,
    "user_code": "U123456"
  },
  "signature": "..."
}
```

## 客户端集成示例

### JavaScript (SPA)

```javascript
// 使用 oidc-client-ts 库
import { UserManager } from 'oidc-client-ts';

const userManager = new UserManager({
  authority: 'http://localhost:9000',
  client_id: 'osys-web',
  redirect_uri: 'http://localhost:3000/auth/callback',
  response_type: 'code',
  scope: 'openid profile email',
  userStore: new WebStorageStateStore({ store: window.localStorage })
});

// 登录
userManager.signinRedirect();

// 获取用户信息
const user = await userManager.getUser();
console.log(user.profile);  // OIDC claims
```

### Java (Spring Boot)

```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          osys:
            client-id: osys-web
            client-secret: secret
            scope: openid,profile,email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          osys:
            issuer-uri: http://localhost:9000
```

### Python

```python
from authlib.integrations.flask_client import OAuth

oauth = OAuth(app)
oauth.register(
    name='osys',
    client_id='osys-web',
    client_secret='secret',
    server_metadata_url='http://localhost:9000/.well-known/openid-configuration',
    client_kwargs={'scope': 'openid profile email'}
)

# 登录
@app.route('/login')
def login():
    return oauth.osys.authorize_redirect(redirect_uri='http://localhost:5000/auth')

# 回调
@app.route('/auth')
def auth():
    token = oauth.osys.authorize_access_token()
    userinfo = oauth.osys.userinfo()
    return userinfo
```

## 验证 ID Token

客户端应验证 ID Token 的：
1. **签名**：使用 JWKS 端点的公钥验证
2. **iss**：发行人必须匹配
3. **aud**：受众必须包含客户端 ID
4. **exp**：未过期
5. **nonce**：与请求时一致（防止重放攻击）

## 配置参考

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        client:
          registration:
            osys-web:
              id: osys-web
              client-id: osys-web
              client-secret: secret
              client-authentication-methods:
                - client_secret_basic
                - client_secret_post
              authorization-grant-types:
                - authorization_code
                - refresh_token
              redirect-uris:
                - http://localhost:3000/auth/callback
              scopes:
                - openid
                - profile
                - email
              token-settings:
                id-token-signature-algorithm: RS256
```
