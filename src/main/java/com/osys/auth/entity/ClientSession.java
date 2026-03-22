package com.osys.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 客户端登录状态实体
 * 记录用户在各个子系统的登录状态，用于 SSO 和登出管理
 */
@Data
@Entity
@Table(name = "client_sessions",
       indexes = {
           @Index(name = "idx_sso_session", columnList = "ssoSessionId"),
           @Index(name = "idx_client_user", columnList = "clientId,userId"),
           @Index(name = "idx_expires_at", columnList = "expiresAt")
       })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的 SSO Session ID
     */
    @Column(name = "sso_session_id", nullable = false, length = 64)
    private String ssoSessionId;

    /**
     * OAuth2 Client ID（子系统标识）
     */
    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Access Token 哈希（用于查找和撤销）
     */
    @Column(name = "access_token_hash", length = 64)
    private String accessTokenHash;

    /**
     * Refresh Token 哈希
     */
    @Column(name = "refresh_token_hash", length = 64)
    private String refreshTokenHash;

    /**
     * 登录时间
     */
    @CreationTimestamp
    @Column(name = "login_at", updatable = false)
    private LocalDateTime loginAt;

    /**
     * Token 过期时间
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 状态：0-已登出, 1-有效
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 标记为已登出
     */
    public void logout() {
        this.status = 0;
    }
}
