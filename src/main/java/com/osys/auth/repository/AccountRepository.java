package com.osys.auth.repository;

import com.osys.auth.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 账号 Repository
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * 根据账号类型和标识查询
     */
    Optional<Account> findByAccountTypeAndAccountId(Account.AccountType accountType, String accountId);

    /**
     * 根据用户ID查询所有账号
     */
    List<Account> findByUserId(Long userId);

    /**
     * 根据用户ID和账号类型查询
     */
    Optional<Account> findByUserIdAndAccountType(Long userId, Account.AccountType accountType);

    /**
     * 检查账号是否存在
     */
    boolean existsByAccountTypeAndAccountId(Account.AccountType accountType, String accountId);

    /**
     * 统计用户的账号数量
     */
    long countByUserId(Long userId);
}
