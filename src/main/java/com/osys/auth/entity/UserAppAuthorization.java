package com.osys.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户应用授权实体
 * 记录用户对各子系统的授权情况，支持"记住授权"功能
 */
@Data
@Entity
@Table(name = "user_app_authorizations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "clientId"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAppAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 应用 Client ID
     */
    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    /**
     * 已授权的权限范围
     */
    @Column(name = "scopes", nullable = false, length = 500)
    private String scopes;

    /**
     * 授权时间
     */
    @CreationTimestamp
    @Column(name = "authorized_at", updatable = false)
    private LocalDateTime authorizedAt;

    /**
     * 是否自动授权（跳过确认页）
     * 0-每次确认, 1-自动授权
     */
    @Column(name = "auto_authorized", nullable = false)
    private Boolean autoAuthorized = false;

    /**
     * 检查是否已授权指定 scope
     */
    public boolean hasScope(String scope) {
        if (scopes == null || scope == null) {
            return false;
        }
        return scopes.contains(scope);
    }
}
