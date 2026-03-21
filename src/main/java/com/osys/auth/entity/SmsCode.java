package com.osys.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 短信验证码实体
 */
@Data
@Entity
@Table(name = "sms_codes", 
       indexes = {
           @Index(name = "idx_mobile_type", columnList = "mobile,type"),
           @Index(name = "idx_expire_at", columnList = "expireAt")
       })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 手机号
     */
    @Column(name = "mobile", nullable = false, length = 20)
    private String mobile;

    /**
     * 验证码
     */
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    /**
     * 类型：LOGIN(登录), BIND(绑定), RESET_PASSWORD(重置密码)
     */
    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SmsType type;

    /**
     * 过期时间
     */
    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    /**
     * 是否已使用
     */
    @Column(name = "used", nullable = false)
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 检查验证码是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireAt);
    }

    /**
     * 标记为已使用
     */
    public void markAsUsed() {
        this.used = true;
    }

    public enum SmsType {
        LOGIN,          // 登录
        BIND,           // 绑定手机号
        RESET_PASSWORD  // 重置密码
    }
}
