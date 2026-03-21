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
-- 2. 登录方式表
-- ============================================
CREATE TABLE IF NOT EXISTS login_methods (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id         BIGINT NOT NULL COMMENT '关联用户ID',
    login_type      VARCHAR(20) NOT NULL COMMENT '登录类型: USERNAME, MOBILE, EMAIL, WECHAT',
    login_id        VARCHAR(64) NOT NULL COMMENT '登录标识(用户名/手机号等)',
    credential      VARCHAR(255) COMMENT '凭证(密码密文/第三方openid)',
    salt            VARCHAR(32) COMMENT '密码盐值',
    is_primary      TINYINT DEFAULT 0 COMMENT '是否主登录方式: 0-否, 1-是',
    is_verified     TINYINT DEFAULT 0 COMMENT '是否已验证: 0-否, 1-是',
    status          TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_login_type_id (login_type, login_id),
    INDEX idx_user_id (user_id),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录方式表';

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
    login_type      VARCHAR(20) NOT NULL COMMENT '登录方式',
    login_id        VARCHAR(64) COMMENT '登录标识',
    ip_address      VARCHAR(45) COMMENT 'IP地址',
    user_agent      VARCHAR(500) COMMENT 'UA',
    status          TINYINT NOT NULL COMMENT '状态: 0-失败, 1-成功',
    fail_reason     VARCHAR(255) COMMENT '失败原因',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志';

-- ============================================
-- 5. OAuth2 客户端注册表 (Spring Authorization Server)
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
-- 7. 初始化测试客户端
-- ============================================
INSERT INTO oauth2_registered_client (
    id, client_id, client_name, 
    client_authentication_methods, authorization_grant_types, 
    redirect_uris, scopes, client_settings, token_settings
) VALUES (
    'web-app-client',
    'web-app',
    'Web Application',
    'client_secret_basic,client_secret_post',
    'authorization_code,refresh_token,password',
    'http://localhost:8080/login/oauth2/code/web-app',
    'openid,profile,read,write',
    '{"requireAuthorizationConsent":true,"requireProofKey":false}',
    '{"accessTokenTimeToLive":"2h","refreshTokenTimeToLive":"7d","reuseRefreshTokens":false}'
) ON DUPLICATE KEY UPDATE client_name = 'Web Application';
