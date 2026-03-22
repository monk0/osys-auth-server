package com.osys.auth.repository;

import com.osys.auth.entity.AuthSource;
import com.osys.auth.enums.AuthSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 认证源 Repository
 */
@Repository
public interface AuthSourceRepository extends JpaRepository<AuthSource, Long> {

    /**
     * 根据编码查询
     */
    Optional<AuthSource> findBySourceCode(String sourceCode);

    /**
     * 查询所有启用的认证源，按优先级排序
     */
    List<AuthSource> findByEnabledAndStatusOrderByPriorityAsc(Boolean enabled, Integer status);

    /**
     * 根据类型查询
     */
    List<AuthSource> findBySourceType(AuthSourceType sourceType);

    /**
     * 检查编码是否存在
     */
    boolean existsBySourceCode(String sourceCode);

    /**
     * 查询可用的外部认证源（非本地）
     */
    List<AuthSource> findBySourceTypeNotAndEnabledAndStatusOrderByPriorityAsc(
            AuthSourceType excludeType, Boolean enabled, Integer status);
}
