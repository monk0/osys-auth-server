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
 * 登录方式实体
 * 支持：USERNAME(用户名密码)、MOBILE(手机号)、EMAIL(邮箱)、WECHAT(微信)等
 */
@Data
@Entity
@Table(name = "login_methods", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"login_type", "login_id"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 登录类型：USERNAME, MOBILE, EMAIL, WECHAT
     */
    @Column(name = "login_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LoginType loginType;

    /**
     * 登录标识（用户名/手机号/邮箱等）
     */
    @Column(name = "login_id", nullable = false, length = 64)
    private String loginId;

    /**
     * 凭证（密码密文/第三方openid）
     * 短信验证码登录此字段可为空
     */
    @Column(name = "credential", length = 255)
    private String credential;

    /**
     * 密码盐值
     */
    @Column(name = "salt", length = 32)
    private String salt;

    /**
     * 是否主登录方式
     */
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    /**
     * 是否已验证
     */
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    /**
     * 状态：0-禁用, 1-正常
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
     * 登录类型枚举
     */
    public enum LoginType {
        USERNAME,   // 用户名密码
        MOBILE,     // 手机号验证码
        EMAIL,      // 邮箱验证码
        WECHAT,     // 微信
        GITHUB,     // GitHub
        DINGTALK    // 钉钉
    }
}
