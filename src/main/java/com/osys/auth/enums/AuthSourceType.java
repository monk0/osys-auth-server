package com.osys.auth.enums;

/**
 * 认证源类型枚举
 */
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
