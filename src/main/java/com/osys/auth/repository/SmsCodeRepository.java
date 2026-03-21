package com.osys.auth.repository;

import com.osys.auth.entity.SmsCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 短信验证码 Repository
 */
@Repository
public interface SmsCodeRepository extends JpaRepository<SmsCode, Long> {

    /**
     * 查询最新未使用的验证码
     */
    Optional<SmsCode> findTopByMobileAndTypeAndUsedFalseOrderByCreatedAtDesc(
            String mobile, SmsCode.SmsType type);

    /**
     * 统计指定手机号在时间段内发送的验证码数量
     */
    long countByMobileAndCreatedAtAfter(String mobile, LocalDateTime after);

    /**
     * 删除过期验证码（可由定时任务调用）
     */
    void deleteByExpireAtBefore(LocalDateTime time);
}
