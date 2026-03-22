package com.osys.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2/OIDC 授权服务器配置
 */
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    /**
     * 授权服务器安全过滤器链
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            // 启用 OIDC
            .oidc(oidc -> oidc
                // 自定义 UserInfo Mapper（可选）
                .userInfoEndpoint(userInfo -> userInfo
                    // .userInfoMapper(new CustomOidcUserInfoMapper())
                )
            );

        // 异常处理
        http.exceptionHandling((exceptions) -> exceptions
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
            )
        );

        // 跨域支持（开发环境）
        http.cors(Customizer.withDefaults());

        return http.build();
    }

    /**
     * 默认安全过滤器链（用于登录页等）
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests((authorize) -> authorize
                // 公开端点
                .requestMatchers(
                    "/login", 
                    "/error", 
                    "/api/auth/**",
                    "/.well-known/**",
                    "/oauth2/jwks"
                ).permitAll()
                // 其他需要认证
                .anyRequest().authenticated()
            )
            // 表单登录
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", false)
            );

        // 禁用 CSRF（开发环境，生产环境需要配置）
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT 密钥对
     */
    @Bean
    public JWKSet jwkSet() {
        RSAKey rsaKey = generateRsaKey();
        return new JWKSet(rsaKey);
    }

    private static RSAKey generateRsaKey() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成 RSA 密钥", ex);
        }
    }

    /**
     * JWT 编码器（用于签名 ID Token 和 Access Token）
     */
    @Bean
    public NimbusJwtEncoder jwtEncoder(JWKSet jwkSet) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(jwkSet));
    }

    /**
     * JWT 解码器（用于验证 Token）
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSet jwkSet) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSet);
    }

    /**
     * Token 自定义（添加自定义 claims 到 ID Token）
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            if (context.getTokenType().getValue().equals("id_token")) {
                // ID Token 自定义 claims
                context.getClaims()
                    .claim("custom_claim", "value")
                    // 添加用户其他信息
                    .claim("user_code", "U12345");  // 实际应从 UserDetails 获取
            }
            
            if (context.getTokenType().getValue().equals("access_token")) {
                // Access Token 自定义 claims
                context.getClaims()
                    .claim("client_id", context.getRegisteredClient().getClientId());
            }
        };
    }

    /**
     * 授权服务器设置
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            // OIDC Discovery Endpoint
            .issuer("http://localhost:9000")
            // 授权端点
            .authorizationEndpoint("/oauth2/authorize")
            // Token 端点
            .tokenEndpoint("/oauth2/token")
            // OIDC UserInfo 端点
            .oidcUserInfoEndpoint("/userinfo")
            // OIDC Client Registration 端点
            .oidcClientRegistrationEndpoint("/connect/register")
            // Token 撤销端点
            .tokenRevocationEndpoint("/oauth2/revoke")
            // Token Introspection 端点
            .tokenIntrospectionEndpoint("/oauth2/introspect")
            // JWK Set 端点（公钥）
            .jwkSetEndpoint("/oauth2/jwks")
            // OIDC Logout 端点
            .oidcLogoutEndpoint("/logout")
            .build();
    }

    /**
     * 内存中的客户端注册（实际应使用数据库存储）
     * 示例：Web Portal 客户端
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // Web Portal 客户端
        RegisteredClient webPortalClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("osys-web")
            .clientSecret("{noop}secret")  // 生产环境使用加密
            .clientIdIssuedAt(java.time.Instant.now())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.PASSWORD)  // 密码模式（用于短信登录）
            // 回调地址
            .redirectUri("http://localhost:3000/auth/callback")
            .redirectUri("http://localhost:8080/login/oauth2/code/osys-web")
            .redirectUri("https://web.osys.local/auth/callback")
            // OIDC Scope
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .scope("read")
            .scope("write")
            // 客户端设置
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)  // 显示授权确认页
                .requireProofKey(false)  // 是否强制 PKCE
                .build())
            // Token 设置
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(2))
                .refreshTokenTimeToLive(Duration.ofDays(7))
                .reuseRefreshTokens(false)
                .idTokenSignatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build())
            .build();

        // Admin 客户端
        RegisteredClient adminClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("osys-admin")
            .clientSecret("{noop}admin-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:3001/auth/callback")
            .redirectUri("https://admin.osys.local/auth/callback")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope("admin")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .refreshTokenTimeToLive(Duration.ofDays(1))
                .build())
            .build();

        // 移动 APP 客户端（使用 PKCE，无 client_secret）
        RegisteredClient mobileClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("osys-mobile")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)  // 公共客户端
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("com.osys.mobile://auth/callback")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .requireProofKey(true)  // 强制 PKCE
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofDays(30))
                .refreshTokenTimeToLive(Duration.ofDays(90))
                .build())
            .build();

        return new InMemoryRegisteredClientRepository(webPortalClient, adminClient, mobileClient);
    }

    /**
     * 授权服务（内存实现，生产环境使用数据库存储）
     */
    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }
}
