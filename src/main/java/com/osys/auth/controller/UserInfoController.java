package com.osys.auth.controller;

import com.osys.auth.service.OidcUserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * OIDC UserInfo 端点
 * 提供标准 OIDC /userinfo 接口
 */
@Slf4j
@RestController
@RequestMapping("/userinfo")
@RequiredArgsConstructor
public class UserInfoController {

    private final OidcUserInfoService userInfoService;

    /**
     * OIDC UserInfo 端点
     * 返回当前登录用户的详细信息
     * 
     * 请求头需要包含 Access Token:
     * Authorization: Bearer {access_token}
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> userInfo(Authentication authentication) {
        log.debug("UserInfo request, authentication: {}", authentication);
        
        OidcUserInfo userInfo = userInfoService.loadUserInfo(authentication);
        
        return ResponseEntity.ok(userInfo.getClaims());
    }

    /**
     * POST 方法也支持（某些客户端使用 POST）
     */
    @GetMapping(produces = "application/json")
    public ResponseEntity<Map<String, Object>> userInfoJson(Authentication authentication) {
        return userInfo(authentication);
    }
}
