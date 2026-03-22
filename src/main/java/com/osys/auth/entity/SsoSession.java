package com.osys.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * SSO 中央会话实体
 * 管理用户在中央认证服务器的全局登录状态
 */
@Data
@Entity
@Table(name = "sso_sessions",
       indexes = {
           @Index(name = "idx_session_id", columnList = "sessionId"),
           @Index(name = "idx_user_id", columnList = "userId"),
           @Index(name = "idx_expires_at", columnList = "expiresAt")
       })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SsoSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SSO Session ID（对外暴露的会话标识）
     */
    @Column(name = "session_id", unique = true, nullable = false, length = 64)
    private String sessionId;

    /**
     * 关联用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 最后访问时间
     */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    /**
     * 过期时间
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * IP 地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User Agent
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 设备指纹
     */
    @Column(name = "device_fingerprint", length = 64)
    private String deviceFingerprint;

    /**
     * 状态：0-已失效, 1-有效
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /**
     * 检查会话是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 更新最后访问时间
     */
    public void touch() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 使会话失效
     */
    public void invalidate() {
        this.status = 0;
    }
}
