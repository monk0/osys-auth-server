package com.osys.auth.repository;

import com.osys.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户编码查询
     */
    Optional<User> findByUserCode(String userCode);

    /**
     * 检查用户编码是否存在
     */
    boolean existsByUserCode(String userCode);
}
