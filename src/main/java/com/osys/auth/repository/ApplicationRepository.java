package com.osys.auth.repository;

import com.osys.auth.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 应用/子系统 Repository
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /**
     * 根据 Client ID 查询
     */
    Optional<Application> findByClientId(String clientId);

    /**
     * 根据应用编码查询
     */
    Optional<Application> findByAppCode(String appCode);

    /**
     * 查询所有启用的应用
     */
    List<Application> findByStatus(Integer status);

    /**
     * 查询启用了 SSO 的应用
     */
    List<Application> findBySsoEnabledAndStatus(Boolean ssoEnabled, Integer status);

    /**
     * 检查应用编码是否存在
     */
    boolean existsByAppCode(String appCode);

    /**
     * 检查 Client ID 是否存在
     */
    boolean existsByClientId(String clientId);
}
