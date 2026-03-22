package com.osys.auth.dto;

import com.osys.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 认证结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 用户对象（认证成功时）
     */
    private User user;

    /**
     * 认证源编码
     */
    private String sourceCode;

    /**
     * 外部用户ID（外部认证源时）
     */
    private String externalId;

    /**
     * 用户属性（用于自动创建用户）
     */
    private Map<String, Object> attributes;

    /**
     * 错误信息（认证失败时）
     */
    private String errorMessage;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 是否需要重定向（外部认证源时）
     */
    private boolean requireRedirect;

    /**
     * 重定向URL（外部认证源时）
     */
    private String redirectUrl;

    public static AuthResult success(User user, String sourceCode) {
        return AuthResult.builder()
                .success(true)
                .user(user)
                .sourceCode(sourceCode)
                .build();
    }

    public static AuthResult success(User user, String sourceCode, String externalId, Map<String, Object> attributes) {
        return AuthResult.builder()
                .success(true)
                .user(user)
                .sourceCode(sourceCode)
                .externalId(externalId)
                .attributes(attributes)
                .build();
    }

    public static AuthResult failure(String errorMessage) {
        return AuthResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static AuthResult failure(String errorCode, String errorMessage) {
        return AuthResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    public static AuthResult redirect(String redirectUrl) {
        return AuthResult.builder()
                .success(false)
                .requireRedirect(true)
                .redirectUrl(redirectUrl)
                .build();
    }
}
