package com.osys.auth.service.auth;

import com.osys.auth.dto.AuthCredentials;
import com.osys.auth.dto.AuthResult;
import com.osys.auth.entity.Account;
import com.osys.auth.entity.User;
import com.osys.auth.repository.AccountRepository;
import com.osys.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 本地认证提供者
 * 处理用户名密码、手机验证码等本地认证方式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalAuthProvider implements AuthProvider {

    public static final String SOURCE_CODE = "LOCAL";
    public static final String SOURCE_TYPE = "LOCAL";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }

    @Override
    public boolean isEnabled() {
        return true; // 本地认证始终启用
    }

    @Override
    public boolean supports(AuthCredentials credentials) {
        // 支持 USERNAME 和 MOBILE 类型的本地认证
        if (credentials.getAccountType() == null) {
            return false;
        }
        return credentials.getAccountType().equalsIgnoreCase("USERNAME")
                || credentials.getAccountType().equalsIgnoreCase("MOBILE");
    }

    @Override
    public AuthResult authenticate(AuthCredentials credentials) {
        log.debug("Local auth attempt: {}", credentials.getAccountId());

        try {
            Account.AccountType accountType = Account.AccountType.valueOf(
                    credentials.getAccountType().toUpperCase());

            // 查找账号
            Optional<Account> accountOpt = accountRepository
                    .findByAccountTypeAndAccountId(accountType, credentials.getAccountId());

            if (accountOpt.isEmpty()) {
                log.warn("Account not found: {}/{}", accountType, credentials.getAccountId());
                return AuthResult.failure("ACCOUNT_NOT_FOUND", "账号不存在");
            }

            Account account = accountOpt.get();

            // 检查账号状态
            if (account.getStatus() != 1) {
                log.warn("Account disabled: {}/{}", accountType, credentials.getAccountId());
                return AuthResult.failure("ACCOUNT_DISABLED", "账号已禁用");
            }

            // 验证凭证（密码）
            if (account.getCredential() != null &&
                    !passwordEncoder.matches(credentials.getCredential(), account.getCredential())) {
                log.warn("Invalid credential for: {}/{}", accountType, credentials.getAccountId());
                return AuthResult.failure("INVALID_CREDENTIAL", "密码错误");
            }

            // 查找用户
            Optional<User> userOpt = userRepository.findById(account.getUserId());
            if (userOpt.isEmpty()) {
                log.error("User not found for account: {}", account.getId());
                return AuthResult.failure("USER_NOT_FOUND", "用户不存在");
            }

            User user = userOpt.get();
            if (user.getStatus() != 1) {
                return AuthResult.failure("USER_DISABLED", "用户已禁用");
            }

            log.info("Local auth success: {}/{}", accountType, credentials.getAccountId());
            return AuthResult.success(user, SOURCE_CODE);

        } catch (Exception e) {
            log.error("Local auth error", e);
            return AuthResult.failure("AUTH_ERROR", "认证失败: " + e.getMessage());
        }
    }

    @Override
    public String getLoginUrl(String redirectUri, String state) {
        // 本地认证不需要重定向
        return null;
    }

    @Override
    public AuthResult handleCallback(Map<String, String> params) {
        // 本地认证不需要回调处理
        return AuthResult.failure("NOT_SUPPORTED", "本地认证不支持回调");
    }

    @Override
    public void syncUserInfo(String externalId) {
        // 本地认证不需要同步
    }
}
