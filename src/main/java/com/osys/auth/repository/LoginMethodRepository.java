package com.osys.auth.repository;

import com.osys.auth.entity.LoginMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 登录方式 Repository
 */
@Repository
public interface LoginMethodRepository extends JpaRepository<LoginMethod, Long> {

    /**
     * 根据登录类型和标识查询
     */
    Optional<LoginMethod> findByLoginTypeAndLoginId(LoginMethod.LoginType loginType, String loginId);

    /**
     * 根据用户ID查询所有登录方式
     */
    List<LoginMethod> findByUserId(Long userId);

    /**
     * 根据用户ID和登录类型查询
     */
    Optional<LoginMethod> findByUserIdAndLoginType(Long userId, LoginMethod.LoginType loginType);

    /**
     * 检查登录方式是否存在
     */
    boolean existsByLoginTypeAndLoginId(LoginMethod.LoginType loginType, String loginId);

    /**
     * 统计用户的登录方式数量
     */
    long countByUserId(Long userId);
}
