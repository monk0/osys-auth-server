package com.osys.auth.repository;

import com.osys.auth.entity.SsoSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SSO 会话 Repository
 */
@Repository
public interface SsoSessionRepository extends JpaRepository<SsoSession, Long> {

    /**
     * 根据 Session ID 查询
     */
    Optional<SsoSession> findBySessionId(String sessionId);

    /**
     * 查询用户的有效会话
     */
    List<SsoSession> findByUserIdAndStatus(Long userId, Integer status);

    /**
     * 查询过期的会话
     */
    List<SsoSession> findByExpiresAtBeforeAndStatus(LocalDateTime time, Integer status);

    /**
     * 使会话失效
     */
    @Modifying
    @Query("UPDATE SsoSession s SET s.status = 0 WHERE s.sessionId = :sessionId")
    void invalidateBySessionId(@Param("sessionId") String sessionId);

    /**
     * 删除过期的会话
     */
    @Modifying
    @Query("DELETE FROM SsoSession s WHERE s.expiresAt < :time")
    void deleteExpiredSessions(@Param("time") LocalDateTime time);

    /**
     * 统计用户的活跃会话数
     */
    long countByUserIdAndStatus(Long userId, Integer status);
}
