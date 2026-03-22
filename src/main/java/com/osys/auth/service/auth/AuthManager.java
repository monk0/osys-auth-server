package com.osys.auth.service.auth;

import com.osys.auth.dto.AuthCredentials;
import com.osys.auth.dto.AuthResult;
import com.osys.auth.entity.AuthSource;
import com.osys.auth.enums.AuthSourceType;
import com.osys.auth.repository.AuthSourceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证管理器
 * 管理所有认证源，按优先级尝试认证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthManager {

    private final AuthSourceRepository authSourceRepository;
    private final List<AuthProvider> authProviders;

    private Map<String, AuthProvider> providerMap = new HashMap<>();
    private List<AuthProvider> sortedProviders = new ArrayList<>();

    @PostConstruct
    public void init() {
        // 初始化认证提供者映射
        for (AuthProvider provider : authProviders) {
            providerMap.put(provider.getSourceCode(), provider);
        }
        sortProviders();
        log.info("AuthManager initialized with {} providers", authProviders.size());
    }

    /**
     * 按优先级排序认证提供者
     */
    private void sortProviders() {
        // 获取数据库中的认证源配置
        List<AuthSource> enabledSources = authSourceRepository
                .findByEnabledAndStatusOrderByPriorityAsc(true, 1);

        // 按优先级排序
        sortedProviders = enabledSources.stream()
                .map(source -> providerMap.get(source.getSourceCode()))
                .filter(Objects::nonNull)
                .filter(AuthProvider::isEnabled)
                .collect(Collectors.toList());

        // 如果没有配置，使用默认的本地认证
        if (sortedProviders.isEmpty()) {
            AuthProvider localProvider = providerMap.get(LocalAuthProvider.SOURCE_CODE);
            if (localProvider != null) {
                sortedProviders.add(localProvider);
            }
        }
    }

    /**
     * 认证
     * 如果指定了 sourceCode，则使用指定认证源
     * 否则按优先级依次尝试所有认证源
     */
    public AuthResult authenticate(AuthCredentials credentials) {
        log.debug("Auth attempt: {}", credentials.getAccountId());

        // 如果指定了认证源，直接使用
        if (credentials.getSourceCode() != null) {
            AuthProvider provider = providerMap.get(credentials.getSourceCode());
            if (provider == null) {
                return AuthResult.failure("PROVIDER_NOT_FOUND", "认证源不存在: " + credentials.getSourceCode());
            }
            if (!provider.isEnabled()) {
                return AuthResult.failure("PROVIDER_DISABLED", "认证源已禁用: " + credentials.getSourceCode());
            }
            return provider.authenticate(credentials);
        }

        // 未指定认证源，按优先级尝试
        for (AuthProvider provider : sortedProviders) {
            try {
                if (provider.supports(credentials)) {
                    log.debug("Trying auth provider: {}", provider.getSourceCode());
                    AuthResult result = provider.authenticate(credentials);
                    if (result.isSuccess()) {
                        log.info("Auth success with provider: {}", provider.getSourceCode());
                        return result;
                    }
                    // 如果不是账号不存在错误，直接返回（比如密码错误）
                    if (!"ACCOUNT_NOT_FOUND".equals(result.getErrorCode())) {
                        return result;
                    }
                }
            } catch (Exception e) {
                log.error("Auth provider {} error", provider.getSourceCode(), e);
            }
        }

        return AuthResult.failure("AUTH_FAILED", "认证失败");
    }

    /**
     * 获取登录 URL（用于外部认证源）
     */
    public String getLoginUrl(String sourceCode, String redirectUri, String state) {
        AuthProvider provider = providerMap.get(sourceCode);
        if (provider == null) {
            return null;
        }
        return provider.getLoginUrl(redirectUri, state);
    }

    /**
     * 处理回调
     */
    public AuthResult handleCallback(String sourceCode, Map<String, String> params) {
        AuthProvider provider = providerMap.get(sourceCode);
        if (provider == null) {
            return AuthResult.failure("PROVIDER_NOT_FOUND", "认证源不存在: " + sourceCode);
        }
        return provider.handleCallback(params);
    }

    /**
     * 获取所有启用的认证源
     */
    public List<AuthProvider> getEnabledProviders() {
        return new ArrayList<>(sortedProviders);
    }

    /**
     * 刷新认证源配置
     */
    public void refreshProviders() {
        log.info("Refreshing auth providers");
        sortProviders();
    }
}
