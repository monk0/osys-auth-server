-- 统一认证服务数据库初始化脚本
-- 数据库: osys_auth
-- 编码: utf8mb4

-- 创建数据库
CREATE DATABASE IF NOT EXISTS osys_auth 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE osys_auth;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    user_code       VARCHAR(32) UNIQUE NOT NULL COMMENT '用户唯一编码(对外展示)',
    nickname        VARCHAR(64) COMMENT '昵称',
    avatar          VARCHAR(255) COMMENT '头像URL',
    status          TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_code (user_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主表';

-- ============================================
-- 2. 账号表 (accounts) - 存储用户认证方式
-- ============================================
CREATE TABLE IF NOT EXISTS accounts (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id         BIGINT NOT NULL COMMENT '关联用户ID',
    source_code     VARCHAR(50) DEFAULT 'LOCAL' COMMENT '认证源编码: LOCAL/LDAP/OIDC/...',
    account_type    VARCHAR(20) NOT NULL COMMENT '账号类型: USERNAME, MOBILE, EMAIL, WECHAT',
    account_id      VARCHAR(64) NOT NULL COMMENT '账号标识(用户名/手机号等)',
    external_id     VARCHAR(255) COMMENT '外部系统用户ID(用于外部认证源)',
    credential      VARCHAR(255) COMMENT '凭证(密码密文/第三方openid)',
    salt            VARCHAR(32) COMMENT '密码盐值',
    is_primary      TINYINT DEFAULT 0 COMMENT '是否主账号: 0-否, 1-是',
    is_verified     TINYINT DEFAULT 0 COMMENT '是否已验证: 0-否, 1-是',
    status          TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_account_type_id (account_type, account_id),
    INDEX idx_user_id (user_id),
    INDEX idx_source_code (source_code),
    INDEX idx_external_id (source_code, external_id),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号表(多种认证方式)';

-- ============================================
-- 3. 短信验证码表
-- ============================================
CREATE TABLE IF NOT EXISTS sms_codes (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    mobile          VARCHAR(20) NOT NULL COMMENT '手机号',
    code            VARCHAR(10) NOT NULL COMMENT '验证码',
    type            VARCHAR(20) NOT NULL COMMENT '类型: LOGIN, BIND, RESET_PASSWORD',
    expire_at       DATETIME NOT NULL COMMENT '过期时间',
    used            TINYINT DEFAULT 0 COMMENT '是否已使用: 0-否, 1-是',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_mobile_type (mobile, type),
    INDEX idx_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信验证码表';

-- ============================================
-- 4. 登录日志表
-- ============================================
CREATE TABLE IF NOT EXISTS login_logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id         BIGINT COMMENT '用户ID(登录成功时)',
    account_type    VARCHAR(20) NOT NULL COMMENT '账号类型',
    account_id      VARCHAR(64) COMMENT '账号标识',
    ip_address      VARCHAR(45) COMMENT 'IP地址',
    user_agent      VARCHAR(500) COMMENT 'UA',
    status          TINYINT NOT NULL COMMENT '状态: 0-失败, 1-成功',
    fail_reason     VARCHAR(255) COMMENT '失败原因',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志';

-- ============================================
-- 5. 认证源配置表 (auth_sources)
-- ============================================
CREATE TABLE IF NOT EXISTS auth_sources (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    source_code         VARCHAR(50) UNIQUE NOT NULL COMMENT '认证源编码',
    source_name         VARCHAR(100) NOT NULL COMMENT '认证源名称',
    source_type         VARCHAR(20) NOT NULL COMMENT '类型: LOCAL/LDAP/OIDC/OAUTH2/SAML/CAS/WECHAT_WORK/DINGTALK/FEISHU/CUSTOM',
    
    enabled             TINYINT DEFAULT 1 COMMENT '是否启用: 0-否, 1-是',
    priority            INT DEFAULT 100 COMMENT '优先级（越小越优先）',
    
    config_json         JSON COMMENT '认证源配置(JSON)',
    attribute_mapping   JSON COMMENT '属性映射配置',
    
    auto_create_user    TINYINT DEFAULT 1 COMMENT '首次登录自动创建用户: 0-否, 1-是',
    default_role        VARCHAR(50) COMMENT '自动创建时默认角色',
    
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_type (source_type),
    INDEX idx_enabled_priority (enabled, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认证源配置表（支持LDAP/OIDC/企业微信等外部认证）';

-- ============================================
-- 6. 应用/子系统注册表 (applications)
-- ============================================
CREATE TABLE IF NOT EXISTS applications (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    client_id           VARCHAR(100) UNIQUE NOT NULL COMMENT 'OAuth2 Client ID',
    app_name            VARCHAR(100) NOT NULL COMMENT '应用名称',
    app_code            VARCHAR(50) UNIQUE NOT NULL COMMENT '应用编码(系统标识)',
    app_type            VARCHAR(20) NOT NULL COMMENT '类型: WEB, APP, SERVICE',
    description         VARCHAR(500) COMMENT '应用描述',
    
    homepage_url        VARCHAR(255) COMMENT '首页URL',
    logo_url            VARCHAR(255) COMMENT 'Logo',
    
    sso_enabled         TINYINT DEFAULT 1 COMMENT '是否启用SSO: 0-否, 1-是',
    sso_session_timeout INT DEFAULT 7200 COMMENT 'SSO会话超时(秒)',
    
    required_scopes     VARCHAR(500) COMMENT '必需权限范围',
    auto_grant_scopes   VARCHAR(500) COMMENT '自动授权的权限',
    
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_app_code (app_code),
    INDEX idx_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用/子系统注册表';

-- ============================================
-- 7. SSO 中央会话表 (sso_sessions)
-- ============================================
CREATE TABLE IF NOT EXISTS sso_sessions (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    session_id          VARCHAR(64) UNIQUE NOT NULL COMMENT 'SSO Session ID',
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_accessed_at    DATETIME COMMENT '最后访问时间',
    expires_at          DATETIME NOT NULL COMMENT '过期时间',
    
    ip_address          VARCHAR(45) COMMENT 'IP地址',
    user_agent          VARCHAR(500) COMMENT 'UA',
    device_fingerprint  VARCHAR(64) COMMENT '设备指纹',
    
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-已失效, 1-有效',
    
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SSO中央会话表';

-- ============================================
-- 8. 客户端登录状态表 (client_sessions)
-- ============================================
CREATE TABLE IF NOT EXISTS client_sessions (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    sso_session_id      VARCHAR(64) NOT NULL COMMENT '关联SSO Session',
    client_id           VARCHAR(100) NOT NULL COMMENT 'OAuth2 Client ID',
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    
    access_token_hash   VARCHAR(64) COMMENT 'Access Token哈希',
    refresh_token_hash  VARCHAR(64) COMMENT 'Refresh Token哈希',
    
    login_at            DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    expires_at          DATETIME NOT NULL COMMENT 'Token过期时间',
    
    status              TINYINT DEFAULT 1 COMMENT '状态: 0-已登出, 1-有效',
    
    INDEX idx_sso_session (sso_session_id),
    INDEX idx_client_user (client_id, user_id),
    INDEX idx_expires_at (expires_at),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户端登录状态表';

-- ============================================
-- 9. 用户应用授权表 (user_app_authorizations)
-- ============================================
CREATE TABLE IF NOT EXISTS user_app_authorizations (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    client_id           VARCHAR(100) NOT NULL COMMENT '应用Client ID',
    
    scopes              VARCHAR(500) NOT NULL COMMENT '已授权的权限范围',
    authorized_at       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    auto_authorized     TINYINT DEFAULT 0 COMMENT '0-每次确认, 1-自动授权',
    
    UNIQUE KEY uk_user_client (user_id, client_id),
    INDEX idx_client_id (client_id),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户应用授权表';

-- ============================================
-- 10. OAuth2 客户端注册表 (Spring Authorization Server)
-- ============================================
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id varchar(100) NOT NULL PRIMARY KEY COMMENT '客户端ID',
    client_id varchar(100) NOT NULL COMMENT '客户端标识',
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '签发时间',
    client_secret varchar(200) DEFAULT NULL COMMENT '客户端密钥',
    client_secret_expires_at timestamp DEFAULT NULL COMMENT '密钥过期时间',
    client_name varchar(200) NOT NULL COMMENT '客户端名称',
    client_authentication_methods varchar(1000) NOT NULL COMMENT '认证方式',
    authorization_grant_types varchar(1000) NOT NULL COMMENT '授权类型',
    redirect_uris varchar(1000) DEFAULT NULL COMMENT '回调地址',
    post_logout_redirect_uris varchar(1000) DEFAULT NULL COMMENT '登出回调地址',
    scopes varchar(1000) NOT NULL COMMENT '权限范围',
    client_settings varchar(2000) NOT NULL COMMENT '客户端设置',
    token_settings varchar(2000) NOT NULL COMMENT 'Token设置'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth2客户端';

-- ============================================
-- 6. OAuth2 授权记录表
-- ============================================
CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id varchar(100) NOT NULL PRIMARY KEY COMMENT '授权ID',
    registered_client_id varchar(100) NOT NULL COMMENT '客户端ID',
    principal_name varchar(200) NOT NULL COMMENT '用户标识',
    authorization_grant_type varchar(100) NOT NULL COMMENT '授权类型',
    authorized_scopes varchar(1000) DEFAULT NULL COMMENT '授权范围',
    attributes blob DEFAULT NULL COMMENT '属性',
    state varchar(500) DEFAULT NULL COMMENT '状态',
    authorization_code_value blob DEFAULT NULL COMMENT '授权码',
    authorization_code_issued_at timestamp DEFAULT NULL COMMENT '授权码签发时间',
    authorization_code_expires_at timestamp DEFAULT NULL COMMENT '授权码过期时间',
    authorization_code_metadata blob DEFAULT NULL COMMENT '授权码元数据',
    access_token_value blob DEFAULT NULL COMMENT '访问令牌',
    access_token_issued_at timestamp DEFAULT NULL COMMENT '访问令牌签发时间',
    access_token_expires_at timestamp DEFAULT NULL COMMENT '访问令牌过期时间',
    access_token_metadata blob DEFAULT NULL COMMENT '访问令牌元数据',
    access_token_type varchar(100) DEFAULT NULL COMMENT '令牌类型',
    access_token_scopes varchar(1000) DEFAULT NULL COMMENT '令牌范围',
    oidc_id_token_value blob DEFAULT NULL COMMENT 'ID令牌',
    oidc_id_token_issued_at timestamp DEFAULT NULL COMMENT 'ID令牌签发时间',
    oidc_id_token_expires_at timestamp DEFAULT NULL COMMENT 'ID令牌过期时间',
    oidc_id_token_metadata blob DEFAULT NULL COMMENT 'ID令牌元数据',
    refresh_token_value blob DEFAULT NULL COMMENT '刷新令牌',
    refresh_token_issued_at timestamp DEFAULT NULL COMMENT '刷新令牌签发时间',
    refresh_token_expires_at timestamp DEFAULT NULL COMMENT '刷新令牌过期时间',
    refresh_token_metadata blob DEFAULT NULL COMMENT '刷新令牌元数据'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth2授权记录';

-- ============================================
-- 10. 初始化测试数据
-- ============================================

-- 初始化子系统应用
INSERT INTO applications (client_id, app_name, app_code, app_type, description, homepage_url, sso_enabled, required_scopes, auto_grant_scopes, status) VALUES
('osys-web', 'OSYS Web Portal', 'WEB_PORTAL', 'WEB', '主站 Web 门户', 'https://web.osys.local', 1, 'openid,profile', 'openid,profile', 1),
('osys-admin', 'OSYS Admin Dashboard', 'ADMIN', 'WEB', '管理后台', 'https://admin.osys.local', 1, 'openid,profile,admin', 'openid,profile', 1),
('osys-api', 'OSYS API Gateway', 'API_GATEWAY', 'SERVICE', 'API 网关服务', NULL, 1, 'openid,profile,service', 'openid,profile', 1),
('osys-mobile', 'OSYS Mobile App', 'MOBILE_APP', 'APP', '移动应用', NULL, 1, 'openid,profile', 'openid,profile', 1)
ON DUPLICATE KEY UPDATE app_name = VALUES(app_name);

-- 初始化 OAuth2 客户端 (与 applications 对应)
INSERT INTO oauth2_registered_client (
    id, client_id, client_name, 
    client_authentication_methods, authorization_grant_types, 
    redirect_uris, scopes, client_settings, token_settings
) VALUES 
-- Web Portal
('osys-web-client', 'osys-web', 'OSYS Web Portal',
 'client_secret_basic,client_secret_post',
 'authorization_code,refresh_token',
 'https://web.osys.local/auth/callback,http://localhost:3000/auth/callback',
 'openid,profile,read,write',
 '{"requireAuthorizationConsent":true,"requireProofKey":true}',
 '{"accessTokenTimeToLive":"2h","refreshTokenTimeToLive":"7d"}'),
-- Admin
('osys-admin-client', 'osys-admin', 'OSYS Admin Dashboard',
 'client_secret_basic,client_secret_post',
 'authorization_code,refresh_token',
 'https://admin.osys.local/auth/callback,http://localhost:3001/auth/callback',
 'openid,profile,read,write,admin',
 '{"requireAuthorizationConsent":true,"requireProofKey":true}',
 '{"accessTokenTimeToLive":"1h","refreshTokenTimeToLive":"1d"}'),
-- API Gateway
('osys-api-client', 'osys-api', 'OSYS API Gateway',
 'client_secret_basic',
 'client_credentials',
 '',
 'service,read',
 '{"requireAuthorizationConsent":false}',
 '{"accessTokenTimeToLive":"24h"}'),
-- Mobile App
('osys-mobile-client', 'osys-mobile', 'OSYS Mobile App',
 'none',
 'authorization_code',
 'com.osys.mobile://auth/callback',
 'openid,profile,read,write',
 '{"requireAuthorizationConsent":true,"requireProofKey":true}',
 '{"accessTokenTimeToLive":"30d","refreshTokenTimeToLive":"90d"}')
ON DUPLICATE KEY UPDATE client_name = VALUES(client_name);
