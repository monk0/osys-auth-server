package com.osys.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 应用/子系统注册实体
 * 管理接入平台的各个子系统
 */
@Data
@Entity
@Table(name = "applications")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * OAuth2 Client ID
     */
    @Column(name = "client_id", unique = true, nullable = false, length = 100)
    private String clientId;

    /**
     * 应用名称
     */
    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    /**
     * 应用编码（系统标识）
     */
    @Column(name = "app_code", unique = true, nullable = false, length = 50)
    private String appCode;

    /**
     * 应用类型：WEB, APP, SERVICE
     */
    @Column(name = "app_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AppType appType;

    /**
     * 应用描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 首页 URL
     */
    @Column(name = "homepage_url", length = 255)
    private String homepageUrl;

    /**
     * Logo URL
     */
    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    /**
     * 是否启用 SSO
     */
    @Column(name = "sso_enabled", nullable = false)
    private Boolean ssoEnabled = true;

    /**
     * SSO 会话超时（秒）
     */
    @Column(name = "sso_session_timeout")
    private Integer ssoSessionTimeout = 7200;

    /**
     * 必需权限范围
     */
    @Column(name = "required_scopes", length = 500)
    private String requiredScopes;

    /**
     * 自动授权的权限
     */
    @Column(name = "auto_grant_scopes", length = 500)
    private String autoGrantScopes;

    /**
     * 状态：0-禁用, 1-启用
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AppType {
        WEB,      // Web 应用
        APP,      // 移动应用
        SERVICE   // 服务/网关
    }
}
