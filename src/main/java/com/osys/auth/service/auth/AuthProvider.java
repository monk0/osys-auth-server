package com.osys.auth.service.auth;

import com.osys.auth.dto.AuthCredentials;
import com.osys.auth.dto.AuthResult;

import java.util.Map;

/**
 * 认证源接口
 * 所有认证方式（本地、LDAP、OIDC等）都实现此接口
 */
public interface AuthProvider {

    /**
     * 获取认证源编码
     */
    String getSourceCode();

    /**
     * 获取认证源类型
     */
    String getSourceType();

    /**
     * 是否启用
     */
    boolean isEnabled();

    /**
     * 认证
     *
     * @param credentials 凭证
     * @return 认证结果
     */
    AuthResult authenticate(AuthCredentials credentials);

    /**
     * 获取登录 URL（用于重定向到外部 IdP）
     *
     * @param redirectUri 回调地址
     * @param state       状态参数
     * @return 登录 URL
     */
    String getLoginUrl(String redirectUri, String state);

    /**
     * 处理回调
     *
     * @param params 回调参数
     * @return 认证结果
     */
    AuthResult handleCallback(Map<String, String> params);

    /**
     * 是否支持此类凭证
     */
    boolean supports(AuthCredentials credentials);

    /**
     * 同步用户信息（可选）
     *
     * @param externalId 外部用户ID
     */
    void syncUserInfo(String externalId);
}
