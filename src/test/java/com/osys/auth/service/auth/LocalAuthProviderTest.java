package com.osys.auth.service.auth;

import com.osys.auth.dto.AuthCredentials;
import com.osys.auth.dto.AuthResult;
import com.osys.auth.entity.Account;
import com.osys.auth.entity.User;
import com.osys.auth.repository.AccountRepository;
import com.osys.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 本地认证提供者测试
 */
@ExtendWith(MockitoExtension.class)
class LocalAuthProviderTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LocalAuthProvider authProvider;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode("U123456")
                .nickname("Test User")
                .status(1)
                .build();

        testAccount = Account.builder()
                .id(1L)
                .userId(1L)
                .sourceCode("LOCAL")
                .accountType(Account.AccountType.USERNAME)
                .accountId("testuser")
                .credential("encoded_password")
                .status(1)
                .build();
    }

    @Test
    void testAuthenticateSuccess() {
        // Given
        AuthCredentials credentials = new AuthCredentials("USERNAME", "testuser", "password");
        
        when(accountRepository.findByAccountTypeAndAccountId(
                Account.AccountType.USERNAME, "testuser"))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("password", "encoded_password"))
                .thenReturn(true);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        // When
        AuthResult result = authProvider.authenticate(credentials);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getUser());
        assertEquals("U123456", result.getUser().getUserCode());
        assertEquals("LOCAL", result.getSourceCode());
    }

    @Test
    void testAuthenticateAccountNotFound() {
        // Given
        AuthCredentials credentials = new AuthCredentials("USERNAME", "nonexistent", "password");
        
        when(accountRepository.findByAccountTypeAndAccountId(
                Account.AccountType.USERNAME, "nonexistent"))
                .thenReturn(Optional.empty());

        // When
        AuthResult result = authProvider.authenticate(credentials);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("ACCOUNT_NOT_FOUND", result.getErrorCode());
    }

    @Test
    void testAuthenticateInvalidPassword() {
        // Given
        AuthCredentials credentials = new AuthCredentials("USERNAME", "testuser", "wrongpassword");
        
        when(accountRepository.findByAccountTypeAndAccountId(
                Account.AccountType.USERNAME, "testuser"))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("wrongpassword", "encoded_password"))
                .thenReturn(false);

        // When
        AuthResult result = authProvider.authenticate(credentials);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("INVALID_CREDENTIAL", result.getErrorCode());
    }

    @Test
    void testAuthenticateAccountDisabled() {
        // Given
        testAccount.setStatus(0);
        AuthCredentials credentials = new AuthCredentials("USERNAME", "testuser", "password");
        
        when(accountRepository.findByAccountTypeAndAccountId(
                Account.AccountType.USERNAME, "testuser"))
                .thenReturn(Optional.of(testAccount));

        // When
        AuthResult result = authProvider.authenticate(credentials);

        // Then
        assertFalse(result.isSuccess());
        assertEquals("ACCOUNT_DISABLED", result.getErrorCode());
    }

    @Test
    void testSupports() {
        assertTrue(authProvider.supports(
                new AuthCredentials("USERNAME", "test", "pass")));
        assertTrue(authProvider.supports(
                new AuthCredentials("MOBILE", "13800138000", "code")));
        assertFalse(authProvider.supports(
                new AuthCredentials("WECHAT", "openid", "token")));
        assertFalse(authProvider.supports(
                new AuthCredentials(null, "test", "pass")));
    }
}
