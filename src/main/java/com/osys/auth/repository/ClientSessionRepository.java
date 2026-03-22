package com.osys.auth.repository;

import com.osys.auth.entity.ClientSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 客户端会话 Repository
 */
@Repository
public interface ClientSessionRepository extends JpaRepository<ClientSession, Long> {

    /**
     * 根据 SSO Session ID 查询
     */
    List<ClientSession> findBySsoSessionId(String ssoSessionId);

    /**
     * 查询用户在指定客户端的会话
     */
    Optional<ClientSession> findByClientIdAndUserIdAndStatus(String clientId, Long userId, Integer status);

    /**
     * 查询用户的所有有效会话
     */
    List<ClientSession> findByUserIdAndStatus(Long userId, Integer status);

    /**
     * 查询过期的会话
     */
    List<ClientSession> findByExpiresAtBeforeAndStatus(LocalDateTime time, Integer status);

    /**
     * 标记为已登出
     */
    @Modifying
    @Query("UPDATE ClientSession c SET c.status = 0 WHERE c.ssoSessionId = :ssoSessionId")
    void logoutBySsoSessionId(@Param("ssoSessionId") String ssoSessionId);

    /**
     * 删除过期的会话
     */
    @Modifying
    @Query("DELETE FROM ClientSession c WHERE c.expiresAt < :time")
    void deleteExpiredSessions(@Param("time") LocalDateTime time);

    /**
     * 统计用户的活跃客户端会话数
     */
    long countByUserIdAndStatus(Long userId, Integer status);
}
