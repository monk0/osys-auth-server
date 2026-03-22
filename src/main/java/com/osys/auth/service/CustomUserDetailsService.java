package com.osys.auth.service;

import com.osys.auth.entity.Account;
import com.osys.auth.entity.User;
import com.osys.auth.repository.AccountRepository;
import com.osys.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 自定义 UserDetailsService
 * 支持多种账号类型（用户名、手机号等）登录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /**
     * 根据账号标识加载用户（Spring Security 标准方法）
     * 格式: "USERNAME:zhangsan" 或 "MOBILE:13800138000"
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 解析账号类型和标识
        String[] parts = username.split(":", 2);
        if (parts.length != 2) {
            throw new UsernameNotFoundException("账号格式错误: " + username);
        }

        Account.AccountType accountType = Account.AccountType.valueOf(parts[0]);
        String accountId = parts[1];

        return loadUserByAccount(accountType, accountId);
    }

    /**
     * 根据账号类型和标识加载用户
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByAccount(Account.AccountType accountType, String accountId) {
        // 查找账号
        Account account = accountRepository
            .findByAccountTypeAndAccountId(accountType, accountId)
            .orElseThrow(() -> new UsernameNotFoundException(
                "账号不存在: " + accountType + "/" + accountId));

        if (account.getStatus() != 1) {
            throw new UsernameNotFoundException("账号已禁用");
        }

        // 查找用户
        User user = userRepository.findById(account.getUserId())
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        if (user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已禁用");
        }

        // 构建 UserDetails
        return org.springframework.security.core.userdetails.User.builder()
            .username(account.getAccountId())  // 使用账号标识作为用户名
            .password(account.getCredential() != null ? account.getCredential() : "")
            .authorities(Collections.emptyList())  // 权限可从其他表加载
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
    }

    /**
     * 根据用户ID获取用户信息（用于 OIDC UserInfo）
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 获取用户的所有账号
     */
    @Transactional(readOnly = true)
    public List<Account> findUserAccounts(Long userId) {
        return accountRepository.findByUserId(userId);
    }
}
