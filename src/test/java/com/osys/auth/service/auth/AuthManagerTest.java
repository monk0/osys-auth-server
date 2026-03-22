package com.osys.auth.service.auth;

import com.osys.auth.dto.AuthCredentials;
import com.osys.auth.dto.AuthResult;
import com.osys.auth.entity.AuthSource;
import com.osys.auth.entity.User;
import com.osys.auth.enums.AuthSourceType;
import com.osys.auth.repository.AuthSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 认证管理器测试
 */
@ExtendWith(MockitoExtension.class)
class AuthManagerTest {

    @Mock
    private AuthSourceRepository authSourceRepository;

    @Mock
    private LocalAuthProvider localAuthProvider;

    @InjectMocks
    private AuthManager authManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode("U123456")
                .nickname("Test User")
                .build();

        // 配置 LocalAuthProvider
        when(localAuthProvider.getSourceCode()).thenReturn("LOCAL");
        when(localAuthProvider.getSourceType()).thenReturn("LOCAL");
        when(localAuthProvider.isEnabled()).thenReturn(true);
    }

    @Test
    void testAuthenticateWithSpecifiedSource() {
        // Given
        AuthCredentials credentials = AuthCredentials.builder()
                .sourceCode("LOCAL")
                .accountType("USERNAME")
                .accountId("testuser")
                .credential("password")
                .build();

        when(localAuthProvider.authenticate(any())).thenReturn(
                AuthResult.success(testUser, "LOCAL"));

        // 初始化
        authManager.init();

        // When
        AuthResult result = authManager.authenticate(credentials);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("U123456", result.getUser().getUserCode());
        verify(localAuthProvider).authenticate(credentials);
    }

    @Test
    void testAuthenticateWithPriority() {
        // Given
        AuthCredentials credentials = AuthCredentials.builder()
                .accountType("USERNAME")
                .accountId("testuser")
                .credential("password")
                .build();

        AuthSource localSource = AuthSource.builder()
                .sourceCode("LOCAL")
                .sourceName("Local")
                .sourceType(AuthSourceType.LOCAL)
                .enabled(true)
                .priority(100)
                .status(1)
                .build();

        when(authSourceRepository.findByEnabledAndStatusOrderByPriorityAsc(true, 1))
                .thenReturn(Collections.singletonList(localSource));
        when(localAuthProvider.supports(any())).thenReturn(true);
        when(localAuthProvider.authenticate(any())).thenReturn(
                AuthResult.success(testUser, "LOCAL"));

        // 初始化
        authManager.init();

        // When
        AuthResult result = authManager.authenticate(credentials);

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    void testAuthenticateProviderNotFound() {
        // Given
        AuthCredentials credentials = AuthCredentials.builder()
                .sourceCode("NONEXISTENT")
                .accountType("USERNAME")
                .accountId("testuser")
                .credential("password")
                .build();

        authManager.init();

        // When
        AuthResult result = authManager.authenticate(credentials);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("PROVIDER_NOT_FOUND", result.getErrorCode());
    }

    @Test
    void testGetLoginUrl() {
        // Given
        when(localAuthProvider.getLoginUrl("http://callback", "state123"))
                .thenReturn("http://login");

        authManager.init();

        // When
        String url = authManager.getLoginUrl("LOCAL", "http://callback", "state123");

        // Then
        assertEquals("http://login", url);
    }

    @Test
    void testGetEnabledProviders() {
        // Given
        AuthSource localSource = AuthSource.builder()
                .sourceCode("LOCAL")
                .enabled(true)
                .priority(100)
                .status(1)
                .build();

        when(authSourceRepository.findByEnabledAndStatusOrderByPriorityAsc(true, 1))
                .thenReturn(Collections.singletonList(localSource));

        authManager.init();

        // When
        List<AuthProvider> providers = authManager.getEnabledProviders();

        // Then
        assertNotNull(providers);
        assertFalse(providers.isEmpty());
    }
}
