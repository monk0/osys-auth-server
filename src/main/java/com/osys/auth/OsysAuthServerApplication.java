package com.osys.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 统一认证服务启动类
 * 基于 Spring Authorization Server 实现 OAuth2/OIDC 协议
 * 支持多种登录方式：用户名密码、手机号验证码等
 */
@SpringBootApplication
public class OsysAuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OsysAuthServerApplication.class, args);
    }
}
