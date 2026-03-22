package com.osys.auth.repository;

import com.osys.auth.entity.UserAppAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户应用授权 Repository
 */
@Repository
public interface UserAppAuthorizationRepository extends JpaRepository<UserAppAuthorization, Long> {

    /**
     * 根据用户ID和 Client ID 查询
     */
    Optional<UserAppAuthorization> findByUserIdAndClientId(Long userId, String clientId);

    /**
     * 查询用户的所有授权
     */
    List<UserAppAuthorization> findByUserId(Long userId);

    /**
     * 查询用户已授权指定应用
     */
    boolean existsByUserIdAndClientId(Long userId, String clientId);

    /**
     * 删除授权记录
     */
    void deleteByUserIdAndClientId(Long userId, String clientId);
}
