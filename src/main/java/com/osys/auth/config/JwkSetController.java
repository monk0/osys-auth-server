package com.osys.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWK Set 端点
 * 提供公钥用于验证 JWT 签名
 * 
 * 注意：Spring Authorization Server 已自动提供 /oauth2/jwks
 * 这个控制器用于额外的密钥管理或文档
 */
@RestController
@RequiredArgsConstructor
public class JwkSetController {

    private final JWKSet jwkSet;

    /**
     * 获取 JWK Set（公钥）
     * 客户端使用这些公钥验证 ID Token 和 Access Token 的签名
     * 
     * 实际端点由 Spring Authorization Server 在 /oauth2/jwks 提供
     * 这里作为备用或扩展
     */
    @GetMapping("/oauth2/jwks.json")
    public Map<String, Object> getJwkSet() {
        return jwkSet.toJSONObject();
    }
}
