package com.osys.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 认证凭证
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthCredentials {

    /**
     * 认证源编码（可选，如不指定则依次尝试所有认证源）
     */
    private String sourceCode;

    /**
     * 账号类型
     */
    private String accountType;

    /**
     * 账号标识（用户名、手机号、邮箱等）
     */
    private String accountId;

    /**
     * 凭证（密码、验证码、Token等）
     */
    private String credential;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;

    /**
     * IP 地址
     */
    private String ipAddress;

    /**
     * User Agent
     */
    private String userAgent;

    public AuthCredentials(String accountType, String accountId, String credential) {
        this.accountType = accountType;
        this.accountId = accountId;
        this.credential = credential;
    }
}
