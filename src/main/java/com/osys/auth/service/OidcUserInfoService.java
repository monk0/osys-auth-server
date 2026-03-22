package com.osys.auth.service;

import com.osys.auth.entity.Account;
import com.osys.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * OIDC UserInfo 服务
 * 提供 /userinfo 端点的用户信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OidcUserInfoService {

    private final CustomUserDetailsService userDetailsService;

    /**
     * 根据认证信息构建 OIDC UserInfo
     */
    public OidcUserInfo loadUserInfo(Authentication authentication) {
        // 从认证信息中获取用户ID（需要在 Token 中包含 sub claim）
        Object principal = authentication.getPrincipal();
        
        if (!(principal instanceof UserDetails)) {
            return OidcUserInfo.builder().build();
        }

        UserDetails userDetails = (UserDetails) principal;
        String username = userDetails.getUsername();
        
        // 解析用户ID（需要从 Token 中获取，这里简化处理）
        // 实际应从 authentication.getTokenAttributes() 获取 sub (user_id)
        
        // 构建 UserInfo
        return buildUserInfo(1L);  // 临时使用固定ID，实际应从 Token 获取
    }

    /**
     * 根据用户ID构建 OIDC UserInfo
     */
    public OidcUserInfo buildUserInfo(Long userId) {
        Optional<User> userOpt = userDetailsService.findById(userId);
        
        if (userOpt.isEmpty()) {
            return OidcUserInfo.builder().build();
        }

        User user = userOpt.get();
        List<Account> accounts = userDetailsService.findUserAccounts(userId);

        // 查找主账号（邮箱或手机号）
        String email = null;
        String phoneNumber = null;
        
        for (Account account : accounts) {
            if (account.getAccountType() == Account.AccountType.EMAIL) {
                email = account.getAccountId();
            } else if (account.getAccountType() == Account.AccountType.MOBILE) {
                phoneNumber = account.getAccountId();
            }
        }

        // 构建 OIDC Standard Claims
        OidcUserInfo.Builder builder = OidcUserInfo.builder()
            // Subject - 用户的唯一标识
            .subject(user.getUserCode())
            // 全名
            .name(user.getNickname())
            // 昵称
            .nickname(user.getNickname())
            // 头像
            .picture(user.getAvatar())
            // 邮箱
            .email(email)
            .emailVerified(email != null)
            // 手机号
            .claim("phone_number", phoneNumber)
            .claim("phone_number_verified", phoneNumber != null);

        // 添加自定义 claims
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("user_id", user.getId());
        customClaims.put("user_code", user.getUserCode());
        customClaims.put("account_types", accounts.stream()
            .map(a -> a.getAccountType().name())
            .toList());
        
        builder.claims(claims -> claims.putAll(customClaims));

        return builder.build();
    }

    /**
     * 根据账号标识构建 UserInfo（简化版）
     */
    public OidcUserInfo buildUserInfoByAccount(String accountId) {
        // 实际实现中需要根据账号查找用户
        return OidcUserInfo.builder()
            .subject(accountId)
            .name("Test User")
            .build();
    }
}
