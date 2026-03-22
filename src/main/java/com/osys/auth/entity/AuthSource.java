package com.osys.auth.entity;

import com.osys.auth.enums.AuthSourceType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

/**
 * 认证源配置实体
 * 支持LDAP/OIDC/企业微信等多种外部认证
 */
@Data
@Entity
@Table(name = "auth_sources",
       indexes = {
           @Index(name = "idx_type", columnList = "sourceType"),
           @Index(name = "idx_enabled_priority", columnList = "enabled,priority")
       })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 认证源编码（唯一标识）
     */
    @Column(name = "source_code", unique = true, nullable = false, length = 50)
    private String sourceCode;

    /**
     * 认证源名称
     */
    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    /**
     * 认证源类型
     */
    @Column(name = "source_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuthSourceType sourceType;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 优先级（越小越优先）
     */
    @Column(name = "priority")
    private Integer priority = 100;

    /**
     * 认证源配置（JSON格式）
     */
    @Column(name = "config_json", columnDefinition = "json")
    private String configJson;

    /**
     * 属性映射配置（JSON格式）
     */
    @Column(name = "attribute_mapping", columnDefinition = "json")
    private String attributeMapping;

    /**
     * 首次登录自动创建用户
     */
    @Column(name = "auto_create_user", nullable = false)
    private Boolean autoCreateUser = true;

    /**
     * 自动创建时默认角色
     */
    @Column(name = "default_role", length = 50)
    private String defaultRole;

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

    /**
     * 检查是否可用
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(enabled) && status == 1;
    }
}
