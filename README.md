# OSYS Auth Server

基于 Spring Authorization Server 的统一认证服务，支持多种登录方式（用户名密码、手机号验证码等）。

## 特性

- ✅ **标准 OAuth2/OIDC 协议** - 基于 Spring Authorization Server
- ✅ **多登录方式** - 用户名密码、手机号验证码（可扩展邮箱、微信等）
- ✅ **用户与登录方式解耦** - 一个用户可绑定多种登录方式
- ✅ **JWT Token** - Access Token + Refresh Token
- ✅ **安全机制** - 密码加密、验证码限流、登录失败锁定

## 技术栈

- Java 17
- Spring Boot 3.2
- Spring Authorization Server 1.3
- Spring Security
- Spring Data JPA
- MySQL 8
- Redis

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
```

### 4. 启动服务

```bash
./mvnw spring-boot:run
```

服务启动后访问：
- 授权端点：`http://localhost:9000/oauth2/authorize`
- Token 端点：`http://localhost:9000/oauth2/token`
- OIDC 配置：`http://localhost:9000/.well-known/openid-configuration`

## API 接口

### 1. 用户名密码登录（OAuth2 Password Grant）

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "username=your_username" \
  -d "password=your_password" \
  -d "client_id=web-app" \
  -d "client_secret=your_client_secret" \
  -d "scope=openid read"
```

### 2. 手机号验证码登录

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

### 3. 刷新 Token

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=your_refresh_token" \
  -d "client_id=web-app" \
  -d "client_secret=your_client_secret"
```

### 4. 绑定手机号（需登录）

```bash
curl -X POST http://localhost:9000/api/user/login-methods/mobile/bind \
  -H "Authorization: Bearer your_access_token" \
  -H "Content-Type: application/json" \
  -d '{"mobile": "13800138000", "code": "123456"}'
```

## 项目结构

```
osys-auth-server/
├── src/
│   ├── main/
│   │   ├── java/com/osys/auth/
│   │   │   ├── config/           # 配置类
│   │   │   ├── controller/       # 控制器
│   │   │   ├── service/          # 业务层
│   │   │   ├── repository/       # 数据访问
│   │   │   ├── entity/           # 实体类
│   │   │   ├── dto/              # 数据传输对象
│   │   │   └── security/         # 安全配置
│   │   └── resources/
│   │       ├── application.yml   # 应用配置
│   │       └── schema.sql        # 数据库脚本
│   └── test/                     # 测试代码
├── pom.xml                       # Maven 配置
├── Dockerfile                    # Docker 构建
└── docker-compose.yml            # 一键启动
```

## 数据库设计

### 核心表

| 表名 | 说明 |
|------|------|
| `users` | 用户主表 |
| `accounts` | 账号表（用户认证方式，支持多种） |
| `sms_codes` | 短信验证码表 |
| `login_logs` | 登录日志表 |
| `oauth2_registered_client` | OAuth2 客户端注册表 |
| `oauth2_authorization` | OAuth2 授权记录表 |

## 数据模型关系

```
users (1) ←──────→ (N) accounts
 用户主表            账号表（多种认证方式）
   │                      │
   │                      ├── USERNAME (用户名密码)
   │                      ├── MOBILE (手机号验证码)  
   │                      ├── EMAIL (邮箱验证码)
   │                      ├── WECHAT (微信)
   │                      └── ... (可扩展)
   │
   └── login_logs (N)
        登录日志
```

## 扩展登录方式

实现 `AuthenticationHandler` 接口即可添加新的登录方式：

```java
@Component
public class WechatAuthHandler implements AuthenticationHandler {
    @Override
    public String getLoginType() {
        return "WECHAT";
    }
    
    @Override
    public Authentication authenticate(LoginRequest request) {
        // 实现微信登录逻辑
    }
}
```

## 配置说明

### 密码策略

```yaml
auth:
  password:
    min-length: 8
    require-uppercase: true
    require-lowercase: true
    require-digit: true
```

### 短信验证码

```yaml
auth:
  sms:
    code-length: 6
    expire-seconds: 300
    send-interval: 60
    max-daily-count: 10
```

### Token 有效期

```yaml
auth:
  jwt:
    access-token-validity: 7200   # 2小时
    refresh-token-validity: 604800  # 7天
```

## Docker 部署

```bash
# 构建镜像
docker build -t osys-auth-server .

# 运行
docker run -p 9000:9000 \
  -e MYSQL_HOST=mysql \
  -e REDIS_HOST=redis \
  osys-auth-server

# 或一键启动
docker-compose up -d
```

## 开发计划

- [x] 基础 OAuth2 认证
- [x] 用户名密码登录
- [x] 手机号验证码登录
- [x] 多登录方式绑定
- [ ] 邮箱验证码登录
- [ ] 微信扫码登录
- [ ] GitHub 登录
- [ ] 多因素认证 (MFA)
- [ ] 用户管理后台

## 许可证

MIT

## 贡献

欢迎 Issue 和 PR！
